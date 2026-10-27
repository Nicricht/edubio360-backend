package cl.edubio360.notification.service;

import cl.edubio360.notification.RabbitConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final ObjectMapper objectMapper;
    private final NotificationService service;

    public NotificationConsumer(ObjectMapper objectMapper, NotificationService service) {
        this.objectMapper = objectMapper;
        this.service = service;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void consume(String message) throws Exception {
        JsonNode payload = objectMapper.readTree(message);
        if (!"orientacion.confirmada".equals(payload.path("event").asText())) {
            throw new IllegalArgumentException("Evento no soportado");
        }
        String destinatario = payload.path("estudianteEmail").asText("sin-destinatario");
        service.registrarEvento("ORIENTACION_CONFIRMADA", destinatario, message, "RABBITMQ");
        log.info("Confirmación de orientación persistida para {}", destinatario);
    }
}
