package com.example.itodo.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {
    private boolean enabled = true;
    private String baseUrl;
    private String apiKey;
    private String model = "deepseek-chat";
    private double temperature = 0.3;
    private int maxTokens = 1024;
    private int timeoutSeconds = 30;
    private int cacheTtlSeconds = 300;
    private int dailyQuotaPerUser = 100;
    private int maxRetries = 3;
    private boolean forceJsonOutput = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public int getDailyQuotaPerUser() {
        return dailyQuotaPerUser;
    }

    public void setDailyQuotaPerUser(int dailyQuotaPerUser) {
        this.dailyQuotaPerUser = dailyQuotaPerUser;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean isForceJsonOutput() {
        return forceJsonOutput;
    }

    public void setForceJsonOutput(boolean forceJsonOutput) {
        this.forceJsonOutput = forceJsonOutput;
    }
}
