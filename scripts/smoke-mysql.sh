#!/usr/bin/env bash
set -euo pipefail

mkdir -p artifacts

cleanup() {
  docker compose logs --no-color > artifacts/docker-compose.log 2>&1 || true
  docker compose down -v >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "[EP02] Construyendo y levantando MySQL + microservicios..."
docker compose up -d --build \
  mysql rabbitmq discovery-server \
  auth-service academic-service guidance-service \
  notification-service analytics-service import-service api-gateway

wait_for() {
  local name="$1"
  local url="$2"
  local max_attempts="${3:-90}"
  for i in $(seq 1 "$max_attempts"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "[OK] $name"
      return 0
    fi
    sleep 2
  done
  echo "[FAIL] $name no respondió en $url" >&2
  docker compose ps >&2 || true
  return 1
}

wait_for "Discovery" "http://localhost:8761/actuator/health"
wait_for "Auth" "http://localhost:8081/actuator/health"
wait_for "Academic" "http://localhost:8082/actuator/health"
wait_for "Guidance" "http://localhost:8083/actuator/health"
wait_for "Notification" "http://localhost:8084/actuator/health"
wait_for "Analytics" "http://localhost:8085/actuator/health"
wait_for "Import" "http://localhost:8086/actuator/health"
wait_for "Gateway" "http://localhost:8080/actuator/health"

echo "[EP02] Verificando bases MySQL..."
for db in authdb academicdb guidancedb notificationdb analyticsdb importdb; do
  docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -Nse "$1"' _     "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='${db}'"     | grep -qx "$db"
  echo "[OK] $db"
done

post_json() {
  local url="$1"
  local payload="$2"
  shift 2
  curl -fsS -X POST "$url" -H "Content-Type: application/json" "$@" -d "$payload"
}

put_json() {
  local url="$1"
  local payload="$2"
  shift 2
  curl -fsS -X PUT "$url" -H "Content-Type: application/json" "$@" -d "$payload"
}

assert_status() {
  local expected="$1"
  shift
  local actual
  actual="$(curl -sS -o /tmp/ep02-response.json -w "%{http_code}" "$@")"
  if [ "$actual" != "$expected" ]; then
    echo "[FAIL] HTTP esperado=$expected actual=$actual" >&2
    cat /tmp/ep02-response.json >&2 || true
    return 1
  fi
}

echo "[EP02] Verificando API Gateway, registro, JWT y rutas protegidas..."
assert_status 401 "http://localhost:8080/api/ofertas"
gateway_email="ep02-gateway-$(date +%s)@example.test"
gateway_payload="$(printf '{"email":"%s","password":"sample-value-123"}' "$gateway_email")"
gateway_status="000"
for attempt in $(seq 1 30); do
  gateway_status="$(curl -sS -o /tmp/ep02-gateway-register.json -w '%{http_code}' \\
    -X POST "http://localhost:8080/api/auth/register" \\
    -H "Content-Type: application/json" -d "$gateway_payload")" || gateway_status="000"
  if [ "$gateway_status" = "201" ]; then break; fi
  sleep 2
done
if [ "$gateway_status" != "201" ]; then
  echo "[FAIL] Registro via Gateway: HTTP $gateway_status" >&2
  cat /tmp/ep02-gateway-register.json >&2 || true
  exit 1
fi
gateway_login="$(post_json "http://localhost:8080/api/auth/login" "$gateway_payload")"
gateway_token="$(printf '%s' "$gateway_login" | jq -r '.token')"
test -n "$gateway_token" && test "$gateway_token" != "null"
assert_status 200 -H "Authorization: Bearer $gateway_token" "http://localhost:8080/api/ofertas"
echo "[OK] Gateway autentica y enruta peticiones JWT"

echo "[EP02] CRUD Auth..."
auth_created="$(post_json "http://localhost:8081/api/usuarios"   '{"email":"ep02-ci@example.test","password":"sample-value-123","role":"STUDENT","active":true}')"
auth_id="$(echo "$auth_created" | jq -r '.id')"
test "$auth_id" != "null"
put_json "http://localhost:8081/api/usuarios/$auth_id"   '{"email":"ep02-ci-updated@example.test","password":"sample-value-456","role":"ORIENTADOR","active":true}' >/dev/null
assert_status 404 "http://localhost:8081/api/usuarios/999999"
assert_status 400 -X POST "http://localhost:8081/api/usuarios" -H "Content-Type: application/json" -d '{}'
assert_status 204 -X DELETE "http://localhost:8081/api/usuarios/$auth_id"

echo "[EP02] CRUD Academic..."
academic_created="$(post_json "http://localhost:8082/api/ofertas"   '{"carrera":"Ingeniería de Datos CI","modalidad":"Presencial","jornada":"Diurna","arancel":3100000,"matricula":175000,"sedeId":1}')"
academic_id="$(echo "$academic_created" | jq -r '.id')"
test "$academic_id" != "null"
put_json "http://localhost:8082/api/ofertas/$academic_id"   '{"carrera":"Ingeniería de Datos CI","modalidad":"Presencial","jornada":"Vespertina","arancel":3200000,"matricula":180000,"sedeId":1}' >/dev/null
assert_status 404 "http://localhost:8082/api/ofertas/999999"
assert_status 400 -X POST "http://localhost:8082/api/ofertas" -H "Content-Type: application/json" -d '{}'

echo "[EP02] CRUD Guidance..."
future_date="$(date -u -d '+2 day' '+%Y-%m-%dT%H:%M:%S')"
guidance_created="$(post_json "http://localhost:8083/api/solicitudes"   "{\"ofertaId\":1,\"motivo\":\"Orientación CI\",\"fechaHora\":\"$future_date\"}"   -H "X-User-Email: student@example.test" -H "X-User-Role: STUDENT")"
guidance_id="$(echo "$guidance_created" | jq -r '.id')"
test "$guidance_id" != "null"
put_json "http://localhost:8083/api/solicitudes/$guidance_id"   "{\"motivo\":\"Orientación CI actualizada\",\"fechaHora\":\"$future_date\"}"   -H "X-User-Email: student@example.test" -H "X-User-Role: STUDENT" >/dev/null
assert_status 404 "http://localhost:8083/api/solicitudes/999999"
assert_status 400 -X POST "http://localhost:8083/api/solicitudes"   -H "Content-Type: application/json" -H "X-User-Email: student@example.test" -H "X-User-Role: STUDENT" -d '{}'

echo "[EP02] CRUD Notification..."
notification_created="$(post_json "http://localhost:8084/api/notificaciones"   '{"tipo":"ORIENTACION","destinatario":"student@example.test","mensaje":"Notificación CI"}')"
notification_id="$(echo "$notification_created" | jq -r '.id')"
test "$notification_id" != "null"
put_json "http://localhost:8084/api/notificaciones/$notification_id"   '{"tipo":"ORIENTACION","destinatario":"student@example.test","mensaje":"Notificación CI actualizada"}' >/dev/null
assert_status 404 "http://localhost:8084/api/notificaciones/999999"
assert_status 400 -X POST "http://localhost:8084/api/notificaciones" -H "Content-Type: application/json" -d '{}'
assert_status 204 -X DELETE "http://localhost:8084/api/notificaciones/$notification_id"

echo "[EP02] CRUD Analytics..."
analytics_created="$(post_json "http://localhost:8085/api/metricas"   '{"nombre":"arancel_promedio_ci","descripcion":"Métrica CI","puntos":[{"etiqueta":"Tecnología","valor":3200000}]}')"
analytics_id="$(echo "$analytics_created" | jq -r '.id')"
test "$analytics_id" != "null"
put_json "http://localhost:8085/api/metricas/$analytics_id"   '{"nombre":"arancel_mediana_ci","descripcion":"Métrica CI actualizada","puntos":[{"etiqueta":"Tecnología","valor":3150000}]}' >/dev/null
assert_status 404 "http://localhost:8085/api/metricas/999999"
assert_status 400 -X POST "http://localhost:8085/api/metricas" -H "Content-Type: application/json" -d '{}'
assert_status 204 -X DELETE "http://localhost:8085/api/metricas/$analytics_id"

echo "[EP02] CRUD Import..."
import_created="$(post_json "http://localhost:8086/api/importaciones"   '{"archivo":"matriculas-ci.csv","estado":"VALIDADO","errores":[]}')"
import_id="$(echo "$import_created" | jq -r '.id')"
test "$import_id" != "null"
put_json "http://localhost:8086/api/importaciones/$import_id"   '{"archivo":"matriculas-ci.csv","estado":"PROCESADO","errores":[{"fila":2,"mensaje":"Fila controlada"}]}' >/dev/null
assert_status 404 "http://localhost:8086/api/importaciones/999999"
assert_status 400 -X POST "http://localhost:8086/api/importaciones" -H "Content-Type: application/json" -d '{}'
assert_status 204 -X DELETE "http://localhost:8086/api/importaciones/$import_id"

echo "[EP02] Verificando tablas JPA en MySQL..."
for spec in   "authdb:users"   "academicdb:ofertas_academicas"   "guidancedb:solicitudes_orientacion"   "notificationdb:notificaciones"   "analyticsdb:metricas"   "importdb:importaciones"; do
  db="${spec%%:*}"
  table="${spec##*:}"
  docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -Nse "$1"' _     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='${db}' AND TABLE_NAME='${table}'"     | grep -qx "$table"
  echo "[OK] $db.$table"
done

assert_status 204 -X DELETE "http://localhost:8082/api/ofertas/$academic_id"
# A successful DELETE must be observable by a fresh GET and in MySQL.
assert_status 404 "http://localhost:8082/api/ofertas/$academic_id"
academic_remaining="$(docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -Nse "$1"' _ \
  "SELECT COUNT(*) FROM academicdb.ofertas_academicas WHERE id=${academic_id}")"
if [ "$academic_remaining" != "0" ]; then
  echo "[FAIL] Academic DELETE returned 204 but id=$academic_id still exists in MySQL" >&2
  exit 1
fi
echo "[OK] Academic DELETE persisted: GET 404 and MySQL contains 0 rows"
assert_status 204 -X DELETE "http://localhost:8083/api/solicitudes/$guidance_id"   -H "X-User-Email: student@example.test" -H "X-User-Role: STUDENT"

echo "[EP02] Ejecutando colección Postman con Newman..."
docker run --rm --network host \
  -v "$PWD:/etc/newman" \
  postman/newman:alpine run /etc/newman/postman/EduBio360-EP02.postman_collection.json \
  --reporters cli,json \
  --reporter-json-export /etc/newman/artifacts/newman-report.json

echo "[SUCCESS] Smoke MySQL + Postman EP02 completado."
