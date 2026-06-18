package com.example.itodo.infra.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        Rule login,
        Rule register,
        Rule refresh
) {

    public record Rule(long permits, long intervalSeconds) {
    }
}
