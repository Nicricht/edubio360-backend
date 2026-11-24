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
  docker stack deploy --with-registry-auth "${configs[@]}" -c cloud/docker-stack.cloud.yml edubio
else
  docker stack deploy --resolve-image never "${configs[@]}" edubio
fi
docker stack services edubio
