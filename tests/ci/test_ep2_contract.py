"""Behavioral contracts for the DOY0101 CI and deployment configuration."""

from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

import yaml


ROOT = Path(__file__).resolve().parents[2]


def read_yaml(path: str):
    with (ROOT / path).open(encoding="utf-8") as stream:
        return yaml.safe_load(stream)


def maven_modules():
    root = ET.parse(ROOT / "pom.xml").getroot()
    ns = {"pom": "http://maven.apache.org/POM/4.0.0"}
    return [element.text for element in root.findall("./pom:modules/pom:module", ns)]


class SecurityPipelineContract(unittest.TestCase):
    def test_dependabot_updates_all_maven_modules_and_actions(self):
        config = read_yaml(".github/dependabot.yml")
        self.assertEqual(config["version"], 2)
        updates = config["updates"]
        maven = next(item for item in updates if item["package-ecosystem"] == "maven")
        self.assertEqual(
            set(maven.get("directories", [])),
            {"/", *(f"/{module}" for module in maven_modules())},
        )
        self.assertTrue(any(item["package-ecosystem"] == "github-actions" for item in updates))

    def test_security_scans_code_and_aggregate_dependencies_without_masking_failures(self):
        workflow = read_yaml(".github/workflows/ci.yml")
        security = workflow["jobs"]["security"]
        self.assertIn("verify", security["needs"] if isinstance(security["needs"], list) else [security["needs"]])
        steps = security["steps"]
        commands = "\n".join(str(step.get("run", "")) for step in steps)
        self.assertIn("--maven-aggregate-project", commands)
        self.assertIn("snyk code test", commands)
        self.assertIn("--severity-threshold=high", commands)
        self.assertIn("SNYK_TOKEN", commands)
        self.assertNotIn("|| true", commands)
        self.assertTrue(all(step.get("continue-on-error") is not True for step in steps))


class ContainerContract(unittest.TestCase):
    def test_runtime_image_is_multistage_and_non_root(self):
        dockerfile = (ROOT / "Dockerfile.service").read_text(encoding="utf-8")
        self.assertIn("FROM maven:", dockerfile)
        self.assertIn("COPY --from=build", dockerfile)
        runtime = dockerfile.split("FROM eclipse-temurin:21-jre", 1)[1]
        self.assertIn("HEALTHCHECK", runtime)
        self.assertIn("/actuator/health", runtime)
        users = [line.split(maxsplit=1)[1] for line in runtime.splitlines() if line.startswith("USER ")]
        self.assertTrue(users, "The runtime stage must select a non-root user")
        self.assertNotIn(users[-1].split(":")[0], ("root", "0"))

    def test_compose_app_healthchecks_and_resource_constraints(self):
        services = read_yaml("docker-compose.yml")["services"]
        app_ports = {
            "discovery-server": 8761,
            "api-gateway": 8080,
            "auth-service": 8081,
            "academic-service": 8082,
            "guidance-service": 8083,
            "notification-service": 8084,
            "analytics-service": 8085,
            "import-service": 8086,
        }
        for name, port in app_ports.items():
            with self.subTest(service=name):
                service = services[name]
                self.assertEqual(service["build"]["args"]["HEALTH_PORT"], port)
                health = service["healthcheck"]
                self.assertIn(f"127.0.0.1:{port}/actuator/health", " ".join(health["test"]))
                limits = service["deploy"]["resources"]["limits"]
                reservations = service["deploy"]["resources"]["reservations"]
                self.assertIn("memory", limits)
                self.assertIn("cpus", limits)
                self.assertIn("memory", reservations)
                self.assertIn("cpus", reservations)
        for dependency in ("mysql", "rabbitmq"):
            with self.subTest(service=dependency):
                self.assertIn("healthcheck", services[dependency])
                self.assertIn("limits", services[dependency]["deploy"]["resources"])

    def test_optional_analytics_replica_avoids_published_port_collisions(self):
        services = read_yaml("docker-compose.yml")["services"]
        replica = services["analytics-replica"]
        primary = services["analytics-service"]
        self.assertIn("scale", replica["profiles"])
        self.assertEqual(replica["image"], primary["image"])
        self.assertEqual(replica["environment"], primary["environment"])
        self.assertNotIn("ports", replica)
        self.assertIn("healthcheck", replica)
        self.assertIn("limits", replica["deploy"]["resources"])


class DeliveryPipelineContract(unittest.TestCase):
    def test_image_build_and_deployment_are_gated_on_security(self):
        workflow = read_yaml(".github/workflows/ci.yml")
        jobs = workflow["jobs"]
        build = jobs["image-build"]
        deploy = jobs["deploy-smoke"]
        build_needs = build["needs"] if isinstance(build["needs"], list) else [build["needs"]]
        deploy_needs = deploy["needs"] if isinstance(deploy["needs"], list) else [deploy["needs"]]
        self.assertIn("security", build_needs)
        self.assertIn("verify", build_needs)
        self.assertIn("image-build", deploy_needs)
        self.assertIn("security", deploy_needs)
        self.assertIn("verify", deploy_needs)
        self.assertIn("push", build["if"])
        self.assertIn("push", deploy["if"])
        self.assertIn("github.sha", str(deploy["env"]["IMAGE_TAG"]))
        self.assertIn("github.sha", str(build["env"]["IMAGE_TAG"]))

    def test_deploy_collects_trace_and_logs_even_on_failure(self):
        jobs = read_yaml(".github/workflows/ci.yml")["jobs"]
        build = jobs["image-build"]
        deploy = jobs["deploy-smoke"]
        self.assertIn("docker build", "\n".join(str(s.get("run", "")) for s in build["steps"]))
        self.assertIn("scripts/smoke-mysql.sh", "\n".join(str(s.get("run", "")) for s in deploy["steps"]))
        uploads = [step for step in deploy["steps"] if step.get("uses", "").startswith("actions/upload-artifact@")]
        self.assertTrue(uploads)
        self.assertTrue(all(step.get("if") == "always()" for step in uploads))
        smoke = (ROOT / "scripts/smoke-mysql.sh").read_text(encoding="utf-8")
        self.assertIn("deployment-trace.txt", smoke)
        self.assertIn("docker compose images", smoke)

    def test_compose_images_are_tagged_by_commit(self):
        services = read_yaml("docker-compose.yml")["services"]
        for name, service in services.items():
            if name in ("mysql", "rabbitmq"):
                continue
            with self.subTest(service=name):
                self.assertIn("${IMAGE_TAG:-local}", service["image"])

    def test_smoke_deployment_checks_optional_replica(self):
        smoke = (ROOT / "scripts/smoke-mysql.sh").read_text(encoding="utf-8")
        self.assertIn("--profile scale", smoke)
        self.assertIn("analytics-replica", smoke)
        self.assertIn("--wait", smoke)


class AssignmentDocumentationContract(unittest.TestCase):
    def test_readme_explains_all_evidence_and_student_responsibilities(self):
        readme = (ROOT / "README.md").read_text(encoding="utf-8")
        self.assertIn("## EP2 DOY0101 — Ingeniería DevOps", readme)
        section = readme.split("## EP2 DOY0101 — Ingeniería DevOps", 1)[1]
        for item in (
            "IE1", "IE2", "IE3", "IE4", "IE5", "SNYK_TOKEN",
            "Dependabot", "Maven", "JaCoCo", "HEALTHCHECK",
            "docker compose", "IMAGE_TAG", "github.sha", "Actions",
            "artifacts", "uso de IA", "reflexiones individuales",
            "entorno simulado", "no persiste",
        ):
            with self.subTest(required=item):
                self.assertIn(item, section)


if __name__ == "__main__":
    unittest.main()
