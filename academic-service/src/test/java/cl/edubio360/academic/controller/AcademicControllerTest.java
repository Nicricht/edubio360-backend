package cl.edubio360.academic.controller;

import cl.edubio360.academic.config.GlobalExceptionHandler;
import cl.edubio360.academic.dto.OfertaResponse;
import cl.edubio360.academic.dto.SedeResponse;
import cl.edubio360.academic.service.AcademicService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AcademicController.class)
@Import(GlobalExceptionHandler.class)
class AcademicControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AcademicService service;

    private OfertaResponse response() {
        return new OfertaResponse(1L, "Ingeniería Informática", "Institución", 1L, "Sede", "Concepción",
                "Presencial", "Diurna", new BigDecimal("3000000"), new BigDecimal("180000"));
    }

    @Test
    void crudYErroresHttp() throws Exception {
        when(service.listar(null)).thenReturn(List.of(response()));
        mvc.perform(get("/api/ofertas")).andExpect(status().isOk());

        when(service.obtener(1L)).thenReturn(response());
        mvc.perform(get("/api/ofertas/1")).andExpect(status().isOk());

        when(service.obtener(999999L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada"));
        mvc.perform(get("/api/ofertas/999999")).andExpect(status().isNotFound());

        when(service.crear(any())).thenReturn(response());
        mvc.perform(post("/api/ofertas").contentType("application/json")
                .content("{\"carrera\":\"Ingeniería\",\"modalidad\":\"Presencial\",\"jornada\":\"Diurna\",\"arancel\":3000000,\"matricula\":180000,\"sedeId\":1}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/ofertas").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/ofertas").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());

        when(service.listarSedes()).thenReturn(List.of(
                new SedeResponse(1L, "Sede", "Institución", "Concepción")));
        mvc.perform(get("/api/sedes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Sede"));

        when(service.listar("info")).thenReturn(List.of(response()));
        mvc.perform(get("/api/ofertas").param("q", "info"))
                .andExpect(status().isOk());

        when(service.actualizar(eq(1L), any())).thenReturn(response());
        mvc.perform(put("/api/ofertas/1").contentType("application/json")
                .content("{\"carrera\":\"Ingeniería\",\"modalidad\":\"Presencial\",\"jornada\":\"Diurna\",\"arancel\":3000000,\"matricula\":180000,\"sedeId\":1}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/ofertas/1")).andExpect(status().isNoContent());
        verify(service).eliminar(1L);
    }
}
