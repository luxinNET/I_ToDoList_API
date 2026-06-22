package com.example.itodo.todo;

import com.example.itodo.common.api.ApiResponse;
import com.example.itodo.common.api.PageResponse;
import com.example.itodo.security.CurrentUser;
import com.example.itodo.todo.dto.TodoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Views")
@RestController
@RequestMapping("/api/v1/views")
public class SmartViewController {

    private final TodoService todoService;

    public SmartViewController(TodoService todoService) {
        this.todoService = todoService;
    }

    @Operation(summary = "我的一天任务")
    @GetMapping("/my-day/todos")
    ApiResponse<PageResponse<TodoResponse>> myDay(@AuthenticationPrincipal CurrentUser currentUser,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(todoService.queryMyDay(currentUser.id(), page, size));
    }

    @Operation(summary = "重要任务")
    @GetMapping("/important/todos")
    ApiResponse<PageResponse<TodoResponse>> important(@AuthenticationPrincipal CurrentUser currentUser,
                                                      @RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(todoService.queryImportant(currentUser.id(), page, size));
    }

    @Operation(summary = "计划内任务")
    @GetMapping("/planned/todos")
    ApiResponse<PageResponse<TodoResponse>> planned(@AuthenticationPrincipal CurrentUser currentUser,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(todoService.queryPlanned(currentUser.id(), page, size));
    }

    @Operation(summary = "已完成任务")
    @GetMapping("/completed/todos")
    ApiResponse<PageResponse<TodoResponse>> completed(@AuthenticationPrincipal CurrentUser currentUser,
                                                      @RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(todoService.queryCompleted(currentUser.id(), page, size));
    }

    @Operation(summary = "全部任务")
    @GetMapping("/all/todos")
    ApiResponse<PageResponse<TodoResponse>> all(@AuthenticationPrincipal CurrentUser currentUser,
                                                @RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(todoService.queryAll(currentUser.id(), page, size));
    }
}
