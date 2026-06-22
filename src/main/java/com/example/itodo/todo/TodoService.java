package com.example.itodo.todo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.itodo.common.api.PageResponse;
import com.example.itodo.common.error.BusinessException;
import com.example.itodo.common.error.ErrorCode;
import com.example.itodo.todo.dto.CreateTodoRequest;
import com.example.itodo.todo.dto.ReorderItemRequest;
import com.example.itodo.todo.dto.ReorderTodosRequest;
import com.example.itodo.todo.dto.TodoQueryRequest;
import com.example.itodo.todo.dto.TodoResponse;
import com.example.itodo.todo.dto.UpdateTodoRequest;
import com.example.itodo.todo.entity.Todo;
import com.example.itodo.todo.entity.TodoList;
import com.example.itodo.todo.entity.TodoStep;
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
public class TodoService {

    private final TodoMapper todoMapper;
    private final TodoStepMapper todoStepMapper;
    private final TodoDtoMapper todoDtoMapper;
    private final TodoListService todoListService;
    private final DefaultTodoListInitializer defaultTodoListInitializer;

    public TodoService(TodoMapper todoMapper,
                       TodoStepMapper todoStepMapper,
                       TodoDtoMapper todoDtoMapper,
                       TodoListService todoListService,
                       DefaultTodoListInitializer defaultTodoListInitializer) {
        this.todoMapper = todoMapper;
        this.todoStepMapper = todoStepMapper;
        this.todoDtoMapper = todoDtoMapper;
        this.todoListService = todoListService;
        this.defaultTodoListInitializer = defaultTodoListInitializer;
    }

    public PageResponse<TodoResponse> queryTodos(UUID userId, TodoQueryRequest request) {
        validateQuery(request);
        LambdaQueryWrapper<Todo> wrapper = buildQuery(userId, request);
        applyDefaultSort(wrapper, request);
        Page<Todo> page = todoMapper.selectPage(new Page<>(request.pageOrDefault(), request.sizeOrDefault()), wrapper);
        return new PageResponse<>(todoDtoMapper.toTodoResponses(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResponse<TodoResponse> queryMyDay(UUID userId, Integer page, Integer size) {
        return queryTodos(userId, new TodoQueryRequest(null, TodoStatus.ACTIVE, null, true, null, null, null, null, null, page, size));
    }

    public PageResponse<TodoResponse> queryImportant(UUID userId, Integer page, Integer size) {
        return queryTodos(userId, new TodoQueryRequest(null, null, true, null, null, null, null, null, null, page, size));
    }

    public PageResponse<TodoResponse> queryCompleted(UUID userId, Integer page, Integer size) {
        return queryTodos(userId, new TodoQueryRequest(null, TodoStatus.COMPLETED, null, null, null, null, null, null, null, page, size));
    }

    public PageResponse<TodoResponse> queryAll(UUID userId, Integer page, Integer size) {
        return queryTodos(userId, new TodoQueryRequest(null, null, null, null, null, null, null, null, null, page, size));
    }

    public PageResponse<TodoResponse> queryPlanned(UUID userId, Integer page, Integer size) {
        TodoQueryRequest request = new TodoQueryRequest(null, null, null, null, null, null, null, null, null, page, size);
        LambdaQueryWrapper<Todo> wrapper = baseTodoQuery(userId)
                .and(nested -> nested.isNotNull(Todo::getDueDate).or().isNotNull(Todo::getRemindAt))
                .orderByAsc(Todo::getDueDate)
                .orderByAsc(Todo::getRemindAt)
                .orderByAsc(Todo::getSortOrder);
        Page<Todo> result = todoMapper.selectPage(new Page<>(request.pageOrDefault(), request.sizeOrDefault()), wrapper);
        return new PageResponse<>(todoDtoMapper.toTodoResponses(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public TodoResponse getTodo(UUID userId, UUID todoId) {
        return todoDtoMapper.toTodoResponse(requireOwnedTodo(userId, todoId));
    }

    @Transactional
    public TodoResponse createTodo(UUID userId, CreateTodoRequest request) {
        UUID listId = request.listId() == null ? defaultTodoListInitializer.ensureDefaultListId(userId) : request.listId();
        todoListService.requireOwnedList(userId, listId);

        Instant now = Instant.now();
        Todo todo = new Todo();
        todo.setId(UUID.randomUUID());
        todo.setOwnerId(userId);
        todo.setListId(listId);
        todo.setTitle(request.title().trim());
        todo.setNote(normalize(request.note()));
        todo.setStatus(TodoStatus.ACTIVE);
        todo.setImportance(Boolean.TRUE.equals(request.important()) ? TodoImportance.IMPORTANT : TodoImportance.NORMAL);
        todo.setDueDate(request.dueDate());
        todo.setRemindAt(request.remindAt());
        todo.setRepeatRule(normalize(request.repeatRule()));
        todo.setSortOrder(request.sortOrder() == null ? nextSortOrder(userId, listId) : request.sortOrder());
        todo.setMyDay(Boolean.TRUE.equals(request.myDay()));
        todo.setVersion(1L);
        todo.setCreatedAt(now);
        todo.setUpdatedAt(now);
        todoMapper.insert(todo);
        return todoDtoMapper.toTodoResponse(todo);
    }

    @Transactional
    public TodoResponse updateTodo(UUID userId, UUID todoId, UpdateTodoRequest request) {
        Todo todo = requireOwnedTodo(userId, todoId);
        boolean changed = false;
        if (request.listId() != null && !request.listId().equals(todo.getListId())) {
            todoListService.requireOwnedList(userId, request.listId());
            todo.setListId(request.listId());
            changed = true;
        }
        if (request.title() != null) {
            if (!StringUtils.hasText(request.title())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Todo title must not be blank");
            }
            todo.setTitle(request.title().trim());
            changed = true;
        }
        if (Boolean.TRUE.equals(request.clearNote())) {
            todo.setNote(null);
            changed = true;
        } else if (request.note() != null) {
            todo.setNote(normalize(request.note()));
            changed = true;
        }
        if (Boolean.TRUE.equals(request.clearDueDate())) {
            todo.setDueDate(null);
            changed = true;
        } else if (request.dueDate() != null) {
            todo.setDueDate(request.dueDate());
            changed = true;
        }
        if (Boolean.TRUE.equals(request.clearRemindAt())) {
            todo.setRemindAt(null);
            changed = true;
        } else if (request.remindAt() != null) {
            todo.setRemindAt(request.remindAt());
            changed = true;
        }
        if (Boolean.TRUE.equals(request.clearRepeatRule())) {
            todo.setRepeatRule(null);
            changed = true;
        } else if (request.repeatRule() != null) {
            todo.setRepeatRule(normalize(request.repeatRule()));
            changed = true;
        }
        if (request.myDay() != null) {
            todo.setMyDay(request.myDay());
            changed = true;
        }
        if (request.important() != null) {
            todo.setImportance(Boolean.TRUE.equals(request.important()) ? TodoImportance.IMPORTANT : TodoImportance.NORMAL);
            changed = true;
        }
        if (request.sortOrder() != null) {
            todo.setSortOrder(request.sortOrder());
            changed = true;
        }
        if (changed) {
            touch(todo);
            todoMapper.updateById(todo);
        }
        return todoDtoMapper.toTodoResponse(todo);
    }

    @Transactional
    public void deleteTodo(UUID userId, UUID todoId) {
        requireOwnedTodo(userId, todoId);
        Instant now = Instant.now();
        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getOwnerId, userId)
                .eq(Todo::getId, todoId)
                .isNull(Todo::getDeletedAt)
                .set(Todo::getDeletedAt, now)
                .set(Todo::getUpdatedAt, now)
                .setSql("version = version + 1"));
        todoStepMapper.update(null, new LambdaUpdateWrapper<TodoStep>()
                .eq(TodoStep::getTodoId, todoId)
                .isNull(TodoStep::getDeletedAt)
                .set(TodoStep::getDeletedAt, now)
                .set(TodoStep::getUpdatedAt, now));
    }

    @Transactional
    public TodoResponse complete(UUID userId, UUID todoId) {
        Todo todo = requireOwnedTodo(userId, todoId);
        todo.setStatus(TodoStatus.COMPLETED);
        todo.setCompletedAt(Instant.now());
        touch(todo);
        todoMapper.updateById(todo);
        return todoDtoMapper.toTodoResponse(todo);
    }

    @Transactional
    public TodoResponse uncomplete(UUID userId, UUID todoId) {
        Todo todo = requireOwnedTodo(userId, todoId);
        todo.setStatus(TodoStatus.ACTIVE);
        todo.setCompletedAt(null);
        touch(todo);
        todoMapper.updateById(todo);
        return todoDtoMapper.toTodoResponse(todo);
    }

    @Transactional
    public TodoResponse markImportant(UUID userId, UUID todoId) {
        return updateImportance(userId, todoId, TodoImportance.IMPORTANT);
    }

    @Transactional
    public TodoResponse unmarkImportant(UUID userId, UUID todoId) {
        return updateImportance(userId, todoId, TodoImportance.NORMAL);
    }

    @Transactional
    public TodoResponse addMyDay(UUID userId, UUID todoId) {
        return updateMyDay(userId, todoId, true);
    }

    @Transactional
    public TodoResponse removeMyDay(UUID userId, UUID todoId) {
        return updateMyDay(userId, todoId, false);
    }

    @Transactional
    public void reorderTodos(UUID userId, ReorderTodosRequest request) {
        todoListService.requireOwnedList(userId, request.listId());
        ensureNoDuplicateIds(request.items());
        List<UUID> ids = request.items().stream().map(ReorderItemRequest::id).toList();
        Long count = todoMapper.selectCount(baseTodoQuery(userId)
                .eq(Todo::getListId, request.listId())
                .in(Todo::getId, ids));
        if (count != ids.size()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Todo not found");
        }
        Instant now = Instant.now();
        for (ReorderItemRequest item : request.items()) {
            todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                    .eq(Todo::getOwnerId, userId)
                    .eq(Todo::getListId, request.listId())
                    .eq(Todo::getId, item.id())
                    .isNull(Todo::getDeletedAt)
                    .set(Todo::getSortOrder, item.sortOrder())
                    .set(Todo::getUpdatedAt, now)
                    .setSql("version = version + 1"));
        }
    }

    Todo requireOwnedTodo(UUID userId, UUID todoId) {
        Todo todo = todoMapper.selectOne(baseTodoQuery(userId).eq(Todo::getId, todoId));
        if (todo == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Todo not found");
        }
        return todo;
    }

    void touchTodo(UUID userId, UUID todoId) {
        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getOwnerId, userId)
                .eq(Todo::getId, todoId)
                .isNull(Todo::getDeletedAt)
                .set(Todo::getUpdatedAt, Instant.now())
                .setSql("version = version + 1"));
    }

    private TodoResponse updateImportance(UUID userId, UUID todoId, String importance) {
        Todo todo = requireOwnedTodo(userId, todoId);
        todo.setImportance(importance);
        touch(todo);
        todoMapper.updateById(todo);
        return todoDtoMapper.toTodoResponse(todo);
    }

    private TodoResponse updateMyDay(UUID userId, UUID todoId, boolean myDay) {
        Todo todo = requireOwnedTodo(userId, todoId);
        todo.setMyDay(myDay);
        touch(todo);
        todoMapper.updateById(todo);
        return todoDtoMapper.toTodoResponse(todo);
    }

    private LambdaQueryWrapper<Todo> buildQuery(UUID userId, TodoQueryRequest request) {
        LambdaQueryWrapper<Todo> wrapper = baseTodoQuery(userId);
        if (request.listId() != null) {
            todoListService.requireOwnedList(userId, request.listId());
            wrapper.eq(Todo::getListId, request.listId());
        }
        if (StringUtils.hasText(request.status())) {
            wrapper.eq(Todo::getStatus, request.status().trim());
        }
        if (request.important() != null) {
            wrapper.eq(Todo::getImportance, Boolean.TRUE.equals(request.important()) ? TodoImportance.IMPORTANT : TodoImportance.NORMAL);
        }
        if (request.myDay() != null) {
            wrapper.eq(Todo::getMyDay, request.myDay());
        }
        if (request.dueFrom() != null) {
            wrapper.ge(Todo::getDueDate, request.dueFrom());
        }
        if (request.dueTo() != null) {
            wrapper.le(Todo::getDueDate, request.dueTo());
        }
        if (request.remindFrom() != null) {
            wrapper.ge(Todo::getRemindAt, request.remindFrom());
        }
        if (request.remindTo() != null) {
            wrapper.le(Todo::getRemindAt, request.remindTo());
        }
        if (StringUtils.hasText(request.keyword())) {
            wrapper.and(nested -> nested.apply("(title ILIKE {0} OR note ILIKE {0})", "%" + request.keyword().trim() + "%"));
        }
        return wrapper;
    }

    private LambdaQueryWrapper<Todo> baseTodoQuery(UUID userId) {
        return new LambdaQueryWrapper<Todo>()
                .eq(Todo::getOwnerId, userId)
                .isNull(Todo::getDeletedAt);
    }

    private void applyDefaultSort(LambdaQueryWrapper<Todo> wrapper, TodoQueryRequest request) {
        if (request.listId() != null) {
            wrapper.orderByAsc(Todo::getSortOrder).orderByAsc(Todo::getCreatedAt);
        } else if (TodoStatus.COMPLETED.equals(request.status())) {
            wrapper.orderByDesc(Todo::getCompletedAt).orderByDesc(Todo::getUpdatedAt);
        } else {
            wrapper.orderByDesc(Todo::getUpdatedAt);
        }
    }

    private void validateQuery(TodoQueryRequest request) {
        if (StringUtils.hasText(request.status())
                && !TodoStatus.ACTIVE.equals(request.status().trim())
                && !TodoStatus.COMPLETED.equals(request.status().trim())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid todo status");
        }
        if (request.dueFrom() != null && request.dueTo() != null && request.dueFrom().isAfter(request.dueTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "dueFrom must be before dueTo");
        }
        if (request.remindFrom() != null && request.remindTo() != null && request.remindFrom().isAfter(request.remindTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "remindFrom must be before remindTo");
        }
    }

    private int nextSortOrder(UUID userId, UUID listId) {
        Todo last = todoMapper.selectOne(baseTodoQuery(userId)
                .eq(Todo::getListId, listId)
                .orderByDesc(Todo::getSortOrder)
                .last("LIMIT 1"));
        return last == null || last.getSortOrder() == null ? 1000 : last.getSortOrder() + 1000;
    }

    private void touch(Todo todo) {
        todo.setUpdatedAt(Instant.now());
        todo.setVersion(todo.getVersion() == null ? 1 : todo.getVersion() + 1);
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
