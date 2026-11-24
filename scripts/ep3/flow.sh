#!/usr/bin/env bash
set -euo pipefail
base="${BASE_URL:-http://localhost:8080}"
image="${IMAGE_PREFIX:-edubio360}/api-gateway:${IMAGE_TAG:-local}"
suffix="$(date +%s)-$RANDOM"
student="ep3-student-$suffix@example.test"
teacher="ep3-teacher-$suffix@example.test"
pass="$(openssl rand -hex 24)"
login() {
  curl -fsS -H 'Content-Type: application/json' -d "$(jq -nc --arg e "$1" --arg p "$pass" '{email:$e,password:$p}')" "$base/api/auth/login" | jq -er '.token'
}
curl -fsS -H 'Content-Type: application/json' -d "$(jq -nc --arg e "$student" --arg p "$pass" '{email:$e,password:$p}')" "$base/api/auth/register" >/dev/null
# El operador crea el orientador en la red privada; no se publica un alta privilegiada.
docker run --rm --network edubio_backend --entrypoint curl "$image" -fsS -H 'Content-Type: application/json' -d "$(jq -nc --arg e "$teacher" --arg p "$pass" '{email:$e,password:$p,role:"ORIENTADOR",active:true}')" http://auth-service:8081/api/usuarios >/dev/null
st="$(login "$student")"; ot="$(login "$teacher")"
test "$(curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $st" "$base/api/usuarios")" = 403
offer="$(curl -fsS -H "Authorization: Bearer $st" "$base/api/ofertas" | jq -er '.[0].id')"
day="$(date -u -d '+2 days' '+%Y-%m-%dT%H:%M:%S')"
request="$(curl -fsS -H "Authorization: Bearer $st" -H 'Content-Type: application/json' -d "$(jq -nc --argjson id "$offer" --arg d "$day" '{ofertaId:$id,motivo:"Prueba integrada EP3",fechaHora:$d}')" "$base/api/solicitudes")"
id="$(jq -er '.id' <<< "$request")"
test "$(jq -r '.estado' <<< "$request")" = PENDIENTE
curl -fsS -X PUT -H "Authorization: Bearer $ot" "$base/api/solicitudes/$id/confirmar" | jq -e '.estado == "CONFIRMADA"' >/dev/null
echo "Solicitud $id confirmada por Gateway"
for attempt in $(seq 1 60); do
  if docker run --rm --network edubio_backend --entrypoint curl "$image" -fsS http://notification-service:8084/api/notificaciones | jq -e --arg e "$student" 'any(.[]; .destinatario == $e and .estado == "PROCESADA")' >/dev/null; then
    echo "RabbitMQ: notificación procesada"; break
  fi
  test "$attempt" -lt 60
  sleep 3
done
if [ -n "${RECEIPT_TABLE:-}" ]; then
  for attempt in $(seq 1 60); do
    if aws dynamodb get-item --table-name "$RECEIPT_TABLE" --consistent-read --key "$(jq -nc --arg id "orientation.confirmed:$id" '{eventId:{S:$id}}')" | jq -e '.Item.estado.S == "CONSTANCIA_GENERADA"' >/dev/null; then
      echo "SQS -> Lambda -> DynamoDB: constancia verificada para solicitud $id"; break
    fi
    test "$attempt" -lt 60
    sleep 5
  done
fi
