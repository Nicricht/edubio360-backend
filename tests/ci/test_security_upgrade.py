from pathlib import Path
from datetime import datetime, timedelta, timezone
import unittest
import xml.etree.ElementTree as ET
import yaml


ROOT = Path(__file__).resolve().parents[2]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


class SpringSecurityUpgradeContract(unittest.TestCase):
    def test_reactor_uses_boot4_compatible_maintained_dependencies(self):
        pom = ET.parse(ROOT / "pom.xml").getroot()
        boot = pom.findtext("m:parent/m:version", namespaces=NS)
        self.assertIsNotNone(boot)
        self.assertGreaterEqual(tuple(map(int, boot.split("."))), (4, 0, 9))

        properties = pom.find("m:properties", namespaces=NS)
        self.assertEqual(properties.findtext("m:spring-cloud.version", namespaces=NS), "2025.1.4")
        self.assertGreaterEqual(
            tuple(map(int, properties.findtext("m:springdoc.version", namespaces=NS).split("."))),
            (3, 1, 0),
        )
        self.assertGreaterEqual(
            tuple(map(int, properties.findtext("m:resilience4j.version", namespaces=NS).split("."))),
            (2, 4, 0),
        )

    def test_guidance_uses_the_matching_boot4_resilience4j_starter(self):
        pom = ET.parse(ROOT / "guidance-service" / "pom.xml").getroot()
        ids = [d.text for d in pom.findall(".//m:dependencies/m:dependency/m:artifactId", NS)]
        self.assertIn("resilience4j-spring-boot4", ids)
        self.assertNotIn("resilience4j-spring-boot3", ids)

    def test_reactor_pins_remediated_versions_for_vulnerable_transitives(self):
        pom = ET.parse(ROOT / "pom.xml").getroot()
        overrides = {}
        for dependency in pom.findall("m:dependencyManagement/m:dependencies/m:dependency", NS):
            group = dependency.findtext("m:groupId", namespaces=NS)
            artifact = dependency.findtext("m:artifactId", namespaces=NS)
            version = dependency.findtext("m:version", namespaces=NS)
            overrides[(group, artifact)] = version

        expected = {
            ("com.google.guava", "guava"): "33.7.2-jre",
            ("org.apache.httpcomponents.client5", "httpclient5"): "5.6.4",
            ("org.apache.httpcomponents.core5", "httpcore5"): "5.4.3",
            ("org.apache.httpcomponents.core5", "httpcore5-h2"): "5.4.3",
            ("org.bouncycastle", "bcprov-jdk18on"): "1.86",
            ("com.rabbitmq", "amqp-client"): "5.34.0",
        }
        for dependency, fixed_version in expected.items():
            with self.subTest(dependency=dependency):
                self.assertEqual(overrides.get(dependency), fixed_version)


class TemporaryJaxbExceptionContract(unittest.TestCase):
    def test_only_authorized_cve_is_temporarily_excepted_and_expires(self):
        policy_path = ROOT / ".snyk"
        self.assertTrue(policy_path.is_file(), "La política temporal de Snyk debe existir")
        policy = yaml.safe_load(policy_path.read_text(encoding="utf-8"))
        self.assertEqual(policy["version"], "v1.25.0")
        self.assertEqual(list(policy["ignore"]), ["SNYK-JAVA-ORGGLASSFISHJAXB-20883142"])
        exceptions = policy["ignore"]["SNYK-JAVA-ORGGLASSFISHJAXB-20883142"]
        self.assertEqual(len(exceptions), 1)
        self.assertEqual(list(exceptions[0]), ["*"])
        details = exceptions[0]["*"]
        self.assertIn("CVE-2026-92877", details["reason"])
        self.assertIn("JAXP", details["reason"])
        self.assertEqual(details["expires"], "2026-11-09T23:59:59.000Z")
        expires = datetime.fromisoformat(details["expires"].replace("Z", "+00:00"))
        now = datetime.now(timezone.utc)
        self.assertGreater(expires, now, "Excepción vencida: CI debe bloquear el despliegue")
        self.assertLessEqual(expires - now, timedelta(days=15))

    def test_security_gate_still_scans_everything_else(self):
        workflow = yaml.safe_load((ROOT / ".github/workflows/ci.yml").read_text(encoding="utf-8"))
        security = workflow["jobs"]["security"]
        scan = next(s for s in security["steps"] if s.get("name") == "Snyk Open Source - Maven reactor")
        self.assertIn("--policy-path=.snyk", scan["run"])
        self.assertIn("--severity-threshold=high", scan["run"])
        self.assertNotIn("--ignore-policy", scan["run"])
        self.assertNotEqual(scan.get("continue-on-error"), True)

    def test_runtime_refuses_external_xml_resources_by_default(self):
        runtime = (ROOT / "Dockerfile.service").read_text(encoding="utf-8").split(
            "FROM eclipse-temurin:21-jre", 1
        )[1]
        self.assertIn("ENV JAVA_TOOL_OPTIONS=", runtime)
        for property_name in (
            "javax.xml.accessExternalDTD",
            "javax.xml.accessExternalSchema",
            "javax.xml.accessExternalStylesheet",
        ):
            with self.subTest(property_name=property_name):
                self.assertIn(f"-D{property_name}=", runtime)

    def test_image_build_checks_effective_jvm_hardening(self):
        workflow = yaml.safe_load((ROOT / ".github/workflows/ci.yml").read_text(encoding="utf-8"))
        build_steps = workflow["jobs"]["image-build"]["steps"]
        run = "\n".join(step.get("run", "") for step in build_steps)
        self.assertIn("--entrypoint java", run)
        self.assertIn("-XshowSettings:properties", run)
        self.assertIn("jaxp-settings.txt", run)


if __name__ == "__main__":
    unittest.main()
