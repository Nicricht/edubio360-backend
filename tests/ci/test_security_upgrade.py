"""Guard the supported framework train selected to remediate Snyk alerts."""

from pathlib import Path
import unittest
import xml.etree.ElementTree as ET


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


if __name__ == "__main__":
    unittest.main()
