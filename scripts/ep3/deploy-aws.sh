#!/usr/bin/env bash
set -euo pipefail
: "${AWS_REGION:?}"
: "${IMAGE_TAG:?}"
: "${IMAGE_PREFIX:?}"
output(){ jq -er --arg k "$1" '.[] | select(.OutputKey==$k) | .OutputValue' artifacts/cloud-outputs.json; }
manager="$(output Manager)"; worker="$(output Worker)"; ip="$(output ManagerIp)"
queue="$(output QueueUrl)"; table="$(output ReceiptTable)"; api="$(output ApiUrl)"
remote() {
  local node="$1" command="$2" id status
  id="$(aws ssm send-command --instance-ids "$node" --document-name AWS-RunShellScript --parameters "$(jq -nc --arg c "$command" '{commands:[$c],executionTimeout:["3600"]}')" --query Command.CommandId --output text)"
  for i in $(seq 1 360); do
    status="$(aws ssm get-command-invocation --command-id "$id" --instance-id "$node" --query Status --output text 2>/dev/null || true)"
    case "$status" in
      Success) aws ssm get-command-invocation --command-id "$id" --instance-id "$node" --query StandardOutputContent --output text; return;;
      Failed|Cancelled|TimedOut|Cancelling) aws ssm get-command-invocation --command-id "$id" --instance-id "$node" --query StandardErrorContent --output text >&2; return 1;;
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
bundle="$(tar -czf - docker-stack.yml cloud/docker-stack.cloud.yml mysql/init/01-create-databases.sql scripts/ep3/swarm-deploy.sh scripts/ep3/swarm-smoke.sh scripts/ep3/flow.sh | base64 -w0)"
remote "$manager" "mkdir -p /opt/edubio; cd /opt/edubio; echo '$bundle' | base64 -d | tar -xzf -;
umask 077
if [ ! -s .secrets ]; then
  printf 'MYSQL_ROOT_PASSWORD=%s\nRABBITMQ_PASSWORD=%s\nJWT_SECRET=%s\n' \$(openssl rand -hex 24) \$(openssl rand -hex 24) \$(openssl rand -hex 48) > .secrets
fi
set -a; . ./.secrets; set +a
export IMAGE_PREFIX='$IMAGE_PREFIX' IMAGE_TAG='$IMAGE_TAG' AWS_REGION='$AWS_REGION' SQS_QUEUE_URL='$queue' RECEIPT_TABLE='$table' BASE_URL='$api' EP3_CLOUD=true
aws ecr get-login-password --region '$AWS_REGION' | docker login --username AWS --password-stdin '${IMAGE_PREFIX%%/*}'
docker pull '$IMAGE_PREFIX/api-gateway:$IMAGE_TAG'
docker node ls
bash scripts/ep3/swarm-deploy.sh
bash scripts/ep3/swarm-smoke.sh
bash scripts/ep3/flow.sh" | tee artifacts/cloud-deploy.txt
echo "HTTPS API: $api" | tee -a artifacts/cloud-deploy.txt
