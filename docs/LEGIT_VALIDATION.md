# Initial legit edition validation

Validated on 2026-11-22 using Java 25:

- Minecraft 26.1.2, 26.2 and 26.3: 1,683 tests each, zero failures/errors, verifyTargetJar passed.
- verifyDistributions: all three installable JARs verified.
- buildSrc tests passed.
- 11 shipping-script tests passed using local fixtures without real GitHub/HTTP writes.
- 6 Discord workflow sender tests passed using fixtures.
- Backend TypeScript check passed and 36 tests passed.
- Packaged adapters for automatic clicks, movement/aiming, entry, gem selection and quest acceptance are absent; Fabric metadata points at 1tsethan/sparkling-mutuals.
- Independent review issues were addressed: preserved floor selection, visible names/equipped heads on invisible carriers, output-time sparkling identification, and delayed Hideyho visibility/dedup.

Local Windows testing uses junit.jupiter.tempdir.cleanup.mode.default=NEVER to retain fixture directories and avoid intermittent Windows directory-removal failures. CI retains ordinary cleanup on Linux; no test assertions are skipped. Local logs are in ignored build/verification.

In-game visual QA remains required for partial occlusion, invisible name carriers, third-person view and version-specific renderer behavior. No release tag, GitHub release or Modrinth publication has been created. The legit edition is not Hypixel-endorsed or approved.
