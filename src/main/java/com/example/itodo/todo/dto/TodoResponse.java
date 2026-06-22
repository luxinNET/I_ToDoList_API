package com.example.itodo.todo.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TodoResponse(
        UUID id,
        UUID listId,
        String title,
        String note,
        String status,
        String importance,
        LocalDate dueDate,
        Instant remindAt,
        String repeatRule,
        Instant completedAt,
        Integer sortOrder,
        Boolean myDay,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
