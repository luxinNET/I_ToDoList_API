package com.example.itodo.calendar;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.itodo.todo.entity.Todo;
import com.example.itodo.todo.TodoStatus;
import com.example.itodo.todo.mapper.TodoMapper;
import com.example.itodo.todo.dto.TodoResponse;
import com.example.itodo.todo.TodoDtoMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CalendarService {

    private final TodoMapper todoMapper;
    private final TodoDtoMapper todoDtoMapper;

    public CalendarService(TodoMapper todoMapper, TodoDtoMapper todoDtoMapper) {
        this.todoMapper = todoMapper;
        this.todoDtoMapper = todoDtoMapper;
    }

    public List<CalendarDayStats> getCalendarOverview(UUID userId, LocalDate from, LocalDate to) {
        List<CalendarDayStats> result = new ArrayList<>();
        LocalDate current = from;
        
        while (!current.isAfter(to)) {
            long total = countByDate(userId, current, null);
            long completed = countByDate(userId, current, TodoStatus.COMPLETED);
            long active = countByDate(userId, current, TodoStatus.ACTIVE);
            long overdue = countOverdueByDate(userId, current);
            
            result.add(new CalendarDayStats(current, total, completed, active, overdue));
            current = current.plusDays(1);
        }
        
        return result;
    }

    public List<TodoResponse> getTodosByDate(UUID userId, LocalDate date) {
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getOwnerId, userId)
               .eq(Todo::getDueDate, date)
               .isNull(Todo::getDeletedAt)
               .orderByAsc(Todo::getSortOrder);
        
        List<Todo> todos = todoMapper.selectList(wrapper);
        return todoDtoMapper.toTodoResponses(todos);
    }

    private long countByDate(UUID userId, LocalDate date, String status) {
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getOwnerId, userId)
               .eq(Todo::getDueDate, date)
               .isNull(Todo::getDeletedAt);
        
        if (status != null) {
            wrapper.eq(Todo::getStatus, status);
        }
        
        return todoMapper.selectCount(wrapper);
    }

    private long countOverdueByDate(UUID userId, LocalDate date) {
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getOwnerId, userId)
               .lt(Todo::getDueDate, date)
               .eq(Todo::getStatus, TodoStatus.ACTIVE)
               .isNull(Todo::getDeletedAt);
        
        return todoMapper.selectCount(wrapper);
    }
}
