# Observabilidad EP3 DOY0101: estado del incremento

Micrometer expone métricas Prometheus en los ocho módulos Java cuando se habilita el stack de observabilidad. Prometheus descubre las tareas Swarm por DNS y Grafana aprovisiona automáticamente el dashboard con disponibilidad, solicitudes HTTP, errores 5xx, latencia, CPU de proceso y memoria JVM. Los valores proceden de las aplicaciones, no de ejemplos ficticios. La validación exige ocho jobs con up=1.

Grafana no es público en AWS: su puerto 3000 debe permanecer sin reglas de entrada de Internet. Acceder por túnel SSM autorizado y contraseña generada en el manager. La regla ALB rechaza las rutas públicas de métricas. Para uso local, no exponer Docker a redes no confiables. Configurar EP3_MONITORING=true y GRAFANA_ADMIN_PASSWORD antes de desplegar. Las métricas no incluyen todavía logs centralizados, trazas distribuidas, CPU/memoria total del contenedor ni cobertura y tiempos del pipeline. Pendiente validar las imágenes, los recursos disponibles, dashboards y ejecución real de monitoring-smoke.sh. No declarar la pauta aprobada hasta reunir evidencias.

Las justificaciones técnicas y reflexiones individuales deben elaborarlas los estudiantes según la pauta.
