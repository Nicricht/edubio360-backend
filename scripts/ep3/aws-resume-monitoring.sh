#!/usr/bin/env bash
set -euo pipefail
: "${AWS_REGION:?}" "${IMAGE_PREFIX:?}" "${IMAGE_TAG:?}" "${EP3_STACK:?}" "${DEPLOY_DURATION_SECONDS:?}"
test "$EP3_STACK" = edubio-devops-ep3
test "$GITHUB_REF" = refs/heads/ep3-devops-cloud
test "$IMAGE_TAG" = c2296701b485be2c557472a7f211c92f34581c13
case "$IMAGE_PREFIX" in */edubio-devops-ep3) ;; *) exit 1;; esac
mkdir -p artifacts
aws cloudformation describe-stacks --stack-name "$EP3_STACK" --query 'Stacks[0].Outputs' --output json > artifacts/cloud-outputs.json
output(){ jq -er --arg k "$1" '.[] | select(.OutputKey==$k) | .OutputValue' artifacts/cloud-outputs.json; }
manager="$(output Manager)"; api="$(output ApiUrl)"; table="$(output ReceiptTable)"
remote(){
  local label="$1" command="$2" id status
  id="$(aws ssm send-command --instance-ids "$manager" --document-name AWS-RunShellScript --parameters "$(jq -nc --arg c "set -euo pipefail; $command" '{commands:[$c],executionTimeout:["1200"]}')" --query Command.CommandId --output text)"
  for i in $(seq 1 132); do
    status="$(aws ssm get-command-invocation --command-id "$id" --instance-id "$manager" --query Status --output text 2>/dev/null || true)"
    case "$status" in
      Success|Failed|Cancelled|TimedOut|Cancelling)
        aws ssm get-command-invocation --command-id "$id" --instance-id "$manager" --query StandardOutputContent --output text
        if [ "$status" != Success ]; then
          aws ssm get-command-invocation --command-id "$id" --instance-id "$manager" --query StandardErrorContent --output text >&2
          return 1
        fi
        return 0;;
    esac
    if (( i == 1 || i % 3 == 0 )); then printf '[%s] %s: %s (%ss)\n' "$(date -u +%T)" "$label" "${status:-Pending}" "$((i*10))" >&2; fi
    sleep 10
  done
  return 1
}
bundle="$(tar -czf - scripts/ep3/resume-monitoring-on-node.sh scripts/ep3/monitoring-smoke.sh scripts/ep3/publish-ci-metrics.sh scripts/ep3/observability-report.py artifacts/ci-metrics.prom | base64 -w0)"
remote "Preparar scripts sin reiniciar aplicaciones" "cd /opt/edubio; test -s .secrets; echo '$bundle' | base64 -d | tar -xzf -"
stage(){
  local name="$1"
  remote "$name" "cd /opt/edubio; export IMAGE_PREFIX='$IMAGE_PREFIX' IMAGE_TAG='$IMAGE_TAG' BASE_URL='$api' DEPLOY_DURATION_SECONDS='$DEPLOY_DURATION_SECONDS'; bash scripts/ep3/resume-monitoring-on-node.sh '$name'"
}
stage verify | tee artifacts/cloud-resume.txt
receipt="$(stage receipt)"
aws dynamodb get-item --table-name "$table" --consistent-read --key "$(jq -nc --arg id "$receipt" '{eventId:{S:$id}}')" | jq -e --arg id "$receipt" '.Item | select(.eventId.S==$id and .estado.S=="CONSTANCIA_GENERADA") | {eventId:.eventId.S,estado:.estado.S}' > artifacts/cloud-receipt.json
stage tempo | tee -a artifacts/cloud-resume.txt
stage monitoring | tee -a artifacts/cloud-resume.txt
stage metrics | tee -a artifacts/cloud-resume.txt
stage summary > artifacts/aws-observability-summary.json
jq -e --arg sha "$IMAGE_TAG" '.environment=="aws" and .commit==$sha and (.services|length)==8 and .serverless_receipt.estado=="CONSTANCIA_GENERADA"' artifacts/aws-observability-summary.json >/dev/null
echo "HTTPS API: $api" | tee -a artifacts/cloud-resume.txt
