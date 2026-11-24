# AWS Academy — entrega independiente DevOps y Java Cloud

Rama: ep3-devops-cloud. Stack exclusivo: edubio-devops-ep3. No administrar la pila edubio-ep3 ni las instancias de Fullstack.

## Comprobación previa
Iniciar el Learner Lab, verificar créditos disponibles, región, cuenta, permisos y cuotas. El script scripts/ep3/academy-preflight.sh verifica el rol y perfil existentes de forma previa. No cambiar los roles de laboratorio ni publicar credenciales.

## Configuración inicial de GitHub Actions
El propietario debe restringir el entorno ep3-aws a ep3-devops-cloud antes de utilizar secretos. Configurar variables EP3_AWS_REGION, EP3_AWS_ACCOUNT_ID, EP3_AWS_AUTH_MODE=academy y EP3_AWS_ENABLED=true una vez autorizado el gasto. Para Academy, guardar las tres credenciales temporales únicamente en los secrets del entorno protegido: EP3_LAB_ACCESS_KEY_ID, EP3_LAB_SECRET_ACCESS_KEY y EP3_LAB_SESSION_TOKEN. No pegarlas en chat, archivos ni logs. SNYK_TOKEN debe estar configurado para superar el control de seguridad. Para OIDC se requiere rol federado autorizado; no asumir que LabRole admite OIDC.

## Ejecución
En Actions, seleccionar EduBio360 EP3 JVY0101, rama ep3-devops-cloud y Run workflow. Marcar deploy_isolated_aws solo con autorización de créditos. El pipeline comprueba la cuenta y rama, ejecuta pruebas y seguridad, aprovisiona edubio-devops-ep3, publica en sus repositorios ECR, despliega y verifica mediante SSM y navegador HTTPS.

Para demostrar CD automático después de un push, habilitar EP3_DEVOPS_AUTO_DEPLOY=true únicamente con autorización expresa de despliegues sucesivos, entorno restringido y credenciales vigentes. Desactivar después de obtener la evidencia. No es necesario modificar nico.

## Evidencias
Revisar que el job cloud tenga conclusion success, no skipped; descargar ep3-aws-evidence, que debe contener salidas CloudFormation, log de despliegue, informe de observabilidad y pruebas del navegador. Verificar Manager/Worker Ready, SQS, Lambda, DynamoDB, HTTPS y constancia correlacionada. La URL anterior Fullstack no es evidencia de esta pila. No afirmar aprobación sin resultados reales.

Cuando termine el laboratorio, la disponibilidad depende de AWS Academy y de las instancias. Cualquier detención o eliminación de recursos de la nueva pila requiere autorización y conservación de evidencias; no afectar Fullstack.

## Solicitud única desde GitHub

Un push explícitamente solicitado para desplegar puede usar el marcador `[aws-deploy-once]` en el mensaje del commit. Solo aplica a ep3-devops-cloud, exige EP3_AWS_ENABLED=true y mantiene todas las pruebas, autorización del entorno y verificación de cuenta. No habilita despliegues automáticos futuros por sí solo. El workflow AWS Academy preflight verifica en paralelo la configuración y autenticación mediante operaciones de lectura, sin imprimir secretos ni crear recursos. Si las credenciales caducaron, únicamente el propietario puede renovarlas en el entorno protegido. No usar secretos de Fullstack fuera de la cuenta autorizada.
