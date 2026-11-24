# Verificación previa de AWS

Se separan las rutas hacia el Gateway en dos reglas del Application Load Balancer. AWS admite hasta tres comparaciones por condición y cinco por regla: https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-rules.html . Se conserva el bloqueo de métricas privadas con mayor prioridad y el frontend como destino predeterminado.

La propiedad CreditSpecification de AWS::EC2::Instance admite CPUCredits=standard: https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-properties-ec2-instance-creditspecification.html . No se modifica esa configuración.

La prueba test_cloud_routes.py comprueba las rutas y prioridades a partir de la plantilla. No sustituye una creación real de recursos ni la validación de permisos de AWS Academy.

Antes de habilitar el despliegue, verificar presupuesto restante y gates de integración. La autenticación temporal se configura únicamente en el entorno protegido ep3-aws de GitHub, restringido a la rama aprobada. No compartir credenciales en chat. No se han creado recursos AWS en esta revisión.
