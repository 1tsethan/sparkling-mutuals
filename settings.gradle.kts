pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins { id("dev.kikugie.stonecutter") version "0.9.8" }

rootProject.name = "sparkling-mutuals"
stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"
    create(rootProject) {
        val targets = listOf("26.1.2", "26.2", "26.3")
        versions(targets)
        for (target in targets) {
            version("$target-full", target)
            version("$target-noauto", target)
        }
        vcsVersion = "26.1.2"
    }
}
