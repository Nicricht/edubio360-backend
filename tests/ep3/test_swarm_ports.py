import json
import unittest
from pathlib import Path


class SwarmPortsTest(unittest.TestCase):
    def test_public_ports_use_one_task_per_node(self):
        for filename, name, port in (
            ("docker-stack.yml", "api-gateway", 8080),
            ("docker-stack.frontend.yml", "frontend", 8088),
        ):
            service = json.loads(Path(filename).read_text())["services"][name]
            self.assertEqual(service["deploy"]["mode"], "global")
            self.assertNotIn("replicas", service["deploy"])
            self.assertEqual(service["ports"][0]["mode"], "host")
            self.assertEqual(service["ports"][0]["published"], port)
            self.assertIn("backend", service["networks"])

    def test_database_and_business_services_remain_private(self):
        services = json.loads(Path("docker-stack.yml").read_text())["services"]
        for name, service in services.items():
            if name != "api-gateway":
                self.assertFalse(service.get("ports"), name)


if __name__ == "__main__":
    unittest.main()
