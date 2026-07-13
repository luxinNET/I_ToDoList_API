package com.example.itodo.ai;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiScheduler {

    private final AiReportService aiReportService;

    public AiScheduler(AiReportService aiReportService) {
        this.aiReportService = aiReportService;
    }

    @Scheduled(cron = "0 0 23 * * *")  // 每天 23:00 生成日报
    public void generateDailyReports() {
        // TODO: 查询所有活跃用户，生成日报
        System.out.println("Generating daily reports...");
    }

    @Scheduled(cron = "0 0 9 * * MON")  // 每周一 09:00 生成周报
    public void generateWeeklyReports() {
        // TODO: 查询所有活跃用户，生成上周周报
        System.out.println("Generating weekly reports...");
    }
}
