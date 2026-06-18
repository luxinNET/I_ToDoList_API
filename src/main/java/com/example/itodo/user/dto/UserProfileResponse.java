package com.example.itodo.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String phone,
        String username,
        String displayName,
        String avatarUrl,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
