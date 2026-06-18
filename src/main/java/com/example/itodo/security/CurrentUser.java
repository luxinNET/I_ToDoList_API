package com.example.itodo.security;

import java.util.UUID;

public record CurrentUser(
        UUID id,
        String username,
        String clientType,
        String deviceId
) {
}
