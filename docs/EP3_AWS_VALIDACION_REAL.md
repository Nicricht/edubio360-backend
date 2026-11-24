# Validación funcional AWS Academy — 24-11-2026

Registro factual de la ejecución SSM comunicada por el operador. No sustituye una ejecución exitosa de GitHub Actions ni la reflexión del equipo.

- Versión desplegada: `ea2a28aad05e6618e2f97165a7657412fb250169`.
- Stack: `edubio-ep3`, región us-east-1.
- URL temporal de la sesión de laboratorio: https://zscqyc1qci.execute-api.us-east-1.amazonaws.com
- Ejecución CI previa: https://github.com/Nicricht/edubio360-backend/actions/runs/45337164517 (intento 3: autenticación, seguridad, aprovisionamiento y publicación ECR aprobados; job cloud falló por plazo de salud).
- SSM CommandId: `c828084b-8f2f-4869-840c-fb9ae092b3a9`. Estado comunicado: Success. Scripts ejecutados: swarm-smoke.sh, flow.sh, monitoring-smoke.sh.

## Extracto del resultado remitido por el operador

```text
OK discovery-server /actuator/health
OK auth-service /actuator/health
OK academic-service /actuator/health
OK guidance-service /actuator/health
OK notification-service /actuator/health
OK analytics-service /actuator/health
OK import-service /actuator/health
OK api-gateway /actuator/health
Acceso publicado Gateway aprobado
OK /openapi/auth
OK /openapi/academic
OK /openapi/guidance
OK /openapi/notification
OK /openapi/analytics
OK /openapi/import
edubio_analytics-service scaled to 2
verify: Service converged
edubio_analytics-service scaled to 1
verify: Service converged
Renovación JWT validada en el Gateway
CRUD de ofertas, perfil persistido y restricciones de roles verificados
Aislamiento de solicitudes por estudiante verificado
Solicitud 1 confirmada por Gateway
RabbitMQ: notificación procesada
SQS -> Lambda -> DynamoDB: constancia verificada para solicitud 1
Notificaciones privadas, lectura persistida y cierre de orientación verificados
Prometheus: los ocho servicios Java responden
Loki: registros de los ocho servicios presentes
Tempo: traza compartida Gateway y Academic
cAdvisor: CPU y memoria de contenedores presentes
```

Dos nodos Ready/Active, uno Leader; en el escalamiento las dos réplicas Analytics se observaron Running en nodos distintos. Gateway, frontend, Alloy y cAdvisor reportaron 2/2; MySQL, RabbitMQ, Grafana, Loki, Tempo, Prometheus y Pushgateway 1/1. Grafana informó database ok y versión 12.1.0. El listado inmediatamente posterior a reducir Analytics mostró transitoriamente 2/1; la salida de scale indicó convergencia.

## Pendiente de certificación

- Repetición automatizada con el plazo de arranque corregido; no se ha cambiado main.
- Ingestión cloud de métricas de cobertura y duración CI/CD y prueba Chromium en HTTPS (no incluidas en la ejecución SSM anterior).
- Evidencias visuales, manual, ERS, documentos y defensa/video del equipo.

El acceso AWS Academy depende de la vigencia del laboratorio; no es una URL permanente garantizada.
