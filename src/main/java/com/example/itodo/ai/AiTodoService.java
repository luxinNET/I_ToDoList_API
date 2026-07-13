package com.example.itodo.ai;

import com.example.itodo.ai.dto.AiTodoParseRequest;
import com.example.itodo.ai.dto.AiTodoParseResponse;
import com.example.itodo.ai.dto.ParsedTodo;
import com.example.itodo.common.error.ErrorCode;
import com.example.itodo.todo.entity.Todo;
import com.example.itodo.todo.entity.TodoStep;
import com.example.itodo.todo.mapper.TodoMapper;
import com.example.itodo.todo.mapper.TodoStepMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiTodoService {

    private final LlmClient llmClient;
    private final TodoMapper todoMapper;
    private final TodoStepMapper todoStepMapper;

    public AiTodoService(LlmClient llmClient, TodoMapper todoMapper, TodoStepMapper todoStepMapper) {
        this.llmClient = llmClient;
        this.todoMapper = todoMapper;
        this.todoStepMapper = todoStepMapper;
    }

    public AiTodoParseResponse parseTodo(AiTodoParseRequest request) {
        String systemPrompt = """
            You are a task parsing assistant. Parse the user input into a structured todo task.
            Current date: %s
            Timezone: %s
            
            Output JSON with fields: title, note, dueDate (YYYY-MM-DD), remindAt (ISO 8601), importance (NORMAL or IMPORTANT), tags (array of strings).
            """.formatted(LocalDate.now().toString(), request.timezone());

        String userPrompt = request.text();

        // Call LLM to parse
        String llmResponse = llmClient.chat(systemPrompt, userPrompt);

        // TODO: Parse LLM response JSON and extract fields
        // For MVP, return a simple parsed result
        ParsedTodo parsedTodo = new ParsedTodo(
                extractTitle(request.text()),
                null,
                null,
                null,
                "NORMAL",
                null,
                null
        );

        return new AiTodoParseResponse(
                "preview-" + System.currentTimeMillis(),
                parsedTodo,
                "AI 解析结果（预览）"
        );
    }

    public List<Map<String, Object>> decomposeTask(UUID todoId, String hint) {
        // Get todo
        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) {
            throw new AiException(ErrorCode.RESOURCE_NOT_FOUND, "Todo not found: " + todoId);
        }

        String systemPrompt = """
            You are a task decomposition assistant. Given a task title and optional hint, generate a list of subtasks.
            Output JSON array with objects containing: title, sortOrder.
            """;

        String userPrompt = "Task: " + todo.getTitle() + "\nHint: " + (hint != null ? hint : "");

        String llmResponse = llmClient.chat(systemPrompt, userPrompt);

        // TODO: Parse LLM response JSON and return structured steps
        // For MVP, return dummy steps
        return List.of(
                Map.of("title", "Step 1: Research", "sortOrder", 0),
                Map.of("title", "Step 2: Plan", "sortOrder", 1),
                Map.of("title", "Step 3: Execute", "sortOrder", 2)
        );
    }

    private String extractTitle(String text) {
        // Simple extraction for MVP
        return text.length() > 50 ? text.substring(0, 50) + "..." : text;
    }
}
