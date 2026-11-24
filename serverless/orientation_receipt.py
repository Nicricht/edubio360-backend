import json
import logging
import os
from datetime import datetime, timezone

log = logging.getLogger()
log.setLevel(logging.INFO)


def process(record, table):
    event = json.loads(record["body"])
    request_id = event["solicitudId"]
    if (event.get("event") != "orientacion.confirmada"
            or type(request_id) is not int or request_id <= 0
            or event.get("eventId") != f"orientation.confirmed:{request_id}"):
        raise ValueError("Evento inválido")
    try:
        table.put_item(
            Item={"eventId": event["eventId"], "solicitudId": request_id,
                  "estado": "CONSTANCIA_GENERADA",
                  "procesadoEn": datetime.now(timezone.utc).isoformat()},
            ConditionExpression="attribute_not_exists(eventId)")
    except Exception as exc:
        if getattr(exc, "response", {}).get("Error", {}).get("Code") != "ConditionalCheckFailedException":
            raise
    log.info("Constancia registrada: %s", event["eventId"])


def handler(event, context):
    import boto3
    table = boto3.resource("dynamodb").Table(os.environ["TABLE_NAME"])
    failures = []
    for record in event["Records"]:
        try:
            process(record, table)
        except Exception:
            log.exception("Fallo al procesar mensaje")
            failures.append({"itemIdentifier": record["messageId"]})
    return {"batchItemFailures": failures}
