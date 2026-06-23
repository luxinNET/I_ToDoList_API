package com.example.itodo.sync;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.itodo.reminder.ReminderDtoMapper;
import com.example.itodo.reminder.entity.Reminder;
import com.example.itodo.reminder.mapper.ReminderMapper;
import com.example.itodo.sync.dto.SyncBootstrapResponse;
import com.example.itodo.sync.dto.SyncChangesResponse;
import com.example.itodo.sync.dto.TodoTagLinkResponse;
import com.example.itodo.tag.TagDtoMapper;
import com.example.itodo.tag.entity.Tag;
import com.example.itodo.tag.entity.TodoTag;
import com.example.itodo.tag.mapper.TagMapper;
import com.example.itodo.tag.mapper.TodoTagMapper;
import com.example.itodo.todo.TodoDtoMapper;
import com.example.itodo.todo.entity.Todo;
import com.example.itodo.todo.entity.TodoList;
import com.example.itodo.todo.entity.TodoStep;
import com.example.itodo.todo.mapper.TodoListMapper;
import com.example.itodo.todo.mapper.TodoMapper;
import com.example.itodo.todo.mapper.TodoStepMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SyncService {

    private final TodoListMapper todoListMapper;
    private final TodoMapper todoMapper;
    private final TodoStepMapper todoStepMapper;
    private final TodoDtoMapper todoDtoMapper;
    private final TagMapper tagMapper;
    private final TodoTagMapper todoTagMapper;
    private final TagDtoMapper tagDtoMapper;
    private final ReminderMapper reminderMapper;
    private final ReminderDtoMapper reminderDtoMapper;
    private final SyncChangeService syncChangeService;

    public SyncService(TodoListMapper todoListMapper,
                       TodoMapper todoMapper,
                       TodoStepMapper todoStepMapper,
                       TodoDtoMapper todoDtoMapper,
                       TagMapper tagMapper,
                       TodoTagMapper todoTagMapper,
                       TagDtoMapper tagDtoMapper,
                       ReminderMapper reminderMapper,
                       ReminderDtoMapper reminderDtoMapper,
                       SyncChangeService syncChangeService) {
        this.todoListMapper = todoListMapper;
        this.todoMapper = todoMapper;
        this.todoStepMapper = todoStepMapper;
        this.todoDtoMapper = todoDtoMapper;
        this.tagMapper = tagMapper;
        this.todoTagMapper = todoTagMapper;
        this.tagDtoMapper = tagDtoMapper;
        this.reminderMapper = reminderMapper;
        this.reminderDtoMapper = reminderDtoMapper;
        this.syncChangeService = syncChangeService;
    }

    public SyncBootstrapResponse bootstrap(UUID userId) {
        List<TodoList> lists = todoListMapper.selectList(new LambdaQueryWrapper<TodoList>()
                .eq(TodoList::getOwnerId, userId)
                .isNull(TodoList::getDeletedAt)
                .orderByAsc(TodoList::getSortOrder)
                .orderByAsc(TodoList::getCreatedAt));
        List<Todo> todos = todoMapper.selectList(new LambdaQueryWrapper<Todo>()
                .eq(Todo::getOwnerId, userId)
                .isNull(Todo::getDeletedAt)
                .orderByAsc(Todo::getListId)
                .orderByAsc(Todo::getSortOrder)
                .orderByAsc(Todo::getCreatedAt));
        List<UUID> todoIds = todos.stream().map(Todo::getId).toList();
        List<TodoStep> steps = todoIds.isEmpty()
                ? List.of()
                : todoStepMapper.selectList(new LambdaQueryWrapper<TodoStep>()
                .in(TodoStep::getTodoId, todoIds)
                .isNull(TodoStep::getDeletedAt)
                .orderByAsc(TodoStep::getTodoId)
                .orderByAsc(TodoStep::getSortOrder)
                .orderByAsc(TodoStep::getCreatedAt));
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId)
                .isNull(Tag::getDeletedAt)
                .orderByAsc(Tag::getName));
        List<UUID> tagIds = tags.stream().map(Tag::getId).toList();
        List<TodoTagLinkResponse> todoTags = todoIds.isEmpty() || tagIds.isEmpty()
                ? List.of()
                : todoTagMapper.selectList(new LambdaQueryWrapper<TodoTag>()
                .in(TodoTag::getTodoId, todoIds)
                .in(TodoTag::getTagId, tagIds))
                .stream()
                .map(link -> new TodoTagLinkResponse(link.getTodoId(), link.getTagId()))
                .toList();
        List<Reminder> reminders = reminderMapper.selectList(new LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getUserId, userId)
                .isNull(Reminder::getDeletedAt)
                .orderByAsc(Reminder::getRemindAt));

        return new SyncBootstrapResponse(
                syncChangeService.currentVersion(userId),
                todoDtoMapper.toListResponses(lists),
                todoDtoMapper.toTodoResponses(todos),
                todoDtoMapper.toStepResponses(steps),
                tagDtoMapper.toTagResponses(tags),
                todoTags,
                reminderDtoMapper.toReminderResponses(reminders));
    }

    public SyncChangesResponse changes(UUID userId, Long sinceVersion, Integer limit) {
        return new SyncChangesResponse(
                sinceVersion == null ? 0L : sinceVersion,
                syncChangeService.currentVersion(userId),
                syncChangeService.changesSince(userId, sinceVersion, limit));
    }
}
