#!/usr/bin/env bash
set -euo pipefail
: "${IMAGE_PREFIX:?}"
: "${IMAGE_TAG:?}"
: "${MYSQL_ROOT_PASSWORD:?}"
: "${RABBITMQ_PASSWORD:?}"
: "${JWT_SECRET:?}"
if [ "${EP3_CLOUD:-false}" = true ]; then
  : "${AWS_REGION:?}"
  : "${SQS_QUEUE_URL:?}"
  docker stack deploy --with-registry-auth -c docker-stack.yml -c cloud/docker-stack.cloud.yml edubio
else
  docker stack deploy --resolve-image never -c docker-stack.yml edubio
fi
docker stack services edubio
