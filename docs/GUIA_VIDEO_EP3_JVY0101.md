# Guía de grabación — JVY0101 EP3

Duración objetivo: 7:40 (pauta: 3–8 minutos). Un MP4 o formato similar, voces reales de ambos integrantes, sin obligación de mostrar sus rostros. Es una pauta de acciones y evidencias, no una narración atribuida a los estudiantes. Preparar local y AWS con anticipación; editar los tiempos de espera, no simular resultados.

## Preparación
- Abrir el backend y frontend de `ep3-devops-cloud`, sin modificar `nico` ni la pila original.
- Iniciar el laboratorio AWS Academy y comprobar la URL https://swakubk30d.execute-api.us-east-1.amazonaws.com. No mostrar secretos, token JWT, claves, contraseñas o correos reales.
- Descargar los artefactos [CI Swarm](https://github.com/Nicricht/edubio360-backend/actions/runs/45518491823/artifacts/14978529212) y [AWS final](https://github.com/Nicricht/edubio360-backend/actions/runs/45518492502/artifacts/14982145576) antes de grabar.
- Preparar Docker local siguiendo README y [EP3_JVY0101.md](EP3_JVY0101.md). Grabar el arranque real y recortar la espera.
- Abrir GitHub Actions, CloudFormation, EC2, SQS, Lambda, DynamoDB, API Gateway. Para consola remota, usar el acceso SSM autorizado del laboratorio.
- Mostrar un push propio real en la rama de entrega. Mostrar su build/test y las pruebas cloud existentes enlazadas; no afirmar que el push nuevo provocó un despliegue real si su job cloud fue omitido. Si se decide grabar otro despliegue real, requiere sesión vigente, gates y autorización de recursos; no repetirlo solo por estética.

## Orden de grabación

| Tiempo | Voz | Mostrar y ejecutar | IE |
|---|---|---|---|
| 0:00–0:20 | Integrante 1 | Propósito de EduBío y recorrido HTTPS → Gateway → servicios | IE16 |
| 0:20–1:15 | Integrante 1 | Dockerfile.service, build de imágenes y contenedores locales; comprobar salud y seis OpenAPI | IE2 |
| 1:15–2:15 | Integrante 1 | docker-stack.yml, docker node ls, docker stack services edubio; Manager y Worker AWS | IE3 |
| 2:15–3:05 | Integrante 1 | Escalar Analytics 1→2→1, dos nodos, respuesta; mostrar las tablas de carga de ambos servicios (384 solicitudes) | IE8 |
| 3:05–3:40 | Integrante 1 | Argumentación propia sobre escalabilidad, disponibilidad y mantenibilidad; explicar límites de Manager y MySQL únicos | IE9 |
| 3:40–4:40 | Integrante 2 | Push real, Actions, gates, ECR, CloudFormation y evidencia de despliegue por etapas; abrir URL funcional | IE6 |
| 4:40–5:20 | Integrante 2 | HTTP API y ALB interno; mostrar 401 sin JWT y 403 con rol insuficiente | IE12 |
| 5:20–6:10 | Integrante 2 | SQS y DLQ, código productor/puente, Lambda y configuración de evento | IE11, IE13 |
| 6:10–7:20 | Integrante 2 | Flujo real: solicitar orientación, confirmar, verificar notificación y constancia DynamoDB; correlacionar ID de solicitud; mostrar resultados de carga | IE14, IE15 |
| 7:20–7:40 | Ambos | Cierre propio, resultados comprobados y audio claro | IE16, IE17 |

## Comandos útiles (Bash / WSL, entorno aislado)

```bash
docker node ls
docker stack services edubio
docker service ps edubio_analytics-service
docker service scale edubio_analytics-service=2
docker service ps edubio_analytics-service
docker service scale edubio_analytics-service=1
```

Comprobar convergencia y salud después de escalar. Las acciones de escala crean carga temporal, usar solo el clúster de esta rama, no la pila Fullstack. No mostrar ni copiar el token de incorporación.

```bash
curl -fsS http://localhost:8080/actuator/health
for s in auth academic guidance notification analytics import; do
  curl -fsS "http://localhost:8080/openapi/$s" | jq '.paths | keys'
done
curl -fsS https://swakubk30d.execute-api.us-east-1.amazonaws.com/actuator/health
curl -i https://swakubk30d.execute-api.us-east-1.amazonaws.com/api/usuarios
```

Para la comprobación local de los ocho componentes internos, usar `scripts/ep3/swarm-smoke.sh` desde Bash con el stack local configurado. La última petición sin JWT debe retornar 401; no sustituye la prueba 403 autenticada con rol insuficiente.

## Evidencias a mostrar sin confundir entornos

- [Aprovisionamiento y publicación de ECR](https://github.com/Nicricht/edubio360-backend/actions/runs/45498949435) (job original falló posteriormente en arranque).
- [Arranque de ocho servicios, carga y flujo SQS → Lambda → DynamoDB](https://github.com/Nicricht/edubio360-backend/actions/runs/45508192123) (falló posteriormente en Tempo).
- [Recuperación de observabilidad y navegador AWS, intento 2 aprobado](https://github.com/Nicricht/edubio360-backend/actions/runs/45518492502/attempts/2).
- [CI y capturas Grafana aprobadas](https://github.com/Nicricht/edubio360-backend/actions/runs/45518491823); [DevSecOps aprobado](https://github.com/Nicricht/edubio360-backend/actions/runs/45518491790).
- [Artefacto AWS](https://github.com/Nicricht/edubio360-backend/actions/runs/45518492502/artifacts/14982145576): `cloud-receipt.json`, `aws-observability-summary.json`, `prior-cloud-deploy.txt`, `http-solicitud.json`, capturas de navegador.

El flujo registrado usa una solicitud de prueba; no es una constancia académica real. Para una demostración nueva, identificar la nueva solicitud, sin reutilizar su ID como si fuese el resultado del nuevo flujo. Cada integrante debe comprender y justificar personalmente las decisiones.

## Entrega
Subir enlaces de ambos repositorios a AVA y enviar copia al correo del docente. Subir el archivo MP4 o similar a AVA y enviar copia al correo. Revisar duración, audio y que no se muestren credenciales. La disponibilidad de la URL no está garantizada fuera de una sesión de AWS Academy.
