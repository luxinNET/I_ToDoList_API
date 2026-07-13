package com.example.itodo.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.itodo.todo.entity.Todo;
import com.example.itodo.todo.entity.TodoStatus;
import com.example.itodo.todo.mapper.TodoMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class ReportAggregator {

    private final TodoMapper todoMapper;

    public ReportAggregator(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    public Map<String, Object> aggregateDaily(UUID userId, LocalDate date) {
        long totalCompleted = countByDateAndStatus(userId, date, TodoStatus.COMPLETED);
        long totalCreated = countCreatedOn(userId, date);
        long overdueCount = countOverdue(userId, date);
        long totalActive = countByDateAndStatus(userId, date, TodoStatus.ACTIVE);

        return Map.of(
                "date", date.toString(),
                "totalCompleted", totalCompleted,
                "totalCreated", totalCreated,
                "overdueCount", overdueCount,
                "totalActive", totalActive,
                "completionRate", totalCreated > 0 ? (double) totalCompleted / totalCreated : 0.0
        );
    }

    public Map<String, Object> aggregateWeekly(UUID userId, LocalDate weekStart) {
        long totalCompleted = 0;
        long totalCreated = 0;
        long totalActive = 0;
        
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            totalCompleted += countByDateAndStatus(userId, date, TodoStatus.COMPLETED);
            totalCreated += countCreatedOn(userId, date);
            totalActive += countByDateAndStatus(userId, date, TodoStatus.ACTIVE);
        }

        return Map.of(
                "weekStart", weekStart.toString(),
                "totalCompleted", totalCompleted,
                "totalCreated", totalCreated,
                "totalActive", totalActive,
                "completionRate", totalCreated > 0 ? (double) totalCompleted / totalCreated : 0.0
        );
    }

    private long countByDateAndStatus(UUID userId, LocalDate date, TodoStatus status) {
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getOwnerId, userId)
               .eq(Todo::getDueDate, date)
               .eq(Todo::getStatus, status.name())
               .isNull(Todo::getDeletedAt);
        
        return todoMapper.selectCount(wrapper);
    }

    private long countCreatedOn(UUID userId, LocalDate date) {
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getOwnerId, userId)
               .eq(Todo::getCreatedAt, date)
               .isNull(Todo::getDeletedAt);
        
        return todoMapper.selectCount(wrapper);
    }

    private long countOverdue(UUID userId, LocalDate date) {
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getOwnerId, userId)
               .lt(Todo::getDueDate, date)
               .eq(Todo::getStatus, TodoStatus.ACTIVE.name())
               .isNull(Todo::getDeletedAt);
        
        return todoMapper.selectCount(wrapper);
    }
}
