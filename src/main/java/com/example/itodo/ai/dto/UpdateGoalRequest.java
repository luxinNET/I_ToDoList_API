package com.example.itodo.ai.dto;

import java.time.LocalDate;

public record UpdateGoalRequest(
    String title,
    String description,
    String status,
    LocalDate targetDate,
    String color
) {
}
