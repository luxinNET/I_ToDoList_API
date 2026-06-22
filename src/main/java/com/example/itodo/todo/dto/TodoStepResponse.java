package com.example.itodo.todo.dto;

import java.time.Instant;
import java.util.UUID;

public record TodoStepResponse(
        UUID id,
        UUID todoId,
        String title,
        Boolean isCompleted,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
