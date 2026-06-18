package com.example.itodo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank String account,
        @NotBlank @Size(min = 8, max = 128) String password,
        String clientType,
        String deviceId
) {
}
