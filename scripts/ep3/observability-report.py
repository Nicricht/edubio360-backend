"""Generate a compact factual report from the current run's collected evidence."""
import json
import os
from pathlib import Path

ART = Path("artifacts")
SERVICES = {
    "discovery-server", "api-gateway", "auth-service", "academic-service",
    "guidance-service", "notification-service", "analytics-service", "import-service",
}


def series(name, label=None):
    data = json.loads((ART / name).read_text())
    if data.get("status") != "success":
        raise ValueError(name + ": query not successful")
    result = {}
    for item in data["data"]["result"]:
        key = item["metric"][label] if label else "value"
        result[key] = float(item["value"][1])
    if not result:
        raise ValueError(name + ": empty result")
    return result


def report():
    health = series("prometheus-health.json", "job")
    logs = series("loki-counts.json", "service")
    window_file = ART / "loki-window.txt"
    log_window = window_file.read_text().strip() if window_file.exists() else "30m"
    if log_window not in {"30m", "2h"}:
        raise ValueError("invalid log window")
    usage = series("prometheus-http-usage.json", "job")
    errors = series("prometheus-http-errors.json", "job")
    coverage = series("prometheus-coverage.json", "module")
    for label, observed in [("health", health), ("logs", logs), ("HTTP", usage), ("HTTP errors", errors)]:
        missing = SERVICES - observed.keys()
        if missing:
            raise ValueError(label + ": missing " + ", ".join(sorted(missing)))
    result = {
        "environment": "aws" if os.getenv("EP3_CLOUD") == "true" else "ci-swarm",
        "commit": os.environ.get("IMAGE_TAG", "unknown"),
        "log_window": log_window,
        "services": {name: {
            "up": health[name], "log_count": logs[name],
            **({"log_count_30m": logs[name]} if log_window == "30m" else {}),
            "http_requests_total": usage[name], "http_5xx_total": errors[name],
        } for name in sorted(SERVICES)},
        "coverage_line_ratio": coverage,
        "deployment_seconds": series("prometheus-deploy-duration.json")["value"],
        "observed_cpu_series": series("container-cpu.json")["value"],
        "observed_memory_series": series("container-memory.json")["value"],
    }
    if result["environment"] == "aws":
        receipt = json.loads((ART / "cloud-receipt.json").read_text())
        if receipt.get("estado") != "CONSTANCIA_GENERADA":
            raise ValueError("constancia no verificada")
        result["serverless_receipt"] = receipt
        result["nodes"] = (ART / "cloud-nodes.txt").read_text()
    for name in ("analytics", "academic"):
        load = ART / (name + "-load-summary.tsv")
        if load.exists():
            result[name + "_load_tsv"] = load.read_text()
    return result


if __name__ == "__main__":
    print(json.dumps(report(), ensure_ascii=False, indent=2, allow_nan=False))
