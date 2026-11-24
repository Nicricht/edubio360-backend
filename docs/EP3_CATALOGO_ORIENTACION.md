# EP3: paquete de trabajo catálogo y orientación

Este paquete amplía las entidades y DTO de ofertas para conservar área, duración, nivel, descripción, grupo de comparación, imagen y cupos, con pruebas de creación y actualización. Los campos nuevos son opcionales para conservar compatibilidad con los clientes EP2 y registros anteriores. No se ha certificado la migración de una base MySQL existente.

Rama frontend complementaria: https://github.com/Nicricht/edubio360-frontend/tree/feature/ep3-catalogo-orientacion

## Tareas de colaboración pendientes

- Catálogo: trasladar las 21 ofertas existentes del frontend a una carga reproducible de MySQL, conservando sus datos, sin borrar registros existentes ni presentar datos de ejemplo como oferta vigente. Resolver la selección de sedes por ID y la migración de registros previos.
- Orientación: completar el ciclo de asignación, reprogramación y finalización con resultado e historial; comprobar propiedad y permisos de cada solicitud. Mantener el evento orientacion.confirmada para no romper el paquete cloud.
- Notificaciones: habilitar consulta y lectura de notificaciones propias con identidad verificada por el servidor; no exponer notificaciones ajenas.
- Pruebas: añadir casos de éxito, datos inválidos, conflicto de estado y acceso a recursos ajenos; ejecutar Maven y comprobar MySQL, Swagger y las APIs a través del Gateway.
- Evidencias: conservar respuestas HTTP y capturas reales para el documento de integración.

El otro integrante coordina Auth, usuarios, sesión JWT, CORS, autorización global del Gateway, Swarm, CI/CD, AWS y observabilidad. Acordar juntos el contrato de roles y los permisos de endpoints antes de fusionar.

## Integración

Partir de esta rama, realizar cambios propios verificables y registrar cada cambio significativo en un commit. Mantener la autoría de los commits existentes. Abrir PR hacia main y fusionar solo cuando las pruebas y la revisión sean satisfactorias. La rama feature/ep3-cloud y el PR #55 siguen independientes; coordinar la integración de ambos paquetes sin duplicar ni revertir cambios.
