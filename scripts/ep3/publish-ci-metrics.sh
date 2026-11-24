#!/usr/bin/env bash
set -euo pipefail
: "${DEPLOY_DURATION_SECONDS:?}"
test -s artifacts/ci-metrics.prom
image="${IMAGE_PREFIX:-edubio360}/api-gateway:${IMAGE_TAG:-local}"
{
  cat artifacts/ci-metrics.prom
  printf '# TYPE edubio_ci_deploy_duration_seconds gauge\nedubio_ci_deploy_duration_seconds %s\n' "$DEPLOY_DURATION_SECONDS"
} | docker run --rm -i --network "${MONITOR_NETWORK:-edubio_backend}" --entrypoint curl "$image" -fsS --retry 12 --retry-all-errors -X PUT --data-binary @- http://pushgateway:9091/metrics/job/edubio_ci
for attempt in $(seq 1 60); do
  if docker run --rm --network "${MONITOR_NETWORK:-edubio_backend}" --entrypoint curl "$image" -fsS --get --data-urlencode 'query=edubio_ci_coverage_ratio' http://prometheus:9090/api/v1/query > artifacts/prometheus-coverage.json &&
     jq -e '.data.result | length > 0' artifacts/prometheus-coverage.json >/dev/null &&
     docker run --rm --network "${MONITOR_NETWORK:-edubio_backend}" --entrypoint curl "$image" -fsS --get --data-urlencode 'query=edubio_ci_deploy_duration_seconds' http://prometheus:9090/api/v1/query > artifacts/prometheus-deploy-duration.json &&
     jq -e '(.data.result[0].value[1] | tonumber) >= 0' artifacts/prometheus-deploy-duration.json >/dev/null; then
    echo 'Métricas CI/CD verificadas en Prometheus'
    exit 0
  fi
  sleep 5
done
exit 1
