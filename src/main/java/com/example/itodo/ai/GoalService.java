package com.example.itodo.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.itodo.ai.entity.Goal;
import com.example.itodo.ai.mapper.GoalMapper;
import com.example.itodo.todo.entity.Todo;
import com.example.itodo.todo.mapper.TodoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@Service
public class GoalService {

    private final GoalMapper goalMapper;
    private final TodoMapper todoMapper;
    private final LlmClient llmClient;

    public GoalService(GoalMapper goalMapper, TodoMapper todoMapper, LlmClient llmClient) {
        this.goalMapper = goalMapper;
        this.todoMapper = todoMapper;
        this.llmClient = llmClient;
    }

    public List<Goal> getGoals(UUID userId) {
        LambdaQueryWrapper<Goal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goal::ownerId, userId)
               .isNull(Goal::deletedAt)
               .orderByAsc(Goal::sortOrder);
        
        return goalMapper.selectList(wrapper);
    }

    @Transactional
    public Goal createGoal(UUID userId, Map<String, Object> request) {
        Goal goal = new Goal(
                UUID.randomUUID(),
                userId,
                (String) request.get("title"),
                (String) request.get("description"),
                "ACTIVE",
                null,
                (String) request.get("color"),
                0,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                null
        );
        
        goalMapper.insert(goal);
        return goal;
    }

    public Goal getGoal(UUID userId, UUID goalId) {
        Goal goal = goalMapper.selectById(goalId);
        if (goal == null || !goal.ownerId().equals(userId) || goal.deletedAt() != null) {
            throw new RuntimeException("Goal not found");
        }
        return goal;
    }

    @Transactional
    public Goal updateGoal(UUID userId, UUID goalId, Map<String, Object> request) {
        Goal goal = getGoal(userId, goalId);
        
        // Update fields
        Goal updated = new Goal(
                goal.id(),
                goal.ownerId(),
                (String) request.getOrDefault("title", goal.title()),
                (String) request.getOrDefault("description", goal.description()),
                (String) request.getOrDefault("status", goal.status()),
                goal.targetDate(),
                (String) request.getOrDefault("color", goal.color()),
                goal.sortOrder(),
                goal.createdAt(),
                OffsetDateTime.now(),
                goal.deletedAt()
        );
        
        goalMapper.updateById(updated);
        return updated;
    }

    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {
        Goal goal = getGoal(userId, goalId);
        Goal deleted = new Goal(
                goal.id(),
                goal.ownerId(),
                goal.title(),
                goal.description(),
                goal.status(),
                goal.targetDate(),
                goal.color(),
                goal.sortOrder(),
                goal.createdAt(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        goalMapper.updateById(deleted);
    }

    public String generateAiPlan(UUID userId, UUID goalId, String context) {
        Goal goal = getGoal(userId, goalId);
        
        String systemPrompt = """
            You are a goal planning assistant. Given a goal title and context, generate a detailed execution plan.
            Output JSON with milestones array. Each milestone has: label, todos (array with title, dueDate, importance).
            """;
        
        String userPrompt = "Goal: " + goal.title() + "\nContext: " + context;
        
        return llmClient.chat(systemPrompt, userPrompt);
    }
}
