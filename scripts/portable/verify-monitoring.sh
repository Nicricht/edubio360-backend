#!/usr/bin/env bash
set -euo pipefail
out="${1:?Ruta del paquete}"
mkdir -p artifacts
(cd "$out/imagenes-docker" && sha256sum -c SHA256-monitoring.txt)
docker load -i "$out/imagenes-docker/monitoring.tar.gz"
compose=(docker compose -f "$out/compose-monitoring.json")
cleanup() {
  "${compose[@]}" ps -a > artifacts/portable-monitoring-services.txt 2>&1 || true
  "${compose[@]}" logs --tail 80 > artifacts/portable-monitoring-logs.txt 2>&1 || true
  "${compose[@]}" down -v --remove-orphans || true
}
trap cleanup EXIT
started=$SECONDS
"${compose[@]}" up -d --no-build --pull never --wait --wait-timeout 900
export DEPLOY_DURATION_SECONDS=$((SECONDS-started))
export IMAGE_TAG=portable MONITOR_NETWORK=edubio360-portable_default MONITOR_COMPOSE=true
cp "$out/ci-metrics.prom" artifacts/ci-metrics.prom
bash scripts/ep3/monitoring-smoke.sh
bash scripts/ep3/publish-ci-metrics.sh
curl -fsS -u admin:edubio-local-demo http://127.0.0.1:3000/api/search | jq -e 'any(.[]; .title | contains("EduBío 360"))' > artifacts/portable-dashboard.json
echo 'Observabilidad portable verificada' | tee artifacts/portable-monitoring-result.txt
