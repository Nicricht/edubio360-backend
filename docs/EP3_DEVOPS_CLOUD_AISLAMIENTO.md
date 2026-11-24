# Separación de entregas académicas

- Fullstack: ramas `nico` de ambos repositorios, congeladas. Su pila AWS `edubio-ep3` no se modifica.
- DevOps y Java Cloud: ramas `ep3-devops-cloud` de ambos repositorios.
- Infraestructura nueva: CloudFormation `edubio-devops-ep3`, repositorios ECR bajo `edubio-devops-ep3/`, VPC, EC2, ALB, HTTP API, SQS y DynamoDB independientes. AWS Academy puede compartir el rol de laboratorio ya existente; no se cambia dicho rol.
- Frontend fijado a `1cb471aaa416a3cf346a40c536506fdbf7dc605a` para reproducibilidad.
- Cada push en la rama nueva ejecuta CI completo. AWS requiere EP3_AWS_ENABLED, cuenta verificada y autorización explícita: marcar deploy_isolated_aws en ejecución manual, o habilitar EP3_DEVOPS_AUTO_DEPLOY=true para los pushes posteriores únicamente después de autorizar créditos y restringir el entorno. Por defecto no se crean recursos. No activar ni aprovisionar sin autorización de créditos. El entorno de autenticación ep3-aws debe permitir la rama nueva.
- La URL nueva se obtiene de la salida ApiUrl de la pila nueva; no reemplazar los enlaces entregados para Fullstack.
- Cambiar la rama predeterminada no cambia el contenido de nico ni los recursos AWS. La disponibilidad del laboratorio y las instancias debe verificarse por separado.
- Evidencias de la pila aislada: pendientes de ejecución real. No confundir con las evidencias anteriores.
