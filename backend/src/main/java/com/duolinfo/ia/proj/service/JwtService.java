package com.duolinfo.ia.proj.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.duolinfo.ia.proj.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final long TOKEN_LIFETIME_MILLIS = 1000L * 60 * 60 * 24;

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret:}") String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET precisa conter ao menos 32 bytes aleatórios.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateToken(User user, String profileType) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
            .subject(user.getEmail())
            .claim("id", user.getId())
            .claim("profileType", profileType)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(issuedAt.plusMillis(TOKEN_LIFETIME_MILLIS)))
            .signWith(signingKey)
            .compact();
    }

    public String generateToken(User user) {
        return generateToken(user, "USER");
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Optional<Long> getUserIdFromToken(String token) {
        Object value = getClaims(token).get("id");
        if (value instanceof Number number) {
            return Optional.of(number.longValue());
        }
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value.toString()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
