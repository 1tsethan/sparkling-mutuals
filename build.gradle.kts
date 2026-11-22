import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.21"
    kotlin("jvm") version "2.4.20"
}

val targetMinecraft = stonecutter.current.version
val edition = project.name.substringAfter('-', "standard")
val targetProperties = Properties().apply {
    rootProject.file("versions/$targetMinecraft/gradle.properties").inputStream().use(::load)
}
fun targetProperty(name: String): String = requireNotNull(targetProperties.getProperty(name))
extensions.extraProperties["minecraft_version"] = targetMinecraft
extensions.extraProperties["fabric_api_version"] = targetProperty("fabric_api_version")
extensions.extraProperties["hm_api_version"] = targetProperty("hm_api_version")
stonecutter.constants {
    put("standard", edition == "standard")
    put("extended", edition != "standard")
    put("updater", edition == "full")
}
if (edition == "full") {
    sourceSets.main { java.srcDir(rootProject.file("src/updater/java")) }
    sourceSets.test { java.srcDir(rootProject.file("src/updaterTest/java")) }
}
val developerBuild = providers.gradleProperty("developerBuild").map { edition != "standard" && it.toBooleanStrict() }.orElse(false)
val modVersion = property("mod_version").toString() + if (developerBuild.get()) "-dev" else ""
version = modVersion
group = "net.johnceo.sparklingmutuals"
base.archivesName = "sparkling-mutuals"

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://maven.azureaaron.net/releases") }
        filter { includeModule("net.azureaaron", "hm-api") }
    }
    exclusiveContent {
        forRepository { maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") }
        filter { includeGroup("me.djtheredstoner") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$targetMinecraft")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    implementation(include("net.azureaaron:hm-api:${property("hm_api_version")}")!!)
    runtimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth.version")}")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    runs {
        named("client") {
            ideConfigGenerated(true)
            runDir("../../run/$edition/$targetMinecraft")
            if (providers.gradleProperty("clientSmoke").isPresent) {
                vmArg("-Ddevauth.enabled=false")
                vmArg("-Dmixin.debug.verbose=true")
                vmArg("-Dmixin.debug.export=true")
            }
        }
        named("server") { ideConfigGenerated(false) }
    }
}

tasks.test {
    useJUnitPlatform()
    failOnNoDiscoveredTests = true
}

// Local, test-classpath research only; does not publish data or change the target matrix.
if (targetMinecraft == "26.1.2") tasks.register<JavaExec>("replayShardMarket") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "net.johnceo.sparklingmutuals.skyblock.market.ShardReplayCli"
    doFirst {
        args(providers.gradleProperty("marketManifest").get(), providers.gradleProperty("marketReport").get())
        if (providers.gradleProperty("marketSelectTraining").isPresent) args("select-training")
        else if (providers.gradleProperty("marketExportSamples").isPresent) args("export-samples")
    }
}
tasks.processResources {
    val properties = mapOf("version" to modVersion, "minecraft" to targetMinecraft,
        "developer" to developerBuild.get(), "edition" to edition)
    inputs.properties(properties)
    filesMatching("fabric.mod.json") { expand(properties) }
    if (edition != "standard") {
        val defaults = rootProject.file("src/extended/resources/extended-defaults.properties")
        val mixins = rootProject.file("src/extended/resources/sparkling-mutuals.extended.mixins.json")
        val baseDefaults = rootProject.file("src/main/resources/defaults/sparkling-mutuals.properties")
        val baseMixins = rootProject.file("src/main/resources/sparkling-mutuals.mixins.json")
        val contest = rootProject.file("src/main/resources/defaults/sparkling-mutuals-contest.properties")
        inputs.files(defaults, mixins, baseDefaults, baseMixins, contest)
        doLast {
            destinationDir.resolve("defaults/sparkling-mutuals.properties").writeText(baseDefaults.readText() + defaults.readText())
            destinationDir.resolve("defaults/sparkling-mutuals-contest.properties").writeText(contest.readText() + "autoContestDowntime=true\n")
            val json = groovy.json.JsonSlurper()
            val config = (json.parse(baseMixins) as Map<*, *>).toMutableMap()
            val extra = json.parse(mixins) as Map<*, *>
            config["client"] = (config["client"] as List<*>) + (extra["client"] as List<*>)
            destinationDir.resolve("sparkling-mutuals.mixins.json")
                .writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(config)))
        }
    }
}
tasks.withType<JavaCompile>().configureEach { options.release = 25 }
kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_25 }
    if (edition == "standard") {
        sourceSets.named("main") {
            kotlin.exclude("**/safari/automation/entry/**", "**/alerts/SparkleWarpPolicy.kt", "**/alerts/WarpAlertTimer.kt")
        }
        sourceSets.named("test") {
            kotlin.exclude("**/safari/automation/entry/**", "**/alerts/SparkleWarpPolicyTest.kt")
        }
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
}
tasks.jar {
    val prefix = when (edition) {
        "standard" -> "sparkling-mutuals-standard"
        "full" -> "sparkling-mutuals"
        "noauto" -> "sparkling-mutuals-full-noautoupdate"
        else -> error("Unknown edition $edition")
    }
    archiveFileName = "$prefix-$modVersion+mc$targetMinecraft.jar"
    from(rootProject.file("licenses")) { into("licenses") }
    from(rootProject.file("LICENSE")) { rename { "LICENSE_sparkling-mutuals" } }
}
tasks.named<Jar>("sourcesJar") { from(rootProject.file("licenses")) { into("licenses") } }
