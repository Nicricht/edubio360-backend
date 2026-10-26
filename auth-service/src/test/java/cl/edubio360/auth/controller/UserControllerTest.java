package cl.edubio360.auth.controller;

import cl.edubio360.auth.config.GlobalExceptionHandler;
import cl.edubio360.auth.config.SecurityConfig;
import cl.edubio360.auth.dto.UserResponse;
import cl.edubio360.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class UserControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserService service;

    @Test
    void crudYErroresHttp() throws Exception {
        UserResponse response = new UserResponse(1L, "user@example.test", "STUDENT", true);
        when(service.listar()).thenReturn(List.of(response));
        mvc.perform(get("/api/usuarios")).andExpect(status().isOk());

        when(service.obtener(1L)).thenReturn(response);
        mvc.perform(get("/api/usuarios/1")).andExpect(status().isOk());

        when(service.obtener(999999L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        mvc.perform(get("/api/usuarios/999999")).andExpect(status().isNotFound());

        when(service.crear(any())).thenReturn(response);
        mvc.perform(post("/api/usuarios").contentType("application/json")
                .content("{\"email\":\"user@example.test\",\"password\":\"sample-value-123\",\"role\":\"STUDENT\",\"active\":true}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/usuarios").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());

        when(service.actualizar(eq(1L), any())).thenReturn(response);
        mvc.perform(put("/api/usuarios/1").contentType("application/json")
                .content("{\"email\":\"user@example.test\",\"password\":\"sample-value-123\",\"role\":\"STUDENT\",\"active\":true}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/usuarios/1")).andExpect(status().isNoContent());
    }
}
