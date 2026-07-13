package com.example.itodo.ai.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AiReport(
    UUID id,
    UUID userId,
    String type,
    java.time.LocalDate periodStart,
    java.time.LocalDate periodEnd,
    String statsJson,
    String content,
    String model,
    OffsetDateTime createdAt
) {
}
