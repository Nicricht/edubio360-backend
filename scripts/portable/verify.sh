#!/usr/bin/env bash
set -euo pipefail
out="${1:?Ruta del paquete}"
mkdir -p artifacts
(cd "$out/imagenes-docker" && sha256sum -c SHA256.txt)
docker load -i "$out/imagenes-docker/edubio360.tar.gz"
compose=(docker compose -f "$out/compose.json")
cleanup() {
  "${compose[@]}" ps -a > artifacts/portable-services.txt 2>&1 || true
  "${compose[@]}" logs --tail 50 > artifacts/portable-logs.txt 2>&1 || true
  "${compose[@]}" down -v || true
}
trap cleanup EXIT
"${compose[@]}" up -d --no-build --pull never --wait --wait-timeout 600
for port in 8761 8081 8082 8083 8084 8085 8086 8080; do
  curl -fsS --max-time 10 "http://127.0.0.1:$port/actuator/health" | jq -e '.status=="UP"'
done
for service in auth academic guidance notification analytics import; do
  curl -fsS --retry 12 --retry-all-errors --retry-delay 5 --retry-max-time 90 "http://127.0.0.1:8080/openapi/$service" | jq -e '.paths | length>0' >/dev/null
done
curl -fsS --retry 12 --retry-all-errors --retry-delay 5 --retry-max-time 90 http://127.0.0.1:8088/api/ofertas | jq -e 'type=="array" and length>0'
test "$(curl -sS -o /dev/null -w '%{http_code}' http://127.0.0.1:8088/api/usuarios)" = 401
COMPOSE_FILE="$out/compose.json" FLOW_COMPOSE=true BASE_URL=http://127.0.0.1:8088 bash scripts/ep3/flow.sh | tee artifacts/portable-role-flow.txt
# Browser runs on an internal-only network, without access to external services.
docker network create --internal portable-browser-offline
trap 'docker network disconnect portable-browser-offline "$("${compose[@]}" ps -q frontend)" 2>/dev/null || true; docker network rm portable-browser-offline 2>/dev/null || true; cleanup' EXIT
docker network connect --alias frontend portable-browser-offline "$("${compose[@]}" ps -q frontend)"
docker run --rm --init --network portable-browser-offline --ipc=host \
  --user "$(id -u):$(id -g)" -e HOME=/tmp -e FRONTEND_URL=http://frontend \
  -v "$PWD/scripts/ep3/browser-smoke.py:/work/browser-smoke.py:ro" \
  -v "$PWD/artifacts:/work/artifacts" -w /work edubio360-browser:1.56.0 python /work/browser-smoke.py
echo 'Paquete portable: carga local, salud, REST y navegador en red aislada aprobados.' | tee artifacts/portable-result.txt
