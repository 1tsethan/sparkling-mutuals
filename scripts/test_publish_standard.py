import hashlib
import json
from pathlib import Path
import tempfile
import unittest

from publish_standard import standard_uploads


class StandardUploadTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.folder = Path(self.temp.name)
        self.rows = []
        payload = b"fixture jar bytes"
        for edition, prefix in {
            "standard": "sparkling-mutuals-standard",
            "full": "sparkling-mutuals",
            "noauto": "sparkling-mutuals-full-noautoupdate",
        }.items():
            for target in ("26.1.2", "26.2", "26.3"):
                filename = f"{prefix}-1.2.3+mc{target}.jar"
                (self.folder / filename).write_bytes(payload)
                self.rows.append(dict(edition=edition, minecraft=target, name=filename,
                                      size=len(payload), sha256=hashlib.sha256(payload).hexdigest()))

    def manifest(self):
        (self.folder / "build-manifest.json").write_text(
            json.dumps(dict(version="1.2.3", files=self.rows)), encoding="utf-8")

    def test_only_exact_standard_targets_selected(self):
        self.manifest()
        uploads = standard_uploads(self.folder, "1.2.3")
        self.assertEqual([item.minecraft for item in uploads], ["26.1.2", "26.2", "26.3"])
        self.assertTrue(all(item.name.startswith("sparkling-mutuals-standard-") for item in uploads))

    def test_substituted_bytes_are_rejected_before_upload(self):
        self.manifest()
        (self.folder / self.rows[0]["name"]).write_bytes(b"wrong jar bytes")
        with self.assertRaisesRegex(ValueError, "digest"):
            standard_uploads(self.folder, "1.2.3")

    def test_renaming_full_manifest_entry_cannot_make_it_standard(self):
        self.rows[0]["edition"] = "full"
        self.rows[3]["edition"] = "standard"
        self.manifest()
        with self.assertRaisesRegex(ValueError, "name"):
            standard_uploads(self.folder, "1.2.3")
