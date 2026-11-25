# Recuperación del despliegue AWS aislado

- Ejecución original: [45498949435](https://github.com/Nicricht/edubio360-backend/actions/runs/45498949435).
- Pruebas Maven, Swarm, Grafana y Fullstack CI aprobadas; Snyk y publicación ECR aprobados.
- Diagnóstico real: [45507500167](https://github.com/Nicricht/edubio360-backend/actions/runs/45507500167). Dos nodos Ready, MySQL y RabbitMQ saludables después de la inicialización. Worker con carga de 23.19 y memoria disponible de 5417 MiB. Los contenedores observados no reportan OOMKilled. Analytics registró 432.628 s de arranque; Guidance registró UnknownHostException de mysql al inicio.
- Corrección: arranque secuencial tras salud de MySQL, RabbitMQ y Eureka, periodo inicial de salud de 600 s, distribución parcial de servicios en ambos nodos. Se conservan dos nodos y escalamiento de Academic/Analytics en nodos distintos.
- Recuperación autorizada solo en el stack edubio-devops-ep3, con las imágenes del commit c2296701 ya verificadas; no se vuelven a crear instancias ni se publican imágenes. El flujo de recuperación exige las comprobaciones originales aprobadas y ejecuta las pruebas cloud completas. La configuración de arranque también se aplica a futuros despliegues normales.
- Pendiente: resultado de la recuperación. No constituye una certificación exitosa hasta que el pipeline termine.

## Recuperación #2 (25/11/2026 UTC)

[Run 45508192123](https://github.com/Nicricht/edubio360-backend/actions/runs/45508192123) y [artefacto](https://github.com/Nicricht/edubio360-backend/actions/runs/45508192123/artifacts/14979091581): ocho servicios saludables, seis OpenAPI, dos nodos Ready, 384 peticiones sin error, escalamiento 1→2→1 de Analytics y Academic en nodos distintos. JWT, CRUD, roles, RabbitMQ y SQS→Lambda→DynamoDB aprobados. Falló la observabilidad por indisponibilidad de Tempo; no se certificó aún el navegador público. La recuperación focalizada reutiliza las imágenes y la evidencia anterior, verifica los recursos actuales y la constancia en DynamoDB, recupera únicamente Tempo si es necesario y ejecuta las validaciones restantes. Se aumenta su límite a 768 MiB dentro de las mismas instancias, sin crear recursos. Se permite consultar registros de una ventana de 2 h en la recuperación (etiquetada explícitamente), sin afirmar que pertenecen a 30 min. El tiempo importado es la duración medida por GitHub de la etapa AWS previa, incluyendo arranque y pruebas; no es un cronómetro de arranque puro. Los despliegues futuros informan fases y pulsos de SSM, no stdout en directo. La aprobación del flujo de recuperación sigue pendiente.

## Cierre de recuperación

[Run 45518492502, intento 2](https://github.com/Nicricht/edubio360-backend/actions/runs/45518492502/attempts/2) aprobado tras renovar la sesión AWS Academy. Tempo recuperado, trazas, logs de 2 h, métricas y navegador HTTPS aprobados. [Artefacto final](https://github.com/Nicricht/edubio360-backend/actions/runs/45518492502/artifacts/14982145576). El primer intento falló por denegación explícita de credenciales del laboratorio. Se conservaron las imágenes c2296701 y los datos. El resultado exitoso corresponde al conjunto de ejecuciones, no al job original.
