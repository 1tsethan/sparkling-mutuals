# Sparkling Mutuals edition behavior

This repository is the legit edition maintained by 1tsethan, derived from current upstream master at a8569c0 (v4.4.2). The user-facing name remains Sparkling Mutuals. The Cheat checkout remains a separate repository.

## Presentation and tracking

Unique, Full Clear and Sparkle mode/completion code and backend progress updates are retained. Local visibility gates apply to target presentation and discovery output, not shared objective state. Entities must be current, loaded, in range, in the camera view and visible past terrain. Normally visible names and equipped heads can identify invisible carrier entities. All geometry uses depth testing, and labels use normal depth-aware text.

Vanilla glowing outlines are replaced by visible boxes because the vanilla glow compositor shows silhouettes through walls. Hidden-height projections, unloaded nest sites, retained mound waypoints and last-seen critter markers are not drawn. Floor-drop selection still follows the existing mode/objective controls; labels are generic because spawn-order inference is not a normal visual identification of a reward. Discovery banners and nearby-target HUDs require a visible local entity. Particle hints and party reports still update internal state but do not trigger a discovery alert. Queued discovery banners revalidate sight before display and sound.

## Removed gameplay and hidden-information features

Automatic party invitation, warping, reparty, leech acceptance, entry, movement/aiming, ticket/menu input, hotbar gem selection, mound clicking, quest acceptance, reconnect and SkyBlock joining are removed. Retired config bindings are inert and absent from the settings catalog and saved flags; legacy values cannot restore them. Gameplay mixins and native input adapters are removed. The shared command boundary accepts only informational party/profile queries and party chat.

Candle interaction hitbox expansion, clear-water rendering, painting removal, capsule trajectory prediction, Doomspiral lottery and the Phantomleaf location solver are not activated in this edition. Passive progress and history helpers remain. Existing historical documents and pure protocol/policy helpers may describe upstream behavior; the runtime startup and mixin manifest define what this edition activates.

## Storage and distribution

The directory is config/sparkling-mutuals-legit. No files are imported, moved or deleted from legacy or cheat storage. The Fabric mod ID and assets remain sparkling-mutuals for compatibility, so the two editions must not be installed together. Sources, issues, updater and tag publishing target 1tsethan/sparkling-mutuals; the original authors, license and third-party notices remain.

Java 25, Gradle/Stonecutter and the Minecraft 26.1.2, 26.2 and 26.3 matrix are preserved. Releases must verify all three JARs before publication. No release tag, release or Modrinth upload is created during initial setup. Modrinth publication is a future goal.

## Validation limits

The automated suite covers config isolation, retired switches, dispatch restrictions, visible identification policy and queued visibility loss, alongside existing mode/completion/sync tests. Visual behavior still needs a manual in-game pass, including partial occlusion, invisible name carriers and third-person view. This is not a claim of Hypixel endorsement or approval. Current server rules take priority over feature requests.
