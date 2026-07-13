package com.example.itodo.ai;

import com.example.itodo.ai.entity.AiReport;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class AiReportService {

    private final LlmClient llmClient;
    private final ReportAggregator reportAggregator;

    public AiReportService(LlmClient llmClient, ReportAggregator reportAggregator) {
        this.llmClient = llmClient;
        this.reportAggregator = reportAggregator;
    }

    public String generateDailyReport(UUID userId, LocalDate date) {
        Map<String, Object> stats = reportAggregator.aggregateDaily(userId, date);
        
        String systemPrompt = """
            You are a productivity assistant. Based on the task statistics, generate a concise daily report.
            Include: 1) Overview of completed tasks, 2) Highlights and areas for improvement, 3) Suggestions for tomorrow.
            Keep it under 200 words.
            """;
        
        String userPrompt = "Stats: " + stats.toString();
        
        return llmClient.chat(systemPrompt, userPrompt);
    }

    public String generateWeeklyReport(UUID userId, LocalDate weekStart) {
        Map<String, Object> stats = reportAggregator.aggregateWeekly(userId, weekStart);
        
        String systemPrompt = """
            You are a productivity assistant. Based on the task statistics, generate a concise weekly report.
            Include: 1) Week overview, 2) Key achievements, 3) Areas for improvement, 4) Next week's priorities.
            Keep it under 300 words.
            """;
        
        String userPrompt = "Stats: " + stats.toString();
        
        return llmClient.chat(systemPrompt, userPrompt);
    }
}
