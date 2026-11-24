package cl.edubio360.auth.service;

import cl.edubio360.auth.dto.AuthRequests.LoginResponse;
import cl.edubio360.auth.dto.UserRequest;
import cl.edubio360.auth.model.UserEntity;
import cl.edubio360.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    UserService service;
    UserEntity user;

    @BeforeEach
    void setUp() {
        service = new UserService(users, encoder, jwt);
        user = new UserEntity("user@example.test", "hash", "STUDENT");
    }

    @Test
    void listaObtieneCreaActualizaYElimina() {
        when(users.findAll()).thenReturn(List.of(user));
        assertEquals(1, service.listar().size());

        when(users.findById(1L)).thenReturn(Optional.of(user));
        assertEquals("user@example.test", service.obtener(1L).email());

        when(encoder.encode(anyString())).thenReturn("hash");
        when(users.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));
        UserRequest request = new UserRequest("new@example.test", "sample-value-123", "ADMIN", true);
        assertEquals("ADMIN", service.crear(request).role());

        when(users.findByEmailIgnoreCase("updated@example.test")).thenReturn(Optional.empty());
        UserRequest update = new UserRequest("updated@example.test", "sample-value-456", "ORIENTADOR", true);
        assertEquals("ORIENTADOR", service.actualizar(1L, update).role());

        service.eliminar(1L);
        verify(users).delete(user);
    }

    @Test
    void creaUsuarioInactivo() {
        when(users.findByEmailIgnoreCase("inactive@example.test")).thenReturn(Optional.empty());
        when(encoder.encode(anyString())).thenReturn("hash");
        when(users.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

        UserRequest request = new UserRequest(
                " Inactive@Example.Test ",
                "sample-value-123",
                "STUDENT",
                false);

        assertFalse(service.crear(request).active());
    }

    @Test
    void registraYAutentica() {
        when(users.findByEmailIgnoreCase("student@example.test")).thenReturn(Optional.empty());
        when(encoder.encode(anyString())).thenReturn("hash");
        when(users.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals("STUDENT", service.registrar(" Student@Example.Test ", "sample-value-123").role());

        when(users.findByEmailIgnoreCase("user@example.test")).thenReturn(Optional.of(user));
        when(encoder.matches("sample-value-123", "hash")).thenReturn(true);
        when(jwt.createToken(user)).thenReturn("token");
        LoginResponse login = service.login("user@example.test", "sample-value-123");
        assertEquals("token", login.token());
    }

    @Test
    void rechazaDuplicadoCredencialesEInexistente() {
        when(users.findByEmailIgnoreCase("user@example.test")).thenReturn(Optional.of(user));
        assertThrows(ResponseStatusException.class,
                () -> service.crear(new UserRequest("user@example.test", "sample-value-123", "STUDENT", true)));

        when(encoder.matches(anyString(), anyString())).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> service.login("user@example.test", "bad-value"));

        when(users.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.obtener(99L));
    }

    @Test
    void actualizaManteniendoElMismoCorreo() throws Exception {
        var id = UserEntity.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(user, 1L);

        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.findByEmailIgnoreCase("user@example.test")).thenReturn(Optional.of(user));
        when(encoder.encode("sample-value-456")).thenReturn("hash-2");

        UserRequest request = new UserRequest(
                "USER@EXAMPLE.TEST",
                "sample-value-456",
                "STUDENT",
                true);

        assertEquals("user@example.test", service.actualizar(1L, request).email());
    }

    @Test
    void renovacionRequiereCuentaActivaYTokenValido() {
        when(users.findByEmailIgnoreCase("user@example.test")).thenReturn(Optional.of(user));
        when(jwt.renewToken(user, "old")).thenReturn("new");
        assertEquals("new", service.renovar("user@example.test", "Bearer old").token());
        assertThrows(ResponseStatusException.class, () -> service.renovar("user@example.test", null));
        when(jwt.renewToken(user, "bad")).thenThrow(new io.jsonwebtoken.JwtException("invalid"));
        assertThrows(ResponseStatusException.class, () -> service.renovar("user@example.test", "Bearer bad"));
        when(users.findByEmailIgnoreCase("missing@example.test")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.renovar("missing@example.test", "Bearer old"));
        user.actualizar("user@example.test", "hash", "STUDENT", false);
        assertThrows(ResponseStatusException.class, () -> service.renovar("user@example.test", "Bearer old"));
    }

    @Test
    void sesionActualVerificaCuentaActiva() {
        when(users.findByEmailIgnoreCase("user@example.test")).thenReturn(Optional.of(user));
        assertEquals("STUDENT", service.sesionActual("user@example.test").role());
        user.actualizar("user@example.test", "hash", "STUDENT", false);
        assertThrows(ResponseStatusException.class, () -> service.sesionActual("user@example.test"));
        when(users.findByEmailIgnoreCase("missing@example.test")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.sesionActual("missing@example.test"));
    }

    @Test
    void guardaPerfilYPermiteEditarSinCambiarClave() {
        var perfil = new cl.edubio360.auth.dto.ProfileData("12345678-5", "Ana", "Prueba", "Biobío", "Concepción", "Dirección");
        when(encoder.encode("sample-value-123")).thenReturn("hash");
        when(users.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals("Ana", service.registrar("ana@example.test", "sample-value-123", perfil).perfil().nombre());
        when(users.findByEmailIgnoreCase("user@example.test")).thenReturn(Optional.of(user));
        assertEquals("Ana", service.actualizarPerfil("user@example.test", perfil).perfil().nombre());
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.findByEmailIgnoreCase("otra@example.test")).thenReturn(Optional.empty());
        assertEquals("otra@example.test", service.actualizar(1L, new UserRequest("otra@example.test", null, "STUDENT", true, perfil)).email());
        assertEquals("hash", user.getPasswordHash());
        assertThrows(ResponseStatusException.class, () -> service.crear(new UserRequest("new@example.test", null, "STUDENT", true)));
        user.actualizar("otra@example.test", "hash", "STUDENT", false);
        assertThrows(ResponseStatusException.class, () -> service.actualizarPerfil("user@example.test", perfil));
        when(users.findByEmailIgnoreCase("missing@example.test")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.actualizarPerfil("missing@example.test", perfil));
    }

    @Test
    void rechazaLoginDeUsuarioInactivoYCorreoInexistente() {
        user.actualizar("user@example.test", "hash", "STUDENT", false);
        when(users.findByEmailIgnoreCase("user@example.test")).thenReturn(Optional.of(user));

        assertThrows(ResponseStatusException.class,
                () -> service.login("user@example.test", "sample-value-123"));
        verify(encoder, never()).matches(anyString(), anyString());

        when(users.findByEmailIgnoreCase("missing@example.test")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,
                () -> service.login("missing@example.test", "sample-value-123"));
    }
}
