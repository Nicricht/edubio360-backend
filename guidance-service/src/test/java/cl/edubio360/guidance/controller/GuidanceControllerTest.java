package cl.edubio360.guidance.controller;

import cl.edubio360.guidance.config.GlobalExceptionHandler;
import cl.edubio360.guidance.model.SolicitudOrientacion;
import cl.edubio360.guidance.service.GuidanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GuidanceController.class)
@Import(GlobalExceptionHandler.class)
class GuidanceControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean GuidanceService service;

    private SolicitudOrientacion solicitud() {
        return new SolicitudOrientacion("student@example.test", 1L, "Orientación",
                LocalDateTime.now().plusDays(2));
    }

    @Test
    void crudYErroresHttp() throws Exception {
        when(service.listar()).thenReturn(List.of(solicitud()));
        mvc.perform(get("/api/solicitudes")).andExpect(status().isOk());

        when(service.obtener(1L)).thenReturn(solicitud());
        mvc.perform(get("/api/solicitudes/1")).andExpect(status().isOk());

        when(service.obtener(999999L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrada"));
        mvc.perform(get("/api/solicitudes/999999")).andExpect(status().isNotFound());

        when(service.crear(anyString(), anyString(), any())).thenReturn(solicitud());
        mvc.perform(post("/api/solicitudes")
                .header("X-User-Email", "student@example.test")
                .header("X-User-Role", "STUDENT")
                .contentType("application/json")
                .content("{\"ofertaId\":1,\"motivo\":\"Orientación\",\"fechaHora\":\"2099-01-01T10:00:00\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/solicitudes")
                .header("X-User-Email", "student@example.test")
                .header("X-User-Role", "STUDENT")
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());

        when(service.actualizar(eq(1L), anyString(), anyString(), any())).thenReturn(solicitud());
        mvc.perform(put("/api/solicitudes/1")
                .header("X-User-Email", "student@example.test")
                .header("X-User-Role", "STUDENT")
                .contentType("application/json")
                .content("{\"motivo\":\"Actualizada\",\"fechaHora\":\"2099-01-02T10:00:00\"}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/solicitudes/1")
                .header("X-User-Email", "student@example.test")
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNoContent());
    }

    @Test
    void endpointsDeDominio() throws Exception {
        when(service.mias(anyString(), anyString())).thenReturn(List.of(solicitud()));
        mvc.perform(get("/api/solicitudes/mias")
                .header("X-User-Email", "student@example.test")
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk());

        when(service.confirmar(eq(1L), anyString(), anyString())).thenReturn(solicitud());
        mvc.perform(put("/api/solicitudes/1/confirmar")
                .header("X-User-Email", "orientador@example.test")
                .header("X-User-Role", "ORIENTADOR"))
                .andExpect(status().isOk());

        when(service.cancelar(eq(1L), anyString(), anyString())).thenReturn(solicitud());
        mvc.perform(put("/api/solicitudes/1/cancelar")
                .header("X-User-Email", "student@example.test")
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk());
    }
}
