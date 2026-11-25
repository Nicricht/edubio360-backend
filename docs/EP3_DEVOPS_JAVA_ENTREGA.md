# EP3 DOY0101 y JVY0101: índice de entrega

Esta rama es exclusiva de DevOps y Java Cloud. Fullstack permanece en nico sin cambios. Este registro técnico no sustituye las justificaciones, conclusiones ni reflexiones propias del equipo.

## Estado técnico final: validado por etapas

[Recuperación AWS exitosa, intento 2](https://github.com/Nicricht/edubio360-backend/actions/runs/45518492502/attempts/2) · [Artefacto AWS final](https://github.com/Nicricht/edubio360-backend/actions/runs/45518492502/artifacts/14982145576) · [CI Swarm aprobado](https://github.com/Nicricht/edubio360-backend/actions/runs/45518491823) · [DevSecOps aprobado](https://github.com/Nicricht/edubio360-backend/actions/runs/45518491790). [Observabilidad final](../observabilidad.md) · [Guía de video JVY0101](GUIA_VIDEO_EP3_JVY0101.md). El despliegue original falló y se completó por recuperación focalizada; no afirmar que el job cloud original terminó exitoso.

## Evidencias y versiones
- Frontend fijado: 1cb471aaa416a3cf346a40c536506fdbf7dc605a.
- Frontend CI: https://github.com/Nicricht/edubio360-frontend/actions/runs/45482382946
- Auditoría npm: https://github.com/Nicricht/edubio360-frontend/actions/runs/45482382994
- Backend: https://github.com/Nicricht/edubio360-backend/actions?query=branch%3Aep3-devops-cloud
- Evidencias históricas de la versión anterior: Swarm 45360418593, DevSecOps 45360418701, portable 45359397058, bloqueo de seguridad 45322418192. No equivalen a una certificación de la nueva pila AWS.

## DOY0101 (pauta páginas 2–5)
| IE | Peso | Implementación y evidencia exigida |
|---|---:|---|
| IE1 | 20% | Prometheus, Loki, Alloy, Tempo, cAdvisor: salud, uso, errores, logs, trazas de los ocho servicios. Artefacto ep3-swarm. |
| IE2 | 20% | CloudFormation, EC2, Swarm manager/worker y monitoreo automatizado. Nueva pila validada por recuperación, enlace AWS arriba. |
| IE3 | 10% | observability/edubio.json; JaCoCo, Pushgateway, duración, CPU/RAM, errores HTTP; datos reales. |
| IE4 | 10% | Documentación CI/CD y observabilidad; el equipo explica su aporte a decisiones técnicas. |
| IE5 | 20% | Snyk, Maven Enforcer, JaCoCo, auditoría npm y trazabilidad por SHA. |
| IE6 | 20% | Dependencias entre jobs de CI, bloqueo de seguridad y evidencia histórica 45322418192. |

## JVY0101 (pauta páginas 2–8)
| IE | Peso | Implementación y evidencia exigida |
|---|---:|---|
| IE1 | 10% | Dockerfile.service, build-images.sh, imágenes Java sin root. |
| IE4 | 10% | ep3-cloud.yml: build, test, provisión y despliegue. |
| IE5 | 10% | cloud/infra.json, provision.sh y salidas reales de CloudFormation. |
| IE7 | 10% | docker-stack.yml, swarm-init.sh, deploy-aws.sh, manager y worker. |
| IE10 | 10% | serverless/orientation_receipt.py, pruebas y configuración Lambda. |
| IE2 | 5% | Arranque local, salud de ocho componentes, seis OpenAPI. |
| IE3 | 5% | docker stack services, swarm-smoke.sh. |
| IE6 | 5% | Despliegue por etapas y recuperación AWS exitosa; URL HTTPS comprobada. |
| IE8 | 5% | analytics-load-summary.tsv, escenarios 1/2 réplicas y concurrencia 1/8. |
| IE9 | 5% | Argumentación propia del equipo sobre disponibilidad, escalabilidad y mantenibilidad; manager y BD no son HA. |
| IE11 | 6% | SQS y DLQ para confirmación de orientación. |
| IE12 | 4% | HTTP API, Spring Gateway, JWT, 401/403, red privada. |
| IE13 | 5% | Lambda y constancia DynamoDB. |
| IE14 | 2% | RabbitMQ, puente, SQS y consumidor Lambda. |
| IE15 | 3% | Flujo completo, navegador y prueba de carga en nube. |
| IE16 | 3% | Video 3–8 minutos, orden y lenguaje técnico. |
| IE17 | 2% | Voz real de ambos estudiantes y audio claro. |

## Cierre y entrega
DOY0101: GitHub en AVA y correo; informe, declaración de IA y reflexiones individuales redactadas sin IA. JVY0101: enlaces de los repositorios en AVA y correo y video de 3–8 minutos en ambos medios. La validación AWS por etapas está respaldada por los runs y artefactos enlazados; falta grabar el video y completar las justificaciones y reflexiones propias. Un job skipped no es una prueba aprobada. La nueva pila edubio-devops-ep3 requiere autorización de créditos y sesión AWS vigente. No sobrescribir edubio-ep3.

Referencia de citación de IA indicada en la pauta: https://bibliotecas.duoc.cl/ia.
