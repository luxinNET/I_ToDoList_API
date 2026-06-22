package com.example.itodo.todo;

import com.example.itodo.common.api.ApiResponse;
import com.example.itodo.security.CurrentUser;
import com.example.itodo.todo.dto.CreateTodoStepRequest;
import com.example.itodo.todo.dto.ReorderStepsRequest;
import com.example.itodo.todo.dto.TodoStepResponse;
import com.example.itodo.todo.dto.UpdateTodoStepRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Steps")
@RestController
@RequestMapping("/api/v1/todos/{todoId}/steps")
public class TodoStepController {

    private final TodoStepService todoStepService;

    public TodoStepController(TodoStepService todoStepService) {
        this.todoStepService = todoStepService;
    }

    @Operation(summary = "获取任务步骤")
    @GetMapping
    ApiResponse<List<TodoStepResponse>> listSteps(@AuthenticationPrincipal CurrentUser currentUser,
                                                  @PathVariable UUID todoId) {
        return ApiResponse.ok(todoStepService.listSteps(currentUser.id(), todoId));
    }

    @Operation(summary = "创建任务步骤")
    @PostMapping
    ApiResponse<TodoStepResponse> createStep(@AuthenticationPrincipal CurrentUser currentUser,
                                             @PathVariable UUID todoId,
                                             @Valid @RequestBody CreateTodoStepRequest request) {
        return ApiResponse.ok(todoStepService.createStep(currentUser.id(), todoId, request));
    }

    @Operation(summary = "重排任务步骤")
    @PostMapping("/reorder")
    ApiResponse<Void> reorderSteps(@AuthenticationPrincipal CurrentUser currentUser,
                                   @PathVariable UUID todoId,
                                   @Valid @RequestBody ReorderStepsRequest request) {
        todoStepService.reorderSteps(currentUser.id(), todoId, request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "更新任务步骤")
    @PatchMapping("/{stepId}")
    ApiResponse<TodoStepResponse> updateStep(@AuthenticationPrincipal CurrentUser currentUser,
                                             @PathVariable UUID todoId,
                                             @PathVariable UUID stepId,
                                             @Valid @RequestBody UpdateTodoStepRequest request) {
        return ApiResponse.ok(todoStepService.updateStep(currentUser.id(), todoId, stepId, request));
    }

    @Operation(summary = "删除任务步骤")
    @DeleteMapping("/{stepId}")
    ApiResponse<Void> deleteStep(@AuthenticationPrincipal CurrentUser currentUser,
                                 @PathVariable UUID todoId,
                                 @PathVariable UUID stepId) {
        todoStepService.deleteStep(currentUser.id(), todoId, stepId);
        return ApiResponse.ok(null);
    }
}
