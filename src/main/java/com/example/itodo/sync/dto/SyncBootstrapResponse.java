package com.example.itodo.sync.dto;

import com.example.itodo.reminder.dto.ReminderResponse;
import com.example.itodo.tag.dto.TagResponse;
import com.example.itodo.todo.dto.TodoListResponse;
import com.example.itodo.todo.dto.TodoResponse;
import com.example.itodo.todo.dto.TodoStepResponse;

import java.util.List;

public record SyncBootstrapResponse(
        Long currentVersion,
        List<TodoListResponse> lists,
        List<TodoResponse> todos,
        List<TodoStepResponse> steps,
        List<TagResponse> tags,
        List<TodoTagLinkResponse> todoTags,
        List<ReminderResponse> reminders
) {
}
