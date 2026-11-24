package cl.edubio360.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private final SecretKey key;

    @Value("${ep3.strict.access:false}")
    private boolean strictAccess;

    public JwtGlobalFilter(@Value("${security.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublic(path) || (strictAccess && "GET".equals(exchange.getRequest().getMethod().name())
                && (path.equals("/api/ofertas") || path.matches("/api/ofertas/[0-9]+") || path.equals("/api/sedes")))) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange, "Token JWT requerido");
        }

        try {
            String token = authorization.substring(7);
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            if (strictAccess && !permitted(path, exchange.getRequest().getMethod().name(), role)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory()
                        .wrap("{\"status\":403,\"message\":\"Acceso denegado\"}".getBytes(StandardCharsets.UTF_8))));
            }
            ServerWebExchange mutated = exchange.mutate()
                    .request(request -> request.headers(headers -> {
                        headers.set("X-User-Email", email);
                        headers.set("X-User-Role", role);
                    }))
                    .build();
            return chain.filter(mutated);
        } catch (JwtException | IllegalArgumentException ex) {
            return unauthorized(exchange, "Token JWT inválido o vencido");
        }
    }

    static boolean permitted(String path, String method, String role) {
        if (!java.util.Set.of("STUDENT", "ORIENTADOR", "ADMIN").contains(role == null ? "" : role)) {
            return false;
        }
        if (path.startsWith("/api/usuarios") || path.startsWith("/api/importaciones")
                || path.startsWith("/api/notificaciones")) {
            return "ADMIN".equals(role);
        }
        if ((path.startsWith("/api/ofertas") || path.startsWith("/api/sedes")
                || path.startsWith("/api/analytics") || path.startsWith("/api/metricas"))
                && !"GET".equals(method)) {
            return "ADMIN".equals(role);
        }
        if (path.startsWith("/api/solicitudes") && "GET".equals(method)
                && "STUDENT".equals(role)) {
            return path.equals("/api/solicitudes/mias");
        }
        return true;
    }

    private boolean isPublic(String path) {
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.startsWith("/actuator/")
                || path.startsWith("/openapi/");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
