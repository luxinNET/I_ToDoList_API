package com.example.itodo.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "统一错误对象")
public record ApiError(
        String code,
        String message,
        List<FieldViolation> details
) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
