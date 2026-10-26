package cl.edubio360.importer.controller;

import cl.edubio360.importer.config.GlobalExceptionHandler;
import cl.edubio360.importer.model.Importacion;
import cl.edubio360.importer.service.ImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImportController.class)
@Import(GlobalExceptionHandler.class)
class ImportControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ImportService service;

    @Test
    void crudYErroresHttp() throws Exception {
        Importacion entity = new Importacion("datos.csv", "VALIDADO");
        when(service.listar()).thenReturn(List.of(entity));
        mvc.perform(get("/api/importaciones")).andExpect(status().isOk());

        when(service.obtener(1L)).thenReturn(entity);
        mvc.perform(get("/api/importaciones/1")).andExpect(status().isOk());

        when(service.obtener(999999L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrada"));
        mvc.perform(get("/api/importaciones/999999")).andExpect(status().isNotFound());

        when(service.crear(any())).thenReturn(entity);
        mvc.perform(post("/api/importaciones").contentType("application/json")
                .content("{\"archivo\":\"datos.csv\",\"estado\":\"VALIDADO\",\"errores\":[]}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/importaciones").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());

        when(service.actualizar(eq(1L), any())).thenReturn(entity);
        mvc.perform(put("/api/importaciones/1").contentType("application/json")
                .content("{\"archivo\":\"datos.csv\",\"estado\":\"PROCESADO\",\"errores\":[]}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/importaciones/1")).andExpect(status().isNoContent());
    }

    @Test
    void cargaArchivoConRolAdmin() throws Exception {
        Importacion entity = new Importacion("datos.csv", "VALIDADO");
        when(service.validarArchivo(any())).thenReturn(entity);
        MockMultipartFile file = new MockMultipartFile("file", "datos.csv", "text/csv", "a,b".getBytes());

        mvc.perform(multipart("/api/importaciones/archivo").file(file).header("X-User-Role", "ADMIN"))
                .andExpect(status().isCreated());

        mvc.perform(multipart("/api/importaciones/archivo").file(file).header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
    }
}
