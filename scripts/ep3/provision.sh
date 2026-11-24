#!/usr/bin/env bash
set -euo pipefail
: "${AWS_REGION:?}"
: "${EP3_STACK:=edubio-ep3}"
mkdir -p artifacts
jq --rawfile code serverless/orientation_receipt.py '.Resources.ReceiptFunction.Properties.Code.ZipFile=$code' cloud/infra.json > /tmp/edubio-ep3-template.json
aws cloudformation validate-template --template-body file:///tmp/edubio-ep3-template.json >/dev/null
aws cloudformation deploy --stack-name "$EP3_STACK" --template-file /tmp/edubio-ep3-template.json --capabilities CAPABILITY_IAM --no-fail-on-empty-changeset
aws cloudformation describe-stacks --stack-name "$EP3_STACK" --query 'Stacks[0].Outputs' --output json > artifacts/cloud-outputs.json
