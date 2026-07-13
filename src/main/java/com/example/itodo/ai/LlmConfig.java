package com.example.itodo.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    @Bean
    public RestClient.Builder llmRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public LlmClient llmClient(RestClient.Builder restClientBuilder, LlmProperties properties) {
        return new OpenAiCompatibleLlmClient(restClientBuilder, properties);
    }
}
