package com.neha.paynudge.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {
    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Bean
    public LlmClient llmClient(@Value("${paynudge.llm.api-key:}") String apiKey,
                               @Value("${paynudge.llm.base-url}") String baseUrl,
                               @Value("${paynudge.llm.model}") String model) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("No API key set: using the offline mock model. Set GEMINI_API_KEY to use a real model.");
            return new MockLlmClient();
        }
        log.info("Using model {} at {}", model, baseUrl);
        return new ApiLlmClient(baseUrl, apiKey, model);
    }
}
