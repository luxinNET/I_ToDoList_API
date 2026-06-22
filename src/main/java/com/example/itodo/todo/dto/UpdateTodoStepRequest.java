package com.example.itodo.todo.dto;

import jakarta.validation.constraints.Size;

public record UpdateTodoStepRequest(
        @Size(max = 255) String title,
        Boolean isCompleted,
        Integer sortOrder
) {
}
