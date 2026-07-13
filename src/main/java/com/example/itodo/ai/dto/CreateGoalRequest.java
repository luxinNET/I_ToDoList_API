package com.example.itodo.ai.dto;

import java.time.LocalDate;

public record CreateGoalRequest(
    String title,
    String description,
    LocalDate targetDate,
    String color
) {
}
