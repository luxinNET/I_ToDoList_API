package com.example.itodo.ai.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AiTodoParseResponse(
    String previewId,
    ParsedTodo todo,
    String explanation
) {
}
