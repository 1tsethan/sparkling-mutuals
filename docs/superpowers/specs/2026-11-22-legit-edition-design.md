# Sparkling Mutuals legit edition

## Intent and agreed requirements
Keep the user-facing name Sparkling Mutuals. Use the existing public fork https://github.com/1tsethan/sparkling-mutuals, owned by 1tsethan, and the local Legit/sparkling-mutuals checkout. Johnceoskyblock receives a collaborator invitation with write access. Cheat-only changes stay in Cheat/sparkling-mutuals. The legit edition must follow Hypixel rules; the supplied handoff is a specification, not evidence of Hypixel approval. Modrinth publication is a future goal, not part of this initial publication authorization.

## Approach
Use targeted changes to the current upstream code, on branch legit-edition. Preserve existing history and submit a PR into master. The existing owner fork has no unique commits and is 580 commits behind upstream at inspection. Do not overwrite or force-push master. A rewrite would risk breaking the Safari behavior the user wants preserved. A runtime toggle retaining automation would violate the handoff requirement that old config and sync cannot reactivate it.

## Gameplay and presentation
Remove auto-party, auto-warp, reparty, auto-entry, ticket selection, auto-clicking, gem selection, quest acceptance, leech auto-accept, reconnect and automatic SkyBlock joining. Audit movement, aiming, hotbar selection, inventory/menu clicks, and server-bound commands, removing automatic gameplay actions and their settings and hooks. Preserve intentional manual commands where permitted; distinguish passive sync from automated party management.

Keep Unique, Full Clear and Sparkle mode logic, mode gates, completion requirements, objective tracking, backend sync, shared progress, biome completion, and hotspot assignments. Restrict target presentation and discovery alerts at their output boundaries without suppressing shared state updates.

Every retained entity marker must require a loaded target, normal render distance, current local visibility and terrain occlusion, including glow, boxes, labels, teammates, drops, structures, predicted positions and last-seen locations. Remote reports, particles and caches may update internal state but cannot independently reveal locations or trigger discovery alerts. Assess whether current local identification is sufficient for each alert.

### Proposed conservative compliance decisions requiring review
The handoff's line-of-sight rules are necessary but do not prove compliance. Hypixel explicitly disallows automation and extra information unavailable through normal play. Remove candle interaction hitbox expansion and clear-water modifications. Audit capsule prediction, hidden structure information, player distance/health indicators and overlays; remove features that do not clearly fit the allowed categories. Prefer cosmetic highlighting of already visible and locally identifiable targets without new hidden information. If a requested preserved feature conflicts with published rules, document the conflict before implementing it. Do not describe the result as Hypixel-approved.

## Identity and distribution
Keep Sparkling Mutuals as the display name and preserve upstream license, credits, assets and Kotlin/Java structure. Use config/sparkling-mutuals-legit as a separate storage namespace with no automatic import of cheat or legacy config. Keep the current Fabric mod ID unless implementation evidence requires otherwise; only one edition should be installed at once. Point updater, release workflow, buildSrc verification, Ship.ps1 and mod sources/issues links to 1tsethan/sparkling-mutuals. Audit hardcoded references by purpose; preserve upstream credits and protocol/asset identifiers where needed.

Preserve Java 25, Gradle wrapper, Stonecutter and 26.1.2/26.2/26.3 matrix, aggregate build check, and tag-based release workflow verifying three JARs. Do not create a release tag or publish JARs during setup. For future Modrinth distribution preserve version-specific artifacts and dependency metadata; no Modrinth project, token, auto-upload or publishing workflow is created now.

## GitHub configuration
Inspect source settings and rulesets and compare after configuration. Public visibility, master default, Issues/Projects/Wiki enabled, Discussions off. Enable all three merge methods, auto-merge and merged-branch deletion. Disable branch update suggestions and web sign-off. Mirror source merge/squash message defaults. Master Ruleset: active on default branch, no bypass actors, deletion/force-push blocked, PR required, conversations resolved, zero approvals, up-to-date passing GitHub Actions build check, all three merge methods allowed. Enable Actions and all actions/reusable workflows; default permissions read-only, no Actions PR creation/approval, approval for first-time contributors' fork workflows. No deployment environments. Credentials, webhooks and secrets require separate handling and must never be copied from upstream.

## Verification and completion
Add meaningful behavioral tests for occlusion, unloaded/out-of-range targets, delayed first sighting, remote-only detection, legacy config isolation and automation activation paths. Retain and run mode/completion/sync regression tests. Run buildSrc checks, backend checks/tests, Ship script tests, tests and verifyTargetJar for all three versions, and verifyDistributions. Inspect artifacts and verify the PR aggregate build result. In-game rendering must be tested in a suitable Minecraft client; disclose if unavailable rather than claim visual verification from unit tests. Verify collaborator invitation status and actual GitHub settings separately. Do not claim setup complete with missing authorization, failing checks or unresolved rule conflicts.

## Builder discretion
Choose visibility/raycast implementation using existing APIs for all three versions, avoid stale per-frame state, and share a presentation policy where it reduces inconsistencies. Choose test seams appropriate to the existing suite. Preserve backend protocol compatibility. Existing GitHub integration can read but cannot manage collaborator/settings; the Composio GitHub connection is pending authorization. No production mod changes have been made yet.
