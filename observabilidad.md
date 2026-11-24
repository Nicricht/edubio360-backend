# Evidencias de observabilidad — EP3 Ingeniería DevOps

## Alcance y estado
Rama: ep3-devops-cloud. Fullstack permanece congelado en nico. Este archivo registra evidencia factual de pruebas; el equipo debe validar los resultados y redactar sus propios análisis y reflexiones.

## Ejecución de referencia aprobada
[GitHub Actions 45360418593](https://github.com/Nicricht/edubio360-backend/actions/runs/45360418593) — versión anterior, entorno Swarm en runner, no nueva infraestructura AWS.

Extractos textuales verificados del log del job swarm (138329584119, 24-11-2026 UTC):
```text
09:02:08 Prometheus: los ocho servicios Java responden
09:02:42 Loki: registros de los ocho servicios presentes
09:02:48 Tempo: traza compartida Gateway y Academic
09:02:49 cAdvisor: CPU y memoria de contenedores presentes
09:02:55 Métricas CI/CD verificadas en Prometheus
```

| Prueba | Evidencia |
|---|---|
| Disponibilidad | prometheus-health.json, ocho servicios Java |
| Logs | loki-counts.json, registros de ocho servicios |
| Trazabilidad | tempo-trace.json, Gateway y Academic |
| Recursos | container-cpu.json, container-memory.json |
| Cobertura | prometheus-coverage.json y ci-metrics.prom |
| Duración del despliegue | prometheus-deploy-duration.json |
| Dashboard | observability/edubio.json, configuración Grafana |

[Artefacto ep3-swarm de referencia](https://github.com/Nicricht/edubio360-backend/actions/runs/45360418593/artifacts/14904487475).
[Métricas CI/CD de referencia](https://github.com/Nicricht/edubio360-backend/actions/runs/45360418593/artifacts/14904366879).

## Seguridad y calidad
En la rama nueva, commit 76a13756651665a1cd10a4743a381f6ff46b444b, [DevSecOps 45483011248](https://github.com/Nicricht/edubio360-backend/actions/runs/45483011248) aprobó Maven, Snyk Open Source (9 proyectos sin rutas vulnerables reportadas al umbral alto), Snyk Code, imagen no-root y despliegue simulado. [Reporte Snyk](https://github.com/Nicricht/edubio360-backend/actions/runs/45483011248/artifacts/14962596169). [Bloqueo histórico por vulnerabilidades](https://github.com/Nicricht/edubio360-backend/actions/runs/45322418192).

## Validación de la nueva rama
[Run 45483011255](https://github.com/Nicricht/edubio360-backend/actions/runs/45483011255): Maven y 14 pruebas Python aprobaron, pero bash -n detectó un error de sintaxis en swarm-smoke.sh. Se corrige en el mismo cambio que incorpora este documento. No se ejecutó Swarm ni AWS en ese run. Las nuevas pruebas de contadores HTTP y capturas Grafana se encuentran implementadas y requieren ejecución satisfactoria. Consultar [ejecuciones de la rama](https://github.com/Nicricht/edubio360-backend/actions?query=branch%3Aep3-devops-cloud).

## AWS
[Registro histórico AWS](docs/EP3_AWS_VALIDACION_REAL.md): corresponde a la pila anterior edubio-ep3; no acredita la nueva edubio-devops-ep3. La pila nueva requiere aprovisionamiento autorizado y validación real. No se presentan capturas simuladas como pruebas.

## Integración y revisión
Configuración: docker-stack.monitoring.yml, observability/, scripts/ep3/monitoring-smoke.sh, scripts/ep3/publish-ci-metrics.sh, scripts/ep3/grafana-browser.py y .github/workflows/ep3-cloud.yml. Los artefactos se generan en CI; incorporar resultados de la última ejecución exitosa y capturas reales al cierre. El análisis técnico, las decisiones y las reflexiones individuales deben ser redactados por los estudiantes. Declarar y citar el uso real de IA según la pauta: https://bibliotecas.duoc.cl/ia.

## Validación de la nueva rama (981b5517)

[Run 45485036677](https://github.com/Nicricht/edubio360-backend/actions/runs/45485036677): verificación, Swarm, escalamiento, flujo de negocio y observabilidad aprobaron. Prometheus confirmó contadores HTTP y errores; las métricas CI/CD se verificaron. La etapa de captura gráfica falló al buscar el campo de usuario de Grafana, por lo que Swagger y Chromium posteriores se omitieron. [Artefacto con evidencia de esa ejecución parcial](https://github.com/Nicricht/edubio360-backend/actions/runs/45485036677/artifacts/14962549960). [DevSecOps del mismo commit aprobado](https://github.com/Nicricht/edubio360-backend/actions/runs/45485036599).

Corrección: acceso a Grafana por la red overlay privada, autenticación de sesión mediante API, comprobación de usuario y dashboard y capturas reales de siete paneles. Pendiente de nueva ejecución exitosa. No representa certificación AWS.

## Evidencia CI del 24/11/2026 (commit `6e11900d`)

[Run Java Cloud](https://github.com/Nicricht/edubio360-backend/actions/runs/45493740583), [artefacto real ep3-swarm](https://github.com/Nicricht/edubio360-backend/actions/runs/45493740583/artifacts/14968078780): los ocho servicios estuvieron disponibles; existen contadores HTTP y logs de los ocho, traza Gateway/Academic, 24 series CPU y 24 de RAM, 274 s de despliegue medido y 384 solicitudes de carga con cero fallos. El informe registra un HTTP 5xx de Academic durante la ventana de observación; no se confunde con los resultados de las pruebas de carga. La API autenticada de Grafana retornó el dashboard con 11 paneles; Chromium mostró error de carga de interfaz y no se certifican las capturas. Se incorpora preflight aislado del navegador con locale explícito antes de levantar Swarm, sin sustituir la evidencia real. [DevSecOps aprobado](https://github.com/Nicricht/edubio360-backend/actions/runs/45493740170). [Comprobación AWS de solo lectura aprobada](https://github.com/Nicricht/edubio360-backend/actions/runs/45493740382); no equivale a despliegue.
