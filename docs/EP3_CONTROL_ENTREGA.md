# EduBío 360 — control de evidencias EP3

Fecha de revisión: 24-11-2026. Documento de control, no informe final ni declaración de cumplimiento. Las evidencias corresponden a commits identificables; no se utilizan capturas simuladas.

## Referencias de las pautas

- JVY0101, EP3 Integración y Despliegue de Soluciones Cloud, páginas 2–8.
- DOY0101, EP3 Observabilidad y entornos reales en DevOps, páginas 2–5.
- DSY1104, EP3 Integrando backend y frontend a la tienda online, páginas 2–10, Anexo 2 Documentación APIs e Integración y Anexo 4 Manual de usuario.

## Evidencias existentes

- Backend integral: https://github.com/Nicricht/edubio360-backend/actions/runs/45323230049 — commit 2518cfbaf15951cc20c5d4bd9585b61f4c31e943; Maven, JaCoCo, Snyk dependencias y código, imagen no-root, MySQL, Postman, Gateway JWT y escalamiento de Analytics en Compose. Es un runner de prueba, no AWS.
- Frontend integral: https://github.com/Nicricht/edubio360-frontend/actions/runs/45321633056 — commit ac68fd6dac5ca8c43d49f8b7d868defe85a30821; pruebas, cobertura, compilación y Docker.
- Remediación de JAXB: https://github.com/Nicricht/edubio360-backend/actions/runs/45322199452 — historial de eliminación de la dependencia vulnerable sin excepción de Snyk.
- Evidencia negativa: https://github.com/Nicricht/edubio360-backend/actions/runs/45322418192 — Snyk falló y los jobs de imagen y despliegue se omitieron.
- Swarm: https://github.com/Nicricht/edubio360-backend/actions/runs/45323230054 — aún no certificado al corte.

## JVY0101: 17 indicadores

| Indicador | Evidencia de aceptación | Estado al corte |
|---|---|---|
| IE1 | Dockerfile.service; build de cada servicio; usuario no-root | Build de todos en rama de remediación, falta certificación última combinación |
| IE4 | Workflow CI/CD automatizado hasta cloud | Configurado; no ejecutado en AWS |
| IE5 | CloudFormation y outputs de aprovisionamiento | Plantilla existente; ejecución pendiente |
| IE7 | docker node ls con manager y worker Ready; servicios distribuidos | Pendiente AWS |
| IE10 | Fuente Python serverless, pruebas y configuración de event mapping | Código existente; prueba real pendiente |
| IE2 | Contenedores y acceso a endpoints | Aprobado en Compose; grabación pendiente |
| IE3 | Stack Swarm funcional | En validación |
| IE6 | Pipeline con servicio accesible en nube | Pendiente AWS |
| IE8 | Réplicas 1→2→1 con llamadas exitosas | Compose aprobado; Swarm pendiente |
| IE9 | Argumentación propia del equipo sobre escalabilidad, disponibilidad y mantenibilidad | Presentación del equipo |
| IE11 | Cola SQS creada y función dentro del dominio | Pendiente AWS |
| IE12 | Acceso HTTPS por API Gateway, backend no expuesto directamente y 401/403 | JWT probado en Compose; acceso cloud pendiente |
| IE13 | Lambda y ejecución verificable | Pendiente AWS |
| IE14 | Evento productor → SQS → Lambda | Pendiente AWS |
| IE15 | Petición externa a constancia persistida y escalamiento | Pendiente AWS |
| IE16 | Video coherente y técnico de 3–8 min | Grabación de ambos estudiantes |
| IE17 | Audio claro y voz de ambos integrantes | Grabación de ambos estudiantes |

## DOY0101: seis indicadores

| Indicador | Evidencia de aceptación | Estado al corte |
|---|---|---|
| IE1 (20%) | Prometheus y Loki muestran disponibilidad, uso, errores y logs de todos los microservicios | Configurado; ejecución pendiente |
| IE2 (20%) | Swarm en AWS con monitoreo y trazabilidad automatizados | Pendiente AWS |
| IE3 (10%) | Grafana muestra CPU, RAM, errores, cobertura y duración del despliegue, obtenidas de datos reales | Configurado; falta comprobar ingestión y dashboard |
| IE4 (10%) | Documentación de integración en CI/CD y explicación propia del aporte a decisiones | Evidencias técnicas parciales; explicación del equipo pendiente |
| IE5 (20%) | JaCoCo, Snyk, Maven Enforcer, scripts de auditoría y trazabilidad | Controles existentes y aprobados en backend integral |
| IE6 (20%) | Falla crítica bloquea despliegue | Evidencia negativa de ejecución 45322418192; gate cloud pendiente de ejecución |

**IA:** la pauta exige declaración y cita de herramientas utilizadas, revisión de contenido y reflexiones individuales sin IA. Este archivo solo inventaría evidencia y estado; no contiene conclusiones, justificaciones técnicas ni reflexiones de los estudiantes. Cada integrante debe redactarlas y validarlas personalmente. Referencia indicada por la pauta: https://bibliotecas.duoc.cl/ia.

## DSY1104: 12 indicadores

| Indicador grupal | Evidencia | Estado |
|---|---|---|
| IE3.1.1 (8%) | JPA, MySQL, modelos y lógica de negocio | Prueba backend aprobada |
| IE3.2.1 (8%) | CRUD y Swagger | Postman aprobado; inventario Swagger y prueba ampliada pendientes |
| IE3.2.2 (6%) | React → Gateway → REST → MySQL | Chromium integrado pendiente |
| IE3.3.1 (6%) | JWT, roles, generación, verificación y renovación segura | JWT aprobado; renovación candidata en pruebas |
| IE3.3.2 (6%) | Persistencia al recargar y cierre seguro | Tests frontend aprobados; renovación candidata pendiente |
| IE3.3.3 (6%) | Restricción de rutas, 401/403 y separación por propietario | Tests y Gateway existentes; flujo navegador pendiente |

Presentación individual (60%): IE3.1.2 (12%), IE3.2.3 (12%), IE3.2.4 (10%), IE3.3.4 (10%), IE3.3.5 (8%), IE3.3.6 (8%). Cada integrante debe demostrar y explicar el sistema completo. No se asigna nota ni se presume cumplimiento individual.

## Entregables y criterios de cierre

- Dos repositorios públicos y dos ZIP del commit final: todavía no etiquetados como entrega.
- ERS actualizado: pendiente revisión del equipo con requisitos originales.
- Documento de integración (Anexo 2): incluir URL real, Swagger, operaciones, entradas, respuestas, roles, y capturas de solicitud y resultado.
- Inventario de endpoints: ejecutar export-api-contracts.py contra el Gateway levantado para obtener JSON OpenAPI y Excel. Los contratos exportados no sustituyen las pruebas.
- Manual de usuario (Anexo 4): introducción, requisitos, instalación/configuración, registro/login, interfaz inicial, funciones y navegación. Cada captura debe tener número, título, imagen con borde negro de 1 px y nota. Capturas reales pendientes; no usar imágenes fabricadas.
- Video JVY0101: MP4 o similar, 3–8 minutos, voz real de ambos integrantes, demostración local, clúster, CI/CD cloud, escalamiento, cola, función y flujo completo. No se ha grabado.
- AVA y correo al docente: entrega por los estudiantes.
- División de responsabilidades: solamente tras certificar una versión funcional completa; mantener historial real de contribuciones.

## Condición para dar por terminada la entrega

Integrar y validar la renovación de sesión, aprobar el flujo Swarm/observabilidad/Chromium, autorizar y realizar despliegue AWS Academy, generar evidencias reales y revisar documentación. No declarar los puntos pendientes como aprobados. No publicar en main antes de la instrucción de cierre y las pruebas acordadas.
