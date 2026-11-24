import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
STACK = "edubio-devops-ep3"


class IsolatedDeliveryTest(unittest.TestCase):
    def test_unique_ecr_names(self):
        resources = json.loads((ROOT / "cloud/infra.json").read_text())["Resources"]
        repositories = [v["Properties"]["RepositoryName"] for v in resources.values()
                        if v["Type"] == "AWS::ECR::Repository"]
        self.assertEqual(len(repositories), 10)
        self.assertEqual(len(set(repositories)), 10)
        self.assertTrue(all(name.startswith(STACK + "/") for name in repositories))
        self.assertEqual(resources["Api"]["Properties"]["Name"], STACK)
        self.assertEqual(resources["Link"]["Properties"]["Name"], STACK)

    def test_explicit_deployment_gate(self):
        workflow = (ROOT / ".github/workflows/ep3-cloud.yml").read_text()
        self.assertIn("github.ref == 'refs/heads/ep3-devops-cloud'", workflow)
        self.assertIn("inputs.deploy_isolated_aws == true", workflow)
        self.assertIn("EP3_STACK: " + STACK, workflow)
        self.assertNotIn("EP3_STACK: edubio-ep3\n", workflow)
        self.assertIn("default: false", workflow)

    def test_scripts_reject_other_stacks(self):
        for name in ("provision.sh", "deploy-aws.sh"):
            script = (ROOT / "scripts/ep3" / name).read_text()
            self.assertIn('test "$EP3_STACK" = ' + STACK, script)
        self.assertIn('describe-stacks --stack-name "$EP3_STACK"',
                      (ROOT / "scripts/ep3/deploy-aws.sh").read_text())


if __name__ == "__main__":
    unittest.main()
