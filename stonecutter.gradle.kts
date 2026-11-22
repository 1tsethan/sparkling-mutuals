import buildlogic.TargetSpec
import buildlogic.verifyDistribution
import buildlogic.verifyJar
import buildlogic.PublishReleaseTask
import buildlogic.collectDistribution
import buildlogic.verifyCollectedDistribution
import buildlogic.verifyEditionDistribution

plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.17.21" apply false
    kotlin("jvm") version "2.4.20" apply false
}
stonecutter active "26.1.2"

val releaseVersion = providers.gradleProperty("mod_version")
tasks.register("validateReleaseTag") {
    doLast { require(System.getenv("GITHUB_REF") == "refs/tags/v${releaseVersion.get()}") { "Tag must match mod_version" } }
}
val testBuildLogic = tasks.register<GradleBuild>("testBuildLogic") {
    group = "verification"
    description = "Runs the distribution and release build-helper regression tests."
    dir = file("buildSrc")
    tasks = listOf("test")
    buildName = "sparkling-build-logic-tests"
}
val buildAllTargets = tasks.register("buildAllTargets") {
    group = "build"
    description = "Tests every edition and Minecraft target and collects nine verified installable JARs."
    dependsOn(testBuildLogic)
}
val verifyDistributions = tasks.register("verifyDistributions") {
    group = "verification"
    description = "Verifies all nine installable JARs without rebuilding them."
    mustRunAfter(buildAllTargets)
}

gradle.projectsEvaluated {
    val builds = subprojects.sortedBy { it.name }
    fun edition(p: Project) = p.name.substringAfter('-', "standard")
    fun spec(p: Project) = TargetSpec(p.property("minecraft_version").toString(),
        p.property("fabric_api_version").toString(), p.property("hm_api_version").toString())
    val targets = builds.filter { edition(it) == "standard" }.map(::spec)
    val distribution = layout.buildDirectory.dir("distributions/${releaseVersion.get()}")
    val suppliedDirectory = providers.gradleProperty("distributionDir").map { layout.projectDirectory.dir(it) }
        .orElse(distribution)
    fun jars(directory: File) =
        requireNotNull(directory.listFiles()) { "Missing distribution directory: $directory" }
            .filter { it.isFile && it.extension == "jar" }.map { it.toPath() }
    fun artifact(p: Project) = p.tasks.named<Jar>("jar").get().archiveFile.get().asFile.toPath()
    builds.forEach { p ->
        p.tasks.register("verifyTargetJar") {
            group = "verification"
            dependsOn(p.tasks.named("build"))
            doLast { verifyJar(artifact(p), spec(p), p.version.toString(), edition(p)) }
        }
    }
    tasks.register("buildEdition") {
        group = "build"
        description = "Builds the selected edition for all targets. Defaults to Standard."
        val selected = providers.gradleProperty("edition").getOrElse("standard")
        require(selected in setOf("standard", "full", "noauto")) { "Unknown edition: $selected" }
        val selectedBuilds = builds.filter { edition(it) == selected }
        dependsOn(selectedBuilds.map { it.tasks.named("verifyTargetJar") })
        doLast { verifyEditionDistribution(selectedBuilds.map(::artifact), targets, releaseVersion.get(), selected) }
    }
    buildAllTargets.configure {
        dependsOn(builds.map { it.tasks.named("verifyTargetJar") })
        doLast {
            val destination = distribution.get().asFile
            collectDistribution(builds.map(::artifact), targets, releaseVersion.get(), destination.toPath())
            logger.lifecycle("Verified nine-JAR distribution: $destination")
        }
    }
    verifyDistributions.configure {
        doLast {
            val directory = suppliedDirectory.get().asFile
            verifyDistribution(jars(directory), targets, releaseVersion.get())
            logger.lifecycle("Verified all nine installable JARs in $directory")
        }
    }
    tasks.register("collectReleaseDistribution") {
        group = "verification"
        doLast {
            val incoming = suppliedDirectory.get().asFile
            collectDistribution(jars(incoming), targets, releaseVersion.get(), distribution.get().asFile.toPath())
        }
    }
    tasks.register("verifyStandardForModrinth") {
        group = "verification"
        doLast {
            val verified = verifyCollectedDistribution(suppliedDirectory.get().asFile.toPath(), targets, releaseVersion.get())
            verifyEditionDistribution(verified.jars.filter { it.edition == "standard" }.map { it.path },
                targets, releaseVersion.get(), "standard")
        }
    }
    tasks.register<PublishReleaseTask>("publishVerifiedRelease") {
        group = "publishing"
        description = "Publishes a complete verified distribution, exclusively in the matching tag-push workflow."
        modVersion = releaseVersion
        targetProperties = targets.associate { it.minecraft to "${it.fabricApi}|${it.hmApi}" }
        distributionDirectory = suppliedDirectory
    }
}
