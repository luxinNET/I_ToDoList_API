package com.example.itodo.todo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.itodo.todo.entity.TodoList;
import com.example.itodo.todo.mapper.TodoListMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DefaultTodoListInitializer {

    private static final String DEFAULT_LIST_NAME = "任务";

    private final TodoListMapper todoListMapper;

    public DefaultTodoListInitializer(TodoListMapper todoListMapper) {
        this.todoListMapper = todoListMapper;
    }

    @Transactional
    public void initializeForNewUser(UUID userId) {
        ensureDefaultList(userId);
    }

    @Transactional
    public TodoList ensureDefaultList(UUID userId) {
        TodoList existing = todoListMapper.selectOne(new LambdaQueryWrapper<TodoList>()
                .eq(TodoList::getOwnerId, userId)
                .eq(TodoList::getIsSystem, true)
                .isNull(TodoList::getDeletedAt)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }

        Instant now = Instant.now();
        TodoList todoList = new TodoList();
        todoList.setId(UUID.randomUUID());
        todoList.setOwnerId(userId);
        todoList.setName(DEFAULT_LIST_NAME);
        todoList.setSortOrder(0);
        todoList.setIsSystem(true);
        todoList.setIsShared(false);
        todoList.setCreatedAt(now);
        todoList.setUpdatedAt(now);
        todoListMapper.insert(todoList);
        return todoList;
    }

    @Transactional
    public UUID ensureDefaultListId(UUID userId) {
        return ensureDefaultList(userId).getId();
    }
}
