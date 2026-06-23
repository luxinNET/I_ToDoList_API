package com.example.itodo.tag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.itodo.common.error.BusinessException;
import com.example.itodo.common.error.ErrorCode;
import com.example.itodo.sync.SyncChangeService;
import com.example.itodo.sync.SyncOperation;
import com.example.itodo.sync.SyncResourceType;
import com.example.itodo.tag.dto.CreateTagRequest;
import com.example.itodo.tag.dto.TagResponse;
import com.example.itodo.tag.dto.UpdateTagRequest;
import com.example.itodo.tag.entity.Tag;
import com.example.itodo.tag.entity.TodoTag;
import com.example.itodo.tag.mapper.TagMapper;
import com.example.itodo.tag.mapper.TodoTagMapper;
import com.example.itodo.todo.TodoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TagService {

    private final TagMapper tagMapper;
    private final TodoTagMapper todoTagMapper;
    private final TagDtoMapper tagDtoMapper;
    private final TodoService todoService;
    private final SyncChangeService syncChangeService;

    public TagService(TagMapper tagMapper,
                      TodoTagMapper todoTagMapper,
                      TagDtoMapper tagDtoMapper,
                      TodoService todoService,
                      SyncChangeService syncChangeService) {
        this.tagMapper = tagMapper;
        this.todoTagMapper = todoTagMapper;
        this.tagDtoMapper = tagDtoMapper;
        this.todoService = todoService;
        this.syncChangeService = syncChangeService;
    }

    public List<TagResponse> listTags(UUID userId) {
        List<Tag> tags = tagMapper.selectList(baseTagQuery(userId)
                .orderByAsc(Tag::getName)
                .orderByAsc(Tag::getCreatedAt));
        return tagDtoMapper.toTagResponses(tags);
    }

    @Transactional
    public TagResponse createTag(UUID userId, CreateTagRequest request) {
        String name = requireName(request.name());
        String color = normalize(request.color());
        Tag existing = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId)
                .eq(Tag::getName, name)
                .last("LIMIT 1"));
        if (existing != null && existing.getDeletedAt() == null) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Tag name already exists");
        }

        Instant now = Instant.now();
        Tag tag = existing == null ? new Tag() : existing;
        if (existing == null) {
            tag.setId(UUID.randomUUID());
            tag.setUserId(userId);
            tag.setCreatedAt(now);
        }
        tag.setName(name);
        tag.setColor(color);
        tag.setUpdatedAt(now);
        tag.setDeletedAt(null);
        if (existing == null) {
            tagMapper.insert(tag);
            syncChangeService.recordChange(userId, SyncResourceType.TAG, tag.getId(), SyncOperation.CREATE);
        } else {
            tagMapper.updateById(tag);
            syncChangeService.recordChange(userId, SyncResourceType.TAG, tag.getId(), SyncOperation.UPDATE);
        }
        return tagDtoMapper.toTagResponse(tag);
    }

    @Transactional
    public TagResponse updateTag(UUID userId, UUID tagId, UpdateTagRequest request) {
        Tag tag = requireOwnedTag(userId, tagId);
        boolean changed = false;
        if (request.name() != null) {
            String name = requireName(request.name());
            if (!name.equals(tag.getName())) {
                ensureNameAvailable(userId, tagId, name);
                tag.setName(name);
                changed = true;
            }
        }
        if (request.color() != null) {
            tag.setColor(normalize(request.color()));
            changed = true;
        }
        if (changed) {
            tag.setUpdatedAt(Instant.now());
            tagMapper.updateById(tag);
            syncChangeService.recordChange(userId, SyncResourceType.TAG, tagId, SyncOperation.UPDATE);
        }
        return tagDtoMapper.toTagResponse(tag);
    }

    @Transactional
    public void deleteTag(UUID userId, UUID tagId) {
        requireOwnedTag(userId, tagId);
        Instant now = Instant.now();
        tagMapper.update(null, new LambdaUpdateWrapper<Tag>()
                .eq(Tag::getUserId, userId)
                .eq(Tag::getId, tagId)
                .isNull(Tag::getDeletedAt)
                .set(Tag::getDeletedAt, now)
                .set(Tag::getUpdatedAt, now));
        todoTagMapper.delete(new LambdaQueryWrapper<TodoTag>().eq(TodoTag::getTagId, tagId));
        syncChangeService.recordChange(userId, SyncResourceType.TAG, tagId, SyncOperation.DELETE);
    }

    @Transactional
    public void attachTag(UUID userId, UUID todoId, UUID tagId) {
        todoService.requireOwnedTodo(userId, todoId);
        requireOwnedTag(userId, tagId);
        Long count = todoTagMapper.selectCount(new LambdaQueryWrapper<TodoTag>()
                .eq(TodoTag::getTodoId, todoId)
                .eq(TodoTag::getTagId, tagId));
        if (count == 0) {
            TodoTag todoTag = new TodoTag();
            todoTag.setTodoId(todoId);
            todoTag.setTagId(tagId);
            todoTagMapper.insert(todoTag);
            todoService.touchTodo(userId, todoId);
            syncChangeService.recordChange(userId, SyncResourceType.TODO_TAG, todoId, SyncOperation.CREATE);
        }
    }

    @Transactional
    public void detachTag(UUID userId, UUID todoId, UUID tagId) {
        todoService.requireOwnedTodo(userId, todoId);
        requireOwnedTag(userId, tagId);
        int deleted = todoTagMapper.delete(new LambdaQueryWrapper<TodoTag>()
                .eq(TodoTag::getTodoId, todoId)
                .eq(TodoTag::getTagId, tagId));
        if (deleted > 0) {
            todoService.touchTodo(userId, todoId);
            syncChangeService.recordChange(userId, SyncResourceType.TODO_TAG, todoId, SyncOperation.DELETE);
        }
    }

    public Tag requireOwnedTag(UUID userId, UUID tagId) {
        Tag tag = tagMapper.selectOne(baseTagQuery(userId).eq(Tag::getId, tagId));
        if (tag == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Tag not found");
        }
        return tag;
    }

    private LambdaQueryWrapper<Tag> baseTagQuery(UUID userId) {
        return new LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId)
                .isNull(Tag::getDeletedAt);
    }

    private void ensureNameAvailable(UUID userId, UUID currentTagId, String name) {
        Long count = tagMapper.selectCount(baseTagQuery(userId)
                .eq(Tag::getName, name)
                .ne(Tag::getId, currentTagId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Tag name already exists");
        }
    }

    private String requireName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Tag name must not be blank");
        }
        return value.trim();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
