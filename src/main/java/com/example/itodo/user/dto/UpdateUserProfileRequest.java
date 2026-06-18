package com.example.itodo.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 64) String displayName,
        @Size(max = 512) String avatarUrl
) {
}
