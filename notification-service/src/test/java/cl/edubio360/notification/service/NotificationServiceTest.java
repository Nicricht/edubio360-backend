package cl.edubio360.notification.service;

import cl.edubio360.notification.dto.NotificacionRequest;
import cl.edubio360.notification.model.Notificacion;
import cl.edubio360.notification.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificacionRepository repository;
    NotificationService service;
    Notificacion entity;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository);
        entity = new Notificacion("ORIENTACION", "student@example.test", "Mensaje");
    }

    @Test
    void crudCompleto() {
        when(repository.findAll()).thenReturn(List.of(entity));
        assertEquals(1, service.listar().size());

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        assertSame(entity, service.obtener(1L));

        when(repository.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));
        NotificacionRequest request = new NotificacionRequest("AVISO", "student@example.test", "Nuevo");
        assertEquals("AVISO", service.crear(request).getTipo());
        assertEquals("AVISO", service.actualizar(1L, request).getTipo());

        service.eliminar(1L);
        verify(repository).delete(entity);
    }

    @Test
    void registraEventoConEnvio() {
        when(repository.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));
        Notificacion saved = service.registrarEvento("ORIENTACION", "student@example.test", "payload", "RABBITMQ");
        assertEquals("PROCESADA", saved.getEstado());
        assertEquals(1, saved.getEnvios().size());
    }

    @Test
    void consultaYMarcaSoloPropias() {
        when(repository.findByDestinatarioIgnoreCaseOrderByCreatedAtDesc("student@example.test")).thenReturn(List.of(entity));
        assertEquals(1, service.propias("student@example.test").size());
        service.marcarPropiasLeidas("student@example.test");
        assertTrue(entity.isLeida());
    }

    @Test
    void notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.obtener(99L));
    }
}
