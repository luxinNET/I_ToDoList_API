package com.example.itodo.tag;

import com.example.itodo.common.api.ApiResponse;
import com.example.itodo.security.CurrentUser;
import com.example.itodo.tag.dto.CreateTagRequest;
import com.example.itodo.tag.dto.TagResponse;
import com.example.itodo.tag.dto.UpdateTagRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tags")
@RestController
@RequestMapping("/api/v1")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Operation(summary = "查询标签")
    @GetMapping("/tags")
    ApiResponse<List<TagResponse>> listTags(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(tagService.listTags(currentUser.id()));
    }

    @Operation(summary = "创建标签")
    @PostMapping("/tags")
    ApiResponse<TagResponse> createTag(@AuthenticationPrincipal CurrentUser currentUser,
                                       @Valid @RequestBody CreateTagRequest request) {
        return ApiResponse.ok(tagService.createTag(currentUser.id(), request));
    }

    @Operation(summary = "更新标签")
    @PatchMapping("/tags/{tagId}")
    ApiResponse<TagResponse> updateTag(@AuthenticationPrincipal CurrentUser currentUser,
                                       @PathVariable UUID tagId,
                                       @Valid @RequestBody UpdateTagRequest request) {
        return ApiResponse.ok(tagService.updateTag(currentUser.id(), tagId, request));
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/tags/{tagId}")
    ApiResponse<Void> deleteTag(@AuthenticationPrincipal CurrentUser currentUser,
                                @PathVariable UUID tagId) {
        tagService.deleteTag(currentUser.id(), tagId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "给任务绑定标签")
    @PostMapping("/todos/{todoId}/tags/{tagId}")
    ApiResponse<Void> attachTag(@AuthenticationPrincipal CurrentUser currentUser,
                                @PathVariable UUID todoId,
                                @PathVariable UUID tagId) {
        tagService.attachTag(currentUser.id(), todoId, tagId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "移除任务标签")
    @DeleteMapping("/todos/{todoId}/tags/{tagId}")
    ApiResponse<Void> detachTag(@AuthenticationPrincipal CurrentUser currentUser,
                                @PathVariable UUID todoId,
                                @PathVariable UUID tagId) {
        tagService.detachTag(currentUser.id(), todoId, tagId);
        return ApiResponse.ok(null);
    }
}
