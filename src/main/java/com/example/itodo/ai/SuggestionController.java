package com.example.itodo.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "AI Suggestions")
@RestController
@RequestMapping("/api/v1/ai/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @Operation(summary = "获取个性化任务建议")
    @GetMapping
    public List<Map<String, Object>> getSuggestions(
            @AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser,
            @RequestParam(defaultValue = "5") int limit) {
        return suggestionService.getSuggestions(currentUser.id(), limit);
    }
}
