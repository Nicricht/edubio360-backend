#!/usr/bin/env bash
# Read-only diagnosis of the existing isolated stack. No database or application credentials printed.
set -euo pipefail
test "${EP3_STACK:?}" = edubio-devops-ep3
test "${GITHUB_REF:?}" = refs/heads/ep3-devops-cloud
mkdir -p artifacts
aws cloudformation describe-stacks --stack-name "$EP3_STACK" --query 'Stacks[0].Outputs' --output json > artifacts/cloud-outputs.json
for role in Manager Worker; do
  node="$(jq -er --arg k "$role" '.[]|select(.OutputKey==$k)|.OutputValue' artifacts/cloud-outputs.json)"
  aws ec2 describe-instances --instance-ids "$node" --query 'Reservations[].Instances[].{Id:InstanceId,Type:InstanceType,State:State.Name,PrivateIp:PrivateIpAddress}' --output json > "artifacts/$role-instance.json"
  command='hostname; uptime; free -m; df -h /; docker info --format "{{.Swarm.LocalNodeState}}"; docker ps -a --format "{{.Names}} {{.Status}}"; docker stats --no-stream --format "{{.Name}} {{.CPUPerc}} {{.MemUsage}}"; docker network ls; for c in $(docker ps -aq --filter label=com.docker.swarm.service.name); do docker inspect --format "{{.Name}} OOM={{.State.OOMKilled}} Exit={{.State.ExitCode}} Health={{if .State.Health}}{{.State.Health.Status}}{{end}}" "$c"; docker inspect --format "{{if .State.Health}}{{range .State.Health.Log}}{{.ExitCode}} {{.Output}}{{end}}{{end}}" "$c" | head -c 500; docker logs --tail 100 "$c" 2>&1 | grep -Ei "exception|caused by|connection refused|timed out|access denied|unknown database|outofmemory|failed to|started .* in" | sed -E "s#(password|secret|token|authorization)[=: ]+[^ ,;]+#\1=REDACTED#Ig" | tail -12 || true; done; if [ "$(docker info --format "{{.Swarm.ControlAvailable}}")" = true ]; then docker node ls; docker stack services edubio; fi'
  id="$(aws ssm send-command --instance-ids "$node" --document-name AWS-RunShellScript --parameters "$(jq -nc --arg c "$command" '{commands:[$c],executionTimeout:["120"]}')" --query Command.CommandId --output text)"
  for i in $(seq 1 36); do
    status="$(aws ssm get-command-invocation --command-id "$id" --instance-id "$node" --query Status --output text 2>/dev/null || true)"
    case "$status" in Success|Failed|Cancelled|TimedOut) break;; esac
    sleep 5
  done
  aws ssm get-command-invocation --command-id "$id" --instance-id "$node" --query '{Status:Status,Output:StandardOutputContent,Error:StandardErrorContent}' --output json > "artifacts/$role-diagnostic.json"
  jq -r '.Output,.Error' "artifacts/$role-diagnostic.json"
  test "$status" = Success
done
