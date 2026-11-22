package net.johnceo.sparklingmutuals.config

//? if extended {
/*import java.nio.file.FileAlreadyExistsException
*///? }
import java.nio.file.Files
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader

/** Resolves the configuration directory before the edition opens its stores. */
internal object ConfigStorage {
//? if extended {
/*    private const val folder = "sparkling-mutuals"
*///? } else {
    private const val folder = "sparkling-mutuals-legit"
//? }
    val directory: Path get() = FabricLoader.getInstance().configDir.resolve(folder)
//? if extended {
/*
    private val active = setOf(
        "sparkling-mutuals.properties", "sparkling-mutuals-contest.properties",
        "sparkling-mutuals-pbs.properties", "sparkling-mutuals-honeyhives.properties",
        "Sparkling Mutual's PBs.properties"
    ) + setOf("runs", "sparklings", "achievements", "playtime", "sale-watches", "box-guidance",
        "box-total", "shard-history").map { "sparkling-mutuals-$it.json" }
    private val locks = setOf("sale-watches", "box-guidance", "box-total", "shard-history")
        .map { "sparkling-mutuals-$it.json.lock" }.toSet()
    private const val retiredCache = "sparkling-mutuals-market-prices.json"

    fun migrate(root: Path): Path {
        val target = Files.createDirectories(root.resolve(folder))
        val archive = target.resolve("backups").resolve("legacy")
        Files.newDirectoryStream(root).use { entries ->
            for (source in entries) {
                if (!Files.isRegularFile(source)) continue
                val name = source.fileName.toString()
                when {
                    name in active || name in locks -> {
                        try { Files.move(source, target.resolve(name)) }
                        catch (_: FileAlreadyExistsException) { archive(source, archive) }
                    }
                    name == retiredCache -> Files.deleteIfExists(source)
                    name.endsWith(".bak") &&
                        active.any { name.startsWith("$it.") } -> archive(source, archive)
                }
            }
        }
        Files.deleteIfExists(archive.resolve(retiredCache))
        return target
    }

    private fun archive(source: Path, directory: Path) {
        Files.createDirectories(directory)
        val name = source.fileName.toString()
        var suffix = 0
        while (true) {
            val filename = if (suffix == 0) name else "$name.$suffix"
            try {
                Files.move(source, directory.resolve(filename))
                return
            } catch (_: FileAlreadyExistsException) {
                suffix++
            }
        }
    }
*///? } else {
    fun migrate(root: Path): Path = Files.createDirectories(root.resolve(folder))
//? }
}
