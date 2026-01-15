package com.bezhuang.my_little_app_backend.config.ai;

import com.bezhuang.my_little_app_backend.service.AiConfigService;
import org.springframework.stereotype.Component;

/**
 * SiliconFlow AI 配置类
 * 从数据库读取配置
 */
@Component
public class SiliconFlowConfig {

    private final AiConfigService aiConfigService;

    public SiliconFlowConfig(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
    }

    public String getApiKey() {
        return aiConfigService.getSiliconFlowApiKey();
    }

    public String getBaseUrl() {
        return aiConfigService.getSiliconFlowBaseUrl();
    }

    public String getModel() {
        return aiConfigService.getSiliconFlowModel();
    }

    public String getReasonerModel() {
        return aiConfigService.getSiliconFlowReasonerModel();
    }

    public int getMaxTokens() {
        return aiConfigService.getSiliconFlowMaxTokens();
    }

    public double getTemperature() {
        return aiConfigService.getSiliconFlowTemperature();
    }

    public boolean isEnabled() {
        return aiConfigService.isSiliconFlowEnabled();
    }
}
