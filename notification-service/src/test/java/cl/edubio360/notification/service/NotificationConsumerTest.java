package cl.edubio360.notification.service;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationConsumerTest {
    @Test
    void persisteEventoValido() throws Exception {
        NotificationService service = mock(NotificationService.class);
        NotificationConsumer consumer = new NotificationConsumer(new ObjectMapper(), service);
        consumer.consume("{\"event\":\"orientacion.confirmada\",\"estudianteEmail\":\"student@example.test\"}");
        verify(service).registrarEvento(eq("ORIENTACION_CONFIRMADA"), eq("student@example.test"), anyString(), eq("RABBITMQ"));
    }

    @Test
    void rechazaEventoNoSoportado() {
        NotificationService service = mock(NotificationService.class);
        NotificationConsumer consumer = new NotificationConsumer(new ObjectMapper(), service);
        assertThrows(IllegalArgumentException.class, () -> consumer.consume("{\"event\":\"otro\"}"));
    }
}
