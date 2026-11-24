package cl.edubio360.notification.controller;

import cl.edubio360.notification.dto.NotificacionRequest;
import cl.edubio360.notification.model.Notificacion;
import cl.edubio360.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Notificacion> listar() { return service.listar(); }

    @GetMapping("/mias")
    public List<Notificacion> propias(@RequestHeader("X-User-Email") String email) { return service.propias(email); }

    @PutMapping("/mias/leidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLeidas(@RequestHeader("X-User-Email") String email) { service.marcarPropiasLeidas(email); }

    @GetMapping("/{id}")
    public Notificacion obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Notificacion crear(@Valid @RequestBody NotificacionRequest request) { return service.crear(request); }

    @PutMapping("/{id}")
    public Notificacion actualizar(@PathVariable Long id, @Valid @RequestBody NotificacionRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}
