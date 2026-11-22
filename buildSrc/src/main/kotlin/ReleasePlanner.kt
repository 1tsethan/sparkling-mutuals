package buildlogic

data class ReleaseAsset(val id: Long, val name: String, val size: Long, val sha256: String?)
data class ExistingRelease(val draft: Boolean, val body: String?, val assets: List<ReleaseAsset>, val prerelease: Boolean = false)
enum class ReleaseDecision { CREATE_DRAFT, UPDATE_DRAFT, PUBLISHED_NOOP }
data class ReleasePlan(val decision: ReleaseDecision, val preserveBody: Boolean)
data class RemoteRelease(val id: Long, val state: ExistingRelease)
interface ReleaseApi {
    fun find(tag: String): RemoteRelease?
    fun createDraft(tag: String, body: String): RemoteRelease
    fun generatedNotes(tag: String): String
    fun updateBody(id: Long, body: String)
    fun deleteAsset(id: Long)
    fun upload(id: Long, file: VerifiedAsset): ReleaseAsset
    fun publish(id: Long)
}
fun planRelease(tag: String, modVersion: String, files: List<VerifiedJar>, manifest: VerifiedFile, existing: ExistingRelease?): ReleasePlan {
    val prerelease = prereleaseForTag(tag)
    require(tag == "v$modVersion") { "Tag must match the internal mod version" }
    val expected = githubEditions.flatMap { edition -> SUPPORTED_TARGETS.map { edition to it } }.toSet()
    require(files.size == expected.size && files.map { it.edition to it.minecraft }.toSet() == expected) {
        "GitHub publication requires exactly six verified Full/noauto JARs"
    }
    files.forEach {
        require(it.modVersion == modVersion && it.path.fileName.toString() == distributionJarName(it.edition, modVersion, it.minecraft) &&
            it.isValid()) { "Invalid verified target: ${it.edition} ${it.minecraft}" }
    }
    require(manifest.path.fileName.toString() == MANIFEST_NAME && manifest.isValid()) { "Invalid verified distribution manifest" }
    if (existing == null) return ReleasePlan(ReleaseDecision.CREATE_DRAFT, false)
    require(existing.prerelease == prerelease) { "Release prerelease classification does not match its tag; review it manually" }
    val owned = existing.assets.filter { it.name.endsWith(".jar", ignoreCase = true) || it.name.equals(MANIFEST_NAME, ignoreCase = true) }
    val expectedAssets = files + manifest
    val names = expectedAssets.map { it.path.fileName.toString() }.toSet()
    require(owned.all { it.name in names } && owned.map { it.name }.distinct().size == owned.size) {
        "Unexpected or duplicate distribution assets; review them manually"
    }
    if (!existing.draft) {
        require(expectedAssets.all { file -> owned.singleOrNull { it.name == file.path.fileName.toString() }?.matches(file) == true }) {
            "Published release differs from the verified distribution; it will not be modified"
        }
        return ReleasePlan(ReleaseDecision.PUBLISHED_NOOP, !existing.body.isNullOrBlank())
    }
    return ReleasePlan(ReleaseDecision.UPDATE_DRAFT, !existing.body.isNullOrBlank())
}

internal fun prereleaseForTag(tag: String): Boolean {
    require(tag.matches(Regex("v[0-9]+\\.[0-9]+\\.[0-9]+(?:-beta\\.[0-9]+)?"))) { "Use a stable vX.Y.Z or beta vX.Y.Z-beta.N tag" }
    return tag.contains("-beta.")
}

private val sha256Pattern = Regex("[a-f0-9]{64}")
private fun VerifiedAsset.isValid() = size > 0 && sha256.matches(sha256Pattern)
private fun ReleaseAsset.matches(file: VerifiedAsset) = name == file.path.fileName.toString() &&
    size == file.size && sha256?.removePrefix("sha256:") == file.sha256

/** Only a complete, verified draft can cross the final publish boundary. */
fun publishVerifiedRelease(api: ReleaseApi, tag: String, modVersion: String, files: List<VerifiedJar>, manifest: VerifiedFile): ReleaseDecision {
    planRelease(tag, modVersion, files, manifest, null)
    var remote = api.find(tag)
    val initial = planRelease(tag, modVersion, files, manifest, remote?.state)
    if (initial.decision == ReleaseDecision.PUBLISHED_NOOP) return initial.decision
    if (remote == null) remote = api.createDraft(tag, api.generatedNotes(tag))
    val releaseId = remote.id
    fun draft(): RemoteRelease {
        val current = requireNotNull(api.find(tag)) { "Release disappeared during staging" }
        require(current.id == releaseId && current.state.draft) { "Release changed or was published during staging" }
        planRelease(tag, modVersion, files, manifest, current.state)
        return current
    }
    if (draft().state.body.isNullOrBlank()) {
        val notes = api.generatedNotes(tag)
        // Generating notes can take time; do not overwrite notes written meanwhile.
        if (draft().state.body.isNullOrBlank()) api.updateBody(releaseId, notes)
    }
    val expectedAssets = files + manifest
    for (file in expectedAssets) {
        val asset = draft().state.assets.singleOrNull { it.name == file.path.fileName.toString() }
        if (asset?.matches(file) == true) continue
        if (asset != null) { draft(); api.deleteAsset(asset.id) }
        draft()
        require(api.upload(releaseId, file).matches(file)) { "Uploaded asset digest/size mismatch" }
    }
    val ready = draft()
    require(expectedAssets.all { file -> ready.state.assets.singleOrNull { it.name == file.path.fileName.toString() }?.matches(file) == true }) {
        "Draft must contain all six matching JARs and the manifest"
    }
    api.publish(releaseId)
    val published = requireNotNull(api.find(tag)) { "Published release could not be read back" }
    require(published.id == releaseId && !published.state.draft) { "Release publication could not be confirmed" }
    planRelease(tag, modVersion, files, manifest, published.state)
    return initial.decision
}
