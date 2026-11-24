#!/usr/bin/env bash
set -euo pipefail
image="${IMAGE_PREFIX:-edubio360}/api-gateway:${IMAGE_TAG:-local}"
mkdir -p artifacts
network="${MONITOR_NETWORK:-edubio_backend}"
resource_selector='container_label_com_docker_swarm_service_name=~"edubio_.+"'
if [ "${MONITOR_COMPOSE:-false}" = true ]; then resource_selector='container_label_com_docker_compose_project="edubio360-portable"'; fi
netcurl() {
  docker run --rm --network "$network" --entrypoint curl "$image" -fsS --max-time 15 "$@"
}
retry() {
  local n
  for n in $(seq 1 60); do
    if "$@"; then return 0; fi
    sleep 5
  done
  return 1
}
prom() { netcurl --get --data-urlencode "query=$1" http://prometheus:9090/api/v1/query; }
java_health() {
  prom 'up{job=~"discovery-server|auth-service|academic-service|guidance-service|notification-service|analytics-service|import-service|api-gateway"} == 1' > artifacts/prometheus-health.json &&
    jq -e '[.data.result[].metric.job] | unique | length == 8' artifacts/prometheus-health.json >/dev/null
}
retry java_health
echo 'Prometheus: los ocho servicios Java responden'
retry netcurl http://grafana:3000/api/health
retry netcurl http://loki:3100/ready
retry netcurl http://tempo:3200/ready
check_logs() {
  netcurl --get --data-urlencode 'query=sum by (service) (count_over_time({job="edubio-docker",service=~"discovery-server|auth-service|academic-service|guidance-service|notification-service|analytics-service|import-service|api-gateway"}[30m]))' http://loki:3100/loki/api/v1/query > artifacts/loki-counts.json &&
    jq -e '[.data.result[].metric.service] | unique | length == 8' artifacts/loki-counts.json >/dev/null
}
retry check_logs
echo 'Loki: registros de los ocho servicios presentes'
trace="$(openssl rand -hex 16)"
parent="$(openssl rand -hex 8)"
curl -fsS -H "traceparent: 00-$trace-$parent-01" "${BASE_URL:-http://127.0.0.1:8080}/api/ofertas" >/dev/null
check_trace() {
  netcurl -H 'Accept: application/json' "http://tempo:3200/api/traces/$trace" > artifacts/tempo-trace.json &&
    jq -e '[.. | objects | select(.key? == "service.name") | .value.stringValue] | (index("api-gateway") != null and index("academic-service") != null)' artifacts/tempo-trace.json >/dev/null
}
retry check_trace
echo 'Tempo: traza compartida Gateway y Academic'
check_resources() {
  prom "count(container_memory_working_set_bytes{$resource_selector})" > artifacts/container-memory.json &&
    jq -e '(.data.result[0].value[1] | tonumber) > 0' artifacts/container-memory.json >/dev/null &&
    prom "count(container_cpu_usage_seconds_total{$resource_selector})" > artifacts/container-cpu.json &&
    jq -e '(.data.result[0].value[1] | tonumber) > 0' artifacts/container-cpu.json >/dev/null
}
retry check_resources
echo 'cAdvisor: CPU y memoria de contenedores presentes'

# Real HTTP request and error counters from the observed services.
# A zero is reported only when the corresponding request counter exists.
http_usage='sum by(job) (http_server_requests_seconds_count)'
http_errors='sum by(job) (http_server_requests_seconds_count{status=~"5.."}) or on(job) (0 * sum by(job) (http_server_requests_seconds_count))'
check_http_metrics() {
  prom "$http_usage" > artifacts/prometheus-http-usage.json &&
    jq -e '.status=="success" and (.data.result|length)>0' artifacts/prometheus-http-usage.json >/dev/null &&
  prom "$http_errors" > artifacts/prometheus-http-errors.json &&
    jq -e '.status=="success" and (.data.result|length)>0' artifacts/prometheus-http-errors.json >/dev/null
}
retry check_http_metrics
echo 'Prometheus: contadores HTTP y errores verificados'
