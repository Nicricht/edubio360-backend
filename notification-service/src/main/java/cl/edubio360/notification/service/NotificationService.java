package cl.edubio360.notification.service;

import cl.edubio360.notification.dto.NotificacionRequest;
import cl.edubio360.notification.model.Notificacion;
import cl.edubio360.notification.repository.NotificacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {
    private final NotificacionRepository repository;

    public NotificationService(NotificacionRepository repository) {
        this.repository = repository;
    }

    public List<Notificacion> listar() {
        return repository.findAll();
    }

    public Notificacion obtener(Long id) {
        return get(id);
    }

    @Transactional
    public Notificacion crear(NotificacionRequest request) {
        return repository.save(new Notificacion(request.tipo(), request.destinatario(), request.mensaje()));
    }

    @Transactional
    public Notificacion actualizar(Long id, NotificacionRequest request) {
        Notificacion entity = get(id);
        entity.actualizar(request.tipo(), request.destinatario(), request.mensaje());
        return entity;
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(get(id));
    }

    @Transactional
    public Notificacion registrarEvento(String tipo, String destinatario, String mensaje, String canal) {
        Notificacion entity = new Notificacion(tipo, destinatario, mensaje);
        entity.marcarProcesada(canal);
        return repository.save(entity);
    }

    public List<Notificacion> propias(String email) {
        return repository.findByDestinatarioIgnoreCaseOrderByCreatedAtDesc(email);
    }

    @Transactional
    public void marcarPropiasLeidas(String email) {
        propias(email).forEach(Notificacion::marcarLeida);
    }

    private Notificacion get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada"));
    }
}
