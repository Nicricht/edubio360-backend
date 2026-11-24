#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
mkdir -p artifacts
for service in discovery-server auth-service academic-service guidance-service notification-service analytics-service import-service api-gateway; do
  case "$service" in
    discovery-server) port=8761;; auth-service) port=8081;; academic-service) port=8082;; guidance-service) port=8083;;
    notification-service) port=8084;; analytics-service) port=8085;; import-service) port=8086;; api-gateway) port=8080;;
  esac
  ready=false
  for attempt in $(seq 1 120); do
    if docker run --rm --network edubio_backend --entrypoint curl "${IMAGE_PREFIX:-edubio360}/api-gateway:${IMAGE_TAG:-local}" -fsS --max-time 5 "http://$service:$port/actuator/health" >/dev/null 2>&1; then ready=true; break; fi
    sleep 5
  done
  test "$ready" = true
  echo "OK $service /actuator/health"
done
for name in auth academic guidance notification analytics import; do
  curl -fsS --retry 12 --retry-all-errors --retry-delay 5 "$BASE_URL/openapi/$name" | jq -e '.paths | length > 0' >/dev/null
done
test "$(curl -sS -o /dev/null -w '%{http_code}' "$BASE_URL/api/usuarios")" = 401
curl -fsS --retry 12 --retry-all-errors "$BASE_URL/api/ofertas" | jq -e 'type == "array" and length > 0' >/dev/null
docker node ls
docker stack services edubio
docker service scale edubio_analytics-service=2
for attempt in $(seq 1 120); do
  if [ "$(docker service ls --filter name=edubio_analytics-service --format '{{.Replicas}}')" = "2/2" ]; then break; fi
  sleep 5
done
test "$(docker service ls --filter name=edubio_analytics-service --format '{{.Replicas}}')" = "2/2"
docker service ps edubio_analytics-service
for i in $(seq 1 15); do
  curl -fsS --retry 6 --retry-all-errors "$BASE_URL/openapi/analytics" >/dev/null
done
docker service scale edubio_analytics-service=1
docker stack services edubio
