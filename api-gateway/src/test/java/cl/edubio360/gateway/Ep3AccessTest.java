package cl.edubio360.gateway;

import org.junit.jupiter.api.Test;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;

class Ep3AccessTest {
    private static final String SECRET = "ep3-test-key-long-enough-for-hmac-sha256";
    private JwtGlobalFilter filter(boolean strict) {
        JwtGlobalFilter filter = new JwtGlobalFilter(SECRET);
        ReflectionTestUtils.setField(filter, "strictAccess", strict);
        return filter;
    }

    private GatewayFilterChain chain() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        return chain;
    }

    private String token(String role) {
        return Jwts.builder().subject("student@example.test").claim("role", role)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @Test void publicoNoRequiereToken() {
        JwtGlobalFilter filter = filter(true);
        for (String path : java.util.List.of("/api/auth/login", "/api/auth/register",
                "/actuator/health", "/openapi/academic", "/api/ofertas", "/api/ofertas/1", "/api/sedes")) {
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
            GatewayFilterChain chain = chain();
            filter.filter(exchange, chain).block();
            verify(chain).filter(exchange);
        }
        assertEquals(-100, filter.getOrder());
    }

    @Test void sinTokenOInvalidoDevuelve401() {
        JwtGlobalFilter filter = filter(true);
        GatewayFilterChain chain = chain();
        MockServerWebExchange missing = MockServerWebExchange.from(MockServerHttpRequest.get("/api/solicitudes/mias").build());
        filter.filter(missing, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, missing.getResponse().getStatusCode());
        assertTrue(missing.getResponse().getBodyAsString().block().contains("Token JWT requerido"));
        MockServerWebExchange invalid = MockServerWebExchange.from(MockServerHttpRequest.get("/api/solicitudes/mias")
                .header("Authorization", "Bearer bad").build());
        filter.filter(invalid, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, invalid.getResponse().getStatusCode());
        assertTrue(invalid.getResponse().getBodyAsString().block().contains("inválido"));
        verifyNoInteractions(chain);
    }

    @Test void tokenValidoReemplazaCabecerasNoConfiables() {
        JwtGlobalFilter filter = filter(true);
        GatewayFilterChain chain = chain();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/solicitudes/mias")
                .header("Authorization", "Bearer " + token("STUDENT"))
                .header("X-User-Role", "ADMIN").header("X-User-Email", "fake").build());
        filter.filter(exchange, chain).block();
        var capture = org.mockito.ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(capture.capture());
        assertEquals("STUDENT", capture.getValue().getRequest().getHeaders().getFirst("X-User-Role"));
        assertEquals("student@example.test", capture.getValue().getRequest().getHeaders().getFirst("X-User-Email"));
    }

    @Test void rolInsuficienteDevuelve403() {
        GatewayFilterChain chain = chain();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/usuarios")
                .header("Authorization", "Bearer " + token("STUDENT")).build());
        filter(true).filter(exchange, chain).block();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertTrue(exchange.getResponse().getBodyAsString().block().contains("Acceso denegado"));
        verifyNoInteractions(chain);
    }

    @Test void permisosDeOrientacionYNotificaciones() {
        assertTrue(JwtGlobalFilter.permitted("/api/notificaciones/mias", "GET", "STUDENT"));
        assertTrue(JwtGlobalFilter.permitted("/api/notificaciones/mias/leidas", "PUT", "STUDENT"));
        assertFalse(JwtGlobalFilter.permitted("/api/notificaciones/1", "GET", "STUDENT"));
        assertFalse(JwtGlobalFilter.permitted("/api/solicitudes", "GET", "ORIENTADOR"));
        assertTrue(JwtGlobalFilter.permitted("/api/solicitudes/disponibles", "GET", "ORIENTADOR"));
        assertTrue(JwtGlobalFilter.permitted("/api/solicitudes", "GET", "ADMIN"));
    }

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
