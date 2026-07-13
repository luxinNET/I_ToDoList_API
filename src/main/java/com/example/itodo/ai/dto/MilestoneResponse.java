package com.example.itodo.ai.dto;

import com.example.itodo.todo.dto.TodoResponse;
import java.util.List;

public record MilestoneResponse(
    String label,
    List<TodoResponse> todos,
    Double completionRate
) {
}
