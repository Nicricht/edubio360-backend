#!/usr/bin/env bash
# Read-only: does not provision resources or export credentials.
set -euo pipefail
export AWS_PAGER=""
export AWS_DEFAULT_REGION="${AWS_REGION:-us-east-1}"
role="${EP3_LAB_ROLE_NAME:-LabRole}"
echo '=== Existing IAM role and trusted services ==='
aws iam get-role --role-name "$role" --query 'Role.AssumeRolePolicyDocument.Statement[].Principal.Service' --output json
echo '=== Instance profiles associated with role ==='
aws iam list-instance-profiles-for-role --role-name "$role" --query 'InstanceProfiles[].InstanceProfileName' --output table
echo '=== Existing CloudFormation stacks ==='
aws cloudformation list-stacks --query "StackSummaries[?StackStatus!='DELETE_COMPLETE'].{Name:StackName,Status:StackStatus}" --output table
echo '=== SSM managed instances ==='
aws ssm describe-instance-information --query 'length(InstanceInformationList)' --output json
echo '=== EC2 types available in region (not proof of launch permission) ==='
aws ec2 describe-instance-type-offerings --location-type region --filters Name=instance-type,Values=t3.large,t3.medium,t3.xlarge --query 'InstanceTypeOfferings[].InstanceType' --output table
echo '=== Read-only preflight complete; resource creation permissions remain unverified ==='
