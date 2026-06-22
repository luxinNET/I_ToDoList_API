package com.example.itodo.todo.dto;

import java.time.Instant;
import java.util.UUID;

public record TodoListResponse(
        UUID id,
        String name,
        String color,
        String icon,
        Integer sortOrder,
        Boolean isSystem,
        Boolean isShared,
        Instant createdAt,
        Instant updatedAt
) {
}
