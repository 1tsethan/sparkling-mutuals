"""Send only the verified Standard artifacts to the configured Modrinth project."""

from dataclasses import dataclass
import hashlib
import json
import os
from pathlib import Path
import re
import sys
from urllib.error import HTTPError
from urllib.request import HTTPRedirectHandler, Request, build_opener
from uuid import uuid4


TARGETS = ("26.1.2", "26.2", "26.3")
API = "https://api.modrinth.com/v2"


@dataclass(frozen=True)
class Upload:
    minecraft: str
    name: str
    content: bytes


def standard_uploads(directory: Path, version: str) -> list[Upload]:
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+(?:-beta\.[0-9]+)?", version):
        raise ValueError("Standard publishing requires a stable or beta version")
    manifest = json.loads((directory / "build-manifest.json").read_bytes())
    if manifest["version"] != version:
        raise ValueError("Manifest version does not match the release")
    expected = {target: f"sparkling-mutuals-standard-{version}+mc{target}.jar" for target in TARGETS}
    standard = [row for row in manifest["files"] if row["edition"] == "standard"]
    if len(standard) != len(TARGETS) or {row["minecraft"] for row in standard} != set(TARGETS):
        raise ValueError("Expected exactly one Standard artifact for each Minecraft target")
    artifacts = []
    for row in sorted(standard, key=lambda item: TARGETS.index(item["minecraft"])):
        target = row["minecraft"]
        name = expected[target]
        if row["name"] != name:
            raise ValueError(f"Unexpected Standard asset name for {target}")
        path = directory / name
        if path.is_symlink() or not 0 < path.stat().st_size <= 64 * 1024 * 1024:
            raise ValueError(f"Invalid Standard asset size or path for {target}")
        content = path.read_bytes()
        if len(content) != row["size"] or hashlib.sha256(content).hexdigest() != row["sha256"]:
            raise ValueError(f"Standard asset digest mismatch for {target}")
        artifacts.append(Upload(target, name, content))
    return artifacts


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def request(path, token, *, data=None, content_type=None):
    headers = {"Authorization": token, "User-Agent": "johnceoskyblock/sparkling-mutuals-publisher"}
    if content_type:
        headers["Content-Type"] = content_type
    try:
        with build_opener(NoRedirect()).open(Request(API + path, data=data, headers=headers), timeout=90) as response:
            body = response.read(8 * 1024 * 1024 + 1)
            if len(body) > 8 * 1024 * 1024:
                raise ValueError("Unexpected Modrinth response size")
            return json.loads(body)
    except HTTPError as failure:
        raise RuntimeError(f"Modrinth request failed with HTTP {failure.code}") from None


def upload_body(project, version, artifact):
    boundary = uuid4().hex
    data = {
        "project_id": project,
        "name": f"Sparkling Mutuals {version} for Minecraft {artifact.minecraft}",
        "version_number": f"{version}+mc{artifact.minecraft}",
        "version_type": "beta" if "-beta." in version else "release",
        "game_versions": [artifact.minecraft],
        "loaders": ["fabric"],
        "dependencies": [
            {"project_id": "P7dR8mSH", "dependency_type": "required"},
            {"project_id": "Ha28R6CL", "dependency_type": "required"},
        ],
        "featured": False,
        "file_parts": ["standard"],
        "primary_file": "standard",
    }
    prefix = (
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"data\"\r\n"
        f"Content-Type: application/json\r\n\r\n{json.dumps(data)}\r\n"
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"standard\"; filename=\"{artifact.name}\"\r\n"
        "Content-Type: application/java-archive\r\n\r\n"
    ).encode()
    return prefix + artifact.content + f"\r\n--{boundary}--\r\n".encode(), f"multipart/form-data; boundary={boundary}"


def publish(project, token, version, artifacts):
    existing = request(f"/project/{project}/version", token)
    pending = []
    for artifact in artifacts:
        number = f"{version}+mc{artifact.minecraft}"
        matches = [row for row in existing if row["version_number"] == number]
        if not matches:
            pending.append(artifact)
            continue
        expected = hashlib.sha512(artifact.content).hexdigest()
        if len(matches) != 1 or matches[0]["loaders"] != ["fabric"] or matches[0]["game_versions"] != [artifact.minecraft]:
            raise ValueError(f"Conflicting Modrinth version {number}")
        files = matches[0]["files"]
        if len(files) != 1 or files[0]["filename"] != artifact.name or files[0]["hashes"]["sha512"] != expected:
            raise ValueError(f"Conflicting Modrinth asset for {number}")
    for artifact in pending:
        body, content_type = upload_body(project, version, artifact)
        request("/version", token, data=body, content_type=content_type)
        print(f"Uploaded {artifact.name}")


def main(directory: Path):
    ref = os.environ.get("GITHUB_REF", "")
    if (os.environ.get("GITHUB_REPOSITORY") != "johnceoskyblock/sparkling-mutuals"
            or os.environ.get("GITHUB_EVENT_NAME") != "push" or not ref.startswith("refs/tags/v")):
        raise SystemExit("Standard publishing requires the upstream tag-push workflow")
    version = ref.removeprefix("refs/tags/v")
    project = os.environ.get("MODRINTH_PROJECT_ID", "")
    token = os.environ.get("MODRINTH_TOKEN", "")
    if not re.fullmatch(r"[A-Za-z0-9]{8}", project) or not token:
        raise SystemExit("Configure the Modrinth project ID and publishing token")
    publish(project, token, version, standard_uploads(directory, version))


if __name__ == "__main__":
    main(Path(sys.argv[1]))
