package cl.edubio360.analytics.controller;

import cl.edubio360.analytics.config.GlobalExceptionHandler;
import cl.edubio360.analytics.model.Metrica;
import cl.edubio360.analytics.service.AnalyticsService;
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

@WebMvcTest(AnalyticsController.class)
@Import(GlobalExceptionHandler.class)
class AnalyticsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AnalyticsService service;

    @Test
    void crudResumenYErrores() throws Exception {
        Metrica metric = new Metrica("arancel", "Descripción");
        metric.addPunto("Tecnología", 3000000d);

        when(service.listar()).thenReturn(List.of(metric));
        mvc.perform(get("/api/metricas")).andExpect(status().isOk());
        mvc.perform(get("/api/analytics/resumen")).andExpect(status().isOk());

        when(service.obtener(1L)).thenReturn(metric);
        mvc.perform(get("/api/metricas/1")).andExpect(status().isOk());

        when(service.obtener(999999L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrada"));
        mvc.perform(get("/api/metricas/999999")).andExpect(status().isNotFound());

        when(service.crear(any())).thenReturn(metric);
        mvc.perform(post("/api/metricas").contentType("application/json")
                .content("{\"nombre\":\"arancel\",\"descripcion\":\"Descripción\",\"puntos\":[{\"etiqueta\":\"Tecnología\",\"valor\":3000000}]}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/metricas").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());

        when(service.actualizar(eq(1L), any())).thenReturn(metric);
        mvc.perform(put("/api/metricas/1").contentType("application/json")
                .content("{\"nombre\":\"arancel\",\"descripcion\":\"Actualizada\",\"puntos\":[]}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/metricas/1")).andExpect(status().isNoContent());
    }
}
