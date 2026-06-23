package com.example.itodo.todo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.itodo.common.error.BusinessException;
import com.example.itodo.common.error.ErrorCode;
import com.example.itodo.sync.SyncChangeService;
import com.example.itodo.sync.SyncOperation;
import com.example.itodo.sync.SyncResourceType;
import com.example.itodo.todo.dto.CreateTodoStepRequest;
import com.example.itodo.todo.dto.ReorderItemRequest;
import com.example.itodo.todo.dto.ReorderStepsRequest;
import com.example.itodo.todo.dto.TodoStepResponse;
import com.example.itodo.todo.dto.UpdateTodoStepRequest;
import com.example.itodo.todo.entity.TodoStep;
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
public class TodoStepService {

    private final TodoStepMapper todoStepMapper;
    private final TodoDtoMapper todoDtoMapper;
    private final TodoService todoService;
    private final SyncChangeService syncChangeService;

    public TodoStepService(TodoStepMapper todoStepMapper,
                           TodoDtoMapper todoDtoMapper,
                           TodoService todoService,
                           SyncChangeService syncChangeService) {
        this.todoStepMapper = todoStepMapper;
        this.todoDtoMapper = todoDtoMapper;
        this.todoService = todoService;
        this.syncChangeService = syncChangeService;
    }

    public List<TodoStepResponse> listSteps(UUID userId, UUID todoId) {
        todoService.requireOwnedTodo(userId, todoId);
        List<TodoStep> steps = todoStepMapper.selectList(baseStepQuery(todoId)
                .orderByAsc(TodoStep::getSortOrder)
                .orderByAsc(TodoStep::getCreatedAt));
        return todoDtoMapper.toStepResponses(steps);
    }

    @Transactional
    public TodoStepResponse createStep(UUID userId, UUID todoId, CreateTodoStepRequest request) {
        todoService.requireOwnedTodo(userId, todoId);
        Instant now = Instant.now();
        TodoStep step = new TodoStep();
        step.setId(UUID.randomUUID());
        step.setTodoId(todoId);
        step.setTitle(request.title().trim());
        step.setIsCompleted(Boolean.TRUE.equals(request.isCompleted()));
        step.setSortOrder(request.sortOrder() == null ? nextSortOrder(todoId) : request.sortOrder());
        step.setCreatedAt(now);
        step.setUpdatedAt(now);
        todoStepMapper.insert(step);
        todoService.touchTodo(userId, todoId);
        syncChangeService.recordChange(userId, SyncResourceType.STEP, step.getId(), SyncOperation.CREATE);
        return todoDtoMapper.toStepResponse(step);
    }

    @Transactional
    public TodoStepResponse updateStep(UUID userId, UUID todoId, UUID stepId, UpdateTodoStepRequest request) {
        todoService.requireOwnedTodo(userId, todoId);
        TodoStep step = requireStep(todoId, stepId);
        boolean changed = false;
        if (request.title() != null) {
            if (!StringUtils.hasText(request.title())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Step title must not be blank");
            }
            step.setTitle(request.title().trim());
            changed = true;
        }
        if (request.isCompleted() != null) {
            step.setIsCompleted(request.isCompleted());
            changed = true;
        }
        if (request.sortOrder() != null) {
            step.setSortOrder(request.sortOrder());
            changed = true;
        }
        if (changed) {
            step.setUpdatedAt(Instant.now());
            todoStepMapper.updateById(step);
            todoService.touchTodo(userId, todoId);
            syncChangeService.recordChange(userId, SyncResourceType.STEP, stepId, SyncOperation.UPDATE);
        }
        return todoDtoMapper.toStepResponse(step);
    }

    @Transactional
    public void deleteStep(UUID userId, UUID todoId, UUID stepId) {
        todoService.requireOwnedTodo(userId, todoId);
        requireStep(todoId, stepId);
        todoStepMapper.update(null, new LambdaUpdateWrapper<TodoStep>()
                .eq(TodoStep::getTodoId, todoId)
                .eq(TodoStep::getId, stepId)
                .isNull(TodoStep::getDeletedAt)
                .set(TodoStep::getDeletedAt, Instant.now())
                .set(TodoStep::getUpdatedAt, Instant.now()));
        todoService.touchTodo(userId, todoId);
        syncChangeService.recordChange(userId, SyncResourceType.STEP, stepId, SyncOperation.DELETE);
    }

    @Transactional
    public void reorderSteps(UUID userId, UUID todoId, ReorderStepsRequest request) {
        todoService.requireOwnedTodo(userId, todoId);
        ensureNoDuplicateIds(request.items());
        List<UUID> ids = request.items().stream().map(ReorderItemRequest::id).toList();
        Long count = todoStepMapper.selectCount(baseStepQuery(todoId).in(TodoStep::getId, ids));
        if (count != ids.size()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Step not found");
        }
        Instant now = Instant.now();
        for (ReorderItemRequest item : request.items()) {
            todoStepMapper.update(null, new LambdaUpdateWrapper<TodoStep>()
                    .eq(TodoStep::getTodoId, todoId)
                    .eq(TodoStep::getId, item.id())
                    .isNull(TodoStep::getDeletedAt)
                    .set(TodoStep::getSortOrder, item.sortOrder())
                    .set(TodoStep::getUpdatedAt, now));
            syncChangeService.recordChange(userId, SyncResourceType.STEP, item.id(), SyncOperation.UPDATE);
        }
        todoService.touchTodo(userId, todoId);
    }

    private TodoStep requireStep(UUID todoId, UUID stepId) {
        TodoStep step = todoStepMapper.selectOne(baseStepQuery(todoId).eq(TodoStep::getId, stepId));
        if (step == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Step not found");
        }
        return step;
    }

    private LambdaQueryWrapper<TodoStep> baseStepQuery(UUID todoId) {
        return new LambdaQueryWrapper<TodoStep>()
                .eq(TodoStep::getTodoId, todoId)
                .isNull(TodoStep::getDeletedAt);
    }

    private int nextSortOrder(UUID todoId) {
        TodoStep last = todoStepMapper.selectOne(baseStepQuery(todoId)
                .orderByDesc(TodoStep::getSortOrder)
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
}
