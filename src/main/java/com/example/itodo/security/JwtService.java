package com.example.itodo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_CLIENT_TYPE = "client_type";
    private static final String CLAIM_DEVICE_ID = "device_id";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret);
    }

    public IssuedAccessToken issue(CurrentUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        String token = Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.id().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USERNAME, nullToEmpty(user.username()))
                .claim(CLAIM_CLIENT_TYPE, nullToEmpty(user.clientType()))
                .claim(CLAIM_DEVICE_ID, nullToEmpty(user.deviceId()))
                .signWith(signingKey)
                .compact();
        return new IssuedAccessToken(token, expiresAt);
    }

    public CurrentUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new CurrentUser(
                    UUID.fromString(claims.getSubject()),
                    emptyToNull(claims.get(CLAIM_USERNAME, String.class)),
                    emptyToNull(claims.get(CLAIM_CLIENT_TYPE, String.class)),
                    emptyToNull(claims.get(CLAIM_DEVICE_ID, String.class)));
        } catch (IllegalArgumentException | JwtException exception) {
            throw new InvalidJwtException("Invalid access token", exception);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record IssuedAccessToken(String token, Instant expiresAt) {
    }

    public static class InvalidJwtException extends RuntimeException {

        public InvalidJwtException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
