# AWS Academy Learner Lab

The dedicated deployment stack supports either newly created IAM roles (standard AWS account) or an existing authorized Lambda execution role and EC2 instance profile (Academy). The Academy path does not create IAM roles or profiles. It must not modify pre-existing lab resources.

## Read-only preflight

Run from a checked-out feature/ep3-integral branch in CloudShell: bash scripts/ep3/academy-preflight.sh. Verify the existing role trusts Lambda, the profile contains an EC2-trusted role, and the permissions cover SSM, ECR reads, SQS, DynamoDB and CloudWatch logs. A successful list operation does not prove permission to create resources. EC2 type offerings do not prove the academy permits launching that size.

The dedicated stack name is edubio-ep3 unless explicitly configured. Review any existing stack with that name before creating or updating. Use EP3_ACADEMY=true, EP3_LAB_ROLE_ARN (existing authorized role ARN) and EP3_LAB_INSTANCE_PROFILE (existing authorized profile name) with provision.sh. No permanent credentials are required for running AWS CLI in the active CloudShell session. Never paste temporary AWS keys into chat, commit them, or store them in logs.

## Pending validation

Actual permissions, resource quotas, budget remaining, SSM connectivity, ECR image publication, and the complete cloud smoke test remain unverified. GitHub Actions OIDC requires separate authorized federation; the existing LabRole is not assumed to trust GitHub. The cloud job must remain gated on security and quality. Do not deploy this increment until the integration and security checks pass. CloudShell is not a substitute for demonstrating an automated CI/CD deployment. Do not delete unrelated resources or reset the lab.

## Autenticación de CI/CD para Academy

El job cloud admite EP3_AWS_AUTH_MODE=academy y solo se ejecuta por workflow_dispatch en este modo, además de exigir EP3_AWS_ENABLED=true, las pruebas de integración y ambos escaneos Snyk. La configuración exige región, cuenta esperada (EP3_AWS_ACCOUNT_ID) y las tres credenciales temporales AWS de la sesión activa, introducidas exclusivamente como secrets del entorno ep3-aws de GitHub por el propietario. No se deben guardar en variables, commits, capturas ni chat. Los nombres son EP3_LAB_ACCESS_KEY_ID, EP3_LAB_SECRET_ACCESS_KEY y EP3_LAB_SESSION_TOKEN. El propietario debe restringir el entorno a la rama aprobada, revisar el workflow y habilitar protección/aprobación del entorno antes de guardar secretos; si no puede protegerlo, no utilizar este modo. Eliminar los secretos cuando termine el despliegue. El rol y el perfil se obtienen por AWS CLI; el job no crea roles nuevos. No sustituye la verificación de presupuesto, permisos de creación o estado real de la sesión. La modalidad estándar OIDC sigue disponible.
