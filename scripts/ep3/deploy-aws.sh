#!/usr/bin/env bash
set -euo pipefail
: "${AWS_REGION:?}"
: "${IMAGE_TAG:?}"
: "${IMAGE_PREFIX:?}"
: "${EP3_STACK:?}"
test "$EP3_STACK" = edubio-devops-ep3
test "${GITHUB_REF:-refs/heads/ep3-devops-cloud}" = refs/heads/ep3-devops-cloud
case "$IMAGE_PREFIX" in */edubio-devops-ep3) ;; *) echo "ECR no autorizado" >&2; exit 1;; esac
# Refresh outputs from the isolated stack rather than trusting stale artifacts.
aws cloudformation describe-stacks --stack-name "$EP3_STACK" --query 'Stacks[0].Outputs' --output json > artifacts/cloud-outputs.json
output(){ jq -er --arg k "$1" '.[] | select(.OutputKey==$k) | .OutputValue' artifacts/cloud-outputs.json; }
manager="$(output Manager)"; worker="$(output Worker)"; ip="$(output ManagerIp)"
queue="$(output QueueUrl)"; table="$(output ReceiptTable)"; api="$(output ApiUrl)"
remote() {
  local node="$1" command="$2" stage="${3:-SSM}" id status previous=""
  id="$(aws ssm send-command --instance-ids "$node" --document-name AWS-RunShellScript --parameters "$(jq -nc --arg c "set -euo pipefail; $command" '{commands:[$c],executionTimeout:["3600"]}')" --query Command.CommandId --output text)"
  for i in $(seq 1 360); do
    status="$(aws ssm get-command-invocation --command-id "$id" --instance-id "$node" --query Status --output text 2>/dev/null || true)"
    if [ "$status" != "$previous" ] || (( i % 3 == 0 )); then
      printf "[%s] %s: %s (%ss)\n" "$(date -u +%T)" "$stage" "${status:-pending}" "$((i*10))" >&2
      previous="$status"
    fi
    case "$status" in
      Success) aws ssm get-command-invocation --command-id "$id" --instance-id "$node" --query StandardOutputContent --output text; return;;
      Failed|Cancelled|TimedOut|Cancelling) aws ssm get-command-invocation --command-id "$id" --instance-id "$node" --query StandardOutputContent --output text; aws ssm get-command-invocation --command-id "$id" --instance-id "$node" --query StandardErrorContent --output text >&2; return 1;;
    esac
    sleep 10
  done
  return 1
}
for node in "$manager" "$worker"; do
  for i in $(seq 1 90); do
    if [ "$(aws ssm describe-instance-information --filters "Key=InstanceIds,Values=$node" --query 'InstanceInformationList[0].PingStatus' --output text)" = Online ]; then break; fi
    test "$i" -lt 90; sleep 10
  done
  remote "$node" "cloud-init status --wait; systemctl is-active docker"
done
remote "$manager" "if [ \$(docker info --format '{{.Swarm.LocalNodeState}}') = inactive ]; then docker swarm init --advertise-addr '$ip' >/dev/null; fi"
token="$(remote "$manager" 'docker swarm join-token -q worker')"
echo "::add-mask::$token"
remote "$worker" "if [ \$(docker info --format '{{.Swarm.LocalNodeState}}') = inactive ]; then docker swarm join --token '$token' '$ip:2377' >/dev/null; fi"
# Archivos de definición del commit, no se copian credenciales desde el runner.
bundle="$(tar -czf - docker-stack.yml docker-stack.monitoring.yml docker-stack.frontend.yml observability cloud/docker-stack.cloud.yml mysql/init/01-create-databases.sql scripts/ep3/swarm-deploy.sh scripts/ep3/swarm-smoke.sh scripts/ep3/flow.sh scripts/ep3/monitoring-smoke.sh scripts/ep3/publish-ci-metrics.sh scripts/ep3/observability-report.py artifacts/ci-metrics.prom | base64 -w0)"
remote "$manager" "mkdir -p /opt/edubio; cd /opt/edubio; echo '$bundle' | base64 -d | tar -xzf -;
umask 077
if [ ! -s .secrets ]; then
  printf 'MYSQL_ROOT_PASSWORD=%s\nRABBITMQ_PASSWORD=%s\nJWT_SECRET=%s\n' \$(openssl rand -hex 24) \$(openssl rand -hex 24) \$(openssl rand -hex 48) > .secrets
fi
if ! grep -q '^GRAFANA_ADMIN_PASSWORD=' .secrets; then
  printf 'GRAFANA_ADMIN_PASSWORD=%s\n' \$(openssl rand -hex 24) >> .secrets
fi
set -a; . ./.secrets; set +a
export EP3_MONITORING=true EP3_FRONTEND=true
export IMAGE_PREFIX='$IMAGE_PREFIX' IMAGE_TAG='$IMAGE_TAG' AWS_REGION='$AWS_REGION' SQS_QUEUE_URL='$queue' RECEIPT_TABLE='$table' BASE_URL='$api' EP3_CLOUD=true
aws ecr get-login-password --region '$AWS_REGION' | docker login --username AWS --password-stdin '${IMAGE_PREFIX%%/*}'
docker pull '$IMAGE_PREFIX/api-gateway:$IMAGE_TAG'
docker node ls
mkdir -p artifacts; date +%s > artifacts/deploy-started.txt" "Preparación del nodo" | tee artifacts/cloud-deploy.txt
run_stage() {
  local label="$1" command="$2"
  echo "=== $label ==="
  remote "$manager" "cd /opt/edubio; set -a; . ./.secrets; set +a; export EP3_MONITORING=true EP3_FRONTEND=true IMAGE_PREFIX='$IMAGE_PREFIX' IMAGE_TAG='$IMAGE_TAG' AWS_REGION='$AWS_REGION' SQS_QUEUE_URL='$queue' RECEIPT_TABLE='$table' BASE_URL='$api' EP3_CLOUD=true; $command" "$label" | tee -a artifacts/cloud-deploy.txt
}
run_stage "Arranque de Swarm" "bash scripts/ep3/swarm-deploy.sh"
run_stage "Salud y escalamiento" "bash scripts/ep3/swarm-smoke.sh"
run_stage "Flujo funcional y serverless" "bash scripts/ep3/flow.sh"
run_stage "Observabilidad" "bash scripts/ep3/monitoring-smoke.sh"
run_stage "Publicación de métricas" "DEPLOY_DURATION_SECONDS=\$((\$(date +%s)-\$(cat artifacts/deploy-started.txt))) bash scripts/ep3/publish-ci-metrics.sh"
# Read only the compact, explicitly scoped result of the completed run.
remote "$manager" "cd /opt/edubio; EP3_CLOUD=true IMAGE_TAG='$IMAGE_TAG' python3 scripts/ep3/observability-report.py" > artifacts/aws-observability-summary.json
jq -e --arg sha "$IMAGE_TAG" '.environment=="aws" and .commit==$sha and (.services|length)==8 and .serverless_receipt.estado=="CONSTANCIA_GENERADA"' artifacts/aws-observability-summary.json >/dev/null
echo "HTTPS API: $api" | tee -a artifacts/cloud-deploy.txt
