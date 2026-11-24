"""Derive Compose monitoring from the certified Swarm definitions."""
import copy
import json
import re
import shutil
import sys
from pathlib import Path

out = Path(sys.argv[1])
config = json.loads((out / "compose.json").read_text())
monitor = json.loads(Path("docker-stack.monitoring.yml").read_text())
shutil.copytree("observability", out / "observability", dirs_exist_ok=True)
prom = out / "observability/prometheus.yml"
prom.write_text(prom.read_text().replace("tasks.", ""))
dashboard = out / "observability/edubio.json"
dashboard.write_text(dashboard.read_text().replace("container_label_com_docker_swarm_service_name", "container_label_com_docker_compose_service").replace("edubio_.+", ".+"))
for name, original in monitor["services"].items():
    spec = copy.deepcopy(original)
    if name in config["services"]:
        config["services"][name]["environment"].update(spec["environment"])
        config["services"][name]["logging"] = spec["logging"]
        config["services"][name]["labels"] = spec["labels"]
        continue
    spec["networks"] = {"default": None}
    spec["pull_policy"] = "never"
    spec["restart"] = "unless-stopped"
    if "deploy" in spec:
        spec["deploy"] = {"resources": spec["deploy"]["resources"]}
    for port in spec.get("ports", []):
        port.pop("mode", None)
        port["host_ip"] = "127.0.0.1"
    if name == "grafana":
        spec["environment"]["GF_SECURITY_ADMIN_PASSWORD"] = "edubio-local-demo"
    config["services"][name] = spec
config["configs"] = monitor["configs"]
config["volumes"].update(monitor["volumes"])
assert len(config["services"]) == 18
(out / "compose-monitoring.json").write_text(json.dumps(config, indent=2) + "\n")
