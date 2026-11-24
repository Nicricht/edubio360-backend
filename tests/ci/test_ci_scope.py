import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("ci_scope", Path(__file__).resolve().parents[2] / "scripts/ci-scope.py")
scope = importlib.util.module_from_spec(spec)
spec.loader.exec_module(scope)


class ScopeTest(unittest.TestCase):
    def test_docs_only(self):
        self.assertEqual(scope.classify(["README.md", "docs/manual.docx"]), "docs")

    def test_business_java_and_tests(self):
        self.assertEqual(scope.classify(["academic-service/src/main/java/cl/example/service/OfferService.java", "docs/manual.md"]), "fast")
        self.assertEqual(scope.classify(["auth-service/src/test/java/cl/example/AuthTest.java"]), "fast")

    def test_contract_security_infrastructure_and_unknown_require_full(self):
        for path in ["pom.xml", ".snyk", "Dockerfile.service", "docker-stack.yml", "cloud/infra.json", ".github/workflows/ci.yml", "scripts/ep3/browser-smoke.py", "auth-service/src/main/java/cl/example/service/AuthService.java", "api-gateway/src/main/java/cl/example/Filter.java", "academic-service/src/main/java/cl/example/controller/OfferController.java", "academic-service/src/main/resources/application.yml", "docs/script.py"]:
            with self.subTest(path=path):
                self.assertEqual(scope.classify(["README.md", path]), "full")

    def test_main_pr_dispatch_and_new_branch_full(self):
        sha = "a" * 40
        for event, ref, before in [("push", "refs/heads/main", sha), ("pull_request", "refs/pull/1/merge", sha), ("workflow_dispatch", "refs/heads/nico", sha), ("push", "refs/heads/nico", "0" * 40)]:
            self.assertEqual(scope.mode(event, ref, before, sha), "full")

    @patch.object(scope.subprocess, "check_output", return_value=b"README.md\0docs/manual.md\0")
    def test_push_compares_entire_range(self, diff):
        self.assertEqual(scope.mode("push", "refs/heads/nico", "a"*40, "b"*40), "docs")
        self.assertEqual(diff.call_args.args[0][-2:], ["a"*40, "b"*40])
