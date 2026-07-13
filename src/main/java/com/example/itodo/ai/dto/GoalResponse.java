package com.example.itodo.ai.dto;

import java.time.LocalDate;
import java.util.List;

public record GoalResponse(
    String id,
    String title,
    String description,
    String status,
    LocalDate targetDate,
    String color,
    Double completionRate,
    List<MilestoneResponse> milestones
) {
}

public record MilestoneResponse(
    String label,
    List<TodoResponse> todos,
    Double completionRate
) {
}

public record CreateGoalRequest(
    String title,
    String description,
    LocalDate targetDate,
    String color
) {
}

public record UpdateGoalRequest(
    String title,
    String description,
    String status,
    LocalDate targetDate,
    String color
) {
}

public record AiPlanRequest(
    String context
) {
}

public record AiPlanResponse(
    String previewId,
    List<MilestoneResponse> milestones
) {
}
