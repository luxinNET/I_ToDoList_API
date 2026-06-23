package com.example.itodo.security;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class JwtSecretValidator {

    private static final String DEFAULT_SECRET = "change-me-to-a-strong-256-bit-secret-in-production";

    private final JwtProperties jwtProperties;
    private final Environment environment;

    public JwtSecretValidator(JwtProperties jwtProperties, Environment environment) {
        this.jwtProperties = jwtProperties;
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        String secret = jwtProperties.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }
        if (isProdProfile() && DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException("JWT secret must be provided through JWT_SECRET in production");
        }
    }

    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
