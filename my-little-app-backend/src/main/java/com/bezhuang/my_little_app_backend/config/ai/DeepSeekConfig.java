package com.bezhuang.my_little_app_backend.config.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek AI 配置类
 */
@Configuration
public class DeepSeekConfig {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model}")
    private String model;

    @Value("${deepseek.reasoner-model}")
    private String reasonerModel;

    @Value("${deepseek.max.tokens}")
    private int maxTokens;

    @Value("${deepseek.temperature:0.7}")
    private double temperature;

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public String getReasonerModel() {
        return reasonerModel;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }
}
