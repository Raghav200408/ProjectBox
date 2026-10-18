package com.boxproject.BoxProject.security;

import com.boxproject.BoxProject.model.UserResponseDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirationSeconds;
    private static final String ISSUER = "boxproject";

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-seconds:3600}") long expirationSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        if (expirationSeconds <= 0) throw new IllegalArgumentException("JWT expiration must be positive.");
        this.expirationSeconds = expirationSeconds;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String generateToken(UserResponseDTO user) {
        Instant now = Instant.now();
        return Jwts.builder().issuer(ISSUER).subject(user.getUserId().toString())
                .claim("role", user.getRole())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    public Claims parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer(ISSUER)
                .build().parseSignedClaims(token).getPayload();
        if (claims.getExpiration() == null || claims.getIssuedAt() == null) {
            throw new IllegalArgumentException("Missing JWT timestamps.");
        }
        return claims;
    }
}
