import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile

SCRIPT = Path(__file__).resolve().parents[2] / "scripts/ep3/verified-jars.py"
spec = importlib.util.spec_from_file_location("verified_jars", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class VerifiedJarsTest(unittest.TestCase):
    def test_exact_commit_and_checksum(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            bundle = root / "verified-jars"
            for name in module.MODULES:
                target = root / name / "target"
                target.mkdir(parents=True)
                with zipfile.ZipFile(target / "test.jar", "w") as jar:
                    jar.writestr("META-INF/MANIFEST.MF", "Main-Class: Launcher\nStart-Class: App\n")
                    jar.writestr("BOOT-INF/classes/App.class", b"test")
            module.prepare("a" * 40, root, bundle)
            module.check("a" * 40, bundle)
            with self.assertRaises(ValueError):
                module.check("b" * 40, bundle)
            with zipfile.ZipFile(bundle / module.MODULES[0] / "app.jar", "a") as jar:
                jar.writestr("tampered.txt", "changed")
            with self.assertRaises(ValueError):
                module.check("a" * 40, bundle)


if __name__ == "__main__":
    unittest.main()
