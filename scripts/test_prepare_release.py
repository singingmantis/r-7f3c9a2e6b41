"""Packaging checks use synthetic ZIPs; they do NOT test provider playback."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

from prepare_release import MODULES, prepare


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "build").mkdir()
        for doc in ("LICENSE", "ATTRIBUTION.md"):
            (self.root / doc).write_text("test fixture", encoding="utf-8")
        self.entries = []
        for name, directory in MODULES.items():
            target = self.root / directory / "build" / f"{name}.cs3"
            target.parent.mkdir(parents=True)
            with zipfile.ZipFile(target, "w") as archive:
                archive.writestr("classes.dex", b"synthetic test data")
                archive.writestr("manifest.json", json.dumps({"name": name, "version": 1001}))
            data = target.read_bytes()
            self.entries.append({
                "internalName": name, "version": 1001,
                "url": "https://old.invalid/plugin.cs3",
                "fileHash": "sha256-" + hashlib.sha256(data).hexdigest(),
                "fileSize": len(data),
            })

    def run_prepare(self):
        (self.root / "build/plugins.json").write_text(json.dumps(self.entries), encoding="utf-8")
        prepare(self.root, "singingmantis/example", "a" * 40, self.root / "out")

    def test_catalog_points_to_personal_repo(self):
        self.run_prepare()
        entries = json.loads((self.root / "out/plugins.json").read_text())
        self.assertEqual(len(list((self.root / "out").glob("*.cs3"))), len(MODULES))
        for entry in entries:
            self.assertEqual(entry["url"], f'https://raw.githubusercontent.com/singingmantis/example/builds/{entry["internalName"]}.cs3')

    def test_rejects_tampered_package(self):
        self.entries[0]["fileHash"] = "sha256-invalid"
        with self.assertRaisesRegex(ValueError, "mismatch"):
            self.run_prepare()

    def test_rejects_extra_provider(self):
        self.entries.append(dict(self.entries[0]))
        with self.assertRaisesRegex(ValueError, "exactly"):
            self.run_prepare()

    def test_rejects_version_mismatch(self):
        self.entries[0]["version"] += 1
        with self.assertRaisesRegex(ValueError, "Manifest mismatch"):
            self.run_prepare()

    def test_rejects_missing_catalog_entry(self):
        self.entries.pop()
        with self.assertRaisesRegex(ValueError, "exactly"):
            self.run_prepare()

    def test_project_configuration_matches_catalog(self):
        project = Path(__file__).resolve().parents[1]
        configured = {line.split("=", 1)[0] for line in (project / "sites.properties").read_text().splitlines() if line.startswith("Ev")}
        self.assertEqual(configured, set(MODULES))
        settings = (project / "settings.gradle.kts").read_text()
        for name, directory in MODULES.items():
            self.assertIn('include("' + name + '")', settings)
            self.assertTrue((project / directory / "build.gradle.kts").is_file())

    def test_rejects_missing_package(self):
        (self.root / 'DiziBox/build/EvDiziBox.cs3').unlink()
        with self.assertRaises(FileNotFoundError):
            self.run_prepare()


if __name__ == "__main__":
    unittest.main()
