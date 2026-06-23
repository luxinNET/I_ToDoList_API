package com.example.itodo.search;

import com.example.itodo.common.api.ApiResponse;
import com.example.itodo.common.api.PageResponse;
import com.example.itodo.security.CurrentUser;
import com.example.itodo.todo.TodoService;
import com.example.itodo.todo.dto.TodoQueryRequest;
import com.example.itodo.todo.dto.TodoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Search")
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final TodoService todoService;

    public SearchController(TodoService todoService) {
        this.todoService = todoService;
    }

    @Operation(summary = "搜索任务")
    @GetMapping("/todos")
    ApiResponse<PageResponse<TodoResponse>> searchTodos(@AuthenticationPrincipal CurrentUser currentUser,
                                                        @RequestParam String keyword,
                                                        @RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(todoService.queryTodos(currentUser.id(), new TodoQueryRequest(
                null, null, null, null, null, null, null, null, keyword, null, page, size)));
    }
}
