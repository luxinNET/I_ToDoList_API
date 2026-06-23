package com.example.itodo.tag.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

@TableName("todo_tags")
public class TodoTag {

    @TableId
    private UUID todoId;
    private UUID tagId;

    public UUID getTodoId() { return todoId; }
    public void setTodoId(UUID todoId) { this.todoId = todoId; }
    public UUID getTagId() { return tagId; }
    public void setTagId(UUID tagId) { this.tagId = tagId; }
}
