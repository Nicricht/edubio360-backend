package cl.edubio360.auth.controller;

import cl.edubio360.auth.config.GlobalExceptionHandler;
import cl.edubio360.auth.config.SecurityConfig;
import cl.edubio360.auth.dto.AuthRequests.LoginResponse;
import cl.edubio360.auth.dto.UserResponse;
import cl.edubio360.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void rechazaPostFueraDeApiSinTokenCsrf() throws Exception {
        mvc.perform(post("/internal/maintenance"))
                .andExpect(status().isForbidden());
    }
}
