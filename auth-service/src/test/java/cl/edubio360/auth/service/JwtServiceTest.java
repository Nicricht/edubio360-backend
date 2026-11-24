package cl.edubio360.auth.service;

import cl.edubio360.auth.model.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void renuevaSinAmpliarElLimiteAbsolutoYRechazaRolesAntiguos() {
        String secret = "edubio360-test-secret-key-123456789012345";
        JwtService service = new JwtService(secret, 15);
        UserEntity user = new UserEntity("student@example.test", "hash", "STUDENT");
        String original = service.createToken(user);
        String nuevo = service.renewToken(user, original);
        var parser = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).build();
        Claims anterior = parser.parseSignedClaims(original).getPayload();
        Claims renovado = parser.parseSignedClaims(nuevo).getPayload();
        assertNotEquals(original, nuevo);
        assertEquals(anterior.get("sessionEnd", Long.class), renovado.get("sessionEnd", Long.class));
        assertEquals("STUDENT", renovado.get("role", String.class));
        assertThrows(io.jsonwebtoken.JwtException.class, () -> service.renewToken(user, "invalid"));
        user.actualizar("student@example.test", "hash", "ADMIN", true);
        assertThrows(io.jsonwebtoken.JwtException.class, () -> service.renewToken(user, original));
    }

    @Test
    void limitaExpiracionAlFinDeLaSesion() {
        String secret = "edubio360-test-secret-key-123456789012345";
        var key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        var user = new UserEntity("student@example.test", "hash", "STUDENT");
        var service = new JwtService(secret, 15);
        long end = java.time.Instant.now().plusSeconds(60).getEpochSecond();
        String corto = Jwts.builder().subject(user.getEmail()).claim("role", "STUDENT")
                .claim("sessionEnd", end).expiration(Date.from(java.time.Instant.ofEpochSecond(end)))
                .signWith(key).compact();
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(service.renewToken(user, corto)).getPayload();
        assertEquals(end, claims.getExpiration().toInstant().getEpochSecond());
        String sinLimite = Jwts.builder().subject(user.getEmail()).claim("role", "STUDENT").signWith(key).compact();
        assertThrows(io.jsonwebtoken.JwtException.class, () -> service.renewToken(user, sinLimite));
        String vencido = Jwts.builder().subject(user.getEmail()).claim("role", "STUDENT")
                .claim("sessionEnd", end).expiration(Date.from(java.time.Instant.now().minusSeconds(60))).signWith(key).compact();
        assertThrows(io.jsonwebtoken.JwtException.class, () -> service.renewToken(user, vencido));
    }

    @Test
    void generaTokenConUsuarioRolYExpiracion() {
        String secret = "edubio360-test-secret-key-123456789012345";
        JwtService service = new JwtService(secret, 30);
        UserEntity user = new UserEntity("student@example.test", "hash", "STUDENT");

        String token = service.createToken(user);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("student@example.test", claims.getSubject());
        assertEquals("STUDENT", claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }
}
