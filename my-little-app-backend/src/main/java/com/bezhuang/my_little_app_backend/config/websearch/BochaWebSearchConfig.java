package com.bezhuang.my_little_app_backend.config.websearch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 博查AI Web Search API 配置
 */
@Configuration
public class BochaWebSearchConfig {

    @Value("${bocha.websearch.api.key:}")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR-BOCHA-API-KEY-HERE");
    }
}
