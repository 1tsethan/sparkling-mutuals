package buildlogic

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.lang.classfile.Annotation
import java.lang.classfile.AnnotationElement
import java.lang.classfile.ClassBuilder
import java.lang.classfile.ClassFile
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DistributionVerifierTest {
    @TempDir lateinit var dir: Path
    private val targets = listOf(
        TargetSpec("26.1.2", "0.155.3+26.1.2", "1.0.3+26.1"),
        TargetSpec("26.2", "0.161.0+26.2", "1.0.4+26.2"),
        TargetSpec("26.3", "0.163.0+26.3", "1.0.5+26.3"))
    private fun zip(entries: Map<String, ByteArray>): ByteArray = ByteArrayOutputStream().use { bytes ->
        ZipOutputStream(bytes).use { z -> entries.forEach { (name, data) ->
            z.putNextEntry(ZipEntry(name)); z.write(data); z.closeEntry()
        } }; bytes.toByteArray()
    }
    private val editions = listOf("standard", "full", "noauto")
    private val modPackage = "net/johnceo/sparklingmutuals/"
    private val mixinName = "${modPackage}mixin/SafariCapsuleMixin"

    private fun bytecode(name: String, contents: (ClassBuilder) -> Unit = {}): ByteArray =
        ClassFile.of().build(ClassDesc.ofInternalName(name), contents)

    private fun mixinBytecode(name: String) = bytecode(name) {
        it.with(RuntimeInvisibleAnnotationsAttribute.of(Annotation.of(ClassDesc.of("org.spongepowered.asm.mixin.Mixin"))))
    }

    private fun jar(target: TargetSpec, edition: String = "full",
                    change: (MutableMap<String, Any>, MutableMap<String, ByteArray>) -> Unit = { _, _ -> }): Path {
        val hmName = "META-INF/jars/hm-api-${target.hmApi}.jar"
        val entries = mutableMapOf(
            "${modPackage}SparklingMutuals.class" to bytecode("${modPackage}SparklingMutuals"),
            "assets/sparkling-mutuals/icon.png" to byteArrayOf(1),
            "LICENSE_sparkling-mutuals" to "CC0".toByteArray(),
            "licenses/HM-API-LICENSE" to "Apache".toByteArray(),
            "sparkling-mutuals.mixins.json" to JsonOutput.toJson(mapOf("required" to true,
                "package" to "net.johnceo.sparklingmutuals.mixin", "client" to listOf("SafariCapsuleMixin"))).toByteArray(),
            "$mixinName.class" to mixinBytecode(mixinName),
            hmName to zip(mapOf("fabric.mod.json" to JsonOutput.toJson(mapOf("id" to "hm-api", "version" to target.hmApi,
                "depends" to mapOf("minecraft" to "~${target.minecraft}", "java" to ">=25"))).toByteArray()))
        )
        val required = if (edition == "standard") listOf("safari/visibility/LocalVisibility",
            "safari/visibility/VisibleDiscoveryMemory") else listOf("alerts/SparkleWarping",
            "safari/automation/AutoClicker", "safari/automation/entry/CjsSafariEntry")
        for (name in required + if (edition == "full") listOf("update/UpdateInstall",
            "update/UpdateTransport", "config/ReleaseUpdateChecker") else emptyList()) {
            entries["$modPackage$name.class"] = bytecode("$modPackage$name")
        }
        val metadata = mutableMapOf<String, Any>("schemaVersion" to 1, "id" to "sparkling-mutuals", "version" to "3.3.0",
            "environment" to "client", "icon" to "assets/sparkling-mutuals/icon.png",
            "custom" to mapOf("sparkling-mutuals:edition" to edition),
            "entrypoints" to mapOf("main" to listOf(mapOf("value" to "net.johnceo.sparklingmutuals.SparklingMutuals", "adapter" to "kotlin"))),
            "mixins" to listOf("sparkling-mutuals.mixins.json"), "jars" to listOf(mapOf("file" to hmName)),
            "depends" to mapOf("minecraft" to target.minecraft, "java" to ">=25", "fabricloader" to ">=0.19.5",
                "fabric-api" to "*", "fabric-language-kotlin" to "*"))
        change(metadata, entries)
        entries["fabric.mod.json"] = JsonOutput.toJson(metadata).toByteArray()
        val prefix = when (edition) {
            "standard" -> "sparkling-mutuals-standard"
            "noauto" -> "sparkling-mutuals-full-noautoupdate"
            else -> "sparkling-mutuals"
        }
        return dir.resolve("$prefix-3.3.0+mc${target.minecraft}.jar").also { Files.write(it, zip(entries)) }
    }
    private fun matrixJars() = editions.flatMap { edition -> targets.map { jar(it, edition) } }

    @Test fun refusesJarsMissingTheirEditionImplementation() {
        val checks = mapOf(
            "standard" to "safari/visibility/LocalVisibility",
            "full" to "update/UpdateInstall",
            "noauto" to "safari/automation/AutoClicker",
        )
        for ((edition, required) in checks) {
            val damaged = jar(targets.first(), edition) { _, entries ->
                entries.remove("$modPackage$required.class")
            }
            assertThrows(IllegalArgumentException::class.java) { verifyJar(damaged, targets.first(), "3.3.0", edition) }
        }
    }

    @Test fun completeDistributionHasExactlyNineVerifiedEditionTargetPairs() {
        val files = matrixJars()
        val result = verifyDistribution(files, targets, "3.3.0")
        assertEquals(editions.associateWith { setOf("26.1.2", "26.2", "26.3") },
            result.groupBy { it.edition }.mapValues { (_, jars) -> jars.map { it.minecraft }.toSet() })
        assertEquals(9, result.size)
        result.forEach { verified ->
            assertEquals("3.3.0", verified.modVersion); assertTrue(verified.size > 0)
            assertEquals(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(verified.path)).joinToString("") { "%02x".format(it) }, verified.sha256)
        }
    }
    @Test fun wrongInternalMinecraftFailsDespiteCorrectFilename() {
        val p = jar(targets[2]) { m, _ -> m["depends"] = mapOf("minecraft" to "26.2", "java" to ">=25", "fabricloader" to ">=0.19.5") }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(p, targets[2], "3.3.0", "full") }
    }
    @Test fun anEditionSubstitutionFailsDespiteCorrectFilename() {
        val path = jar(targets.first()) { metadata, _ ->
            metadata["custom"] = mapOf("sparkling-mutuals:edition" to "standard")
        }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(path, targets.first(), "3.3.0", "full") }
    }
    @Test fun missingAndDuplicateTargetsCannotBecomeACompleteDistribution() {
        val files = matrixJars()
        assertThrows(IllegalArgumentException::class.java) { verifyDistribution(files.dropLast(1), targets, "3.3.0") }
        assertThrows(IllegalArgumentException::class.java) { verifyDistribution(files.dropLast(1) + files.first(), targets, "3.3.0") }
        assertThrows(IllegalArgumentException::class.java) {
            verifyEditionDistribution(files.take(2) + files[3], targets, "3.3.0", "standard")
        }
    }
    @Test fun wrongVersionAndMissingRuntimeResourcesAreRejected() {
        val missing = listOf("net/johnceo/sparklingmutuals/SparklingMutuals.class", "assets/sparkling-mutuals/icon.png",
            "LICENSE_sparkling-mutuals", "licenses/HM-API-LICENSE", "sparkling-mutuals.mixins.json",
            "net/johnceo/sparklingmutuals/mixin/SafariCapsuleMixin.class")
        for (entry in missing) {
            val p = jar(targets[0]) { _, e -> e.remove(entry) }
            assertThrows(IllegalArgumentException::class.java, { verifyJar(p, targets[0], "3.3.0", "full") }, entry)
        }
        val p = jar(targets[0]) { m, _ -> m["version"] = "3.2.0" }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(p, targets[0], "3.3.0", "full") }
    }
    @Test fun sourceOrDevelopmentNamesCannotBeInstalled() {
        val p = jar(targets[0])
        for (suffix in listOf("-sources", "-dev", "-test")) {
            val renamed = Files.copy(p, dir.resolve("sparkling-mutuals-3.3.0+mc26.1.2$suffix.jar"))
            assertThrows(IllegalArgumentException::class.java) { verifyJar(renamed, targets[0], "3.3.0", "full") }
        }
    }
    @Test fun wrongNestedTargetAndEmbeddedDevAuthAreRejected() {
        val p = jar(targets[2]) { _, e -> e["META-INF/jars/hm-api-1.0.5+26.3.jar"] = zip(mapOf("fabric.mod.json" to
            "{\"id\":\"hm-api\",\"version\":\"1.0.5+26.3\",\"depends\":{\"minecraft\":\"~26.2\"}}".toByteArray())) }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(p, targets[2], "3.3.0", "full") }
        val dev = jar(targets[0]) { _, e -> e["META-INF/jars/devauth.jar"] = zip(mapOf("fabric.mod.json" to "{\"id\":\"devauth\",\"version\":\"1.2.2\"}".toByteArray())) }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(dev, targets[0], "3.3.0", "full") }
    }
    @Test fun incompleteInvocationCannotReplaceAPreviousCompleteDistribution() {
        val destination = dir.resolve("distribution")
        Files.createDirectories(destination)
        Files.writeString(destination.resolve("previous.jar"), "keep until a complete replacement is verified")
        val files = matrixJars()
        assertThrows(IllegalArgumentException::class.java) { collectDistribution(files.dropLast(1), targets, "3.3.0", destination) }
        assertEquals("keep until a complete replacement is verified", Files.readString(destination.resolve("previous.jar")))
        collectDistribution(files, targets, "3.3.0", destination)
        assertFalse(Files.exists(destination.resolve("previous.jar")))
        assertEquals(9, Files.list(destination).use { s -> s.filter { it.fileName.toString().endsWith(".jar") }.count() })
        assertTrue(Files.exists(destination.resolve("build-manifest.json")))
    }
    @Test fun nativeSettingsMustNotBundleTheOldSettingsLibrary() {
        val target=targets.first()
        val bad=jar(target) { metadata,entries ->
            entries["META-INF/jars/moulconfig.jar"]=byteArrayOf(1)
            metadata["jars"]=(metadata["jars"] as List<*>)+mapOf("file" to "META-INF/jars/moulconfig.jar")
        }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(bad, target, "3.3.0", "full") }
    }
    @Test fun targetsMustShipTheSameFeaturesAndResources() {
        for (entry in listOf("net/johnceo/sparklingmutuals/OnlyOneTarget.class", "defaults/settings.properties", "assets/sparkling-mutuals/icon.png")) {
            val files=targets.map { target -> jar(target) { _,entries ->
                if (target.minecraft == "26.3") entries[entry] = if (entry.endsWith(".class")) bytecode(entry.removeSuffix(".class")) else byteArrayOf(9)
            } }
            assertThrows(IllegalArgumentException::class.java, { verifyEditionDistribution(files, targets, "3.3.0", "full") }, entry)
        }
    }

    @Test fun parityComparesTargetsWithinTheirOwnEdition() {
        val jars = editions.flatMap { edition -> targets.map { target ->
            jar(target, edition) { _, entries -> entries["assets/sparkling-mutuals/edition.txt"] = edition.toByteArray() }
        } }
        assertEquals(9, verifyDistribution(jars, targets, "3.3.0").size)
    }

    @Test fun manifestRevalidationRejectsTamperingOrUnexpectedFiles() {
        val destination = dir.resolve("distribution")
        val jars = collectDistribution(matrixJars(), targets, "3.3.0", destination)
        val verified = verifyCollectedDistribution(destination, targets, "3.3.0")
        assertEquals(jars, verified.jars)
        assertEquals("build-manifest.json", verified.manifest.path.fileName.toString())
        val manifest = verified.manifest.path
        val original = Files.readString(manifest)
        val document = JsonSlurper().parseText(original) as Map<*, *>
        val rows = document["files"] as List<*>
        assertEquals(setOf("standard", "full", "noauto"), rows.map { (it as Map<*, *>)["edition"] }.toSet())
        Files.writeString(manifest, original.replace(jars.first().sha256, "0".repeat(64)))
        assertThrows(IllegalArgumentException::class.java) { verifyCollectedDistribution(destination, targets, "3.3.0") }
        Files.writeString(manifest, original)
        Files.writeString(destination.resolve("unverified.txt"), "unexpected")
        assertThrows(IllegalArgumentException::class.java) { verifyCollectedDistribution(destination, targets, "3.3.0") }
    }

    @Test fun aCompiledMixinWithoutRegistrationIsRejected() {
        val extra = "${modPackage}mixin/AdditionalMixin"
        val path = jar(targets.first()) { _, entries -> entries["$extra.class"] = mixinBytecode(extra) }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(path, targets.first(), "3.3.0", "full") }
    }

    @Test fun standardCannotShipRestrictedClassesOrReferencesToThem() {
        for (restricted in listOf("safari/automation/AutoClicker", "safari/esp/DuplicoGlow", "mixin/GemzieUseMixin")) {
            val direct = jar(targets.first(), "standard") { _, entries ->
                entries["$modPackage$restricted.class"] = bytecode("$modPackage$restricted")
            }
            assertThrows(IllegalArgumentException::class.java) { verifyJar(direct, targets.first(), "3.3.0", "standard") }
            val referenced = jar(targets.first(), "standard") { _, entries ->
                entries["${modPackage}Feature.class"] = bytecode("${modPackage}Feature") {
                    it.withField("implementation", ClassDesc.ofInternalName("$modPackage$restricted"), ClassFile.ACC_PRIVATE)
                }
            }
            assertThrows(IllegalArgumentException::class.java) { verifyJar(referenced, targets.first(), "3.3.0", "standard") }
        }
    }

    @Test fun standardCannotShipRawWorldDiagnostics() {
        val type = modPackage + "safari/esp/SafariEsp"
        val path = jar(targets.first(), "standard") { _, entries ->
            entries["$type.class"] = bytecode(type) {
                it.withMethodBody("developerSnapshot\$sparkling_mutuals", MethodTypeDesc.of(ConstantDescs.CD_void), ClassFile.ACC_PUBLIC) { body ->
                    body.return_()
                }
            }
        }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(path, targets.first(), "3.3.0", "standard") }
    }

    @Test fun standardAllowsInertSettingsButRejectsRestrictedRegistrationOrDefault() {
        fun settings(registered: Boolean) = bytecode("${modPackage}Settings") {
            it.withField("autoClicker", ConstantDescs.CD_boolean) { field ->
                field.withFlags(ClassFile.ACC_PRIVATE)
                if (registered) field.with(RuntimeVisibleAnnotationsAttribute.of(Annotation.of(
                    ClassDesc.ofInternalName("${modPackage}config/ConfigOption"),
                    AnnotationElement.ofString("name", "Auto-click Rockmite mounds"))))
            }
        }
        val inert = jar(targets.first(), "standard") { _, entries -> entries["${modPackage}Settings.class"] = settings(false) }
        verifyJar(inert, targets.first(), "3.3.0", "standard")
        val registered = jar(targets.first(), "standard") { _, entries -> entries["${modPackage}Settings.class"] = settings(true) }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(registered, targets.first(), "3.3.0", "standard") }
        val enabled = jar(targets.first(), "standard") { _, entries ->
            entries["defaults/sparkling-mutuals.properties"] = "autoClicker=true\n".toByteArray()
        }
        assertThrows(IllegalArgumentException::class.java) { verifyJar(enabled, targets.first(), "3.3.0", "standard") }
    }

    @Test fun onlyFullCanShipTheUpdater() {
        for (edition in editions) {
            val path = jar(targets.first(), edition) { _, entries ->
                entries["${modPackage}update/UpdateInstall.class"] = bytecode("${modPackage}update/UpdateInstall")
            }
            if (edition == "full") verifyJar(path, targets.first(), "3.3.0", edition)
            else assertThrows(IllegalArgumentException::class.java) { verifyJar(path, targets.first(), "3.3.0", edition) }
        }
    }
}
