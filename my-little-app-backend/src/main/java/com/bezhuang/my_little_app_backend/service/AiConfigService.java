package com.bezhuang.my_little_app_backend.service;

import com.bezhuang.my_little_app_backend.entity.AiConfig;
import com.bezhuang.my_little_app_backend.mapper.AiConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 配置服务
 * 从数据库读取和管理 AI 相关配置，支持 Redis 缓存
 */
@Service
public class AiConfigService {

    private static final Logger logger = LoggerFactory.getLogger(AiConfigService.class);

    private final AiConfigMapper aiConfigMapper;

    // DeepSeek 配置键
    private static final String DEEPSEEK_API_KEY = "deepseek_api_key";
    private static final String DEEPSEEK_MODEL = "deepseek_model";
    private static final String DEEPSEEK_REASONER_MODEL = "deepseek_reasoner_model";
    private static final String DEEPSEEK_ENABLED = "deepseek_enabled";
    private static final String DEEPSEEK_BASE_URL = "deepseek_base_url";
    private static final String DEEPSEEK_MAX_TOKENS = "deepseek_max_tokens";
    private static final String DEEPSEEK_TEMPERATURE = "deepseek_temperature";

    // SiliconFlow 配置键
    private static final String SILICONFLOW_API_KEY = "siliconflow_api_key";
    private static final String SILICONFLOW_MODEL = "siliconflow_model";
    private static final String SILICONFLOW_REASONER_MODEL = "siliconflow_reasoner_model";
    private static final String SILICONFLOW_ENABLED = "siliconflow_enabled";
    private static final String SILICONFLOW_BASE_URL = "siliconflow_base_url";
    private static final String SILICONFLOW_MAX_TOKENS = "siliconflow_max_tokens";
    private static final String SILICONFLOW_TEMPERATURE = "siliconflow_temperature";

    // Bocha 配置键
    private static final String BOCHA_API_KEY = "bocha_api_key";
    private static final String BOCHA_ENABLED = "bocha_enabled";
    private static final String BOCHA_SEARCH_LIMIT = "bocha_search_limit";

    // 缓存名称
    private static final String CACHE_NAME = "aiConfig";

    public AiConfigService(AiConfigMapper aiConfigMapper) {
        this.aiConfigMapper = aiConfigMapper;
    }

    /**
     * 获取配置值，如果不存在则返回默认值
     */
    @Cacheable(value = CACHE_NAME, key = "#configKey", unless = "#result == null")
    public String getConfig(String configKey, String defaultValue) {
        logger.debug("从数据库读取配置: {}", configKey);
        AiConfig config = aiConfigMapper.selectByConfigKey(configKey);
        return config != null ? config.getConfigValue() : defaultValue;
    }

    /**
     * 获取配置值，如果不存在则返回空字符串
     */
    @Cacheable(value = CACHE_NAME, key = "#configKey", unless = "#result == null")
    public String getConfig(String configKey) {
        return getConfig(configKey, "");
    }

    /**
     * 清除所有 AI 配置缓存
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearCache() {
        logger.info("已清除 AI 配置缓存");
    }

    // ==================== DeepSeek 配置 ====================

    @Cacheable(value = CACHE_NAME, key = "'" + DEEPSEEK_API_KEY + "'", unless = "#result == null")
    public String getDeepSeekApiKey() {
        return getConfig(DEEPSEEK_API_KEY, "");
    }

    @Cacheable(value = CACHE_NAME, key = "'" + DEEPSEEK_MODEL + "'", unless = "#result == null")
    public String getDeepSeekModel() {
        return getConfig(DEEPSEEK_MODEL, "deepseek-chat");
    }

    @Cacheable(value = CACHE_NAME, key = "'" + DEEPSEEK_REASONER_MODEL + "'", unless = "#result == null")
    public String getDeepSeekReasonerModel() {
        return getConfig(DEEPSEEK_REASONER_MODEL, "deepseek-reasoner");
    }

    @Cacheable(value = CACHE_NAME, key = "'" + DEEPSEEK_ENABLED + "'", unless = "#result == null")
    public boolean isDeepSeekEnabled() {
        return "true".equalsIgnoreCase(getConfig(DEEPSEEK_ENABLED, "true"));
    }

    @Cacheable(value = CACHE_NAME, key = "'" + DEEPSEEK_BASE_URL + "'", unless = "#result == null")
    public String getDeepSeekBaseUrl() {
        return getConfig(DEEPSEEK_BASE_URL, "https://api.deepseek.com");
    }

    @Cacheable(value = CACHE_NAME, key = "'" + DEEPSEEK_MAX_TOKENS + "'", unless = "#result == null")
    public int getDeepSeekMaxTokens() {
        String value = getConfig(DEEPSEEK_MAX_TOKENS, "4096");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 4096;
        }
    }

    @Cacheable(value = CACHE_NAME, key = "'" + DEEPSEEK_TEMPERATURE + "'", unless = "#result == null")
    public double getDeepSeekTemperature() {
        String value = getConfig(DEEPSEEK_TEMPERATURE, "0.7");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.7;
        }
    }

    // ==================== SiliconFlow 配置 ====================

    @Cacheable(value = CACHE_NAME, key = "'" + SILICONFLOW_API_KEY + "'", unless = "#result == null")
    public String getSiliconFlowApiKey() {
        return getConfig(SILICONFLOW_API_KEY, "");
    }

    @Cacheable(value = CACHE_NAME, key = "'" + SILICONFLOW_MODEL + "'", unless = "#result == null")
    public String getSiliconFlowModel() {
        return getConfig(SILICONFLOW_MODEL, "deepseek-ai/DeepSeek-V2.5");
    }

    @Cacheable(value = CACHE_NAME, key = "'" + SILICONFLOW_REASONER_MODEL + "'", unless = "#result == null")
    public String getSiliconFlowReasonerModel() {
        return getConfig(SILICONFLOW_REASONER_MODEL, "deepseek-ai/DeepSeek-V2.5");
    }

    @Cacheable(value = CACHE_NAME, key = "'" + SILICONFLOW_ENABLED + "'", unless = "#result == null")
    public boolean isSiliconFlowEnabled() {
        return "true".equalsIgnoreCase(getConfig(SILICONFLOW_ENABLED, "false"));
    }

    @Cacheable(value = CACHE_NAME, key = "'" + SILICONFLOW_BASE_URL + "'", unless = "#result == null")
    public String getSiliconFlowBaseUrl() {
        return getConfig(SILICONFLOW_BASE_URL, "https://api.siliconflow.cn/v1");
    }

    @Cacheable(value = CACHE_NAME, key = "'" + SILICONFLOW_MAX_TOKENS + "'", unless = "#result == null")
    public int getSiliconFlowMaxTokens() {
        String value = getConfig(SILICONFLOW_MAX_TOKENS, "4096");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 4096;
        }
    }

    @Cacheable(value = CACHE_NAME, key = "'" + SILICONFLOW_TEMPERATURE + "'", unless = "#result == null")
    public double getSiliconFlowTemperature() {
        String value = getConfig(SILICONFLOW_TEMPERATURE, "0.7");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.7;
        }
    }

    // ==================== Bocha 配置 ====================

    @Cacheable(value = CACHE_NAME, key = "'" + BOCHA_API_KEY + "'", unless = "#result == null")
    public String getBochaApiKey() {
        return getConfig(BOCHA_API_KEY, "");
    }

    @Cacheable(value = CACHE_NAME, key = "'" + BOCHA_ENABLED + "'", unless = "#result == null")
    public boolean isBochaEnabled() {
        return "true".equalsIgnoreCase(getConfig(BOCHA_ENABLED, "false"));
    }

    @Cacheable(value = CACHE_NAME, key = "'" + BOCHA_SEARCH_LIMIT + "'", unless = "#result == null")
    public int getBochaSearchLimit() {
        String value = getConfig(BOCHA_SEARCH_LIMIT, "5");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    /**
     * 获取所有 AI 配置（用于缓存预热）
     */
    @Cacheable(value = CACHE_NAME, key = "'all_config'", unless = "#result == null")
    public Map<String, Object> getAllConfig() {
        logger.info("从数据库加载所有 AI 配置");
        Map<String, Object> config = new HashMap<>();

        // DeepSeek 配置
        config.put("deepseekApiKey", getDeepSeekApiKey());
        config.put("deepseekModel", getDeepSeekModel());
        config.put("deepseekEnabled", isDeepSeekEnabled());
        config.put("deepseekBaseUrl", getDeepSeekBaseUrl());
        config.put("deepseekMaxTokens", getDeepSeekMaxTokens());
        config.put("deepseekTemperature", getDeepSeekTemperature());

        // SiliconFlow 配置
        config.put("siliconflowApiKey", getSiliconFlowApiKey());
        config.put("siliconflowModel", getSiliconFlowModel());
        config.put("siliconflowEnabled", isSiliconFlowEnabled());
        config.put("siliconflowBaseUrl", getSiliconFlowBaseUrl());
        config.put("siliconflowMaxTokens", getSiliconFlowMaxTokens());
        config.put("siliconflowTemperature", getSiliconFlowTemperature());

        // Bocha 配置
        config.put("bochaApiKey", getBochaApiKey());
        config.put("bochaEnabled", isBochaEnabled());
        config.put("bochaSearchLimit", getBochaSearchLimit());

        return config;
    }
}
