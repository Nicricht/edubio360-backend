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
# Controlled read-only load on the actual Analytics endpoint, with an isolated
# test account (same registration mechanism as flow.sh).
bench_email="ep3-load-$(date +%s)-$RANDOM@example.test"
bench_pass="$(openssl rand -hex 24)"
bench_credentials="$(jq -nc --arg e "$bench_email" --arg p "$bench_pass" '{email:$e,password:$p}')"
curl -fsS -H 'Content-Type: application/json' -d "$bench_credentials" "$BASE_URL/api/auth/register" >/dev/null
BENCH_TOKEN="$(curl -fsS -H 'Content-Type: application/json' -d "$bench_credentials" "$BASE_URL/api/auth/login" | jq -er '.token')"
export BENCH_TOKEN BASE_URL
unset bench_credentials bench_pass
printf 'replicas\tconcurrency\trequests\terrors\tmean_seconds\tp95_seconds\trequests_per_second\n' > artifacts/analytics-load-summary.tsv
benchmark() {
  local replicas="$1" concurrency="$2" requests=48 start end elapsed errors mean p95 rate raw
  local service="${3:-analytics}" route="${4:-/api/analytics/resumen}"
  export BENCH_ROUTE="$route"
  raw="artifacts/${service}-load-${replicas}r-${concurrency}c.tsv"
  # Warm the route after registry changes before measuring.
  if [ "$service" = analytics ]; then
    curl -fsS --retry 6 --retry-delay 2 --retry-all-errors --retry-max-time 35 -H "Authorization: Bearer $BENCH_TOKEN" "$BASE_URL$route" | jq -e '.estado=="OPERATIVO"' >/dev/null
  else
    curl -fsS --retry 6 --retry-delay 2 --retry-all-errors --retry-max-time 35 "$BASE_URL$route" | jq -e 'type=="array" and length>0' >/dev/null
  fi
  start="$(date +%s%N)"
  seq "$requests" | xargs -P "$concurrency" -I '{}' sh -c '
    result=$(curl --silent --show-error --connect-timeout 5 --max-time 20 -o /dev/null -w "%{http_code} %{time_total}" -H "Authorization: Bearer $BENCH_TOKEN" "$BASE_URL$BENCH_ROUTE") || result="000 20"
    printf "%s\n" "$result"
  ' > "$raw"
  end="$(date +%s%N)"
  elapsed="$(awk -v n="$((end-start))" 'BEGIN{printf "%.6f",n/1e9}')"
  test "$(wc -l < "$raw")" -eq "$requests"
  errors="$(awk '$1!=200{n++} END{print n+0}' "$raw")"
  mean="$(awk '{s+=$2} END{printf "%.6f",s/NR}' "$raw")"
  p95="$(awk '{print $2}' "$raw" | sort -n | sed -n "$(((requests*95+99)/100))p")"
  rate="$(awk -v n="$requests" -v t="$elapsed" 'BEGIN{printf "%.2f",n/t}')"
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$replicas" "$concurrency" "$requests" "$errors" "$mean" "$p95" "$rate" | tee -a "artifacts/$service-load-summary.tsv"
  test "$errors" -eq 0
}
benchmark 1 1
benchmark 1 8
# Restore the original scale if a measurement fails.
trap 'docker service scale --detach=true edubio_analytics-service=1 >/dev/null 2>&1 || true' EXIT
docker service scale --detach=true edubio_analytics-service=2
scale_attempts=36
if [ "${EP3_CLOUD:-false}" = true ]; then scale_attempts=120; fi
for attempt in $(seq 1 "$scale_attempts"); do
  if [ "$(docker service ls --filter name=edubio_analytics-service --format '{{.Replicas}}')" = "2/2" ]; then break; fi
  sleep 5
done
test "$(docker service ls --filter name=edubio_analytics-service --format '{{.Replicas}}')" = "2/2"
docker service ps edubio_analytics-service | tee artifacts/analytics-replicas.txt
if [ "${EP3_CLOUD:-false}" = true ]; then
  docker node ls | tee artifacts/cloud-nodes.txt
  test "$(docker node ls --format '{{.Status}}' | grep -c '^Ready$')" -eq 2
  test "$(docker node ls --filter role=manager -q | wc -l)" -eq 1
  test "$(docker node ls --filter role=worker -q | wc -l)" -eq 1
  test "$(docker service ps --filter desired-state=running --format '{{.Node}}' edubio_analytics-service | sort -u | wc -l)" -eq 2
fi
for i in $(seq 1 15); do
  curl -fsS --retry 2 --retry-all-errors --retry-max-time 30 "$BASE_URL/openapi/analytics" >/dev/null
done
benchmark 2 1
benchmark 2 8
docker service scale --detach=true edubio_analytics-service=1
for attempt in $(seq 1 "$scale_attempts"); do
  if [ "$(docker service ls --filter name=edubio_analytics-service --format '{{.Replicas}}')" = "1/1" ]; then break; fi
  sleep 5
done
test "$(docker service ls --filter name=edubio_analytics-service --format '{{.Replicas}}')" = "1/1"
trap - EXIT
# Second independent scenario: read-only academic catalogue, no mutation or
# parallel scale of both services. Keep resource consumption bounded.
printf 'replicas\tconcurrency\trequests\terrors\tmean_seconds\tp95_seconds\trequests_per_second\n' > artifacts/academic-load-summary.tsv
benchmark 1 1 academic /api/ofertas
benchmark 1 8 academic /api/ofertas
trap 'docker service scale --detach=true edubio_academic-service=1 >/dev/null 2>&1 || true' EXIT
docker service scale --detach=true edubio_academic-service=2
for attempt in $(seq 1 "$scale_attempts"); do
  if [ "$(docker service ls --filter name=edubio_academic-service --format '{{.Replicas}}')" = "2/2" ]; then break; fi
  sleep 5
done
test "$(docker service ls --filter name=edubio_academic-service --format '{{.Replicas}}')" = "2/2"
docker service ps edubio_academic-service | tee artifacts/academic-replicas.txt
if [ "${EP3_CLOUD:-false}" = true ]; then
  test "$(docker service ps --filter desired-state=running --format '{{.Node}}' edubio_academic-service | sort -u | wc -l)" -eq 2
fi
for i in $(seq 1 15); do
  curl -fsS --retry 2 --retry-all-errors --retry-max-time 30 "$BASE_URL/openapi/academic" >/dev/null
done
benchmark 2 1 academic /api/ofertas
benchmark 2 8 academic /api/ofertas
docker service scale --detach=true edubio_academic-service=1
for attempt in $(seq 1 "$scale_attempts"); do
  if [ "$(docker service ls --filter name=edubio_academic-service --format '{{.Replicas}}')" = "1/1" ]; then break; fi
  sleep 5
done
test "$(docker service ls --filter name=edubio_academic-service --format '{{.Replicas}}')" = "1/1"
trap - EXIT
unset BENCH_TOKEN BENCH_ROUTE
docker stack services edubio
