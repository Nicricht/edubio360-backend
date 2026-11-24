# Recuperación del despliegue AWS aislado

- Ejecución original: [45498949435](https://github.com/Nicricht/edubio360-backend/actions/runs/45498949435).
- Pruebas Maven, Swarm, Grafana y Fullstack CI aprobadas; Snyk y publicación ECR aprobados.
- Diagnóstico real: [45507500167](https://github.com/Nicricht/edubio360-backend/actions/runs/45507500167). Dos nodos Ready, MySQL y RabbitMQ saludables después de la inicialización. Worker con carga de 23.19 y memoria disponible de 5417 MiB. Los contenedores observados no reportan OOMKilled. Analytics registró 432.628 s de arranque; Guidance registró UnknownHostException de mysql al inicio.
- Corrección: arranque secuencial tras salud de MySQL, RabbitMQ y Eureka, periodo inicial de salud de 600 s, distribución parcial de servicios en ambos nodos. Se conservan dos nodos y escalamiento de Academic/Analytics en nodos distintos.
- Recuperación autorizada solo en el stack edubio-devops-ep3, con las imágenes del commit c2296701 ya verificadas; no se vuelven a crear instancias ni se publican imágenes. El flujo de recuperación exige las comprobaciones originales aprobadas y ejecuta las pruebas cloud completas. La configuración de arranque también se aplica a futuros despliegues normales.
- Pendiente: resultado de la recuperación. No constituye una certificación exitosa hasta que el pipeline termine.
