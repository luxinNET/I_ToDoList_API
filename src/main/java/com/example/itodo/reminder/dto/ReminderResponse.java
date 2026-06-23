package com.example.itodo.reminder.dto;

import java.time.Instant;
import java.util.UUID;

public record ReminderResponse(
        UUID id,
        UUID todoId,
        Instant remindAt,
        String channel,
        String status,
        Instant sentAt,
        Instant createdAt,
        Instant updatedAt
) {
}
