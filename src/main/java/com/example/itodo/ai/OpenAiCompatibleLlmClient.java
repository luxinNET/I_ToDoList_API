package com.example.itodo.ai;

import com.example.itodo.common.error.ErrorCode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);
    private static final Pattern JSON_PATTERN = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```|(\\{[\\s\\S]*\\})");

    private final RestClient restClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(RestClient.Builder restClientBuilder, LlmProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return chatWithRetry(systemPrompt, userPrompt, properties.getMaxRetries());
    }

    private String chatWithRetry(String systemPrompt, String userPrompt, int maxRetries) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                // 使用 HashMap 构建可变 request
                Map<String, Object> request = new HashMap<>();
                request.put("model", properties.getModel());
                request.put("messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ));
                request.put("temperature", properties.getTemperature());
                request.put("max_tokens", properties.getMaxTokens());

                // 如果强制 JSON 输出，添加 response_format 参数
                if (properties.isForceJsonOutput()) {
                    request.put("response_format", Map.of("type", "json_object"));
                }

                Map<String, Object> response = restClient.post()
                        .uri("/chat/completions")
                        .body(request)
                        .retrieve()
                        .body(Map.class);

                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");

            } catch (Exception e) {
                attempt++;
                lastException = e;
                log.warn("LLM call attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L * attempt); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AiException(ErrorCode.AI_SERVICE_UNAVAILABLE, "LLM call interrupted", ie);
                    }
                }
            }
        }

        throw new AiException(ErrorCode.AI_SERVICE_UNAVAILABLE, 
                "LLM call failed after " + maxRetries + " attempts: " + lastException.getMessage(), lastException);
    }

    @Override
    public <T> T chatForObject(String systemPrompt, String userPrompt, Class<T> type) {
        String jsonResponse = chat(systemPrompt, userPrompt);
        
        // 尝试提取 JSON（容错：LLM 可能在 markdown code block 中返回 JSON）
        String extractedJson = extractJson(jsonResponse);
        
        try {
            return objectMapper.readValue(extractedJson, type);
        } catch (Exception e) {
            log.error("Failed to parse LLM JSON response: {}. Original response: {}", e.getMessage(), jsonResponse);
            throw new AiException(ErrorCode.AI_PARSE_FAILED, "Failed to parse AI response as JSON", e);
        }
    }

    private String extractJson(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        // 尝试从 markdown code block 中提取 JSON
        Matcher matcher = JSON_PATTERN.matcher(text);
        if (matcher.find()) {
            String json = matcher.group(1);
            if (json == null) {
                json = matcher.group(2);
            }
            if (json != null) {
                return json.trim();
            }
        }

        // 如果没有找到 JSON，返回原始文本
        return text;
    }
}
