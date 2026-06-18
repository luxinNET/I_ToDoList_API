package com.example.itodo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatMiniProgramLoginRequest(
        @NotBlank String code,
        String encryptedData,
        String iv,
        String clientType,
        String deviceId
) {
}
