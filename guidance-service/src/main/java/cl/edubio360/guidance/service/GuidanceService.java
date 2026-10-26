package cl.edubio360.guidance.service;

import cl.edubio360.guidance.AcademicValidationService;
import cl.edubio360.guidance.RabbitConfig;
import cl.edubio360.guidance.dto.ActualizarSolicitudRequest;
import cl.edubio360.guidance.dto.CrearSolicitudRequest;
import cl.edubio360.guidance.model.SolicitudOrientacion;
import cl.edubio360.guidance.repository.SolicitudRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class GuidanceService {
    private final SolicitudRepository solicitudes;
    private final AcademicValidationService academicValidation;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public GuidanceService(SolicitudRepository solicitudes,
                           AcademicValidationService academicValidation,
                           RabbitTemplate rabbitTemplate,
                           ObjectMapper objectMapper) {
        this.solicitudes = solicitudes;
        this.academicValidation = academicValidation;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public List<SolicitudOrientacion> listar() {
        return solicitudes.findAll();
    }

    public SolicitudOrientacion obtener(Long id) {
        return get(id);
    }

    @Transactional
    public SolicitudOrientacion crear(String email, String role, CrearSolicitudRequest request) {
        requireRole(role, "STUDENT");
        academicValidation.validarOferta(request.ofertaId());
        return solicitudes.save(new SolicitudOrientacion(email, request.ofertaId(), request.motivo(), request.fechaHora()));
    }

    @Transactional
    public SolicitudOrientacion actualizar(Long id, String email, String role, ActualizarSolicitudRequest request) {
        requireRole(role, "STUDENT");
        SolicitudOrientacion solicitud = get(id);
        ensureOwner(solicitud, email);
        try {
            solicitud.actualizar(request.motivo(), request.fechaHora());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
        return solicitud;
    }

    @Transactional
    public void eliminar(Long id, String email, String role) {
        requireRole(role, "STUDENT");
        SolicitudOrientacion solicitud = get(id);
        ensureOwner(solicitud, email);
        solicitudes.delete(solicitud);
    }

    public List<SolicitudOrientacion> mias(String email, String role) {
        requireRole(role, "STUDENT");
        return solicitudes.findByEstudianteEmailOrderByCreatedAtDesc(email);
    }

    @Transactional
    public SolicitudOrientacion cancelar(Long id, String email, String role) {
        requireRole(role, "STUDENT");
        SolicitudOrientacion solicitud = get(id);
        ensureOwner(solicitud, email);
        try {
            solicitud.cancelar();
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
        return solicitud;
    }

    @Transactional
    public SolicitudOrientacion confirmar(Long id, String orientadorEmail, String role) {
        requireRole(role, "ORIENTADOR");
        SolicitudOrientacion solicitud = get(id);
        try {
            solicitud.confirmar(orientadorEmail);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
        publicarConfirmacion(solicitud);
        return solicitud;
    }

    private SolicitudOrientacion get(Long id) {
        return solicitudes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
    }

    private void ensureOwner(SolicitudOrientacion solicitud, String email) {
        if (!solicitud.getEstudianteEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede modificar una solicitud de otro estudiante");
        }
    }

    private void requireRole(String actual, String expected) {
        if (actual == null || !actual.equals(expected)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La operación requiere rol " + expected);
        }
    }

    private void publicarConfirmacion(SolicitudOrientacion solicitud) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "event", "orientacion.confirmada",
                    "solicitudId", solicitud.getId(),
                    "estudianteEmail", solicitud.getEstudianteEmail(),
                    "orientadorEmail", solicitud.getOrientadorEmail(),
                    "estado", solicitud.getEstado()));
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, message);
        } catch (JacksonException ex) {
            throw new IllegalStateException("No fue posible construir el evento de confirmación", ex);
        }
    }
}
