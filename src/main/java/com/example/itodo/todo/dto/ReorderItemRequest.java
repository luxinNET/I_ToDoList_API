package com.example.itodo.todo.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReorderItemRequest(
        @NotNull UUID id,
        @NotNull Integer sortOrder
) {
}
