package com.example.itodo.calendar;

import com.example.itodo.common.api.ApiResponse;
import com.example.itodo.todo.dto.TodoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Calendar")
@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @Operation(summary = "获取日历概览")
    @GetMapping
    public ApiResponse<List<CalendarDayStats>> getCalendarOverview(
            @AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        
        List<CalendarDayStats> stats = calendarService.getCalendarOverview(currentUser.id(), from, to);
        return ApiResponse.ok(stats);
    }

    @Operation(summary = "获取指定日期的任务")
    @GetMapping("/{date}/todos")
    public ApiResponse<List<TodoResponse>> getTodosByDate(
            @AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser,
            @PathVariable LocalDate date) {
        
        List<TodoResponse> todos = calendarService.getTodosByDate(currentUser.id(), date);
        return ApiResponse.ok(todos);
    }
}
