package com.example.itodo.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.itodo.todo.entity.Todo;
import com.example.itodo.todo.TodoStatus;
import com.example.itodo.todo.mapper.TodoMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SuggestionService {

    private final TodoMapper todoMapper;
    private final LlmClient llmClient;

    public SuggestionService(TodoMapper todoMapper, LlmClient llmClient) {
        this.todoMapper = todoMapper;
        this.llmClient = llmClient;
    }

    public List<Map<String, Object>> getSuggestions(UUID userId, int limit) {
        // Rule-based filtering
        List<Todo> candidates = new ArrayList<>();
        
        // 1. Overdue tasks
        LambdaQueryWrapper<Todo> overdueWrapper = new LambdaQueryWrapper<>();
        overdueWrapper.eq(Todo::getOwnerId, userId)
                     .lt(Todo::getDueDate, LocalDate.now())
                     .eq(Todo::getStatus, TodoStatus.ACTIVE)
                     .isNull(Todo::getDeletedAt)
                     .last("LIMIT " + limit);
        candidates.addAll(todoMapper.selectList(overdueWrapper));
        
        // 2. Today's tasks
        if (candidates.size() < limit) {
            LambdaQueryWrapper<Todo> todayWrapper = new LambdaQueryWrapper<>();
            todayWrapper.eq(Todo::getOwnerId, userId)
                       .eq(Todo::getDueDate, LocalDate.now())
                       .eq(Todo::getStatus, TodoStatus.ACTIVE)
                       .isNull(Todo::getDeletedAt)
                       .last("LIMIT " + (limit - candidates.size()));
            candidates.addAll(todoMapper.selectList(todayWrapper));
        }
        
        // 3. Important tasks
        if (candidates.size() < limit) {
            LambdaQueryWrapper<Todo> importantWrapper = new LambdaQueryWrapper<>();
            importantWrapper.eq(Todo::getOwnerId, userId)
                          .eq(Todo::getImportance, "IMPORTANT")
                          .eq(Todo::getStatus, TodoStatus.ACTIVE)
                          .isNull(Todo::getDeletedAt)
                          .last("LIMIT " + (limit - candidates.size()));
            candidates.addAll(todoMapper.selectList(importantWrapper));
        }
        
        // Convert to response format
        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (Todo todo : candidates) {
            suggestions.add(Map.of(
                    "todoId", todo.getId().toString(),
                    "title", todo.getTitle(),
                    "reason", generateReason(todo),
                    "priority", calculatePriority(todo)
            ));
        }
        
        return suggestions;
    }

    private String generateReason(Todo todo) {
        // Simple rule-based reason
        if (todo.getDueDate() != null && todo.getDueDate().isBefore(LocalDate.now())) {
            return "任务已逾期，建议优先处理";
        }
        if (todo.getDueDate() != null && todo.getDueDate().equals(LocalDate.now())) {
            return "今天到期，建议尽快完成";
        }
        if ("IMPORTANT".equals(todo.getImportance())) {
            return "标记为重要任务";
        }
        return "建议处理";
    }

    private String calculatePriority(Todo todo) {
        if (todo.getDueDate() != null && todo.getDueDate().isBefore(LocalDate.now())) {
            return "HIGH";
        }
        if (todo.getDueDate() != null && todo.getDueDate().equals(LocalDate.now())) {
            return "HIGH";
        }
        if ("IMPORTANT".equals(todo.getImportance())) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
