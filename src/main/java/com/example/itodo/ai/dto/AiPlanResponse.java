package com.example.itodo.ai.dto;

import java.util.List;

public record AiPlanResponse(
    String previewId,
    List<MilestoneResponse> milestones
) {
}
