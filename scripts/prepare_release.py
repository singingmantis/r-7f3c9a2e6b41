"""Validate all compiled plugins and prepare this repository's download catalog."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import zipfile

MODULES = {'EvDiziBox': 'DiziBox', 'EvWebteIzle': 'WebteIzle', 'EvYabanciDizi': 'YabanciDizi', 'EvDiziGom': 'DiziGom', 'EvSezonlukDizi': 'SezonlukDizi', 'EvHDFilmCehennemi': 'HDFilmCehennemi', 'EvFilmModu': 'FilmModu', 'EvKultFilmler': 'KultFilmler', 'EvRareFilmm': 'RareFilmm', 'EvDiziPal': 'DiziPal', 'EvFilmMakinesi': 'FilmMakinesi', 'EvFullHDFilmizlesene': 'FullHDFilmizlesene'}


def prepare(root: Path, repository: str, commit: str, output: Path):
    if not re.fullmatch(r"[A-Za-z0-9-]+/[A-Za-z0-9_.-]+", repository):
        raise ValueError("Expected GitHub owner/repository")
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("Expected full source commit SHA")
    entries = json.loads((root / "build/plugins.json").read_text(encoding="utf-8"))
    if len(entries) != len(MODULES) or {p["internalName"] for p in entries} != set(MODULES):
        raise ValueError("Catalog must contain exactly the configured personal plugins")
    base = f"https://raw.githubusercontent.com/{repository}/builds"
    files = []
    for entry in entries:
        name = entry["internalName"]
        artifact = root / MODULES[name] / "build" / f"{name}.cs3"
        payload = artifact.read_bytes()
        digest = "sha256-" + hashlib.sha256(payload).hexdigest()
        if entry.get("fileHash") != digest or entry.get("fileSize") != len(payload):
            raise ValueError(f"Catalog/package mismatch: {name}")
        with zipfile.ZipFile(artifact) as archive:
            if "classes.dex" not in archive.namelist():
                raise ValueError(f"Missing Android bytecode: {name}")
            manifest = json.loads(archive.read("manifest.json"))
        if manifest["name"] != name or manifest["version"] != entry["version"]:
            raise ValueError(f"Manifest mismatch: {name}")
        if type(entry["version"]) is not int or entry["version"] < 1:
            raise ValueError(f"Invalid plugin version: {name}")
        entry["url"] = f"{base}/{name}.cs3"
        entry["repositoryUrl"] = f"https://github.com/{repository}"
        # This distribution targets Android; don't advertise unshipped JVM jars.
        for key in ("jarUrl", "jarHash", "jarFileSize"):
            entry.pop(key, None)
        files.append(artifact)
    output.mkdir(parents=True, exist_ok=True)
    for artifact in files:
        shutil.copy2(artifact, output / artifact.name)
    for filename in ("LICENSE", "ATTRIBUTION.md"):
        shutil.copy2(root / filename, output / filename)
    documents = {
        "repo.json": {
            "name": "Ev Arsivi",
            "description": "Kisisel film ve dizi eklentileri",
            "manifestVersion": 1,
            "pluginLists": [f"{base}/plugins.json"],
        },
        "plugins.json": entries,
        "source.json": {
            "repository": f"https://github.com/{repository}",
            "commit": commit,
            "source": f"https://github.com/{repository}/tree/{commit}",
            "license": "GPL-3.0",
        },
    }
    for filename, data in documents.items():
        (output / filename).write_text(
            json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
    print(f"Validated {len(entries)} plugins. Repo URL: {base}/repo.json")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--repository", required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--output", type=Path, default=Path("dist"))
    args = parser.parse_args()
    prepare(Path(__file__).resolve().parents[1], args.repository, args.commit, args.output)
