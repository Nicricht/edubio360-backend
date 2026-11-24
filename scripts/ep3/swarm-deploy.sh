#!/usr/bin/env bash
set -euo pipefail
: "${IMAGE_PREFIX:?}"
: "${IMAGE_TAG:?}"
: "${MYSQL_ROOT_PASSWORD:?}"
: "${RABBITMQ_PASSWORD:?}"
: "${JWT_SECRET:?}"
configs=(-c docker-stack.yml)
if [ "${EP3_MONITORING:-false}" = true ]; then
  : "${GRAFANA_ADMIN_PASSWORD:?}"
  configs+=(-c docker-stack.monitoring.yml)
fi
if [ "${EP3_FRONTEND:-false}" = true ]; then
  configs+=(-c docker-stack.frontend.yml)
fi
if [ "${EP3_CLOUD:-false}" = true ]; then
  test "${EP3_FRONTEND:-false}" = true
  : "${AWS_REGION:?}"
  : "${SQS_QUEUE_URL:?}"
  # Start infrastructure first. Do not let six JVMs race MySQL or saturate one EC2 node.
  staged="$(mktemp)"
  trap 'rm -f "$staged"' EXIT
  printf '%s\n' '{"version":"3.8","services":{"auth-service":{"deploy":{"replicas":0}},"academic-service":{"deploy":{"replicas":0}},"guidance-service":{"deploy":{"replicas":0}},"notification-service":{"deploy":{"replicas":0}},"analytics-service":{"deploy":{"replicas":0}},"import-service":{"deploy":{"replicas":0}}}}' > "$staged"
  docker stack deploy --with-registry-auth "${configs[@]}" -c cloud/docker-stack.cloud.yml -c "$staged" edubio
  wait_local_healthy() {
    local service="$1" deadline=$((SECONDS+600)) cid
    while ((SECONDS < deadline)); do
      cid="$(docker ps -q --filter "label=com.docker.swarm.service.name=edubio_$service" | head -n1)"
      if [ -n "$cid" ] && [ "$(docker inspect --format '{{.State.Health.Status}}' "$cid" 2>/dev/null || true)" = healthy ]; then
        echo "Dependencia saludable: $service"; return 0
      fi
      sleep 5
    done
    docker service ps --no-trunc "edubio_$service" >&2
    return 1
  }
  wait_local_healthy mysql
  wait_local_healthy rabbitmq
  wait_local_healthy discovery-server
  declare -A ports=([auth-service]=8081 [academic-service]=8082 [guidance-service]=8083 [notification-service]=8084 [analytics-service]=8085 [import-service]=8086)
  for service in auth-service academic-service notification-service guidance-service analytics-service import-service; do
    echo "Arranque por etapas: $service"
    docker service scale --detach=true "edubio_$service=1"
    deadline=$((SECONDS+900))
    healthy=false
    while ((SECONDS < deadline)); do
      if [ "$(docker service ls --filter "name=edubio_$service" --format '{{.Replicas}}')" = "1/1" ] &&
         timeout 20s docker run --rm --network edubio_backend --entrypoint curl "$IMAGE_PREFIX/api-gateway:$IMAGE_TAG" -fsS --max-time 10 "http://$service:${ports[$service]}/actuator/health" | jq -e '.status=="UP"' >/dev/null; then
        healthy=true; echo "OK $service"; break
      fi
      sleep 10
    done
    if [ "$healthy" != true ]; then
      docker service ps --no-trunc "edubio_$service" >&2
      docker service logs --tail 30 "edubio_$service" >&2 || true
      exit 1
    fi
  done
  rm -f "$staged"
  trap - EXIT
else
  docker stack deploy --resolve-image never "${configs[@]}" edubio
fi
docker stack services edubio
