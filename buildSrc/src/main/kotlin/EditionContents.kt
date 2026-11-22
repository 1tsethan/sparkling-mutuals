package buildlogic

import java.lang.classfile.AnnotationValue
import java.lang.classfile.Attributes
import java.lang.classfile.ClassFile
import java.lang.classfile.constantpool.Utf8Entry
import java.util.Properties

private const val MOD_PACKAGE = "net/johnceo/sparklingmutuals/"
private const val MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;"
private val settingAnnotations = setOf("L${MOD_PACKAGE}config/ConfigOption;", "L${MOD_PACKAGE}config/Category;")

private val extendedMixins = listOf(
    "DisconnectedScreenAccessor", "HypixelDisconnectMixin", "CritterGlowStateMixin", "CritterArmorStandMixin",
    "DoomspiralLotteryHudMixin", "SafariWaterFogMixin", "SafariWaterSurfaceMixin", "SafariWaterOverlayMixin",
    "SafariSodiumWaterMixin", "SafariPaintingMixin", "SafariShinyMixin", "SafariAttackKeyAccessor",
    "SafariAutoClickerMixin", "SafariCandleMixin", "SafariManagerMixin", "CjsEntryFocusMixin", "CjsEntryKeyboardMixin",
    "CjsEntryMouseMixin", "SafariTicketScreenMixin", "PhantomleafSoundMixin", "CapsuleContactPacketMixin",
    "CapsuleShaderRenderMixin", "GemzieUseMixin",
)
private val extendedClasses = listOf(
    "alerts/SparkleAutoParty", "alerts/SparkleWarping", "alerts/SparkleWarpPolicy", "alerts/SparkleWarpMode",
    "alerts/StartPartyRoster", "alerts/ReturningPartyInvites", "alerts/LeechInvitation", "alerts/WarpAlertTimer",
    "alerts/WarpListScreen", "misc/phantomleaf/PhantomleafSolver",
    "safari/ClearWater", "safari/DoomspiralLottery", "safari/esp/DuplicoGlow", "safari/esp/ShywormBodyDiagnostics",
    "safari/automation/AutoClicker", "safari/automation/GemzieAutoSelect", "safari/automation/SafariManagerGuard",
    "safari/automation/SafariTicketSelect", "safari/automation/entry/CjsApproachInput", "safari/automation/entry/CjsEntryInput",
    "safari/automation/entry/CjsEntryDispatch", "safari/automation/entry/CjsSafariEntry", "safari/automation/entry/CjsWalkingKeys",
    "safari/shots/preview/CapsulePreview", "safari/shots/preview/CapsulePreviewRenderer",
    "safari/shots/preview/contacts/CapsuleContactScene", "safari/shots/preview/contacts/CapsuleContactUpdates",
) + extendedMixins.map { "mixin/$it" }

private val extendedReferences = classReferences(extendedClasses)
private val rawWorldDiagnostics = mapOf(
    "debug/SafariDeveloper" to setOf("snapshot", "toggleTesting"),
    "honeyhives/HoneyHives" to setOf("debug"),
    "safari/SafariAssist" to setOf("diagnostics"),
    "safari/esp/SafariEsp" to setOf("debug", "developerSnapshot"),
    "safari/structures/BeeNests" to setOf("debug"),
)
private val updaterReferences = classReferences(listOf("config/ReleaseUpdateChecker"))
private const val UPDATER_PACKAGE = "${MOD_PACKAGE}update/"
private const val UPDATER_ENDPOINT = "api.github.com/repos/johnceoskyblock/sparkling-mutuals/releases"
private val extendedSettings = setOf(
    "autoClicker", "autoContestDowntime", "autoJoinSkyBlock", "autoParty", "autoPartyOnlineOnly", "autoReconnectHypixel",
    "autoSelectSafariTicket", "candleHitbox", "clearWater", "doomspiralLottery", "experimentalCapsulePath", "gemzieGemAutoSelect",
    "hideHauntedPaintings", "hideyhoAutoAccept", "holdManagerOnKick", "leechMode", "leechWhitelist", "phantomleafSolver",
    "repartyDelaySeconds", "safariEntryAutoEnter", "safariTicketPreference", "showCapsuleGuideThroughTerrain", "sparkleWarpMode",
    "warpAlertsEnabled", "warpDelaySeconds", "warpDuo",
)
private val extendedSettingTitles = setOf(
    "auto-accept hideyho quest", "auto-click rockmite mounds", "auto-join skyblock", "auto-party from warp list",
    "auto-reconnect to hypixel", "auto-select podium gems", "auto-select safari ticket", "auto-warp time (seconds)",
    "capsule trajectory guide", "clear water", "doomspiral gambling", "duo runner", "hide haunted paintings", "invite online players only",
    "larger candle hitboxes", "leech mode", "leech whitelist", "party contest entry guard", "phantomleaf solver", "preferred ticket",
    "reparty cutoff (seconds)", "reset warping timers", "show guide through blocks", "sparkle warping", "sparkling auto-entry",
    "wait for party before entry", "warp list", "warping setup", "water opacity by biome",
)

private fun classReferences(classes: List<String>) = Regex(
    Regex.escape(MOD_PACKAGE) + "(?:" + classes.joinToString("|") { Regex.escape(it) } + ")(?=[$;.<]|$)",
)

private fun updaterReference(value: String) = UPDATER_PACKAGE in value || updaterReferences.containsMatchIn(value)

private fun forbiddenSettingKey(key: String, edition: String) =
    (edition != "full" && key == "autoUpdate") ||
        (edition == "standard" && (key in extendedSettings || key.startsWith("warpList.")))

internal fun verifyEditionContents(edition: String, names: List<String>, registeredMixins: Set<String>, read: (String) -> ByteArray) {
    val required = if (edition == "standard") listOf(
        "safari/visibility/LocalVisibility", "safari/visibility/VisibleDiscoveryMemory",
    ) else listOf(
        "alerts/SparkleWarping", "safari/automation/AutoClicker", "safari/automation/entry/CjsSafariEntry",
    )
    val updater = if (edition == "full") listOf(
        "update/UpdateInstall", "update/UpdateTransport", "config/ReleaseUpdateChecker",
    ) else emptyList()
    for (type in required + updater) {
        require("$MOD_PACKAGE$type.class" in names) { "$edition is missing its required class $type" }
    }
    if (edition == "standard") {
        require("extended-defaults.properties" !in names && "sparkling-mutuals.extended.mixins.json" !in names) {
            "Standard contains an extended-only resource"
        }
    }
    if (edition != "full") names.filter { it.endsWith(".properties") }.forEach { name ->
        val defaults = Properties().apply { read(name).inputStream().use(::load) }
        require(defaults.stringPropertyNames().none { forbiddenSettingKey(it, edition) }) { "$edition has restricted settings in $name" }
    }

    val classes = names.filter { it.endsWith(".class") }
    val compiledMixins = mutableSetOf<String>()
    for (name in classes) {
        val model = ClassFile.of().parse(read(name))
        val annotations = model.findAttribute(Attributes.runtimeInvisibleAnnotations()).orElse(null)?.annotations().orEmpty() +
            model.findAttribute(Attributes.runtimeVisibleAnnotations()).orElse(null)?.annotations().orEmpty()
        if (annotations.any { it.className().equalsString(MIXIN_ANNOTATION) }) compiledMixins.add(name)
        if (edition == "full") continue
        if (edition == "standard") {
            val rawMethods = rawWorldDiagnostics[name.removePrefix(MOD_PACKAGE).removeSuffix(".class")].orEmpty()
            require(model.methods().none { it.methodName().stringValue().substringBefore('$') in rawMethods }) {
                "Standard contains raw world diagnostics in $name"
            }
        }

        val references = model.constantPool().asSequence().filterIsInstance<Utf8Entry>()
        for (entry in references) {
            val value = entry.stringValue()
            val classPath = value.replace('.', '/')
            require(!updaterReference(classPath) && UPDATER_ENDPOINT !in value) { "$edition has an updater reference in $name" }
            require(edition != "standard" || (!extendedReferences.containsMatchIn(classPath) &&
                "${MOD_PACKAGE}safari/automation/entry/" !in classPath)) {
                "Standard has a restricted class reference in $name"
            }
            if (edition == "standard" && name.startsWith("${MOD_PACKAGE}compat/WorldRenderCompat")) {
                require(value !in setOf("ALWAYS_PASS", "SEE_THROUGH", "THROUGH_TERRAIN")) {
                    "Standard contains a through-terrain rendering path in $name"
                }
            }
        }

        for (field in model.fields()) {
            val fieldAnnotations = field.findAttribute(Attributes.runtimeVisibleAnnotations()).orElse(null)?.annotations().orEmpty()
            if (fieldAnnotations.none { it.className().stringValue() in settingAnnotations }) continue
            val labels = fieldAnnotations.filter { it.className().stringValue() in settingAnnotations }.flatMap { it.elements() }
                .filter { it.name().equalsString("name") }.mapNotNull { (it.value() as? AnnotationValue.OfString)?.stringValue()?.lowercase() }
            require(!forbiddenSettingKey(field.fieldName().stringValue(), edition) && "auto update" !in labels &&
                (edition != "standard" || labels.none { it in extendedSettingTitles })) {
                "$edition contains a restricted settings registration in $name"
            }
        }
    }
    require(compiledMixins == registeredMixins) {
        "Mixin registrations differ from compiled mixins: missing ${compiledMixins - registeredMixins}, invalid ${registeredMixins - compiledMixins}"
    }
}
