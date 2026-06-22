package com.example.itodo.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTodoStepRequest(
        @NotBlank @Size(max = 255) String title,
        Boolean isCompleted,
        Integer sortOrder
) {
}
