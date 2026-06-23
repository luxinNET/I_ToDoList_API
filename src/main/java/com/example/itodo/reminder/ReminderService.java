package com.example.itodo.reminder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.itodo.common.api.PageResponse;
import com.example.itodo.common.error.BusinessException;
import com.example.itodo.common.error.ErrorCode;
import com.example.itodo.reminder.dto.CreateReminderRequest;
import com.example.itodo.reminder.dto.ReminderQueryRequest;
import com.example.itodo.reminder.dto.ReminderResponse;
import com.example.itodo.reminder.dto.UpdateReminderRequest;
import com.example.itodo.reminder.entity.Reminder;
import com.example.itodo.reminder.mapper.ReminderMapper;
import com.example.itodo.sync.SyncChangeService;
import com.example.itodo.sync.SyncOperation;
import com.example.itodo.sync.SyncResourceType;
import com.example.itodo.todo.TodoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReminderService {

    private final ReminderMapper reminderMapper;
    private final ReminderDtoMapper reminderDtoMapper;
    private final TodoService todoService;
    private final SyncChangeService syncChangeService;

    public ReminderService(ReminderMapper reminderMapper,
                           ReminderDtoMapper reminderDtoMapper,
                           TodoService todoService,
                           SyncChangeService syncChangeService) {
        this.reminderMapper = reminderMapper;
        this.reminderDtoMapper = reminderDtoMapper;
        this.todoService = todoService;
        this.syncChangeService = syncChangeService;
    }

    public PageResponse<ReminderResponse> queryReminders(UUID userId, ReminderQueryRequest request) {
        validateQuery(request);
        LambdaQueryWrapper<Reminder> wrapper = baseReminderQuery(userId);
        if (StringUtils.hasText(request.status())) {
            wrapper.eq(Reminder::getStatus, request.status().trim());
        }
        if (request.remindFrom() != null) {
            wrapper.ge(Reminder::getRemindAt, request.remindFrom());
        }
        if (request.remindTo() != null) {
            wrapper.le(Reminder::getRemindAt, request.remindTo());
        }
        wrapper.orderByAsc(Reminder::getRemindAt).orderByAsc(Reminder::getCreatedAt);
        Page<Reminder> page = reminderMapper.selectPage(new Page<>(request.pageOrDefault(), request.sizeOrDefault()), wrapper);
        return new PageResponse<>(reminderDtoMapper.toReminderResponses(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public ReminderResponse createReminder(UUID userId, UUID todoId, CreateReminderRequest request) {
        todoService.requireOwnedTodo(userId, todoId);
        String channel = normalizeChannel(request.channel());
        Instant now = Instant.now();
        Reminder reminder = new Reminder();
        reminder.setId(UUID.randomUUID());
        reminder.setTodoId(todoId);
        reminder.setUserId(userId);
        reminder.setRemindAt(request.remindAt());
        reminder.setChannel(channel);
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setCreatedAt(now);
        reminder.setUpdatedAt(now);
        reminderMapper.insert(reminder);
        refreshTodoRemindAt(userId, todoId);
        syncChangeService.recordChange(userId, SyncResourceType.REMINDER, reminder.getId(), SyncOperation.CREATE);
        return reminderDtoMapper.toReminderResponse(reminder);
    }

    @Transactional
    public ReminderResponse updateReminder(UUID userId, UUID todoId, UUID reminderId, UpdateReminderRequest request) {
        todoService.requireOwnedTodo(userId, todoId);
        Reminder reminder = requireReminder(userId, todoId, reminderId);
        boolean changed = false;
        if (request.remindAt() != null) {
            reminder.setRemindAt(request.remindAt());
            changed = true;
        }
        if (request.channel() != null) {
            reminder.setChannel(normalizeChannel(request.channel()));
            changed = true;
        }
        if (request.status() != null) {
            reminder.setStatus(normalizeStatus(request.status()));
            changed = true;
        }
        if (changed) {
            reminder.setUpdatedAt(Instant.now());
            reminderMapper.updateById(reminder);
            refreshTodoRemindAt(userId, todoId);
            syncChangeService.recordChange(userId, SyncResourceType.REMINDER, reminderId, SyncOperation.UPDATE);
        }
        return reminderDtoMapper.toReminderResponse(reminder);
    }

    @Transactional
    public void deleteReminder(UUID userId, UUID todoId, UUID reminderId) {
        todoService.requireOwnedTodo(userId, todoId);
        requireReminder(userId, todoId, reminderId);
        Instant now = Instant.now();
        reminderMapper.update(null, new LambdaUpdateWrapper<Reminder>()
                .eq(Reminder::getUserId, userId)
                .eq(Reminder::getTodoId, todoId)
                .eq(Reminder::getId, reminderId)
                .isNull(Reminder::getDeletedAt)
                .set(Reminder::getDeletedAt, now)
                .set(Reminder::getUpdatedAt, now));
        refreshTodoRemindAt(userId, todoId);
        syncChangeService.recordChange(userId, SyncResourceType.REMINDER, reminderId, SyncOperation.DELETE);
    }

    public Reminder requireReminder(UUID userId, UUID todoId, UUID reminderId) {
        Reminder reminder = reminderMapper.selectOne(baseReminderQuery(userId)
                .eq(Reminder::getTodoId, todoId)
                .eq(Reminder::getId, reminderId));
        if (reminder == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Reminder not found");
        }
        return reminder;
    }

    private void refreshTodoRemindAt(UUID userId, UUID todoId) {
        Reminder earliest = reminderMapper.selectOne(baseReminderQuery(userId)
                .eq(Reminder::getTodoId, todoId)
                .eq(Reminder::getStatus, ReminderStatus.PENDING)
                .orderByAsc(Reminder::getRemindAt)
                .last("LIMIT 1"));
        todoService.updateReminderSnapshot(userId, todoId, earliest == null ? null : earliest.getRemindAt());
    }

    private LambdaQueryWrapper<Reminder> baseReminderQuery(UUID userId) {
        return new LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getUserId, userId)
                .isNull(Reminder::getDeletedAt);
    }

    private void validateQuery(ReminderQueryRequest request) {
        if (StringUtils.hasText(request.status())) {
            normalizeStatus(request.status());
        }
        if (request.remindFrom() != null && request.remindTo() != null && request.remindFrom().isAfter(request.remindTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "remindFrom must be before remindTo");
        }
    }

    private String normalizeChannel(String channel) {
        if (!StringUtils.hasText(channel)) {
            return ReminderChannel.IN_APP;
        }
        String normalized = channel.trim();
        if (!ReminderChannel.IN_APP.equals(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid reminder channel");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Reminder status must not be blank");
        }
        String normalized = status.trim();
        if (!ReminderStatus.PENDING.equals(normalized)
                && !ReminderStatus.SENT.equals(normalized)
                && !ReminderStatus.CANCELLED.equals(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid reminder status");
        }
        return normalized;
    }
}
