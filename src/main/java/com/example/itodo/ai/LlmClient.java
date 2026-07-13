package com.example.itodo.ai;

public interface LlmClient {
    String chat(String systemPrompt, String userPrompt);
    
    <T> T chatForObject(String systemPrompt, String userPrompt, Class<T> type);
}
