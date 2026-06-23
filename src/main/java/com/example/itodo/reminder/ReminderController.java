package com.example.itodo.reminder;

import com.example.itodo.common.api.ApiResponse;
import com.example.itodo.common.api.PageResponse;
import com.example.itodo.reminder.dto.CreateReminderRequest;
import com.example.itodo.reminder.dto.ReminderQueryRequest;
import com.example.itodo.reminder.dto.ReminderResponse;
import com.example.itodo.reminder.dto.UpdateReminderRequest;
import com.example.itodo.security.CurrentUser;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "Reminders")
@RestController
@RequestMapping("/api/v1")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Operation(summary = "查询提醒")
    @GetMapping("/reminders")
    ApiResponse<PageResponse<ReminderResponse>> queryReminders(@AuthenticationPrincipal CurrentUser currentUser,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(required = false) Instant remindFrom,
                                                               @RequestParam(required = false) Instant remindTo,
                                                               @RequestParam(required = false) Integer page,
                                                               @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(reminderService.queryReminders(currentUser.id(), new ReminderQueryRequest(status, remindFrom, remindTo, page, size)));
    }

    @Operation(summary = "创建任务提醒")
    @PostMapping("/todos/{todoId}/reminders")
    ApiResponse<ReminderResponse> createReminder(@AuthenticationPrincipal CurrentUser currentUser,
                                                 @PathVariable UUID todoId,
                                                 @Valid @RequestBody CreateReminderRequest request) {
        return ApiResponse.ok(reminderService.createReminder(currentUser.id(), todoId, request));
    }

    @Operation(summary = "更新任务提醒")
    @PatchMapping("/todos/{todoId}/reminders/{reminderId}")
    ApiResponse<ReminderResponse> updateReminder(@AuthenticationPrincipal CurrentUser currentUser,
                                                 @PathVariable UUID todoId,
                                                 @PathVariable UUID reminderId,
                                                 @Valid @RequestBody UpdateReminderRequest request) {
        return ApiResponse.ok(reminderService.updateReminder(currentUser.id(), todoId, reminderId, request));
    }

    @Operation(summary = "删除任务提醒")
    @DeleteMapping("/todos/{todoId}/reminders/{reminderId}")
    ApiResponse<Void> deleteReminder(@AuthenticationPrincipal CurrentUser currentUser,
                                     @PathVariable UUID todoId,
                                     @PathVariable UUID reminderId) {
        reminderService.deleteReminder(currentUser.id(), todoId, reminderId);
        return ApiResponse.ok(null);
    }
}
