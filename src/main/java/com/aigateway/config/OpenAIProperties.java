package com.aigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aigateway.llm.openai")
public class OpenAIProperties {
    private String baseUrl;
    private String apiKey;
    private long timeoutMs = 30000;
    private String defaultModel = "gpt-4o-mini";
}
