package com.example.itodo.todo;

import com.example.itodo.common.api.ApiResponse;
import com.example.itodo.common.api.PageResponse;
import com.example.itodo.security.CurrentUser;
import com.example.itodo.todo.dto.CreateTodoRequest;
import com.example.itodo.todo.dto.ReorderTodosRequest;
import com.example.itodo.todo.dto.TodoQueryRequest;
import com.example.itodo.todo.dto.TodoResponse;
import com.example.itodo.todo.dto.UpdateTodoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Todos")
@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @Operation(summary = "查询任务")
    @GetMapping
    ApiResponse<PageResponse<TodoResponse>> queryTodos(@AuthenticationPrincipal CurrentUser currentUser,
                                                       @RequestParam(required = false) UUID listId,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) Boolean important,
                                                       @RequestParam(required = false) Boolean myDay,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
                                                       @RequestParam(required = false) Instant remindFrom,
                                                       @RequestParam(required = false) Instant remindTo,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) UUID tagId,
                                                       @RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(todoService.queryTodos(currentUser.id(), new TodoQueryRequest(
                listId, status, important, myDay, dueFrom, dueTo, remindFrom, remindTo, keyword, tagId, page, size)));
    }

    @Operation(summary = "创建任务")
    @PostMapping
    ApiResponse<TodoResponse> createTodo(@AuthenticationPrincipal CurrentUser currentUser,
                                         @Valid @RequestBody CreateTodoRequest request) {
        return ApiResponse.ok(todoService.createTodo(currentUser.id(), request));
    }

    @Operation(summary = "重排任务")
    @PostMapping("/reorder")
    ApiResponse<Void> reorderTodos(@AuthenticationPrincipal CurrentUser currentUser,
                                   @Valid @RequestBody ReorderTodosRequest request) {
        todoService.reorderTodos(currentUser.id(), request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "获取任务")
    @GetMapping("/{todoId}")
    ApiResponse<TodoResponse> getTodo(@AuthenticationPrincipal CurrentUser currentUser,
                                      @PathVariable UUID todoId) {
        return ApiResponse.ok(todoService.getTodo(currentUser.id(), todoId));
    }

    @Operation(summary = "更新任务")
    @PatchMapping("/{todoId}")
    ApiResponse<TodoResponse> updateTodo(@AuthenticationPrincipal CurrentUser currentUser,
                                         @PathVariable UUID todoId,
                                         @Valid @RequestBody UpdateTodoRequest request) {
        return ApiResponse.ok(todoService.updateTodo(currentUser.id(), todoId, request));
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/{todoId}")
    ApiResponse<Void> deleteTodo(@AuthenticationPrincipal CurrentUser currentUser,
                                 @PathVariable UUID todoId) {
        todoService.deleteTodo(currentUser.id(), todoId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "完成任务")
    @PostMapping("/{todoId}/complete")
    ApiResponse<TodoResponse> complete(@AuthenticationPrincipal CurrentUser currentUser,
                                       @PathVariable UUID todoId) {
        return ApiResponse.ok(todoService.complete(currentUser.id(), todoId));
    }

    @Operation(summary = "取消完成任务")
    @PostMapping("/{todoId}/uncomplete")
    ApiResponse<TodoResponse> uncomplete(@AuthenticationPrincipal CurrentUser currentUser,
                                         @PathVariable UUID todoId) {
        return ApiResponse.ok(todoService.uncomplete(currentUser.id(), todoId));
    }

    @Operation(summary = "标记重要任务")
    @PostMapping("/{todoId}/important")
    ApiResponse<TodoResponse> markImportant(@AuthenticationPrincipal CurrentUser currentUser,
                                            @PathVariable UUID todoId) {
        return ApiResponse.ok(todoService.markImportant(currentUser.id(), todoId));
    }

    @Operation(summary = "取消重要标记")
    @DeleteMapping("/{todoId}/important")
    ApiResponse<TodoResponse> unmarkImportant(@AuthenticationPrincipal CurrentUser currentUser,
                                              @PathVariable UUID todoId) {
        return ApiResponse.ok(todoService.unmarkImportant(currentUser.id(), todoId));
    }

    @Operation(summary = "加入我的一天")
    @PostMapping("/{todoId}/my-day")
    ApiResponse<TodoResponse> addMyDay(@AuthenticationPrincipal CurrentUser currentUser,
                                       @PathVariable UUID todoId) {
        return ApiResponse.ok(todoService.addMyDay(currentUser.id(), todoId));
    }

    @Operation(summary = "移出我的一天")
    @DeleteMapping("/{todoId}/my-day")
    ApiResponse<TodoResponse> removeMyDay(@AuthenticationPrincipal CurrentUser currentUser,
                                          @PathVariable UUID todoId) {
        return ApiResponse.ok(todoService.removeMyDay(currentUser.id(), todoId));
    }
}
