package com.example.itodo.todo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderTodosRequest(
        @NotNull UUID listId,
        @NotEmpty @Valid List<ReorderItemRequest> items
) {
}
