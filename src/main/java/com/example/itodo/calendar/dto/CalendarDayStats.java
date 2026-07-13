package com.example.itodo.calendar;

import java.time.LocalDate;

public record CalendarDayStats(
    LocalDate date,
    long total,
    long completed,
    long active,
    long overdue
) {
}
