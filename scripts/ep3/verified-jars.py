"""Prepare and verify the executable JARs produced by the successful Maven job."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import zipfile

MODULES = (
    "discovery-server", "auth-service", "academic-service", "guidance-service",
    "notification-service", "analytics-service", "import-service", "api-gateway",
)
ROOT = Path(__file__).resolve().parents[2]
BUNDLE = ROOT / "verified-jars"


def sha256(path):
    with path.open("rb") as file:
        return hashlib.file_digest(file, "sha256").hexdigest()


def executable(path):
    if not path.is_file() or path.is_symlink():
        raise ValueError(f"Missing or invalid JAR: {path}")
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if not any(name.startswith("BOOT-INF/classes/") for name in names):
            raise ValueError(f"Not an executable Spring Boot JAR: {path}")
        manifest = archive.read("META-INF/MANIFEST.MF")
        if b"Main-Class:" not in manifest or b"Start-Class:" not in manifest:
            raise ValueError(f"Missing Spring Boot entry point: {path}")


def prepare(commit, root=ROOT, bundle=BUNDLE):
    if bundle.exists():
        raise ValueError("Artifact directory already exists; refusing stale files")
    bundle.mkdir()
    files = {}
    for module in MODULES:
        candidates = sorted((root / module / "target").glob("*.jar"))
        if len(candidates) != 1:
            raise ValueError(f"Expected exactly one JAR for {module}: {candidates}")
        executable(candidates[0])
        dest = bundle / module / "app.jar"
        dest.parent.mkdir()
        shutil.copyfile(candidates[0], dest)
        files[module] = sha256(dest)
    (bundle / "manifest.json").write_text(
        json.dumps({"commit": commit, "sha256": files}, indent=2) + "\n"
    )


def check(commit, bundle=BUNDLE):
    data = json.loads((bundle / "manifest.json").read_text())
    if data["commit"] != commit or set(data["sha256"]) != set(MODULES):
        raise ValueError("Artifact commit or module list does not match")
    for module in MODULES:
        path = bundle / module / "app.jar"
        executable(path)
        if sha256(path) != data["sha256"][module]:
            raise ValueError(f"Checksum mismatch: {module}")
    print(f"Verified {len(MODULES)} executable JARs from {commit}")


if __name__ == "__main__":
    commit = os.environ["GITHUB_SHA"]
    if len(commit) != 40 or any(c not in "0123456789abcdef" for c in commit):
        raise ValueError("GITHUB_SHA must be an immutable commit SHA")
    commands = {"prepare": prepare, "check": check}
    commands[sys.argv[1]](commit)
