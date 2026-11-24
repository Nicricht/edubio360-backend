"""Generate a local-only deployment from the project's canonical Compose file."""
import json
import sys
from pathlib import Path

out = Path(sys.argv[1])
config = json.loads((out / "compose-build.json").read_text())
services = config["services"]
services.pop("analytics-replica", None)
for service in services.values():
    service.pop("build", None)
    service.pop("container_name", None)
    service["pull_policy"] = "never"
    if service["image"].startswith("edubio360/"):
        env = service.setdefault("environment", {})
        env["JAVA_TOOL_OPTIONS"] = "-XX:MaxRAMPercentage=65 -Djavax.xml.accessExternalDTD= -Djavax.xml.accessExternalSchema= -Djavax.xml.accessExternalStylesheet="
        env["EUREKA_INSTANCE_PREFER_IP_ADDRESS"] = "true"
    for port in service.get("ports", []):
        port["host_ip"] = "127.0.0.1"
# Match the already certified Swarm gateway policy: public read-only catalog,
# mandatory JWT and role enforcement for private and administrative operations.
services["api-gateway"]["environment"]["EP3_STRICT_ACCESS"] = "true"
# Compose config is serialized and parsed again on the destination host.
# Escape the variable for that second interpolation pass.
test = services["mysql"]["healthcheck"]["test"]
test[1] = test[1].replace("$$MYSQL_ROOT_PASSWORD", "$MYSQL_ROOT_PASSWORD").replace("$MYSQL_ROOT_PASSWORD", "$$MYSQL_ROOT_PASSWORD")
# Keep the database initialization path relative to the portable directory.
for volume in services["mysql"]["volumes"]:
    if volume["type"] == "bind":
        volume["source"] = "./mysql/init"
services["frontend"] = {
    "image": "edubio360/frontend:portable",
    "pull_policy": "never",
    "ports": [{"target": 80, "published": "8088", "host_ip": "127.0.0.1", "protocol": "tcp"}],
    "depends_on": {"api-gateway": {"condition": "service_healthy"}},
    "networks": {"default": None},
    "healthcheck": {
        "test": ["CMD", "wget", "-q", "-O", "/dev/null", "http://127.0.0.1/healthz"],
        "interval": "10s", "timeout": "5s", "retries": 12,
    },
    "restart": "unless-stopped",
}
assert len(services) == 11
(out / "compose.json").write_text(json.dumps(config, indent=2) + "\n")
