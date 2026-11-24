package cl.edubio360.guidance.controller;

import cl.edubio360.guidance.dto.ActualizarSolicitudRequest;
import cl.edubio360.guidance.dto.CrearSolicitudRequest;
import cl.edubio360.guidance.model.SolicitudOrientacion;
import cl.edubio360.guidance.service.GuidanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class GuidanceController {
    private final GuidanceService service;

    public GuidanceController(GuidanceService service) {
        this.service = service;
    }

    @GetMapping
    public List<SolicitudOrientacion> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public SolicitudOrientacion obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudOrientacion crear(@RequestHeader("X-User-Email") String email,
                                      @RequestHeader("X-User-Role") String role,
                                      @Valid @RequestBody CrearSolicitudRequest request) {
        return service.crear(email, role, request);
    }

    @PutMapping("/{id}")
    public SolicitudOrientacion actualizar(@PathVariable Long id,
                                           @RequestHeader("X-User-Email") String email,
                                           @RequestHeader("X-User-Role") String role,
                                           @Valid @RequestBody ActualizarSolicitudRequest request) {
        return service.actualizar(id, email, role, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id,
                         @RequestHeader("X-User-Email") String email,
                         @RequestHeader("X-User-Role") String role) {
        service.eliminar(id, email, role);
    }

    @GetMapping("/disponibles")
    public List<SolicitudOrientacion> disponibles(@RequestHeader("X-User-Email") String email,
                                                  @RequestHeader("X-User-Role") String role) {
        return service.disponibles(email, role);
    }

    @PutMapping("/{id}/reprogramar")
    public SolicitudOrientacion reprogramar(@PathVariable Long id, @RequestHeader("X-User-Email") String email,
                                            @RequestHeader("X-User-Role") String role,
                                            @Valid @RequestBody ActualizarSolicitudRequest request) {
        return service.reprogramar(id, email, role, request);
    }

    @PutMapping("/{id}/gestionar")
    public SolicitudOrientacion gestionar(@PathVariable Long id, @RequestHeader("X-User-Email") String email,
                                          @RequestHeader("X-User-Role") String role,
                                          @Valid @RequestBody cl.edubio360.guidance.dto.GestionSolicitudRequest request) {
        return service.gestionar(id, email, role, request);
    }

    @GetMapping("/mias")
    public List<SolicitudOrientacion> mias(@RequestHeader("X-User-Email") String email,
                                           @RequestHeader("X-User-Role") String role) {
        return service.mias(email, role);
    }

    @PutMapping("/{id}/confirmar")
    public SolicitudOrientacion confirmar(@PathVariable Long id,
                                          @RequestHeader("X-User-Email") String email,
                                          @RequestHeader("X-User-Role") String role) {
        return service.confirmar(id, email, role);
    }

    @PutMapping("/{id}/cancelar")
    public SolicitudOrientacion cancelar(@PathVariable Long id,
                                         @RequestHeader("X-User-Email") String email,
                                         @RequestHeader("X-User-Role") String role) {
        return service.cancelar(id, email, role);
    }
}
