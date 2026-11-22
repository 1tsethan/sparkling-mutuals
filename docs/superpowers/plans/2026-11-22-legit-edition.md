# Sparkling Mutuals legit edition implementation plan

Goal: Implement the approved legit edition in the isolated Legit checkout on legit-edition, retaining Safari progress logic.
Spec: ../specs/2026-11-22-legit-edition-design.md (approved 2026-11-22).
Architecture: Remove gameplay automation and its activation surface; gate presentation through current local visibility; preserve progress state. Keep display name and protocol/assets, isolate configuration, retarget distribution.
Tech: Kotlin/Java 25, Fabric, Stonecutter, Gradle, JUnit; Minecraft 26.1.2, 26.2, 26.3.
Execution: Native execution in this session, as requested by continue. GitHub work through browser only.

## Task 1: Distribution identity and config isolation
- [x] Add config isolation regression verifying legacy files are untouched and target folder is sparkling-mutuals-legit.
- [x] Run regression and observe failure.
- [x] Replace ConfigStorage legacy migration with dedicated namespace creation; retarget updater, workflow/build verification and shipping script references to 1tsethan/sparkling-mutuals. Preserve attribution and metadata name.
- [x] Update relevant existing fixtures and run config/build-logic/shipping checks.

## Task 2: Remove automatic gameplay
Files: SparklingMutuals.kt, alerts/, safari/automation/, safari/hideyho/HideyhoQuest.kt, mixin/, config/ConfigManager.kt and SafariSettings.kt, command registration and sync activation sites.
- [x] Trace live activation paths and all movement/click/hotbar/server command dispatches.
- [x] Add regression asserting gameplay automation mixins and startup registrations are absent; verify failure.
- [x] Remove gameplay automation implementation, settings, config persistence and commands, adapting call sites and retaining passive tracking. Remove candle hitbox and clear-water hooks. Audit manual command behavior and remove automatic party management triggered by reports.
- [x] Run tests and compilation; update obsolete automation tests only where their removed behavior has no supported runtime counterpart.

## Task 3: Current local presentation
Files: new safari/visibility/LocalVisibility.kt and policy tests; SafariEsp.kt, TeammateEsp.kt, structures/ renderers, sparkling/SparklingEncounters.kt and output consumers, WorldRenderCompat.kt.
Interface: LocalVisibility.entity(entity: Entity, client: Minecraft = Minecraft.getInstance()): Boolean and position(pos: Vec3, client: Minecraft = Minecraft.getInstance()): Boolean, requiring loaded chunk, range, view direction, raycast terrain visibility and current renderable state. Pure policy seam pins range, loaded and occlusion requirements.
- [x] Write tests for visible/blocked/unloaded/out-of-range/behind-camera targets, remote-only discovery, delayed visible discovery and legacy sightings; observe failures.
- [x] Implement shared presentation guard. Use depth-tested geometry and labels. Disable vanilla glow override because vanilla outline composition exposes silhouettes through terrain. Retain occluded boxes/labels for visible targets. Suppress predicted/last-seen output lacking a current visible model.
- [x] Gate local discovery output without consuming dedup state for invisible targets. Preserve shared progress and mode/completion state updates. Remove particle and shared-only alerts. Remove capsule prediction if it exposes unavailable information.
- [x] Run mode/completion/sync regression tests and visibility tests; review all overlay/hud entry points for alternate leaks.

## Task 4: Verification and delivery
- [x] Update README and contributor instructions with edition boundaries, removed features, config location and future Modrinth goal. Record known limits without claiming Hypixel approval.
- [x] Run buildSrc tests, Ship.Tests.ps1, backend check/tests, all three verifyTargetJar tasks and verifyDistributions with Java 25.
- [x] Review diff, ensure Cheat checkout remains clean, inspect all three artifacts. Report unavailable in-game QA honestly.
- [ ] Via browser: complete repository settings comparison, pin build to GitHub Actions after first run, create PR and verify aggregate checks. No release tags or Modrinth publication.

## Review focus
Old configs and remote reports cannot restore removed automation; target visibility changes between ticks cannot expose labels; particle/remote discovery cannot pre-consume local alert dedup; model-less predictions cannot bypass visibility; config migration cannot move files out of the cheat namespace. Each belongs to regression coverage in the relevant task.

## GitHub work completed
Collaborator invitation 345525815 to johnceoskyblock pending; Issues, auto-merge and branch deletion enabled. Master Ruleset 27841365 active, default branch protected, PR/resolution/strict build required, zero approvals, no bypass. build source pending GitHub Actions registration (currently any source). Actions enabled, all workflows allowed, read-only token, PR creation/approval off, first-time fork contributor approval required. Original private settings inaccessible. Dedicated browser control currently unavailable; desktop input currently conflicts with user input.

## Validation record

Final behavioral matrix: 1,683 tests per Minecraft target, zero failures/errors. All verifyTargetJar tasks and aggregate verifyDistributions passed. buildSrc tests, 11 shipping tests, 6 Discord workflow tests, backend TypeScript checking and 36 backend tests passed. Local Windows runs retain JUnit temp folders to avoid an intermittent Windows cleanup race; assertions are unchanged. CI uses normal Linux cleanup. Independent review findings on floor selection, invisible carriers, queued Hideyho dedup and sparkling HUD visibility were fixed and verified. In-game visual QA remains outstanding and no release is published.
