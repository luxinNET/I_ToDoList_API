package com.example.itodo.todo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TodoQueryRequest(
        UUID listId,
        String status,
        Boolean important,
        Boolean myDay,
        LocalDate dueFrom,
        LocalDate dueTo,
        Instant remindFrom,
        Instant remindTo,
        String keyword,
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
