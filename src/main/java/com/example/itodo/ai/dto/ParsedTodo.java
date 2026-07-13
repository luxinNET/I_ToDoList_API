package com.example.itodo.ai.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ParsedTodo(
    String title,
    String note,
    LocalDate dueDate,
    OffsetDateTime remindAt,
    String importance,
    List<String> tags,
    String listId
) {
}
