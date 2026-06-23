package com.example.itodo.reminder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateReminderRequest(
        @NotNull Instant remindAt,
        @Size(max = 32) String channel
) {
}
