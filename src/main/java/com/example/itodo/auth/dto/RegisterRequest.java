package com.example.itodo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        String email,
        String phone,
        @NotBlank @Size(min = 8, max = 128) String password,
        @Size(max = 64) String displayName
) {
}
