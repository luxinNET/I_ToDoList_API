package com.example.itodo.reminder.dto;

import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateReminderRequest(
        Instant remindAt,
        @Size(max = 32) String channel,
        @Size(max = 32) String status
) {
}
