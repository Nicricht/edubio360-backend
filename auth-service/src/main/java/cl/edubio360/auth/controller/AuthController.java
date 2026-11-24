package cl.edubio360.auth.controller;

import cl.edubio360.auth.dto.AuthRequests.LoginRequest;
import cl.edubio360.auth.dto.AuthRequests.LoginResponse;
import cl.edubio360.auth.dto.AuthRequests.RegisterRequest;
import cl.edubio360.auth.dto.UserResponse;
import cl.edubio360.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService service;

    public AuthController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return request.perfil() == null ? service.registrar(request.email(), request.password())
                : service.registrar(request.email(), request.password(), request.perfil());
    }

    @GetMapping("/me")
    public UserResponse me(@RequestHeader("X-User-Email") String email) {
        return service.sesionActual(email);
    }

    @PutMapping("/me")
    public UserResponse actualizarPerfil(@RequestHeader("X-User-Email") String email,
                                         @Valid @RequestBody cl.edubio360.auth.dto.ProfileData perfil) {
        return service.actualizarPerfil(email, perfil);
    }

    @PostMapping("/refresh")
    public LoginResponse renovar(@RequestHeader("X-User-Email") String email,
                                  @RequestHeader("Authorization") String authorization) {
        return service.renovar(email, authorization);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request.email(), request.password());
    }
}
