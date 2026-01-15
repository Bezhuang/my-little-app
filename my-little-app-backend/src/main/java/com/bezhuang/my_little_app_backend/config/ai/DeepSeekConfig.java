package com.bezhuang.my_little_app_backend.config.ai;

import com.bezhuang.my_little_app_backend.service.AiConfigService;
import org.springframework.stereotype.Component;

/**
 * DeepSeek AI 配置类
 * 从数据库读取配置
 */
@Component
public class DeepSeekConfig {

    private final AiConfigService aiConfigService;

    public DeepSeekConfig(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
    }

    public String getApiKey() {
        return aiConfigService.getDeepSeekApiKey();
    }

    public String getBaseUrl() {
        return aiConfigService.getDeepSeekBaseUrl();
    }

    public String getModel() {
        return aiConfigService.getDeepSeekModel();
    }

    public String getReasonerModel() {
        return aiConfigService.getDeepSeekReasonerModel();
    }

    public int getMaxTokens() {
        return aiConfigService.getDeepSeekMaxTokens();
    }

    public double getTemperature() {
        return aiConfigService.getDeepSeekTemperature();
    }

    public boolean isEnabled() {
        return aiConfigService.isDeepSeekEnabled();
    }
}
