#!/usr/bin/env bash
set -euo pipefail
: "${IMAGE_PREFIX:=edubio360}"
: "${IMAGE_TAG:=local}"
for pair in discovery-server:8761 auth-service:8081 academic-service:8082 guidance-service:8083 notification-service:8084 analytics-service:8085 import-service:8086 api-gateway:8080; do
  name=${pair%:*}; port=${pair#*:}
  docker build -f Dockerfile.service --build-arg MODULE="$name" --build-arg HEALTH_PORT="$port" -t "$IMAGE_PREFIX/$name:$IMAGE_TAG" .
  test "$(docker image inspect --format '{{.Config.User}}' "$IMAGE_PREFIX/$name:$IMAGE_TAG")" = "10001:10001"
  if [ "${PUSH_IMAGES:-false}" = true ]; then docker push "$IMAGE_PREFIX/$name:$IMAGE_TAG"; fi
done
docker build -t "$IMAGE_PREFIX/cloud-event-bridge:$IMAGE_TAG" cloud/bridge
if [ "${PUSH_IMAGES:-false}" = true ]; then docker push "$IMAGE_PREFIX/cloud-event-bridge:$IMAGE_TAG"; fi
