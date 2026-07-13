package com.example.itodo.ai;

import com.example.itodo.ai.entity.AiReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "AI Reports")
@RestController
@RequestMapping("/api/v1/ai/reports")
public class AiReportController {

    private final AiReportService aiReportService;

    public AiReportController(AiReportService aiReportService) {
        this.aiReportService = aiReportService;
    }

    @Operation(summary = "获取日报")
    @GetMapping("/daily")
    public String getDailyReport(
            @AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser,
            @RequestParam LocalDate date) {
        return aiReportService.generateDailyReport(currentUser.id(), date);
    }

    @Operation(summary = "获取周报")
    @GetMapping("/weekly")
    public String getWeeklyReport(
            @AuthenticationPrincipal com.example.itodo.security.CurrentUser currentUser,
            @RequestParam LocalDate weekStart) {
        return aiReportService.generateWeeklyReport(currentUser.id(), weekStart);
    }
}
