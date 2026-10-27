package cl.edubio360.academic.service;

import cl.edubio360.academic.dto.OfertaRequest;
import cl.edubio360.academic.model.OfertaAcademica;
import cl.edubio360.academic.model.Sede;
import cl.edubio360.academic.repository.OfertaRepository;
import cl.edubio360.academic.repository.SedeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicServiceTest {
    @Mock OfertaRepository ofertas;
    @Mock SedeRepository sedes;
    AcademicService service;
    Sede sede;
    OfertaAcademica oferta;

    @BeforeEach
    void setUp() {
        service = new AcademicService(ofertas, sedes);
        sede = new Sede("Sede Centro", "Institución Demo", "Concepción");
        oferta = new OfertaAcademica("Ingeniería Informática", "Presencial", "Diurna",
                new BigDecimal("3000000"), new BigDecimal("180000"), sede);
    }

    @Test
    void listaTodasYFiltra() {
        when(ofertas.findAll()).thenReturn(List.of(oferta));
        assertEquals(1, service.listar(null).size());

        when(ofertas.findByCarreraContainingIgnoreCaseOrSedeInstitucionContainingIgnoreCase("info", "info"))
                .thenReturn(List.of(oferta));
        assertEquals(1, service.listar(" info ").size());

        assertEquals(1, service.listar("   ").size());
        verify(ofertas, times(2)).findAll();
    }

    @Test
    void obtieneCreaActualizaYElimina() {
        when(ofertas.findById(1L)).thenReturn(Optional.of(oferta));
        when(sedes.findById(1L)).thenReturn(Optional.of(sede));
        when(ofertas.save(any(OfertaAcademica.class))).thenAnswer(i -> i.getArgument(0));

        OfertaRequest request = new OfertaRequest("Analista Programador", "Presencial", "Vespertina",
                new BigDecimal("2500000"), new BigDecimal("150000"), 1L);

        assertEquals("Ingeniería Informática", service.obtener(1L).carrera());
        assertEquals("Analista Programador", service.crear(request).carrera());
        assertEquals("Analista Programador", service.actualizar(1L, request).carrera());

        sede.addOferta(oferta);
        assertEquals(1, sede.getOfertas().size());

        service.eliminar(1L);
        assertTrue(sede.getOfertas().isEmpty());
        verify(ofertas).delete(oferta);
    }

    @Test
    void reportaOfertaYSedeInexistentes() {
        when(ofertas.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.obtener(99L));

        when(sedes.findById(99L)).thenReturn(Optional.empty());
        OfertaRequest request = new OfertaRequest("Carrera", "Presencial", "Diurna",
                BigDecimal.ZERO, BigDecimal.ZERO, 99L);
        assertThrows(ResponseStatusException.class, () -> service.crear(request));
    }

    @Test
    void listaSedes() {
        when(sedes.findAll()).thenReturn(List.of(sede));
        assertEquals(1, service.listarSedes().size());
    }
}
