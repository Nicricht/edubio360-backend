package cl.edubio360.auth.service;

import cl.edubio360.auth.dto.AuthRequests.LoginResponse;
import cl.edubio360.auth.dto.UserRequest;
import cl.edubio360.auth.dto.UserResponse;
import cl.edubio360.auth.model.UserEntity;
import cl.edubio360.auth.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public List<UserResponse> listar() {
        return users.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse obtener(Long id) {
        return toResponse(get(id));
    }

    @Transactional
    public UserResponse crear(UserRequest request) {
        String email = normalize(request.email());
        ensureUnique(email, null);
        UserEntity user = new UserEntity(email, passwordEncoder.encode(request.password()), request.role());
        if (!request.active()) {
            user.actualizar(email, passwordEncoder.encode(request.password()), request.role(), false);
        }
        return toResponse(users.save(user));
    }

    @Transactional
    public UserResponse actualizar(Long id, UserRequest request) {
        UserEntity user = get(id);
        String email = normalize(request.email());
        ensureUnique(email, id);
        user.actualizar(email, passwordEncoder.encode(request.password()), request.role(), request.active());
        return toResponse(user);
    }

    @Transactional
    public void eliminar(Long id) {
        users.delete(get(id));
    }

    @Transactional
    public UserResponse registrar(String email, String password) {
        String normalized = normalize(email);
        ensureUnique(normalized, null);
        UserEntity user = users.save(new UserEntity(normalized, passwordEncoder.encode(password), "STUDENT"));
        return toResponse(user);
    }

    public LoginResponse login(String email, String password) {
        UserEntity user = users.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        if (!user.isActive() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        return new LoginResponse(jwtService.createToken(user), user.getEmail(), user.getPrimaryRole());
    }

    public UserResponse sesionActual(String email) {
        UserEntity user = users.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida");
        }
        return toResponse(user);
    }

    private UserEntity get(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private void ensureUnique(String email, Long currentId) {
        users.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
            }
        });
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getPrimaryRole(), user.isActive());
    }
}
