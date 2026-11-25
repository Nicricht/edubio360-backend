#!/usr/bin/env bash
set -euo pipefail
: "${IMAGE_PREFIX:?}" "${IMAGE_TAG:?}" "${BASE_URL:?}"
test "$IMAGE_TAG" = c2296701b485be2c557472a7f211c92f34581c13
case "$IMAGE_PREFIX" in */edubio-devops-ep3) ;; *) exit 1;; esac
image="$IMAGE_PREFIX/api-gateway:$IMAGE_TAG"
case "$1" in
  verify)
    docker node ls
    test "$(docker node ls --format '{{.Status}}' | grep -c '^Ready$')" -eq 2
    for s in discovery-server auth-service academic-service guidance-service notification-service analytics-service import-service api-gateway frontend cloud-event-bridge; do
      actual="$(docker service inspect --format '{{.Spec.TaskTemplate.ContainerSpec.Image}}' "edubio_$s")"
      case "$actual" in "$IMAGE_PREFIX/$s:$IMAGE_TAG"|"$IMAGE_PREFIX/$s:$IMAGE_TAG"@sha256:*) ;; *) echo "Imagen inesperada: $s" >&2; exit 1;; esac
    done
    for s in analytics academic; do
      awk -F '\t' 'NR>1 {n++; if($3!=48||$4!=0) bad=1} END {exit !(n==4&&!bad)}' "artifacts/$s-load-summary.tsv"
    done
    jq -e '.estado=="CONSTANCIA_GENERADA"' artifacts/cloud-receipt.json
    docker stack services edubio
    curl -fsS --max-time 15 "$BASE_URL/actuator/health" | jq -e '.status=="UP"'
    ;;
  receipt) jq -er .eventId artifacts/cloud-receipt.json ;;
  tempo)
    probe(){ docker run --rm --network edubio_backend --entrypoint curl "$image" -fsS --max-time 8 http://tempo:3200/ready; }
    if ! probe; then
      echo "Tempo no responde. Diagnóstico previo a una única recuperación."
      docker service ps --no-trunc edubio_tempo
      docker service logs --tail 30 edubio_tempo 2>&1 || true
      docker service update --detach=true --limit-memory 768M --force edubio_tempo
    fi
    for i in $(seq 1 36); do
      if probe; then echo "Tempo disponible"; exit 0; fi
      sleep 5
    done
    docker service ps --no-trunc edubio_tempo
    docker service logs --tail 30 edubio_tempo 2>&1 || true
    exit 1
    ;;
  monitoring)
    # Reuse the original observation window for startup logs, without restarting healthy JVMs.
    MONITOR_LOG_WINDOW=2h EP3_CLOUD=true bash scripts/ep3/monitoring-smoke.sh
    ;;
  metrics) bash scripts/ep3/publish-ci-metrics.sh ;;
  summary) EP3_CLOUD=true python3 scripts/ep3/observability-report.py ;;
  *) exit 2 ;;
esac
