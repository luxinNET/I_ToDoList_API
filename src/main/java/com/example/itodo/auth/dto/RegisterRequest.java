package com.example.itodo.auth.dto;

import com.example.itodo.common.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @Size(max = 320) String email,
        @Pattern(regexp = ValidationPatterns.MAINLAND_CHINA_PHONE) String phone,
        @NotBlank @Size(min = 8, max = 128) String password,
        @Size(max = 64) String displayName
) {
}
