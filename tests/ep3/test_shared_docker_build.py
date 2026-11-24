import os
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]


class SharedDockerBuildTest(unittest.TestCase):
    def test_shared_reactor_is_independent_of_module(self):
        source = (ROOT / "Dockerfile.service").read_text()
        shared = source.split("FROM sources AS build-all", 1)[1].split("FROM ${PACKAGE_STAGE} AS build", 1)[0]
        self.assertNotIn("ARG MODULE", shared)
        self.assertIn("clean package", shared)
        self.assertNotIn("-pl", shared)
        self.assertIn("ARG PACKAGE_STAGE=build-single", source)
        self.assertIn(' -pl "${MODULE}"', source)
        self.assertIn("USER 10001:10001", source)
        self.assertIn("HEALTHCHECK", source)
        self.assertIn("COPY --from=build", source)

    def run_build(self, fail=""):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            mock = folder / "docker"
            mock.write_text("""#!/bin/bash
printf '%s\\n' "$*" >> "$DOCKER_LOG"
if [[ "$1" == "image" ]]; then
  if [[ -n "$FAIL_IMAGE" && "$*" == *"$FAIL_IMAGE"* ]]; then
    echo root
  else
    echo 10001:10001
  fi
fi
""")
            mock.chmod(0o755)
            log = folder / "calls"
            env = dict(os.environ, PATH=str(folder)+os.pathsep+os.environ["PATH"],
                       DOCKER_LOG=str(log), FAIL_IMAGE=fail, PUSH_IMAGES="true",
                       BUILD_JOBS="2", IMAGE_PREFIX="test", IMAGE_TAG="commit")
            result = subprocess.run(["bash", "scripts/ep3/build-images.sh"], cwd=ROOT,
                                    env=env, capture_output=True, text=True)
            return result, log.read_text().splitlines()

    def test_all_images_share_build_and_are_checked_before_publishing(self):
        result, calls = self.run_build()
        self.assertEqual(result.returncode, 0, result.stderr)
        java = [line for line in calls if line.startswith("build -f")]
        self.assertEqual(len(java), 8)
        self.assertIn("MODULE=discovery-server", java[0])
        self.assertTrue(all("PACKAGE_STAGE=build-all" in line for line in java))
        self.assertEqual(sum(line.startswith("image inspect") for line in calls), 8)
        self.assertEqual(sum(line.startswith("push ") for line in calls), 9)
        first_push = next(i for i, line in enumerate(calls) if line.startswith("push "))
        self.assertFalse(any(line.startswith(("build ", "image inspect")) for line in calls[first_push:]))

    def test_failed_non_root_check_blocks_all_publication(self):
        result, calls = self.run_build("academic-service")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any(line.startswith("push ") for line in calls))


if __name__ == "__main__":
    unittest.main()
