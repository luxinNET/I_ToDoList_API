package com.example.itodo.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt
) {

    public static AuthResponse placeholder(String subject) {
        return new AuthResponse(UUID.nameUUIDFromBytes(subject.getBytes()), "pending-access-token", "pending-refresh-token", Instant.EPOCH, Instant.EPOCH);
    }
}
