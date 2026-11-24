package cl.edubio360.auth.service;

import cl.edubio360.auth.model.UserEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.ttl-minutes:120}") long ttlMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public String createToken(UserEntity user) {
        Instant now = Instant.now();
        return issue(user, now, now.plus(Duration.ofHours(8)));
    }

    public String renewToken(UserEntity user, String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Long end = claims.get("sessionEnd", Long.class);
        Instant now = Instant.now();
        if (end == null || end <= now.getEpochSecond() || !user.getEmail().equals(claims.getSubject())
                || !user.getPrimaryRole().equals(claims.get("role", String.class))) {
            throw new JwtException("Sesión no renovable");
        }
        return issue(user, now, Instant.ofEpochSecond(end));
    }

    private String issue(UserEntity user, Instant now, Instant deadline) {
        Instant expires = now.plus(ttl);
        if (expires.isAfter(deadline)) expires = deadline;
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getPrimaryRole())
                .id(UUID.randomUUID().toString())
                .claim("sessionEnd", deadline.getEpochSecond())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expires))
                .signWith(key)
                .compact();
    }
}
