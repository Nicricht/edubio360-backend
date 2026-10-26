package cl.edubio360.guidance.service;

import cl.edubio360.guidance.AcademicValidationService;
import cl.edubio360.guidance.RabbitConfig;
import cl.edubio360.guidance.dto.ActualizarSolicitudRequest;
import cl.edubio360.guidance.dto.CrearSolicitudRequest;
import cl.edubio360.guidance.model.SolicitudOrientacion;
import cl.edubio360.guidance.repository.SolicitudRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuidanceServiceTest {
    @Mock SolicitudRepository repository;
    @Mock AcademicValidationService academicValidation;
    @Mock RabbitTemplate rabbitTemplate;
    ObjectMapper objectMapper = new ObjectMapper();
    GuidanceService service;
    SolicitudOrientacion solicitud;

    @BeforeEach
    void setUp() {
        service = new GuidanceService(repository, academicValidation, rabbitTemplate, objectMapper);
        solicitud = new SolicitudOrientacion("student@example.test", 1L, "Orientación",
                LocalDateTime.now().plusDays(1));
    }

    @Test
    void listaObtieneCreaYFiltraPropias() {
        when(repository.findAll()).thenReturn(List.of(solicitud));
        assertEquals(1, service.listar().size());

        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        assertSame(solicitud, service.obtener(1L));

        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        CrearSolicitudRequest request = new CrearSolicitudRequest(1L, "Nueva orientación",
                LocalDateTime.now().plusDays(2));
        assertEquals("PENDIENTE", service.crear("student@example.test", "STUDENT", request).getEstado());
        verify(academicValidation).validarOferta(1L);

        when(repository.findByEstudianteEmailOrderByCreatedAtDesc("student@example.test"))
                .thenReturn(List.of(solicitud));
        assertEquals(1, service.mias("student@example.test", "STUDENT").size());
    }

    @Test
    void actualizaCancelaYEliminaComoPropietario() {
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        ActualizarSolicitudRequest update = new ActualizarSolicitudRequest("Motivo actualizado",
                LocalDateTime.now().plusDays(3));
        assertEquals("Motivo actualizado",
                service.actualizar(1L, "student@example.test", "STUDENT", update).getMotivo());

        assertEquals("CANCELADA",
                service.cancelar(1L, "student@example.test", "STUDENT").getEstado());

        SolicitudOrientacion otra = new SolicitudOrientacion("student@example.test", 1L, "Otra",
                LocalDateTime.now().plusDays(1));
        when(repository.findById(2L)).thenReturn(Optional.of(otra));
        service.eliminar(2L, "student@example.test", "STUDENT");
        verify(repository).delete(otra);
    }

    @Test
    void confirmaYPublicaEvento() {
        SolicitudOrientacion mockSolicitud = mock(SolicitudOrientacion.class);
        when(mockSolicitud.getId()).thenReturn(5L);
        when(mockSolicitud.getEstudianteEmail()).thenReturn("student@example.test");
        when(mockSolicitud.getOrientadorEmail()).thenReturn("orientador@example.test");
        when(mockSolicitud.getEstado()).thenReturn("CONFIRMADA");
        when(repository.findById(5L)).thenReturn(Optional.of(mockSolicitud));

        assertSame(mockSolicitud, service.confirmar(5L, "orientador@example.test", "ORIENTADOR"));
        verify(mockSolicitud).confirmar("orientador@example.test");
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY), anyString());
    }

    @Test
    void rechazaRolPropiedadEInexistente() {
        CrearSolicitudRequest request = new CrearSolicitudRequest(1L, "Motivo",
                LocalDateTime.now().plusDays(1));
        assertThrows(ResponseStatusException.class,
                () -> service.crear("student@example.test", "ADMIN", request));

        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        assertThrows(ResponseStatusException.class,
                () -> service.cancelar(1L, "otra@example.test", "STUDENT"));

        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.obtener(99L));
    }

    @Test
    void convierteErroresDeEstadoEnConflict() {
        SolicitudOrientacion mockSolicitud = mock(SolicitudOrientacion.class);
        when(repository.findById(10L)).thenReturn(Optional.of(mockSolicitud));
        when(mockSolicitud.getEstudianteEmail()).thenReturn("student@example.test");

        doThrow(new IllegalStateException("No editable"))
                .when(mockSolicitud).actualizar(anyString(), any(LocalDateTime.class));

        ActualizarSolicitudRequest update = new ActualizarSolicitudRequest(
                "Nuevo motivo", LocalDateTime.now().plusDays(2));

        ResponseStatusException actualizarError = assertThrows(
                ResponseStatusException.class,
                () -> service.actualizar(10L, "student@example.test", "STUDENT", update));
        assertEquals(409, actualizarError.getStatusCode().value());

        doThrow(new IllegalStateException("No cancelable"))
                .when(mockSolicitud).cancelar();

        ResponseStatusException cancelarError = assertThrows(
                ResponseStatusException.class,
                () -> service.cancelar(10L, "student@example.test", "STUDENT"));
        assertEquals(409, cancelarError.getStatusCode().value());

        doThrow(new IllegalStateException("No confirmable"))
                .when(mockSolicitud).confirmar("orientador@example.test");

        ResponseStatusException confirmarError = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmar(10L, "orientador@example.test", "ORIENTADOR"));
        assertEquals(409, confirmarError.getStatusCode().value());
    }

    @Test
    void rechazaRolNulo() {
        CrearSolicitudRequest request = new CrearSolicitudRequest(
                1L, "Motivo", LocalDateTime.now().plusDays(1));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.crear("student@example.test", null, request));

        assertEquals(403, error.getStatusCode().value());
    }

    @Test
    void fallaSiNoPuedeSerializarEventoDeConfirmacion() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        GuidanceService servicio = new GuidanceService(
                repository, academicValidation, rabbitTemplate, mapper);

        SolicitudOrientacion mockSolicitud = mock(SolicitudOrientacion.class);
        when(repository.findById(20L)).thenReturn(Optional.of(mockSolicitud));
        when(mockSolicitud.getId()).thenReturn(20L);
        when(mockSolicitud.getEstudianteEmail()).thenReturn("student@example.test");
        when(mockSolicitud.getOrientadorEmail()).thenReturn("orientador@example.test");
        when(mockSolicitud.getEstado()).thenReturn("CONFIRMADA");

        when(mapper.writeValueAsString(any()))
                .thenThrow(new JacksonException("error de prueba") {});

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> servicio.confirmar(20L, "orientador@example.test", "ORIENTADOR"));

        assertEquals("No fue posible construir el evento de confirmación", error.getMessage());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

}
