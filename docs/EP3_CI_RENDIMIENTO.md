# Optimización CI/CD sin reducir cobertura

Referencia anterior dc71af52: DevSecOps 8m20s (aprobado), Java Cloud 9m31s (falló captura Grafana), portable 12m15s (aprobado). Los tiempos incluyen colas y runner; no son directamente comparables si cambian las etapas ejecutadas.

Maven clean verify sigue ejecutándose en cada pipeline. Los ocho JAR producidos por ese mismo run se transfieren como artefacto interno no recomprimido, identificado por SHA de commit y verificado con SHA-256 antes de construir imágenes. Docker conserva su etapa runtime no-root, healthcheck y JAXP. No se reutiliza un JAR de otro commit ni se usan artefactos de runs fallidos. La construcción desde fuentes sigue disponible localmente. Swarm, AWS y Compose reutilizan los JAR del job verify de su propio pipeline. Snyk conserva los mismos gates y escaneos. La convergencia de Eureka mantiene seis consultas satisfactorias y coincidencia de IP de las réplicas, con intervalo de 2s entre consultas exitosas (10s total) y 5s cuando no converge. No se reducen escenarios de carga, pruebas de negocio, observabilidad ni capturas.

Medir las siguientes ejecuciones completas antes de declarar ahorro. AWS real requiere autorización y credenciales vigentes; no se alteran nico ni sus recursos.
