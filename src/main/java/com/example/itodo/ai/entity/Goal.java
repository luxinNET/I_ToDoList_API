package com.example.itodo.ai.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Goal(
    UUID id,
    UUID ownerId,
    String title,
    String description,
    String status,
    java.time.LocalDate targetDate,
    String color,
    Integer sortOrder,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    OffsetDateTime deletedAt
) {
}
