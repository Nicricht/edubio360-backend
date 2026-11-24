#!/usr/bin/env bash
set -euo pipefail
: "${AWS_REGION:?}"
: "${EP3_STACK:=edubio-devops-ep3}"
# Never update the stack submitted for Fullstack.
test "$EP3_STACK" = edubio-devops-ep3 || { echo "Stack no autorizado para esta rama" >&2; exit 1; }
test "${GITHUB_REF:-refs/heads/ep3-devops-cloud}" = refs/heads/ep3-devops-cloud || exit 1
mkdir -p artifacts
jq --rawfile code serverless/orientation_receipt.py '.Resources.ReceiptFunction.Properties.Code.ZipFile=$code' cloud/infra.json > /tmp/edubio-ep3-template.json
aws cloudformation validate-template --template-body file:///tmp/edubio-ep3-template.json >/dev/null
params=()
if [ "${EP3_ACADEMY:-false}" = true ]; then
  : "${EP3_LAB_ROLE_ARN:?Run academy-preflight.sh and set the authorized existing role ARN}"
  : "${EP3_LAB_INSTANCE_PROFILE:?Set the authorized existing instance profile name}"
  params=(--parameter-overrides "ExistingRoleArn=$EP3_LAB_ROLE_ARN" "ExistingInstanceProfile=$EP3_LAB_INSTANCE_PROFILE")
fi
aws cloudformation deploy --stack-name "$EP3_STACK" --template-file /tmp/edubio-ep3-template.json --capabilities CAPABILITY_IAM --no-fail-on-empty-changeset "${params[@]}"
aws cloudformation describe-stacks --stack-name "$EP3_STACK" --query 'Stacks[0].Outputs' --output json > artifacts/cloud-outputs.json
