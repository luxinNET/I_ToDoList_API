package com.example.itodo.ai;

import com.example.itodo.ai.entity.Goal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Goals")
@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @Operation(summary = "获取目标列表")
    @GetMapping
    public List<Goal> getGoals(@AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser) {
        return goalService.getGoals(currentUser.id());
    }

    @Operation(summary = "创建目标")
    @PostMapping
    public Goal createGoal(
            @AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser,
            @RequestBody Map<String, Object> request) {
        return goalService.createGoal(currentUser.id(), request);
    }

    @Operation(summary = "获取目标详情")
    @GetMapping("/{goalId}")
    public Goal getGoal(
            @AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser,
            @PathVariable UUID goalId) {
        return goalService.getGoal(currentUser.id(), goalId);
    }

    @Operation(summary = "更新目标")
    @PatchMapping("/{goalId}")
    public Goal updateGoal(
            @AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser,
            @PathVariable UUID goalId,
            @RequestBody Map<String, Object> request) {
        return goalService.updateGoal(currentUser.id(), goalId, request);
    }

    @Operation(summary = "删除目标")
    @DeleteMapping("/{goalId}")
    public void deleteGoal(
            @AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser,
            @PathVariable UUID goalId) {
        goalService.deleteGoal(currentUser.id(), goalId);
    }

    @Operation(summary = "AI 生成执行计划")
    @PostMapping("/{goalId}/ai-plan")
    public String generateAiPlan(
            @AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser,
            @PathVariable UUID goalId,
            @RequestBody(required = false) Map<String, String> body) {
        String context = body != null ? body.get("context") : "";
        return goalService.generateAiPlan(currentUser.id(), goalId, context);
    }
}
