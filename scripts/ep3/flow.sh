#!/usr/bin/env bash
set -euo pipefail
base="${BASE_URL:-http://127.0.0.1:8080}"
curl() { command curl --connect-timeout 5 --max-time 25 "$@"; }
image="${IMAGE_PREFIX:-edubio360}/api-gateway:${IMAGE_TAG:-local}"
private_auth() {
  if [ "${FLOW_COMPOSE:-false}" = true ]; then
    docker compose exec -T auth-service curl "$@"
  else
    docker run --rm --network edubio_backend --entrypoint curl "$image" "$@"
  fi
}
private_base=http://auth-service:8081
if [ "${FLOW_COMPOSE:-false}" = true ]; then private_base=http://127.0.0.1:8081; fi
suffix="$(date +%s)-$RANDOM"
student="ep3-student-$suffix@example.test"
teacher="ep3-teacher-$suffix@example.test"
admin="ep3-admin-$suffix@example.test"
other="ep3-other-$suffix@example.test"
pass="$(openssl rand -hex 24)"
login() {
  curl -fsS -H 'Content-Type: application/json' -d "$(jq -nc --arg e "$1" --arg p "$pass" '{email:$e,password:$p}')" "$base/api/auth/login" | jq -er '.token'
}
curl -fsS -H 'Content-Type: application/json' -d "$(jq -nc --arg e "$student" --arg p "$pass" '{email:$e,password:$p}')" "$base/api/auth/register" >/dev/null
# El operador provisiona el primer administrador exclusivamente en la red privada.
admin_id="$(private_auth -fsS -H 'Content-Type: application/json' -d "$(jq -nc --arg e "$admin" --arg p "$pass" '{email:$e,password:$p,role:"ADMIN",active:true}')" "$private_base/api/usuarios" | jq -er '.id')"
teacher_id=""
cleanup_privileged() {
  local code=$?
  trap - EXIT
  for account in "$teacher_id" "$admin_id"; do
    if [ -n "$account" ]; then
      private_auth -fsS --max-time 15 -X DELETE "$private_base/api/usuarios/$account" >/dev/null || echo "No se pudo eliminar cuenta temporal $account" >&2
    fi
  done
  exit "$code"
}
trap cleanup_privileged EXIT
at="$(login "$admin")"
teacher_id="$(curl -fsS -H "Authorization: Bearer $at" -H 'Content-Type: application/json' -d "$(jq -nc --arg e "$teacher" --arg p "$pass" '{email:$e,password:$p,role:"ORIENTADOR",active:true}')" "$base/api/usuarios" | jq -er '.id')"
curl -fsS -H 'Content-Type: application/json' -d "$(jq -nc --arg e "$other" --arg p "$pass" '{email:$e,password:$p}')" "$base/api/auth/register" >/dev/null
other_token="$(login "$other")"

st="$(login "$student")"; ot="$(login "$teacher")"
renewed="$(curl -fsS -X POST -H "Authorization: Bearer $st" "$base/api/auth/refresh" | jq -er '.token')"
test "$st" != "$renewed"
st="$renewed"
curl -fsS -H "Authorization: Bearer $st" "$base/api/auth/me" | jq -e --arg e "$student" '.email==$e and .role=="STUDENT"' >/dev/null
echo "Renovación JWT validada en el Gateway"
test "$(curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $st" "$base/api/usuarios")" = 403
test "$(curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $st" -H 'X-User-Role: ADMIN' "$base/api/usuarios")" = 403
curl -fsS -H "Authorization: Bearer $st" "$base/api/auth/me" | jq -e --arg e "$student" '.email == $e and .role == "STUDENT"' >/dev/null
curl -fsS -X PUT -H "Authorization: Bearer $st" -H 'Content-Type: application/json' -d '{"nombre":"Estudiante EP3","apellidos":"Prueba"}' "$base/api/auth/me" | jq -e '.perfil.nombre == "Estudiante EP3"' >/dev/null
curl -fsS -H "Authorization: Bearer $st" "$base/api/auth/me" | jq -e '.perfil.nombre == "Estudiante EP3"' >/dev/null
sede="$(curl -fsS "$base/api/sedes" | jq -er '.[0].id')"
payload="$(jq -nc --argjson sede "$sede" '{carrera:"Oferta temporal EP3",modalidad:"Presencial",jornada:"Diurno",arancel:1000,matricula:100,sedeId:$sede,area:"Tecnología",duracion:8,nivel:"Profesional",descripcion:"Prueba automatizada"}')"
test "$(curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $st" -H 'Content-Type: application/json' -d "$payload" "$base/api/ofertas")" = 403
temp_offer="$(curl -fsS -H "Authorization: Bearer $at" -H 'Content-Type: application/json' -d "$payload" "$base/api/ofertas" | jq -er '.id')"
curl -fsS "$base/api/ofertas/$temp_offer" | jq -e '.carrera == "Oferta temporal EP3"' >/dev/null
curl -fsS -X PUT -H "Authorization: Bearer $at" -H 'Content-Type: application/json' -d "$(jq '.arancel=2000' <<< "$payload")" "$base/api/ofertas/$temp_offer" | jq -e '.arancel == 2000' >/dev/null
curl -fsS "$base/api/ofertas/$temp_offer" | jq -e '.arancel == 2000' >/dev/null
test "$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE -H "Authorization: Bearer $at" "$base/api/ofertas/$temp_offer")" = 204
test "$(curl -sS -o /dev/null -w '%{http_code}' "$base/api/ofertas/$temp_offer")" = 404
echo "CRUD de ofertas, perfil persistido y restricciones de roles verificados"
offer="$(curl -fsS -H "Authorization: Bearer $st" "$base/api/ofertas" | jq -er '.[0].id')"
day="$(date -u -d '+2 days' '+%Y-%m-%dT%H:%M:%S')"
request="$(curl -fsS -H "Authorization: Bearer $st" -H 'Content-Type: application/json' -d "$(jq -nc --argjson id "$offer" --arg d "$day" '{ofertaId:$id,motivo:"Prueba integrada EP3",fechaHora:$d}')" "$base/api/solicitudes")"
id="$(jq -er '.id' <<< "$request")"
test "$(jq -r '.estado' <<< "$request")" = PENDIENTE
test "$(curl -sS -o /dev/null -w '%{http_code}' -X PUT -H "Authorization: Bearer $other_token" "$base/api/solicitudes/$id/cancelar")" = 403
curl -fsS -H "Authorization: Bearer $other_token" "$base/api/solicitudes/mias" | jq -e 'length == 0' >/dev/null
curl -fsS -H "Authorization: Bearer $st" "$base/api/solicitudes/mias" | jq -e --argjson id "$id" 'any(.[]; .id==$id)' >/dev/null
echo "Aislamiento de solicitudes por estudiante verificado"
curl -fsS -X PUT -H "Authorization: Bearer $ot" "$base/api/solicitudes/$id/confirmar" | jq -e '.estado == "CONFIRMADA"' >/dev/null
echo "Solicitud $id confirmada por Gateway"
for attempt in $(seq 1 60); do
  if curl -fsS -H "Authorization: Bearer $st" "$base/api/notificaciones/mias" | jq -e --arg e "$student" 'any(.[]; .destinatario == $e and .estado == "PROCESADA")' >/dev/null; then
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

curl -fsS -H "Authorization: Bearer $other_token" "$base/api/notificaciones/mias" | jq -e 'length == 0' >/dev/null
test "$(curl -sS -o /dev/null -w '%{http_code}' -X PUT -H "Authorization: Bearer $st" "$base/api/notificaciones/mias/leidas")" = 204
curl -fsS -H "Authorization: Bearer $st" "$base/api/notificaciones/mias" | jq -e 'length>0 and all(.[]; .leida == true)' >/dev/null
curl -fsS -X PUT -H "Authorization: Bearer $ot" -H 'Content-Type: application/json' -d '{"estado":"COMPLETADA","resultado":"Orientación de prueba completada","observacion":"Evidencia EP3"}' "$base/api/solicitudes/$id/gestionar" | jq -e '.estado == "COMPLETADA" and .resultado != null' >/dev/null
curl -fsS -H "Authorization: Bearer $st" "$base/api/solicitudes/mias" | jq -e --argjson id "$id" 'any(.[]; .id==$id and .estado=="COMPLETADA")' >/dev/null
echo "Notificaciones privadas, lectura persistida y cierre de orientación verificados"
# La trampa EXIT limpia las cuentas privilegiadas temporales aun si falla el flujo.
