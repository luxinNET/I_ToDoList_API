package com.example.itodo.todo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.itodo.common.error.BusinessException;
import com.example.itodo.common.error.ErrorCode;
import com.example.itodo.sync.SyncChangeService;
import com.example.itodo.sync.SyncOperation;
import com.example.itodo.sync.SyncResourceType;
import com.example.itodo.todo.dto.CreateTodoListRequest;
import com.example.itodo.todo.dto.ReorderItemRequest;
import com.example.itodo.todo.dto.ReorderListsRequest;
import com.example.itodo.todo.dto.TodoListResponse;
import com.example.itodo.todo.dto.UpdateTodoListRequest;
import com.example.itodo.todo.entity.Todo;
import com.example.itodo.todo.entity.TodoList;
import com.example.itodo.todo.entity.TodoStep;
import com.example.itodo.todo.mapper.TodoListMapper;
import com.example.itodo.todo.mapper.TodoMapper;
import com.example.itodo.todo.mapper.TodoStepMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TodoListService {

    private final TodoListMapper todoListMapper;
    private final TodoMapper todoMapper;
    private final TodoStepMapper todoStepMapper;
    private final TodoDtoMapper todoDtoMapper;
    private final DefaultTodoListInitializer defaultTodoListInitializer;
    private final SyncChangeService syncChangeService;

    public TodoListService(TodoListMapper todoListMapper,
                           TodoMapper todoMapper,
                           TodoStepMapper todoStepMapper,
                           TodoDtoMapper todoDtoMapper,
                           DefaultTodoListInitializer defaultTodoListInitializer,
                           SyncChangeService syncChangeService) {
        this.todoListMapper = todoListMapper;
        this.todoMapper = todoMapper;
        this.todoStepMapper = todoStepMapper;
        this.todoDtoMapper = todoDtoMapper;
        this.defaultTodoListInitializer = defaultTodoListInitializer;
        this.syncChangeService = syncChangeService;
    }

    public List<TodoListResponse> listLists(UUID userId) {
        defaultTodoListInitializer.ensureDefaultList(userId);
        List<TodoList> lists = todoListMapper.selectList(baseListQuery(userId)
                .orderByAsc(TodoList::getSortOrder)
                .orderByAsc(TodoList::getCreatedAt));
        return todoDtoMapper.toListResponses(lists);
    }

    public TodoListResponse getList(UUID userId, UUID listId) {
        return todoDtoMapper.toListResponse(requireOwnedList(userId, listId));
    }

    @Transactional
    public TodoListResponse createList(UUID userId, CreateTodoListRequest request) {
        Instant now = Instant.now();
        TodoList todoList = new TodoList();
        todoList.setId(UUID.randomUUID());
        todoList.setOwnerId(userId);
        todoList.setName(request.name().trim());
        todoList.setColor(normalize(request.color()));
        todoList.setIcon(normalize(request.icon()));
        todoList.setSortOrder(nextSortOrder(userId));
        todoList.setIsSystem(false);
        todoList.setIsShared(false);
        todoList.setCreatedAt(now);
        todoList.setUpdatedAt(now);
        todoListMapper.insert(todoList);
        syncChangeService.recordChange(userId, SyncResourceType.LIST, todoList.getId(), SyncOperation.CREATE);
        return todoDtoMapper.toListResponse(todoList);
    }

    @Transactional
    public TodoListResponse updateList(UUID userId, UUID listId, UpdateTodoListRequest request) {
        TodoList todoList = requireOwnedList(userId, listId);
        boolean changed = false;
        if (request.name() != null) {
            if (!StringUtils.hasText(request.name())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "List name must not be blank");
            }
            todoList.setName(request.name().trim());
            changed = true;
        }
        if (request.color() != null) {
            todoList.setColor(normalize(request.color()));
            changed = true;
        }
        if (request.icon() != null) {
            todoList.setIcon(normalize(request.icon()));
            changed = true;
        }
        if (changed) {
            todoList.setUpdatedAt(Instant.now());
            todoListMapper.updateById(todoList);
            syncChangeService.recordChange(userId, SyncResourceType.LIST, listId, SyncOperation.UPDATE);
        }
        return todoDtoMapper.toListResponse(todoList);
    }

    @Transactional
    public void deleteList(UUID userId, UUID listId) {
        TodoList todoList = requireOwnedList(userId, listId);
        if (Boolean.TRUE.equals(todoList.getIsSystem())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "System list cannot be deleted");
        }

        Instant now = Instant.now();
        List<UUID> todoIds = todoMapper.selectList(new LambdaQueryWrapper<Todo>()
                        .select(Todo::getId)
                        .eq(Todo::getOwnerId, userId)
                        .eq(Todo::getListId, listId)
                        .isNull(Todo::getDeletedAt))
                .stream()
                .map(Todo::getId)
                .toList();

        todoListMapper.update(null, new LambdaUpdateWrapper<TodoList>()
                .eq(TodoList::getId, listId)
                .eq(TodoList::getOwnerId, userId)
                .isNull(TodoList::getDeletedAt)
                .set(TodoList::getDeletedAt, now)
                .set(TodoList::getUpdatedAt, now));
        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getOwnerId, userId)
                .eq(Todo::getListId, listId)
                .isNull(Todo::getDeletedAt)
                .set(Todo::getDeletedAt, now)
                .set(Todo::getUpdatedAt, now)
                .setSql("version = version + 1"));
        if (!todoIds.isEmpty()) {
            todoStepMapper.update(null, new LambdaUpdateWrapper<TodoStep>()
                    .in(TodoStep::getTodoId, todoIds)
                    .isNull(TodoStep::getDeletedAt)
                    .set(TodoStep::getDeletedAt, now)
                    .set(TodoStep::getUpdatedAt, now));
        }
        syncChangeService.recordChange(userId, SyncResourceType.LIST, listId, SyncOperation.DELETE);
        todoIds.forEach(todoId -> syncChangeService.recordChange(userId, SyncResourceType.TODO, todoId, SyncOperation.DELETE));
    }

    @Transactional
    public void reorderLists(UUID userId, ReorderListsRequest request) {
        ensureNoDuplicateIds(request.items());
        List<UUID> ids = request.items().stream().map(ReorderItemRequest::id).toList();
        Long count = todoListMapper.selectCount(baseListQuery(userId).in(TodoList::getId, ids));
        if (count != ids.size()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "List not found");
        }
        Instant now = Instant.now();
        for (ReorderItemRequest item : request.items()) {
            todoListMapper.update(null, new LambdaUpdateWrapper<TodoList>()
                    .eq(TodoList::getOwnerId, userId)
                    .eq(TodoList::getId, item.id())
                    .isNull(TodoList::getDeletedAt)
                    .set(TodoList::getSortOrder, item.sortOrder())
                    .set(TodoList::getUpdatedAt, now));
            syncChangeService.recordChange(userId, SyncResourceType.LIST, item.id(), SyncOperation.UPDATE);
        }
    }

    TodoList requireOwnedList(UUID userId, UUID listId) {
        TodoList todoList = todoListMapper.selectOne(baseListQuery(userId).eq(TodoList::getId, listId));
        if (todoList == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "List not found");
        }
        return todoList;
    }

    private LambdaQueryWrapper<TodoList> baseListQuery(UUID userId) {
        return new LambdaQueryWrapper<TodoList>()
                .eq(TodoList::getOwnerId, userId)
                .isNull(TodoList::getDeletedAt);
    }

    private int nextSortOrder(UUID userId) {
        TodoList last = todoListMapper.selectOne(baseListQuery(userId)
                .orderByDesc(TodoList::getSortOrder)
                .last("LIMIT 1"));
        return last == null || last.getSortOrder() == null ? 1000 : last.getSortOrder() + 1000;
    }

    private void ensureNoDuplicateIds(List<ReorderItemRequest> items) {
        Set<UUID> seen = new HashSet<>();
        for (ReorderItemRequest item : items) {
            if (!seen.add(item.id())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Duplicate reorder item id");
            }
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
