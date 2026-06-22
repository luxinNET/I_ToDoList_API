package com.example.itodo.todo;

import com.example.itodo.todo.dto.TodoListResponse;
import com.example.itodo.todo.dto.TodoResponse;
import com.example.itodo.todo.dto.TodoStepResponse;
import com.example.itodo.todo.entity.Todo;
import com.example.itodo.todo.entity.TodoList;
import com.example.itodo.todo.entity.TodoStep;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TodoDtoMapper {

    TodoListResponse toListResponse(TodoList todoList);

    List<TodoListResponse> toListResponses(List<TodoList> todoLists);

    TodoResponse toTodoResponse(Todo todo);

    List<TodoResponse> toTodoResponses(List<Todo> todos);

    TodoStepResponse toStepResponse(TodoStep todoStep);

    List<TodoStepResponse> toStepResponses(List<TodoStep> todoSteps);
}
