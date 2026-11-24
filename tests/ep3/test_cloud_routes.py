import json
import unittest
from pathlib import Path


class CloudRoutesTest(unittest.TestCase):
    def test_listener_rules_respect_limits_and_route_backend(self):
        resources = json.loads(Path("cloud/infra.json").read_text())["Resources"]
        routes = set()
        priorities = set()
        for item in resources.values():
            if item["Type"] != "AWS::ElasticLoadBalancingV2::ListenerRule":
                continue
            props = item["Properties"]
            self.assertNotIn(props["Priority"], priorities)
            priorities.add(props["Priority"])
            count = 0
            for condition in props["Conditions"]:
                values = condition.get("Values", [])
                self.assertLessEqual(len(values), 3)
                count += len(values)
                if props["Actions"][0].get("TargetGroupArn") == {"Ref": "Targets"}:
                    routes.update(values)
            self.assertLessEqual(count, 5)
        self.assertEqual(routes, {"/api/*", "/openapi/*", "/swagger-ui*", "/v3/api-docs*", "/actuator/health"})
        self.assertLess(resources["MetricsPrivate"]["Properties"]["Priority"], min(p for p in priorities if p != resources["MetricsPrivate"]["Properties"]["Priority"]))
        self.assertEqual(resources["Listener"]["Properties"]["DefaultActions"][0]["TargetGroupArn"], {"Ref": "FrontendTargets"})


if __name__ == "__main__":
    unittest.main()
