#!/usr/bin/env bash
set -euo pipefail
: "${DEPLOY_DURATION_SECONDS:?}"
test -s artifacts/ci-metrics.prom
image="${IMAGE_PREFIX:-edubio360}/api-gateway:${IMAGE_TAG:-local}"
{
  cat artifacts/ci-metrics.prom
  printf '# TYPE edubio_ci_deploy_duration_seconds gauge\nedubio_ci_deploy_duration_seconds %s\n' "$DEPLOY_DURATION_SECONDS"
} | docker run --rm -i --network edubio_backend --entrypoint curl "$image" -fsS --retry 12 --retry-all-errors -X PUT --data-binary @- http://pushgateway:9091/metrics/job/edubio_ci
