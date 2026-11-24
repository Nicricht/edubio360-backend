package cl.edubio360.gateway;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Ep3AccessTest {
    @Test void estudianteNoElevaSuRol() {
        assertFalse(JwtGlobalFilter.permitted("/api/usuarios/1", "PUT", "STUDENT"));
        assertFalse(JwtGlobalFilter.permitted("/api/usuarios", "POST", "STUDENT"));
        assertFalse(JwtGlobalFilter.permitted("/api/ofertas", "POST", "STUDENT"));
    }

    @Test void estudianteConsultaSoloSusSolicitudes() {
        assertTrue(JwtGlobalFilter.permitted("/api/solicitudes/mias", "GET", "STUDENT"));
        assertFalse(JwtGlobalFilter.permitted("/api/solicitudes/1", "GET", "STUDENT"));
        assertTrue(JwtGlobalFilter.permitted("/api/ofertas", "GET", "STUDENT"));
        assertTrue(JwtGlobalFilter.permitted("/api/solicitudes/1/confirmar", "PUT", "ORIENTADOR"));
        assertFalse(JwtGlobalFilter.permitted("/api/ofertas", "GET", null));
    }
}
