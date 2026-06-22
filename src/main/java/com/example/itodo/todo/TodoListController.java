package com.example.itodo.todo;

import com.example.itodo.common.api.ApiResponse;
import com.example.itodo.security.CurrentUser;
import com.example.itodo.todo.dto.CreateTodoListRequest;
import com.example.itodo.todo.dto.ReorderListsRequest;
import com.example.itodo.todo.dto.TodoListResponse;
import com.example.itodo.todo.dto.UpdateTodoListRequest;
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

@Tag(name = "Lists")
@RestController
@RequestMapping("/api/v1/lists")
public class TodoListController {

    private final TodoListService todoListService;

    public TodoListController(TodoListService todoListService) {
        this.todoListService = todoListService;
    }

    @Operation(summary = "获取清单列表")
    @GetMapping
    ApiResponse<List<TodoListResponse>> listLists(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(todoListService.listLists(currentUser.id()));
    }

    @Operation(summary = "创建清单")
    @PostMapping
    ApiResponse<TodoListResponse> createList(@AuthenticationPrincipal CurrentUser currentUser,
                                             @Valid @RequestBody CreateTodoListRequest request) {
        return ApiResponse.ok(todoListService.createList(currentUser.id(), request));
    }

    @Operation(summary = "获取清单")
    @GetMapping("/{listId}")
    ApiResponse<TodoListResponse> getList(@AuthenticationPrincipal CurrentUser currentUser,
                                          @PathVariable UUID listId) {
        return ApiResponse.ok(todoListService.getList(currentUser.id(), listId));
    }

    @Operation(summary = "更新清单")
    @PatchMapping("/{listId}")
    ApiResponse<TodoListResponse> updateList(@AuthenticationPrincipal CurrentUser currentUser,
                                             @PathVariable UUID listId,
                                             @Valid @RequestBody UpdateTodoListRequest request) {
        return ApiResponse.ok(todoListService.updateList(currentUser.id(), listId, request));
    }

    @Operation(summary = "删除清单")
    @DeleteMapping("/{listId}")
    ApiResponse<Void> deleteList(@AuthenticationPrincipal CurrentUser currentUser,
                                 @PathVariable UUID listId) {
        todoListService.deleteList(currentUser.id(), listId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "重排清单")
    @PostMapping("/reorder")
    ApiResponse<Void> reorderLists(@AuthenticationPrincipal CurrentUser currentUser,
                                   @Valid @RequestBody ReorderListsRequest request) {
        todoListService.reorderLists(currentUser.id(), request);
        return ApiResponse.ok(null);
    }
}
