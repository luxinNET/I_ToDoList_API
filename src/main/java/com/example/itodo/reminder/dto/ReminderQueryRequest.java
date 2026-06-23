package com.example.itodo.reminder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

public record ReminderQueryRequest(
        String status,
        Instant remindFrom,
        Instant remindTo,
        @Min(1) Integer page,
        @Min(1) @Max(100) Integer size
) {
    public long pageOrDefault() {
        return page == null ? 1 : page;
    }

    public long sizeOrDefault() {
        return size == null ? 20 : size;
    }
}
