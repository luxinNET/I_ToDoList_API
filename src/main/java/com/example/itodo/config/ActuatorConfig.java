package com.example.itodo.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Map;

@Configuration
public class ActuatorConfig {

    @Bean
    InfoContributor applicationInfoContributor() {
        return builder -> builder.withDetail("application", Map.of(
                "name", "i-todo",
                "description", "Cross-platform TodoList API",
                "initializedAt", Instant.parse("2026-06-18T00:00:00Z")));
    }
}
