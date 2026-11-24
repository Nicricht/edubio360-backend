"""Select CI scope conservatively; unknown changes require full validation."""
import os
from pathlib import PurePosixPath
import re
import subprocess

DOC_EXTENSIONS = {".md", ".pdf", ".docx", ".xlsx", ".png", ".jpg", ".jpeg", ".svg", ".txt"}


def classify(paths):
    if not paths:
        return "full"
    modes = set()
    for path in paths:
        p = PurePosixPath(path)
        if path == "README.md" or (path.startswith("docs/") and p.suffix.lower() in DOC_EXTENSIONS):
            modes.add("docs")
        elif re.fullmatch(r"[^/]+/src/test/java/.+\.java", path):
            modes.add("fast")
        elif re.fullmatch(r"(academic|guidance|analytics|notification|import)-service/src/main/java/.+/(service|util)/[^/]+\.java", path):
            modes.add("fast")
        else:
            return "full"
    return "fast" if "fast" in modes else "docs"


def mode(event, ref, before, after):
    # A release, PR, or explicit dispatch must always exercise the full gates.
    if event != "push" or ref in {"refs/heads/main", "refs/heads/ep3-devops-cloud"}:
        return "full"
    if not re.fullmatch(r"[0-9a-f]{40}", before) or before == "0" * 40:
        return "full"
    try:
        result = subprocess.check_output(["git", "diff", "--name-only", "--no-renames", "-z", before, after])
    except subprocess.CalledProcessError:
        return "full"
    return classify([os.fsdecode(p) for p in result.split(b"\0") if p])


if __name__ == "__main__":
    print("mode=" + mode(os.getenv("GITHUB_EVENT_NAME", ""), os.getenv("GITHUB_REF", ""), os.getenv("BEFORE_SHA", ""), os.getenv("GITHUB_SHA", "")))
