# EduBío 360 — Backend de Microservicios


> **EP02 / defensa oral:** antes de modificar o explicar el proyecto, leer [docs/PAUTA_EVALUACION_EP02_JVY0101.md](docs/PAUTA_EVALUACION_EP02_JVY0101.md). Ese archivo es el documento maestro de la evaluación: contiene la rúbrica, el estado certificado, la evidencia por IE, la arquitectura que debe explicarse y una guía para que otro chat pueda enseñar y preparar la defensa técnica.


Backend de EduBío 360 para exploración, comparación y orientación sobre Educación Superior del Biobío.

La implementación EP02 se construye sobre la arquitectura existente y se alinea con `docs/PAUTA_EVALUACION_EP02_JVY0101.md`.

## Stack

- Java 21
- Spring Boot 3
- Maven
- Spring Data JPA / Hibernate
- MySQL 8
- H2 para pruebas rápidas
- Spring Validation
- Springdoc OpenAPI / Swagger
- Spring Cloud Gateway
- Eureka
- OpenFeign
- Resilience4j
- RabbitMQ
- JUnit / Mockito / MockMvc
- Cucumber
- JaCoCo
- Docker / Docker Compose

## Microservicios

| Servicio | Puerto | Recurso principal |
|---|---:|---|
| Auth Service | 8081 | `/api/usuarios` |
| Academic Service | 8082 | `/api/ofertas` |
| Guidance Service | 8083 | `/api/solicitudes` |
| Notification Service | 8084 | `/api/notificaciones` |
| Analytics Service | 8085 | `/api/metricas` |
| Import Service | 8086 | `/api/importaciones` |
| API Gateway | 8080 | entrada única |
| Discovery Server | 8761 | Eureka |

## Base de datos

MySQL 8 es el motor persistente del backend.

Docker Compose crea una base lógica por microservicio:

```text
authdb
academicdb
guidancedb
notificationdb
analyticsdb
importdb
```

El perfil H2 existe para pruebas y ejecución rápida. La configuración Oracle anterior ya no forma parte de la decisión vigente del backend.

## Perfiles

Por defecto:

```bash
mvn spring-boot:run
```

usa el perfil `h2`.

Para MySQL:

```bash
SPRING_PROFILES_ACTIVE=mysql \
DB_HOST=localhost \
DB_PORT=3306 \
DB_NAME=academicdb \
DB_USER=root \
DB_PASSWORD=root \
mvn spring-boot:run
```

En Windows PowerShell se pueden definir las variables con `$env:NOMBRE="valor"`.

## Ejecución completa

Copiar variables de ejemplo:

```bash
cp .env.example .env
```

Levantar infraestructura y servicios:

```bash
docker compose up --build
```

Detener:

```bash
docker compose down
```

Eliminar también el volumen MySQL:

```bash
docker compose down -v
```

## Construcción Docker y conectividad Maven

Los ocho microservicios comparten una caché de dependencias Maven de BuildKit
(`/root/.m2/repository`, con escritura bloqueada mientras otro módulo descarga).
Esto reduce descargas simultáneas repetidas y el riesgo de saturar las conexiones
a Maven Central. La compilación conserva Java 21 y Spring Boot 3.5.15.
El repositorio también ignora archivos locales `.env` y salidas `target`
en el contexto de construcción Docker.

Si Docker muestra `Unknown host repo.maven.apache.org`,
`Temporary failure in name resolution` o errores de TLS durante la descarga,
el problema puede estar en la red, DNS, proxy o certificados del Docker Engine
**local**. No significa que el código Java esté mal y una caché no puede
resolver una interrupción de red persistente.

Diagnóstico de conectividad sin cambiar archivos ni volúmenes:

```powershell
docker run --rm maven:3.9.9-eclipse-temurin-21 sh -c "getent hosts repo.maven.apache.org"
```

Si el diagnóstico de DNS falla, revisar red/DNS del Docker Desktop local.
Si DNS funciona pero Maven falla por TLS, revisar proxy/certificados de red.
No usar `docker compose down -v` ni eliminar volúmenes como remedio de conectividad.

La CI de GitHub prueba el build en Linux y los servicios con MySQL/RabbitMQ;
un resultado correcto allí **no garantiza** el DNS/TLS de Docker Desktop en
otra computadora.

## Contrato CRUD EP02

Cada microservicio evaluable expone un recurso principal con el patrón:

```text
GET    /api/<recurso>       -> 200
GET    /api/<recurso>/{id}  -> 200 / 404
POST   /api/<recurso>       -> 201 / 400
PUT    /api/<recurso>/{id}  -> 200 / 400 / 404
DELETE /api/<recurso>/{id}  -> 204 / 404
```

Las respuestas de error se homogeneizan mediante `@RestControllerAdvice`.

## Swagger y OpenAPI

**Una interfaz para los seis microservicios a través del API Gateway:**

```text
http://localhost:8080/swagger-ui/index.html
```

En el desplegable «Select a definition», elegir Auth, Academic, Guidance, Notification, Analytics o Import.
El Gateway expone las especificaciones JSON mediante `/openapi/auth`, `/openapi/academic`,
`/openapi/guidance`, `/openapi/notification`, `/openapi/analytics` y `/openapi/import`.
La documentación es pública; las operaciones de negocio bajo `/api/**` mantienen la validación JWT.
Para utilizar «Try it out» en endpoints protegidos, aportar el token de autenticación.

Además, cada microservicio conserva su Swagger individual:



```text
http://localhost:<puerto>/swagger-ui/index.html
http://localhost:<puerto>/v3/api-docs
http://localhost:<puerto>/v3/api-docs.yaml
```

## Pruebas

Verificación completa desde la raíz:

```bash
mvn clean verify
```

Empaquetado:

```bash
mvn clean package
```

Los reportes JaCoCo se generan dentro de `target/site/jacoco/` de los módulos que ejecutan pruebas.

## Postman

La colección EP02 se almacena en:

```text
postman/EduBio360-EP02.postman_collection.json
```

Debe cubrir, por servicio:

1. crear -> 201;
2. listar -> 200;
3. obtener por id -> 200;
4. obtener inexistente -> 404;
5. crear inválido -> 400;
6. actualizar -> 200;
7. eliminar -> 204.

### Postman por API Gateway (nueva colección, sin sustituir la original)

`postman/EduBio360-EP02-Gateway.postman_collection.json` contiene 47 solicitudes:
las 42 de CRUD, autenticación JWT (401 sin token, registro, login, lectura protegida) y
eliminación del usuario temporal al finalizar.

Importarla en Postman y ejecutar **Run collection** con los contenedores encendidos.
`gatewayUrl` apunta a `http://localhost:8080`; el token se obtiene automáticamente.
La colección histórica `EduBio360-EP02.postman_collection.json` no se modifica y mantiene
las pruebas independientes contra cada servicio. GitHub Actions ejecuta **ambas** colecciones
con Newman durante su smoke test Docker. Los resultados se archivan en
`artifacts/newman-report.json` y `artifacts/newman-gateway-report.json`.

## GitFlow

- `main`: versión estable.
- `develop`: integración.
- `feature/*`: funcionalidades y tareas.
- `hotfix/*`: correcciones urgentes.

La EP02 se desarrolla mediante ramas `feature/*` y Pull Requests hacia `develop`.

## Documentación por microservicio

Cada servicio de negocio tiene su propio README con puerto, endpoints, perfiles de base de datos, comandos de ejecución, pruebas y rutas de documentación:

- [Auth Service](auth-service/README.md)
- [Academic Service](academic-service/README.md)
- [Guidance Service](guidance-service/README.md)
- [Notification Service](notification-service/README.md)
- [Analytics Service](analytics-service/README.md)
- [Import Service](import-service/README.md)

## Pauta y trazabilidad

- Pauta consolidada: `docs/PAUTA_EVALUACION_EP02_JVY0101.md`
- Arquitectura actual: `docs/ARQUITECTURA_FASE_1.md`
- Estado de cumplimiento: `docs/CUMPLIMIENTO_EP02.md`

La regla de trabajo es:

```text
pauta -> estado real -> implementación -> prueba -> evidencia
```

Una funcionalidad no se marca como cumplida solamente porque exista código. Debe existir evidencia verificable.
