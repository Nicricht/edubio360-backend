#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
mkdir -p artifacts
curl() { command curl --connect-timeout 5 --max-time 25 "$@"; }
image="${IMAGE_PREFIX:-edubio360}/api-gateway:${IMAGE_TAG:-local}"
declare -A ports=( [discovery-server]=8761 [auth-service]=8081 [academic-service]=8082 [guidance-service]=8083 [notification-service]=8084 [analytics-service]=8085 [import-service]=8086 [api-gateway]=8080 )
pending=(discovery-server auth-service academic-service guidance-service notification-service analytics-service import-service api-gateway)
# Un plazo compartido, sin multiplicar diez minutos por cada servicio.
health_timeout=480
if [ "${EP3_CLOUD:-false}" = true ]; then
  # En EC2, la primera descarga de imágenes y el arranque simultáneo de Java
  # pueden requerir más tiempo que en el runner local.
  health_timeout=1200
fi
deadline=$((SECONDS+health_timeout))
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
echo "Salud interna aprobada; comprobando acceso publicado y rutas Gateway"
if ! curl -fsS --retry 3 --retry-delay 2 --retry-all-errors --retry-max-time 35 "$BASE_URL/actuator/health" | jq -e '.status=="UP"'; then
  echo "Fallo de publicación del Gateway; se guarda evidencia interna y del host" >&2
  docker service inspect edubio_api-gateway --format '{{json .Endpoint.Ports}}' >&2
  docker service ps --no-trunc edubio_api-gateway >&2
  ss -lnt >&2 || true
  docker run --rm --network edubio_backend --entrypoint curl "$image" -fsS --max-time 15 http://api-gateway:8080/actuator/health >&2 || true
  exit 1
fi
echo "Acceso publicado Gateway aprobado"
for name in auth academic guidance notification analytics import; do
  echo "Validando /openapi/$name"
  if ! curl -fsS --retry 12 --retry-all-errors --retry-delay 5 --retry-max-time 90 "$BASE_URL/openapi/$name" | jq -e '.paths | length > 0' >/dev/null; then
    echo "Error de acceso por Gateway: $name" >&2
    docker run --rm --network edubio_backend --entrypoint curl "$image" -fsS --max-time 15 "http://api-gateway:8080/openapi/$name" > "artifacts/internal-openapi-$name.json" || true
    docker run --rm --network edubio_backend --entrypoint curl "$image" -fsS --max-time 15 -H 'Accept: application/json' http://discovery-server:8761/eureka/apps > artifacts/eureka-registry.json || true
    exit 1
  fi
  echo "OK /openapi/$name"
done
test "$(curl -sS -o /dev/null -w '%{http_code}' "$BASE_URL/api/usuarios")" = 401
curl -fsS --retry 3 --retry-all-errors --retry-max-time 45 "$BASE_URL/api/ofertas" | jq -e 'type == "array" and length > 0' >/dev/null
docker node ls
docker stack services edubio
docker service scale --detach=true edubio_analytics-service=2
scale_attempts=36
if [ "${EP3_CLOUD:-false}" = true ]; then scale_attempts=120; fi
for attempt in $(seq 1 "$scale_attempts"); do
  if [ "$(docker service ls --filter name=edubio_analytics-service --format '{{.Replicas}}')" = "2/2" ]; then break; fi
  sleep 5
done
test "$(docker service ls --filter name=edubio_analytics-service --format '{{.Replicas}}')" = "2/2"
docker service ps edubio_analytics-service
for i in $(seq 1 15); do
  curl -fsS --retry 2 --retry-all-errors --retry-max-time 30 "$BASE_URL/openapi/analytics" >/dev/null
done
docker service scale --detach=true edubio_analytics-service=1
for attempt in $(seq 1 "$scale_attempts"); do
  if [ "$(docker service ls --filter name=edubio_analytics-service --format '{{.Replicas}}')" = "1/1" ]; then break; fi
  sleep 5
done
test "$(docker service ls --filter name=edubio_analytics-service --format '{{.Replicas}}')" = "1/1"
docker stack services edubio
