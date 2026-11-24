#!/usr/bin/env bash
set -euo pipefail
image="${IMAGE_PREFIX:-edubio360}/api-gateway:${IMAGE_TAG:-local}"
for attempt in $(seq 1 60); do
  if docker run --rm --network edubio_backend --entrypoint curl "$image" -fsS --get --data-urlencode 'query=up == 1' http://prometheus:9090/api/v1/query | jq -e '[.data.result[].metric.job] | unique | length >= 8' >/dev/null; then
    echo 'Prometheus: ocho servicios Java disponibles'
    docker run --rm --network edubio_backend --entrypoint curl "$image" -fsS http://grafana:3000/api/health
    exit 0
  fi
  sleep 5
done
exit 1
