# Alcance de CI

En ramas de desarrollo, el rango completo de cada push determina el alcance:

- Solo README.md o documentos de docs/: clasificación, sin Maven ni contenedores.
- Pruebas Java o clases de servicio/utilidad de Academic, Guidance, Analytics, Notification o Import: Maven, JaCoCo, contratos CI, Docker Compose config y Snyk en DevSecOps. EP3 integral se omite.
- Cambios de API, autenticación, gateway, dependencias, configuración, infraestructura, scripts y cualquier ruta desconocida: ambos pipelines completos.
- Pull request hacia main, push a main y ejecución manual de EP3: Verify y Swarm completos. Cloud conserva sus condiciones y aprobación ep3-aws. DevSecOps se puede ejecutar manualmente para su validación integral.

No se reutiliza una validación anterior para aprobar AWS. Cloud sigue dependiendo de Verify y Swarm exitosos en la misma ejecución. Las pruebas existentes no se modifican ni se suprimen para la entrega final.
