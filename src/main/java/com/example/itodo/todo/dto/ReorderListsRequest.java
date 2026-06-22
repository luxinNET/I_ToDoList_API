package com.example.itodo.todo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderListsRequest(
        @NotEmpty @Valid List<ReorderItemRequest> items
) {
}
