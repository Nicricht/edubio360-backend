# Remediación de seguridad del pipeline EP2

## Objetivo

Conservar el microservicio EduBio360 y su pipeline EP2, eliminando los hallazgos
de seguridad altos/críticos sin ocultarlos ni desactivar el bloqueo de CI. Una
ejecución verde exige que `verify`, Snyk SCA, Snyk Code, `image-build` y el
despliegue de prueba terminen correctamente.

## Hallazgos de origen

- Snyk Open Source detectó numerosas vulnerabilidades de dependencias de
  Spring Boot 3.5.15 / Spring Cloud 2025.0.3 y bibliotecas transitivas.
- Snyk Code detectó una desactivación global de protección CSRF en
  `auth-service/config/SecurityConfig.java`.
- El servicio usa JSON y tokens JWT en el encabezado `Authorization` a través
  de API Gateway; los endpoints públicos de registro e inicio de sesión no
  pueden exigir un token CSRF de sesión.

## Estrategias evaluadas

1. **Actualizar el stack compatible** (elegida): Spring Boot 4.0.9,
   Spring Cloud 2025.1.4, springdoc 3.x, Resilience4j 2.4.0 y ajustar el
   código/tests afectados. Menos parches transitivos independientes, pero
   requiere migración y pruebas reales.
2. Mantener Boot 3.5 y forzar dependencias transitivas: cambios menos
   visibles al principio, pero mezcla BOMs y puede no cubrir vulnerabilidades
   del propio Spring Framework.
3. Ignorar vulnerabilidades o convertir seguridad en paso no bloqueante:
   descartada porque rompería explícitamente IE3.

## Decisión de excepción acotada (autorizada por el estudiante, 26-10-2026)

La versión publicada de JAXB conserva la única CVE alta
`SNYK-JAVA-ORGGLASSFISHJAXB-20883142` (CVE-2026-92877) y no existe
todavía versión corregida en Maven Central. Se autoriza **solo esa CVE**,
hasta `2026-11-09T23:59:59.000Z`, siempre que:

- `.snyk` contenga una sola regla, con razón y fecha verificadas en CI;
- Docker configure JAXP para rechazar acceso externo a DTD, schemas y
  stylesheets, sin suprimir ninguna otra vulnerabilidad;
- Snyk Open Source continúe fallando con hallazgos altos/críticos distintos
  de esa CVE, y Snyk Code siga siendo bloqueante;
- README nombre la vulnerabilidad, el riesgo residual, la fecha de
  expiración y el procedimiento de eliminar la excepción al publicar el
  parche upstream;
- la expiración haga fallar el pipeline aunque se ignore por error desde
  la herramienta.

Esta decisión complementa la tercera estrategia descartada: **no** se
autoriza desactivar Snyk, reducir umbrales ni crear excepciones generales.

## Alcance y restricciones

- No fusionar a `main` sin revisión; mantener una rama/PR separado.
- Mantener Java 21, Maven, Docker y los ocho módulos existentes.
- Mantener Snyk SCA y SAST como barreras obligatorias, con reportes guardados.
- Configurar CSRF según rutas y tipo de autenticación, sin apagarlo
  globalmente. Conservar la semántica de las rutas JSON existentes.
- Verificar el reactor Maven y los smoke tests en GitHub Actions; no
  presentar resultados supuestos como comprobados.
- Si la migración obliga a cambiar contratos de negocio, detener esa parte
  para revisión, no reescribirlos silenciosamente.
