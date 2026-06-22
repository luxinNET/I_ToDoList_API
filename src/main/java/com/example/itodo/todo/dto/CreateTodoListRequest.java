package com.example.itodo.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTodoListRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 32) String color,
        @Size(max = 64) String icon
) {
}
