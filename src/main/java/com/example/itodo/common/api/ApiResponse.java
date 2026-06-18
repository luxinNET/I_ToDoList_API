package com.example.itodo.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一 API 响应")
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        String traceId
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, String traceId) {
        return new ApiResponse<>(true, data, null, traceId);
    }

    public static ApiResponse<Void> fail(ApiError error, String traceId) {
        return new ApiResponse<>(false, null, error, traceId);
    }

    public ApiResponse<T> withTraceId(String traceId) {
        return new ApiResponse<>(success, data, error, traceId);
    }
}
