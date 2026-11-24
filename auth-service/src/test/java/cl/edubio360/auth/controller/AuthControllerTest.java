package cl.edubio360.auth.controller;

import cl.edubio360.auth.config.GlobalExceptionHandler;
import cl.edubio360.auth.config.SecurityConfig;
import cl.edubio360.auth.dto.AuthRequests.LoginResponse;
import cl.edubio360.auth.dto.UserResponse;
import cl.edubio360.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserService service;

    @Test
    void registraLoginYValida() throws Exception {
        when(service.registrar(anyString(), anyString()))
                .thenReturn(new UserResponse(1L, "user@example.test", "STUDENT", true));
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"email\":\"user@example.test\",\"password\":\"sample-value-123\"}"))
                .andExpect(status().isCreated());

        when(service.login(anyString(), anyString()))
                .thenReturn(new LoginResponse("token", "user@example.test", "STUDENT"));
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"user@example.test\",\"password\":\"sample-value-123\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/register").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renuevaSesionAutenticada() throws Exception {
        when(service.renovar("user@example.test", "Bearer old")).thenReturn(new LoginResponse("new", "user@example.test", "STUDENT"));
        mvc.perform(post("/api/auth/refresh").header("X-User-Email", "user@example.test")
                .header("Authorization", "Bearer old")).andExpect(status().isOk()).andExpect(jsonPath("$.token").value("new"));
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isBadRequest());
    }

    @Test
    void consultaSesionActual() throws Exception {
        when(service.sesionActual("user@example.test"))
                .thenReturn(new UserResponse(1L, "user@example.test", "STUDENT", true));
        mvc.perform(get("/api/auth/me").header("X-User-Email", "user@example.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
        mvc.perform(get("/api/auth/me")).andExpect(status().isBadRequest());
    }

    @Test
    void registraYActualizaPerfilReal() throws Exception {
        var perfil = new cl.edubio360.auth.dto.ProfileData("12345678-5", "Ana", "Prueba", "Biobío", "Concepción", "Dirección de prueba");
        var response = new UserResponse(1L, "user@example.test", "STUDENT", true, perfil);
        when(service.registrar(eq("user@example.test"), anyString(), eq(perfil))).thenReturn(response);
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"email\":\"user@example.test\",\"password\":\"sample-value-123\",\"perfil\":{\"run\":\"12345678-5\",\"nombre\":\"Ana\",\"apellidos\":\"Prueba\",\"region\":\"Biobío\",\"comuna\":\"Concepción\",\"direccion\":\"Dirección de prueba\"}}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.perfil.nombre").value("Ana"));
        when(service.actualizarPerfil("user@example.test", perfil)).thenReturn(response);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/auth/me")
                .header("X-User-Email", "user@example.test").contentType("application/json")
                .content("{\"run\":\"12345678-5\",\"nombre\":\"Ana\",\"apellidos\":\"Prueba\",\"region\":\"Biobío\",\"comuna\":\"Concepción\",\"direccion\":\"Dirección de prueba\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil.nombre").value("Ana"));
    }

    @Test
    void rechazaPostFueraDeApiSinTokenCsrf() throws Exception {
        mvc.perform(post("/internal/maintenance"))
                .andExpect(status().isForbidden());
    }
}
