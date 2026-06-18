package com.example.itodo.auth;

import com.example.itodo.common.web.ClientHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

public record ClientContext(
        String clientType,
        String deviceId,
        String ipAddress,
        String userAgent
) {

    public static ClientContext from(HttpServletRequest servletRequest, String clientType, String deviceId) {
        return new ClientContext(
                firstText(clientType, servletRequest.getHeader(ClientHeaders.CLIENT_TYPE)),
                firstText(deviceId, servletRequest.getHeader(ClientHeaders.DEVICE_ID)),
                clientIp(servletRequest),
                servletRequest.getHeader(HttpHeaders.USER_AGENT));
    }

    private static String firstText(String first, String second) {
        if (hasText(first)) {
            return first.trim();
        }
        return hasText(second) ? second.trim() : null;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
