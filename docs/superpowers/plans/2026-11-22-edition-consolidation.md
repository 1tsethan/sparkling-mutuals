# Edition Consolidation Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement and verify each task.

**Goal:** Build and verify three editions from the upstream repository.

**Architecture:** Stonecutter handles Minecraft versions. Gradle selects the edition, and source selection keeps edition-only code out of other artifacts. The release pipeline consumes a verified nine-artifact distribution.

**Tech stack:** Java 25, Kotlin, Fabric, Gradle and Stonecutter.

**Spec:** [Edition consolidation](../specs/2026-11-22-edition-consolidation-design.md)

## Global constraints

- Minecraft 26.1.2, 26.2 and 26.3. Keep required check `build`.
- Standard is the default. Standard uses the legit config directory and the local sighting rules from b297adf. Full and noauto retain upstream config compatibility.
- Full keeps the legacy artifact filename. All artifacts identify edition and exact Minecraft target.
- A tag, review by Modrinth and successful release verification precede migration and fork archival.

## Review focus

- Switching edition in a dirty build directory must produce the same artifact contents as a fresh build.
- Legacy automation settings must remain ineffective in Standard.
- A wrong or interrupted download must preserve the installed mod.
- An immediate restart with a locked JAR must preserve its last working copy.
- Package selection must reject a Standard artifact sent to GitHub or a Full artifact sent to Modrinth.

## 1. Edition builds and Standard parity

- [x] Add a package test that checks edition metadata, retained Standard local-visibility classes, missing native automation and required Full automation classes. Run it against the current artifact and observe the failure.
- [x] Implement edition selection in the Stonecutter build. Derive the runtime metadata, build path and filenames from the same selection.
- [x] Integrate the legit changes under `src/main` and `src/test`, using narrowly scoped edition source selection for divergent code. Preserve shared mode, progress and sync logic.
- [x] Run each edition on 26.1.2, then verify and test all nine artifacts. Include switching back to Standard after building Full in the same checkout.

## 2. Full updater

- [x] Test artifact identity and manifest verification, download cancellation and filesystem transaction recovery. Add a Windows child-process fixture holding the current JAR open.
- [x] Extend `ReleaseUpdateChecker` and `ModUpdates` with exact Full asset/manifest selection, background downloads and staged install state. Only Full packages this runtime.
- [x] Add the restart helper and connect it to Full shutdown. Wait for the parent to exit, revalidate paths and hashes, save the rollback JAR outside mods and atomically replace the original.
- [x] Update `UpdateDetailsScreen` with opt-in Auto Update, Download Update, progress, retry and restart states. Keep release highlights available in all editions.
- [x] Exercise the matching and mismatched upgrade paths, then verify noauto and Standard contain no Full updater implementation.

## 3. Distribution and release

- [x] Extend `DistributionVerifier` tests for wrong edition metadata, duplicate/missing artifacts, forbidden classes and manifest drift.
- [x] Verify exactly one artifact per edition and target. Generate the release manifest from the verified files. Route six GitHub assets and three Modrinth assets explicitly.
- [x] Extend `.github/workflows/build.yml` and `.github/workflows/release.yml` to the edition matrix. Preserve the aggregate `build` check. Reverify Standard directly before upload and keep the Modrinth job behind its configured gate.
- [x] Run the complete test matrix, shipping and backend checks. Document upgrade and release guidance, review the branch and prepare the upstream PR.
- [ ] Obtain Modrinth guidance and finish live release and migration once prerequisites are met.
