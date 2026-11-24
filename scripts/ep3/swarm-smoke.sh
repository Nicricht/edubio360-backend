#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
mkdir -p artifacts
image="${IMAGE_PREFIX:-edubio360}/api-gateway:${IMAGE_TAG:-local}"
declare -A ports=( [discovery-server]=8761 [auth-service]=8081 [academic-service]=8082 [guidance-service]=8083 [notification-service]=8084 [analytics-service]=8085 [import-service]=8086 [api-gateway]=8080 )
pending=(discovery-server auth-service academic-service guidance-service notification-service analytics-service import-service api-gateway)
# Un plazo compartido, sin multiplicar diez minutos por cada servicio.
deadline=$((SECONDS+480))
while (("${#pending[@]}" > 0 && SECONDS < deadline)); do
  next=()
  for service in "${pending[@]}"; do
    if timeout 12s docker run --rm --network edubio_backend --entrypoint curl "$image" -fsS --max-time 5 "http://$service:${ports[$service]}/actuator/health" >/dev/null 2>&1; then
      echo "OK $service /actuator/health"
    else
      next+=("$service")
    fi
  done
  pending=("${next[@]}")
  if (("${#pending[@]}" > 0)); then
    echo "Servicios pendientes: ${pending[*]}"
    docker stack services edubio
    sleep 5
  fi
done
if (("${#pending[@]}" > 0)); then
  for service in "${pending[@]}"; do docker service ps --no-trunc "edubio_$service" >&2; done
  exit 1
fi
for name in auth academic guidance notification analytics import; do
  curl -fsS --retry 12 --retry-all-errors --retry-delay 5 "$BASE_URL/openapi/$name" | jq -e '.paths | length > 0' >/dev/null
done
test "$(curl -sS -o /dev/null -w '%{http_code}' "$BASE_URL/api/usuarios")" = 401
curl -fsS --retry 12 --retry-all-errors "$BASE_URL/api/ofertas" | jq -e 'type == "array" and length > 0' >/dev/null
docker node ls
docker stack services edubio
docker service scale edubio_analytics-service=2
for attempt in $(seq 1 36); do
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
