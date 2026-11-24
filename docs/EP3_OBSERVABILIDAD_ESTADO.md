# Observabilidad EP3 DOY0101: incremento técnico

La configuración versionada añade Prometheus/Micrometer para los ocho módulos Java; Grafana con dashboard aprovisionado de disponibilidad, peticiones, 5xx, latencia, CPU y memoria JVM y cAdvisor de contenedores; Loki y Alloy para logs etiquetados por servicio; Tempo con exportación OTLP desde Spring Boot; Pushgateway para publicar cobertura calculada de los XML JaCoCo y duración real desde el inicio del despliegue hasta la prueba de salud. No se fabrican métricas. Los puertos de colectores permanecen en la red privada; Grafana requiere contraseña y acceso privado (SSM en AWS). Alloy lee los archivos Docker de cada nodo sin montar el socket y descarta entradas no identificadas como servicios de este stack. cAdvisor requiere montajes de observación del host y no se expone públicamente.

El pipeline conserva los controles de seguridad, compila, prueba, genera métricas, despliega, comprueba salud y publica las mediciones. El dashboard se instala desde JSON. Pendiente: certificar scraping, logs, correlación entre microservicios, CPU/RAM de contenedores, imágenes y publicación de métricas en ejecución real. La prueba de salud de Loki/Tempo no acredita por sí sola ingestión o propagación. La infraestructura académica no se ha aprovisionado.

Referencias técnicas: https://grafana.com/docs/alloy/latest/reference/components/loki/loki.process/ y https://docs.spring.io/spring-boot/4.0/reference/actuator/tracing.html.

Las conclusiones y justificaciones individuales quedan a cargo de los estudiantes conforme a la pauta.

La prueba monitoring-smoke.sh ahora exige registros de los ocho servicios, una traza compartida por Gateway y Academic y métricas de contenedores. publish-ci-metrics.sh exige comprobar los valores publicados en Prometheus. Los archivos JSON son evidencia automática solo tras ejecución satisfactoria. El pipeline incluye la prueba Chromium tanto local como en la URL HTTPS del despliegue cloud. No se considera una prueba aprobada mientras el job esté pendiente o falle.
