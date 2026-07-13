package com.example.itodo.common.error;

import com.example.itodo.common.api.ApiError;
import com.example.itodo.common.api.ApiResponse;
import com.example.itodo.common.api.FieldViolation;
import com.example.itodo.ai.AiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldViolation> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        ApiError error = new ApiError(ErrorCode.VALIDATION_FAILED.name(), "Request validation failed", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(error, traceId(request)));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class
    })
    ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception, HttpServletRequest request) {
        ApiError error = ApiError.of(ErrorCode.VALIDATION_FAILED.name(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(error, traceId(request)));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        ApiError error = ApiError.of(ErrorCode.VALIDATION_FAILED.name(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.fail(error, traceId(request)));
    }

    @ExceptionHandler(AiException.class)
    ResponseEntity<ApiResponse<Void>> handleAiException(AiException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.getCode()) {
            case AI_SERVICE_UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
            case AI_QUOTA_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case INVALID_AI_INPUT -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        log.warn("AI exception: code={}, message={}", exception.getCode(), exception.getMessage());
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(ApiError.of(exception.getCode().name(), exception.getMessage()), traceId(request)));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.getCode()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RESOURCE_CONFLICT -> HttpStatus.CONFLICT;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(ApiError.of(exception.getCode().name(), exception.getMessage()), traceId(request)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception, HttpServletRequest request) {
        String traceId = traceId(request);
        log.error("Unexpected server error traceId={} path={}", traceId, request.getRequestURI(), exception);
        ApiError error = ApiError.of(ErrorCode.INTERNAL_ERROR.name(), "Unexpected server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(error, traceId));
    }

    private FieldViolation toViolation(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage());
    }

    private String traceId(HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        return traceId == null ? request.getHeader("X-Trace-Id") : traceId;
    }
}
