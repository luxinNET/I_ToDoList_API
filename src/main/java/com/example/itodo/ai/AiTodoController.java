package com.example.itodo.ai;

import com.example.itodo.ai.dto.AiTodoParseRequest;
import com.example.itodo.ai.dto.AiTodoParseResponse;
import com.example.itodo.todo.dto.TodoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "AI Todo")
@RestController
@RequestMapping("/api/v1/ai/todos")
public class AiTodoController {

    private final AiTodoService aiTodoService;

    public AiTodoController(AiTodoService aiTodoService) {
        this.aiTodoService = aiTodoService;
    }

    @Operation(summary = "AI 解析自然语言任务")
    @PostMapping("/parse")
    public AiTodoParseResponse parseTodo(
            @AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser,
            @RequestBody AiTodoParseRequest request) {
        return aiTodoService.parseTodo(request);
    }

    @Operation(summary = "确认创建 AI 解析的任务")
    @PostMapping
    public TodoResponse createAiTodo(
            @AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser,
            @RequestBody AiTodoParseResponse preview) {
        // TODO: Implement confirm and create todo
        return null;
    }

    @Operation(summary = "AI 拆解任务")
    @PostMapping("/{todoId}/decompose")
    public List<Map<String, Object>> decomposeTask(
            @AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser,
            @PathVariable UUID todoId,
            @RequestBody(required = false) Map<String, String> body) {
        String hint = body != null ? body.get("hint") : null;
        return aiTodoService.decomposeTask(todoId, hint);
    }
}
