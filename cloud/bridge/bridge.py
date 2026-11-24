import json
import logging
import os
import time

QUEUE = "ep3.cloud.confirmed"
DLQ = QUEUE + ".dlq"
EXCHANGE = "edubio.orientation"
ROUTING_KEY = "orientation.confirmed"


def to_cloud_event(body):
    event = json.loads(body)
    if event.get("event") != "orientacion.confirmada":
        raise ValueError("Evento no soportado")
    request_id = event["solicitudId"]
    if type(request_id) is not int or request_id <= 0:
        raise ValueError("Solicitud inválida")
    return {"eventId": f"orientation.confirmed:{request_id}",
            "event": "orientacion.confirmada", "solicitudId": request_id}


def main():
    import boto3
    import pika
    logging.basicConfig(level=logging.INFO)
    sqs = boto3.client("sqs")
    credentials = pika.PlainCredentials(os.environ["RABBITMQ_USER"],
                                        os.environ["RABBITMQ_PASSWORD"])
    params = pika.ConnectionParameters(host=os.environ["RABBITMQ_HOST"],
                                       credentials=credentials, heartbeat=60,
                                       blocked_connection_timeout=30)
    while True:
        connection = None
        try:
            connection = pika.BlockingConnection(params)
            channel = connection.channel()
            channel.exchange_declare(exchange=EXCHANGE, exchange_type="direct", durable=True)
            channel.queue_declare(queue=DLQ, durable=True)
            channel.queue_declare(queue=QUEUE, durable=True, arguments={
                "x-dead-letter-exchange": "", "x-dead-letter-routing-key": DLQ})
            channel.queue_bind(queue=QUEUE, exchange=EXCHANGE, routing_key=ROUTING_KEY)
            channel.basic_qos(prefetch_count=1)

            def consume(ch, method, properties, body):
                try:
                    event = to_cloud_event(body)
                except (ValueError, KeyError, TypeError):
                    logging.warning("Evento inválido; enviado a DLQ RabbitMQ")
                    ch.basic_reject(method.delivery_tag, requeue=False)
                    return
                try:
                    response = sqs.send_message(QueueUrl=os.environ["SQS_QUEUE_URL"],
                                                MessageBody=json.dumps(event))
                    logging.info("SQS accepted event=%s message=%s",
                                 event["eventId"], response["MessageId"])
                    ch.basic_ack(method.delivery_tag)
                except Exception:
                    logging.exception("No se pudo publicar en SQS; se reintentará")
                    ch.basic_nack(method.delivery_tag, requeue=True)
                    ch.stop_consuming()

            channel.basic_consume(queue=QUEUE, on_message_callback=consume)
            logging.info("Consumidor RabbitMQ listo")
            channel.start_consuming()
        except Exception:
            logging.exception("Conexión RabbitMQ interrumpida")
        finally:
            if connection and connection.is_open:
                connection.close()
        time.sleep(5)


if __name__ == "__main__":
    main()
