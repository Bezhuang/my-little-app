package com.bezhuang.my_little_app_backend.config.ai;

import com.bezhuang.my_little_app_backend.service.AiConfigService;
import org.springframework.stereotype.Component;

/**
 * Bocha Web Search API 配置类
 * 从数据库读取配置
 */
@Component
public class BochaConfig {

    private final AiConfigService aiConfigService;

    public BochaConfig(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
    }

    public String getApiKey() {
        return aiConfigService.getBochaApiKey();
    }

    public boolean isEnabled() {
        return aiConfigService.isBochaEnabled();
    }

    public int getSearchLimit() {
        return aiConfigService.getBochaSearchLimit();
    }
}
