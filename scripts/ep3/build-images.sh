#!/usr/bin/env bash
set -euo pipefail
: "${IMAGE_PREFIX:=edubio360}"
: "${IMAGE_TAG:=local}"
: "${BUILD_JOBS:=2}"
if [[ ! "$BUILD_JOBS" =~ ^[1-8]$ ]]; then
  echo "BUILD_JOBS debe estar entre 1 y 8" >&2
  exit 2
fi

services=(discovery-server:8761 auth-service:8081 academic-service:8082 guidance-service:8083 notification-service:8084 analytics-service:8085 import-service:8086 api-gateway:8080)
started=$SECONDS
build_service() {
  local pair="$1" name port
  name=${pair%:*}; port=${pair#*:}
  docker build -f Dockerfile.service --build-arg PACKAGE_STAGE=build-all --build-arg MODULE="$name" --build-arg HEALTH_PORT="$port" -t "$IMAGE_PREFIX/$name:$IMAGE_TAG" .
  test "$(docker image inspect --format '{{.Config.User}}' "$IMAGE_PREFIX/$name:$IMAGE_TAG")" = "10001:10001"
}

# Primero materializar la etapa Maven compartida; las demás imágenes reutilizan sus capas.
build_service "${services[0]}"
echo "Compilación compartida y primera imagen: $((SECONDS-started)) s"
pids=()
wait_batch() {
  local failed=0 pid
  for pid in "${pids[@]}"; do
    if ! wait "$pid"; then failed=1; fi
  done
  pids=()
  return "$failed"
}
for pair in "${services[@]:1}"; do
  build_service "$pair" &
  pids+=("$!")
  if (( ${#pids[@]} >= BUILD_JOBS )); then wait_batch; fi
done
wait_batch
docker build -t "$IMAGE_PREFIX/cloud-event-bridge:$IMAGE_TAG" cloud/bridge
echo "Construcción y comprobación de imágenes: $((SECONDS-started)) s"
# No publicar un conjunto parcial si alguna construcción o validación falla.
if [ "${PUSH_IMAGES:-false}" = true ]; then
  for pair in "${services[@]}"; do docker push "$IMAGE_PREFIX/${pair%:*}:$IMAGE_TAG"; done
  docker push "$IMAGE_PREFIX/cloud-event-bridge:$IMAGE_TAG"
fi
