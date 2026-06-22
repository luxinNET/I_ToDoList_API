package com.example.itodo.todo.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTodoRequest(
        UUID listId,
        @NotBlank @Size(max = 255) String title,
        String note,
        LocalDate dueDate,
        Instant remindAt,
        @Size(max = 255) String repeatRule,
        Boolean myDay,
        Boolean important,
        Integer sortOrder
) {
}
