# Sparkling Mutuals edition consolidation

Status: proposed design, following the approved Phantom-style build approach.

## Intent and scope

Consolidate maintenance in johnceoskyblock/sparkling-mutuals. A feature or fix should need one PR, with shared Unique, Full Clear, Sparkle, backend synchronization, HUD and history code. Standard starts from the legit fork's b297adf implementation and tests. Full retains original upstream behavior. Existing separate checkouts and the legit fork remain intact until the consolidated release and Standard migration are verified.

## Approaches

The user approved one codebase with build-time edition selection, following Phantom Addons. Runtime-only switches cannot provide the requested absence of restricted code in Standard. Separate branches repeat the maintenance problem. Use Gradle source selection and small edition-specific implementations of shared interfaces; avoid duplicating entire tracking/rendering subsystems. Stonecutter continues to handle Minecraft version differences.

## Build and runtime boundaries

Java 25; Minecraft 26.1.2, 26.2 and 26.3. `-Pedition=standard|full|noauto`, default standard. Build output and task inputs must include edition so successive builds cannot reuse another edition's classes/resources. Each JAR embeds mod ID, edition, exact Minecraft target and version. Preserve mod ID sparkling-mutuals; install only one edition at a time.

Shared code owns progress, sync, history and data policies. Standard-specific render/discovery behavior comes from the legit implementation: current local sightings, camera/range/terrain checks, depth-aware geometry/text and queued alert revalidation. Standard excludes gameplay automation, through-terrain rendering and hidden-discovery code, hooks, settings, commands and mixins from compilation and packaging. Extract mixed-purpose files into common passive behavior and edition-specific behavior as needed. Inert compatibility properties are permitted only where essential to common data loading; they must not register settings or activate restricted behavior. Tests and artifact inspection enforce the boundary.

Full and noauto share the original gameplay features and existing Full configuration behavior. Full's existing filename remains `sparkling-mutuals-VERSION+mcTARGET.jar`; proposed new filenames are `sparkling-mutuals-standard-VERSION+mcTARGET.jar` and `sparkling-mutuals-full-noautoupdate-VERSION+mcTARGET.jar`. This naming is a builder proposal for deterministic compatibility selection. Standard keeps `config/sparkling-mutuals-legit`; Full/noauto keep the original namespace and migration behavior. Do not import or merge one namespace into another.

## Updating

Standard has no custom downloader or installer: its normal update channel is Modrinth. Noauto compiles without automatic update checks, downloader and installer; static installed-version display and bundled release highlights may remain. Full keeps check-only behavior by default. Add persistent Auto Update toggle (default false), Download Update button, progress, retryable error and Ready to install on restart status to the existing update screen. Enabling Auto Update permits background downloads and staging; it never replaces a loaded JAR in the game process.

Full selects only stable releases with a matching Full asset and exact Minecraft target. Use bounded background HTTP, HTTPS, trusted release asset hosts and timeouts. Download to a temporary staging file outside mods. A release manifest generated only after all nine artifacts pass verification lists edition, version, Minecraft target, filename, size and SHA-256. Fetch the manifest from the selected upstream release; reject ambiguous assets, missing/invalid entries, mismatches, oversized downloads and non-success responses. A checksum validates the selected published artifact, not independent publisher authenticity; the official GitHub release is the trust source. Do not execute downloaded code before successful validation.

Inspect the downloaded JAR's Fabric mod ID, embedded edition, Minecraft target and version, then compare its bytes with the manifest hash and size. Publish staging atomically only after validation. Interrupted downloads and verification failures leave the installed JAR untouched and allow retry. Persist a bounded pending-install record containing the current mod path, original hash, staged path/hash and expected identity; validate again at installation.

Use a small separately executable Java helper for restart installation on Windows. Start it during shutdown with exact paths and parent process identity; it waits for the game process to exit and the running JAR lock to release. Revalidate current/staged identity, refuse path traversal/symlinks or unexpected changed files, and replace only the exact currently loaded mod JAR. Keep the previous working JAR outside active mods for rollback. Use same-filesystem temporary moves, recoverable transaction state and rollback on swap failure; never delete the last working copy. Bound retries and leave a clear status record for the next launch. Do not relaunch Minecraft or modify other mods. Integration tests use an actual locked JAR on Windows and exercise process exit, interrupted staging, wrong-edition/version/target, changed installed file and failed swap recovery.

## Build, verification and release

Keep the original repository settings and master ruleset unchanged; preserve required check name `build`. CI tests all three editions on all three targets. Keep shared backend/shipping/build-logic checks. Verify exact distribution membership, metadata, mixin completeness and absence of restricted classes/references/settings in Standard, plus absence of updater implementation in noauto and custom updater in Standard. Missing, duplicate or cross-edition artifacts fail closed.

One tag and one release pipeline produce all nine JARs and the verified manifest. Publish six Full/noauto JARs and manifest to GitHub; publish three verified Standard JARs only to Modrinth. Standard publishing re-verifies the exact files immediately before upload and uses a separately scoped publish job. Upload credentials must not be available to PR builds. No tag or release is created during implementation. Test release planning with fixtures and publish only after explicit release readiness and required credentials/project details exist.

## Modrinth and migration gate

The retained Standard features are not certified by Hypixel or Modrinth. Preserve accurate disclosures of sync/network behavior and edition differences. Obtain Modrinth guidance on linking the shared repository before listing; do not infer that a separate JAR alone guarantees acceptance. Prepare a reviewable support message and listing material; sending a support inquiry requires the user's explicit instruction to message the service. Modrinth credentials/project ID are deployment inputs, not guessed values. Implement verification and publishing preparation independently of external approval.

After Standard is live and the consolidated release is verified, document upgrades from both existing editions, add a migration notice to the legit fork and archive it. Until those conditions hold, leave the fork active and preserve its existing files and updater destination. The user's approved consolidation supersedes earlier separate-repo routing only for this consolidation; update workspace guidance to describe the new edition boundaries when implementation lands.

## Validation and acceptance

Preserve existing tests; adapt edition-specific expectations without deleting shared coverage. Prove Standard parity with the legit fork's restrictions and Full/noauto parity with original gameplay code. All nine JARs must compile/test and pass artifact verification. Verify config isolation and upgrades from original Full and existing legit configs, Full check-only defaults, opt-in staging, retry and rollback. Perform Windows locked-JAR integration testing using disposable test directories, never the user's running Minecraft installation. Manual in-game QA of rendering and updater screen remains a release gate if a safe test instance is unavailable.

## Builder decisions

The builder may select focused interfaces, directory names, temporary-file formats, timeout/size limits and test harness mechanics. Keep the existing UI style and avoid broad visual redesign. Implement on an isolated consolidation branch and submit a PR to upstream; do not merge, release or archive as an incidental implementation step. Report external prerequisites and manual QA separately from passing automated checks.
