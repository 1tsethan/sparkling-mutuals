# Multi-version builds

## Purpose

Build Standard, Full and Full No Auto Update from one Kotlin source tree across three Minecraft versions. Install one matching JAR.

## Prerequisites

| Exact Minecraft requirement | Full filename for mod version V | Fabric API | Bundled HM API |
| --- | --- | --- | --- |
| 26.1.2 | `sparkling-mutuals-V+mc26.1.2.jar` | 0.155.3+26.1.2 | 1.0.3+26.1 |
| 26.2 | `sparkling-mutuals-V+mc26.2.jar` | 0.161.0+26.2 | 1.0.4+26.2 |
| 26.3 | `sparkling-mutuals-V+mc26.3.jar` | 0.163.0+26.3 | 1.0.5+26.3 |

All targets require **Java 25**, **Fabric Loader 0.19.5+**, matching **Fabric API**, and **Fabric Language Kotlin 1.14.1+kotlin.2.4.20**. HM API is bundled; settings have no external UI dependency; DevAuth 1.2.2 is development-only. Do not install source, test or development JARs.

The 26.1 line targets **26.1.2 only**. Other patches are not advertised. **v3.4.0** was the first published three-target release. Each release uses the same mod version across all nine JARs. The filename and embedded metadata identify the edition and exact Minecraft target. The older single-target v3.3.0 release remains unchanged.

The native settings workspace replaces MoulConfig across all targets. Packaging permits only HM API as a nested runtime dependency.

## Procedure

### Build and test

Use the repository wrapper with Java 25. These examples use Windows; replace `./gradlew.bat` with `./gradlew` on Linux/macOS.

```powershell
./gradlew.bat clean buildAllTargets
./gradlew.bat buildEdition
./gradlew.bat buildEdition -Pedition=full
./gradlew.bat :26.2-noauto:test
./gradlew.bat :26.3:verifyTargetJar
./gradlew.bat :26.1.2:test --tests '*BeeNestSurveyTest'
./gradlew.bat testBuildLogic
./gradlew.bat verifyDistributions
```

`buildAllTargets` runs the shared feature tests for all nine combinations and the packaging and release tests. It validates each declared final `jar` task output, stages and validates copied bytes, then replaces `build/distributions/<mod-version>/` with nine installable JARs and `build-manifest.json`, which records each edition, filename, target, size and SHA-256 digest. An incomplete or invalid set cannot replace a previous complete distribution.

`verifyDistributions -PdistributionDir=incoming` validates downloaded artifacts without rebuilding them. It rejects wrong names, internal versions, Minecraft requirements, missing runtime resources, incompatible nested libraries and development dependencies. Per-target reports and artifacts remain under `versions/<target>[-full|-noauto]/build/`; helper tests report under `buildSrc/build/reports/tests/test/`.

### Develop and launch

26.1.2 Standard is the active editor version. Shared sources/resources/tests stay under root `src/`; generated target sources are ignored. Use small Stonecutter conditionals or `compat` adapters for demonstrated API changes. Use `standard` for local sighting behavior, `extended` for Full gameplay and `updater` for Full update code. Package-specific Java updater sources live in `src/updater/java`.

```powershell
./gradlew.bat :26.1.2:runClient
./gradlew.bat :26.2:runClient
./gradlew.bat :26.3:runClient
./gradlew.bat "Set active project to 26.2"
./gradlew.bat "Reset active project"
```

IntelliJ also exposes the Stonecutter switching and Loom client configurations. Reset the active project to 26.1.2 before committing. Client profiles are separate under `run/<edition>/<target>/`. DevAuth uses your local setup; keep credentials and authentication files out of Git. `-PclientSmoke` disables DevAuth and enables verbose mixin checks for local startup testing.

Build tools are pinned to Gradle 9.7.1, Stonecutter 0.9.8, Loom 1.17.21 and Kotlin 2.4.20. Shared properties live in `gradle.properties`; each `versions/<target>/gradle.properties` supplies its exact dependency matrix.

### CI and releases

Branch pushes and pull requests run read-only CI: helper tests, nine target jobs, a Windows updater test and aggregate distribution verification. Download the matching `jar-mc<target>-<edition>` Actions artifact. No PR or branch job publishes a release.

The required `build` status check succeeds only when the shipping, sync backend, build-helper, nine target, Windows updater, and distribution checks succeed. Failed or skipped dependencies keep that gate from passing.

World overlays use each target's native depth comparison without writing depth. On 26.3, line pipelines explicitly declare their translucent color attachment because the shared vanilla line snippet no longer supplies it; filled overlays retain the snippet's existing attachment. Pipeline contract tests and native client rendering cover this compatibility boundary.

For a release, choose a new `mod_version` and matching **`v<mod_version>`** tag. The tag workflow checks out the event's immutable commit, validates the tag, tests and builds all nine JARs and verifies the complete artifact set. Only the final publishing job has release write permission. It checks the tag still points at the tested commit and stages one draft before publishing.

Draft reruns retain maintainer-written release notes, generate notes only when blank, retain unrelated non-JAR assets, reuse matching uploads and replace only stale expected JAR assets. Unexpected or duplicate JAR assets stop publication. A failed upload leaves a draft; GitHub publication requires six matching Full and Full No Auto Update JARs and the verified manifest. Rerunning an identical published release does nothing; a different or unverifiable published asset set fails for manual attention. Tokens stay in the publishing environment and are never forwarded through redirects.

A separately gated Modrinth job verifies the distribution again before uploading three Standard JARs. See [shipping](SHIPPING.md) for the publication setup.

Put version notes and the complete change list in the matching GitHub Release description rather than duplicating them in this guide. Release publication is a separate authorized maintainer action.

### Add a target

Adding a target is a deliberate change: verify official Minecraft/Fabric/library availability, add its Stonecutter settings and properties, inspect actual mapped APIs/mixin signatures, update the verifier/release target set and both CI matrices, then run clean tests, packaging and client/in-game checks. Update this table and installation guidance only to the versions demonstrated by that evidence.

## Expected results

Target-qualified tests/builds, checked JAR metadata and nine-file distribution verification are separate checks. Read the actual run's reports before asserting a pass. Native startup/rendering verifies only the exercised client scenarios; live Hypixel tracking and capture physics remain separate. Optional Iris composition has historical native validation on 26.1.2 only, not a certification of every shader pack.

Source owners: [settings](../settings.gradle.kts), [controller and verification tasks](../stonecutter.gradle.kts), [shared build](../build.gradle.kts), [target properties](../versions), [packaging helpers](../buildSrc/src), [build workflow](../.github/workflows/build.yml), [release workflow](../.github/workflows/release.yml).

## Troubleshooting

Use Java 25 and the repository wrapper. A valid single-target JAR does not form a complete distribution. Do not substitute a development/source JAR or another Minecraft patch. Failed/skipped prerequisite jobs cannot satisfy the aggregate build gate. Release publication failures retain a draft for inspection; never claim a release from a branch build.

## Last updated

2026-11-23 — Added the three edition build matrix and publication destinations.

2026-11-01 — Native settings replace MoulConfig; packaging checks reject the old UI dependency.

2026-10-21 (America/New_York) — Updated the current three-target release to v3.5.0.

2026-10-19 (America/New_York) — Consolidated build guidance and retained v3.4.0 as the first published three-target release during the master rebase. Reviewed runtime source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; documentation-only changes, no local runtime differences or new live validation.

2026-11-02 — Native UI input uses target Minecraft constants. In 26.3, SDL left/right buttons are 1/3 and Return/Escape use scancodes, unlike the older GLFW values. Shared HUD/settings/history/Warp List handlers must not hardcode input numbers.

Distribution verification also compares feature-class inventories and all shared resources with 26.1.2. Only target metadata, nested platform libraries, compiled adapter bytecode and the named 26.1.2-only Iris captured-pose helper may differ. This detects omitted features or mismatched defaults/assets; native input and rendering still require client checks.
