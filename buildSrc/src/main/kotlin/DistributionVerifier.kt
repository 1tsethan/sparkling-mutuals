package buildlogic

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import net.fabricmc.loader.api.Version
import net.fabricmc.loader.api.metadata.version.VersionPredicate
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.security.MessageDigest
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

data class TargetSpec(val minecraft: String, val fabricApi: String, val hmApi: String)

interface VerifiedAsset {
    val path: Path
    val size: Long
    val sha256: String
}

data class VerifiedJar(
    override val path: Path,
    val minecraft: String,
    val modVersion: String,
    override val size: Long,
    override val sha256: String,
    val edition: String,
) : VerifiedAsset

data class VerifiedFile(override val path: Path, override val size: Long, override val sha256: String) : VerifiedAsset
data class VerifiedDistribution(val jars: List<VerifiedJar>, val manifest: VerifiedFile)

internal const val MANIFEST_NAME = "build-manifest.json"
internal val SUPPORTED_TARGETS = setOf("26.1.2", "26.2", "26.3")
private val editions = listOf("standard", "full", "noauto")
internal val githubEditions = listOf("full", "noauto")

internal fun distributionJarName(edition: String, modVersion: String, minecraft: String): String {
    val prefix = when (edition) {
        "standard" -> "sparkling-mutuals-standard"
        "full" -> "sparkling-mutuals"
        "noauto" -> "sparkling-mutuals-full-noautoupdate"
        else -> throw IllegalArgumentException("Unknown edition: $edition")
    }
    return "$prefix-$modVersion+mc$minecraft.jar"
}

fun collectDistribution(paths: List<Path>, targets: List<TargetSpec>, modVersion: String, destination: Path): List<VerifiedJar> {
    val verified = verifyDistribution(paths, targets, modVersion)
    val dest = destination.toAbsolutePath().normalize()
    require(!Files.isSymbolicLink(dest)) { "Distribution destination must not be a symbolic link" }
    val parent = dest.parent
    Files.createDirectories(parent)
    val stage = Files.createTempDirectory(parent, ".${dest.fileName}-stage-")
    val backup = Files.createTempDirectory(parent, ".${dest.fileName}-backup-")
    Files.delete(backup)
    fun move(from: Path, to: Path) {
        try { Files.move(from, to, StandardCopyOption.ATOMIC_MOVE) }
        catch (_: AtomicMoveNotSupportedException) { Files.move(from, to) }
    }
    fun removeTemporary(path: Path) {
        require(path.toAbsolutePath().normalize().parent == parent && path != dest) { "Unsafe temporary directory" }
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) Files.walk(path).use { stream -> stream.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) } }
    }
    var installed = false
    try {
        verified.forEach { Files.copy(it.path, stage.resolve(it.path.fileName)) }
        // Validate the copied bytes too, before replacing the previous complete directory.
        val staged = verifyDistribution(verified.map { stage.resolve(it.path.fileName) }, targets, modVersion)
        Files.writeString(stage.resolve(MANIFEST_NAME), JsonOutput.prettyPrint(JsonOutput.toJson(mapOf(
            "version" to modVersion, "files" to staged.map(::manifestEntry)))))
        if (Files.exists(dest)) move(dest, backup)
        try { move(stage, dest) }
        catch (failure: Exception) {
            if (Files.exists(backup)) try { move(backup, dest) } catch (rollback: Exception) { failure.addSuppressed(rollback) }
            throw failure
        }
        installed = true
        return staged.map { it.copy(path = dest.resolve(it.path.fileName)) }
    } finally {
        removeTemporary(stage)
        // If rollback failed, retain the backup for recovery.
        if (installed) removeTemporary(backup)
    }
}

private fun manifestEntry(file: VerifiedJar) = mapOf(
    "edition" to file.edition,
    "minecraft" to file.minecraft,
    "name" to file.path.fileName.toString(),
    "size" to file.size,
    "sha256" to file.sha256,
)

fun verifyCollectedDistribution(directory: Path, targets: List<TargetSpec>, modVersion: String): VerifiedDistribution {
    require(Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) { "Missing distribution directory: $directory" }
    val files = Files.list(directory).use { it.toList() }
    val manifest = directory.resolve(MANIFEST_NAME)
    require(files.size == 10 && manifest in files) { "Expected exactly nine JARs and $MANIFEST_NAME" }
    val jars = verifyDistribution(files.filter { it != manifest }, targets, modVersion)

    require(Files.isRegularFile(manifest, LinkOption.NOFOLLOW_LINKS)) { "Missing verified manifest: $manifest" }
    val manifestBytes = Files.newInputStream(manifest).use { it.readNBytes(64 * 1024 + 1) }
    require(manifestBytes.size <= 64 * 1024) { "Distribution manifest exceeded limit" }
    val content = json(manifestBytes)
    require(content.keys == setOf("version", "files") && content["version"] == modVersion) {
        "Distribution manifest version or fields mismatch"
    }
    val entries = content["files"] as? List<*> ?: throw IllegalArgumentException("Missing distribution manifest files")
    val normalized = entries.map { value ->
        val entry = value as? Map<*, *> ?: throw IllegalArgumentException("Invalid distribution manifest entry")
        val size = when (val count = entry["size"]) {
            is Int -> count.toLong()
            is Long -> count
            else -> throw IllegalArgumentException("Invalid size in distribution manifest")
        }
        entry.toMutableMap().apply { put("size", size) }
    }
    require(normalized.size == jars.size && normalized.toSet() == jars.map(::manifestEntry).toSet()) {
        "Distribution manifest entries do not match verified JARs"
    }
    return VerifiedDistribution(jars, VerifiedFile(manifest, manifestBytes.size.toLong(),
        hex(MessageDigest.getInstance("SHA-256").digest(manifestBytes))))
}

private const val MAX_ENTRY = 32 * 1024 * 1024
private fun json(bytes: ByteArray): Map<*, *> =
    (JsonSlurper().parseText(bytes.toString(Charsets.UTF_8)) as? Map<*, *>)
        ?: throw IllegalArgumentException("Expected JSON object")
private fun bytes(input: java.io.InputStream): ByteArray = input.readNBytes(MAX_ENTRY + 1).also {
    require(it.size <= MAX_ENTRY) { "Oversized archive entry" }
}
private fun nested(data: ByteArray): Map<String, ByteArray> {
    val entries = linkedMapOf<String, ByteArray>()
    var total = 0L
    ZipInputStream(ByteArrayInputStream(data)).use { z ->
        while (true) {
            val entry = z.nextEntry ?: break
            if (!entry.isDirectory) {
                require(entry.name !in entries) { "Duplicate nested entry ${entry.name}" }
                val value = bytes(z); total += value.size
                require(total <= 128L * 1024 * 1024) { "Oversized nested archive" }
                entries[entry.name] = value
            }
        }
    }
    return entries
}
private fun accepts(requirement: Any?, version: String): Boolean = when (requirement) {
    is String -> VersionPredicate.parse(requirement).test(Version.parse(version))
    is List<*> -> requirement.any { accepts(it, version) }
    null -> true
    else -> false
}

fun verifyJar(path: Path, target: TargetSpec, modVersion: String, edition: String): VerifiedJar {
    require(path.fileName.toString() == distributionJarName(edition, modVersion, target.minecraft)) { "Unexpected installable filename: $path" }
    require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && Files.size(path) in 1..(64L * 1024 * 1024)) { "Missing or oversized JAR: $path" }
    try {
        ZipFile(path.toFile()).use { z ->
            val names = z.entries().asSequence().map { it.name }.toList()
            require(names.size == names.toSet().size) { "Duplicate archive entries" }
            fun entry(name: String) = z.getEntry(name)?.let { z.getInputStream(it).use(::bytes) }
                ?: throw IllegalArgumentException("Missing runtime resource: $name")
            val metadata = json(entry("fabric.mod.json"))
            require(metadata["id"] == "sparkling-mutuals" && metadata["version"] == modVersion && metadata["environment"] == "client") { "Incorrect mod identity/version/environment" }
            require((metadata["custom"] as? Map<*, *>)?.get("sparkling-mutuals:edition") == edition) { "Incorrect embedded edition: $path" }
            val depends = metadata["depends"] as? Map<*, *> ?: error("Missing dependency metadata")
            require(depends["minecraft"] == target.minecraft && depends["java"] == ">=25" && depends["fabricloader"] == ">=0.19.5") { "Incorrect target/Java/loader metadata" }
            require(depends["fabric-api"] != null && accepts(depends["fabric-api"], target.fabricApi)) { "Incompatible Fabric API" }
            require(depends["fabric-language-kotlin"] != null && accepts(depends["fabric-language-kotlin"], "1.14.1+kotlin.2.4.20")) { "Incompatible Fabric Kotlin" }
            val main = (metadata["entrypoints"] as? Map<*, *>)?.get("main") as? List<*>
            require(main?.any { it is Map<*, *> && it["value"] == "net.johnceo.sparklingmutuals.SparklingMutuals" && it["adapter"] == "kotlin" } == true) { "Missing Kotlin entrypoint" }
            entry("net/johnceo/sparklingmutuals/SparklingMutuals.class")
            entry(metadata["icon"] as? String ?: throw IllegalArgumentException("Missing icon"))
            entry("LICENSE_sparkling-mutuals")
            require(names.any { it.startsWith("licenses/") && !it.endsWith("/") }) { "Missing third-party notices" }
            val mixinFiles = metadata["mixins"] as? List<*> ?: throw IllegalArgumentException("Missing mixins")
            require(mixinFiles.isNotEmpty()) { "Empty mixin configuration" }
            require(mixinFiles.size == mixinFiles.toSet().size) { "Duplicate mixin configuration" }
            val registeredMixins = mutableSetOf<String>()
            mixinFiles.forEach { name ->
                require(name is String) { "Unexpected mixin registration" }
                val mixins = json(entry(name))
                require(mixins["required"] == true) { "Mixins must remain required" }
                val prefix = (mixins["package"] as? String ?: throw IllegalArgumentException("Missing mixin package")).replace('.', '/')
                for (side in listOf("mixins", "client", "server")) {
                    if (side != "client" && side !in mixins) continue
                    val registrations = mixins[side] as? List<*> ?: throw IllegalArgumentException("Invalid $side mixins")
                    require(side != "client" || registrations.isNotEmpty()) { "No client mixins" }
                    for (mixin in registrations) {
                        require(mixin is String && mixin.matches(Regex("[\\w$]+(?:\\.[\\w$]+)*"))) { "Invalid mixin class name" }
                        val pathInJar = "$prefix/${mixin.replace('.', '/')}.class"
                        require(registeredMixins.add(pathInJar)) { "Duplicate mixin registration: $pathInJar" }
                        entry(pathInJar)
                    }
                }
            }
            verifyEditionContents(edition, names, registeredMixins, ::entry)
            val hm = "META-INF/jars/hm-api-${target.hmApi}.jar"
            val declared = (metadata["jars"] as? List<*>)?.map { (it as? Map<*, *>)?.get("file") }
            require(declared?.toSet() == setOf(hm) && declared.size == 1) { "Incorrect bundled artifacts" }
            val embedded = names.filter { it.startsWith("META-INF/jars/") && it.endsWith(".jar") }
            require(embedded.toSet() == setOf(hm)) { "Unexpected nested dependency (development JARs forbidden)" }
            for (file in embedded) {
                val content = nested(entry(file))
                val mod = json(content["fabric.mod.json"] ?: throw IllegalArgumentException("Missing nested Fabric metadata"))
                require(!(mod["id"] as? String).orEmpty().contains("devauth", ignoreCase = true)) { "DevAuth must not ship" }
                val requirements = mod["depends"] as? Map<*, *> ?: emptyMap<Any, Any>()
                require(accepts(requirements["minecraft"], target.minecraft) && accepts(requirements["java"], "25") &&
                    accepts(requirements["fabricloader"], "0.19.5") && accepts(requirements["fabric-api"], target.fabricApi)) { "Incompatible nested metadata: $file" }
                if (file == hm) require(mod["id"] == "hm-api" && mod["version"] == target.hmApi) { "Incorrect HM API" }

            }
        }
    } catch (e: Exception) {
        if (e is IllegalArgumentException) throw e
        throw IllegalArgumentException("Invalid distributable $path: ${e.message}", e)
    }
    val digest = Files.newInputStream(path).use { input ->
        val md = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(8192)
        while (true) { val read = input.read(buffer); if (read < 0) break; md.update(buffer, 0, read) }
        hex(md.digest())
    }
    return VerifiedJar(path, target.minecraft, modVersion, Files.size(path), digest, edition)
}

private fun hex(bytes: ByteArray) = java.util.HexFormat.of().formatHex(bytes)

fun verifyDistribution(paths: List<Path>, targets: List<TargetSpec>, modVersion: String): List<VerifiedJar> {
    require(paths.size == editions.size * SUPPORTED_TARGETS.size) { "Expected exactly nine edition/target JARs" }
    return editions.flatMap { edition ->
        val names = targets.map { distributionJarName(edition, modVersion, it.minecraft) }.toSet()
        verifyEditionDistribution(paths.filter { it.fileName.toString() in names }, targets, modVersion, edition)
    }
}

fun verifyEditionDistribution(paths: List<Path>, targets: List<TargetSpec>, modVersion: String, edition: String): List<VerifiedJar> {
    require(targets.size == SUPPORTED_TARGETS.size && targets.map { it.minecraft }.toSet() == SUPPORTED_TARGETS) { "Expected exactly three supported targets" }
    require(paths.size == targets.size && paths.map { it.fileName }.distinct().size == targets.size) { "Missing or duplicate $edition target JARs" }
    val verified = targets.map { target ->
        val file = paths.singleOrNull { it.fileName.toString() == distributionJarName(edition, modVersion, target.minecraft) }
            ?: throw IllegalArgumentException("Missing or duplicate $edition target ${target.minecraft}")
        verifyJar(file, target, modVersion, edition)
    }
    // Platform adapters compile differently, but every target must ship the same feature classes and assets.
    // Only the 26.1.2 immediate renderer needs this anonymous captured-pose adapter for Iris composition.
    val immediatePoseAdapter = "net/johnceo/sparklingmutuals/safari/shots/preview/CapsulePreviewRenderer\$render\$captured\$1.class"
    fun contents(path: Path): Map<String, String> = ZipFile(path.toFile()).use { zip ->
        zip.entries().asSequence().filter { !it.isDirectory && !it.name.startsWith("META-INF/") && it.name != "fabric.mod.json" &&
            !(edition != "standard" && it.name == immediatePoseAdapter) }
            .associate { entry -> entry.name to if (entry.name.endsWith(".class")) "class" else
                zip.getInputStream(entry).use { MessageDigest.getInstance("SHA-256").digest(bytes(it)) }.joinToString("") { "%02x".format(it) } }
    }
    val baseline = contents(verified.first().path)
    verified.drop(1).forEach { jar ->
        val actual = contents(jar.path)
        require(actual == baseline) { "Feature/resource parity differs for $edition ${jar.minecraft}: " +
            (baseline.keys + actual.keys).filter { baseline[it] != actual[it] }.joinToString() }
    }
    return verified
}
