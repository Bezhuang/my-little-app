package com.bezhuang.my_little_app_backend.config.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 硅基流动 API 配置
 */
@ConfigurationProperties(prefix = "siliconflow")
public class SiliconFlowConfig {

    private String apiKey;
    private String baseUrl;  // 匹配 siliconflow.base-url
    private String model;
    private String reasonerModel;  // 推理模型
    private int maxTokens;
    private double temperature;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getReasonerModel() {
        return reasonerModel;
    }

    public void setReasonerModel(String reasonerModel) {
        this.reasonerModel = reasonerModel;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}
