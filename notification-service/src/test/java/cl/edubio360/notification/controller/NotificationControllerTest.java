package cl.edubio360.notification.controller;

import cl.edubio360.notification.config.GlobalExceptionHandler;
import cl.edubio360.notification.model.Notificacion;
import cl.edubio360.notification.service.NotificationService;
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

@WebMvcTest(NotificationController.class)
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean NotificationService service;

    @Test
    void crudYErroresHttp() throws Exception {
        Notificacion entity = new Notificacion("ORIENTACION", "student@example.test", "Mensaje");
        when(service.listar()).thenReturn(List.of(entity));
        mvc.perform(get("/api/notificaciones")).andExpect(status().isOk());

        when(service.obtener(1L)).thenReturn(entity);
        mvc.perform(get("/api/notificaciones/1")).andExpect(status().isOk());

        when(service.obtener(999999L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrada"));
        mvc.perform(get("/api/notificaciones/999999")).andExpect(status().isNotFound());

        when(service.crear(any())).thenReturn(entity);
        mvc.perform(post("/api/notificaciones").contentType("application/json")
                .content("{\"tipo\":\"ORIENTACION\",\"destinatario\":\"student@example.test\",\"mensaje\":\"Mensaje\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/notificaciones").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());

        when(service.actualizar(eq(1L), any())).thenReturn(entity);
        mvc.perform(put("/api/notificaciones/1").contentType("application/json")
                .content("{\"tipo\":\"ORIENTACION\",\"destinatario\":\"student@example.test\",\"mensaje\":\"Actualizada\"}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/notificaciones/1")).andExpect(status().isNoContent());
    }
}
