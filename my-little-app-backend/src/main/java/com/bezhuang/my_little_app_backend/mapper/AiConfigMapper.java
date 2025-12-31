package com.bezhuang.my_little_app_backend.mapper;

import com.bezhuang.my_little_app_backend.entity.AiConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI 配置 Mapper
 */
@Mapper
public interface AiConfigMapper {

    /**
     * 根据配置键获取配置值
     */
    AiConfig selectByConfigKey(@Param("configKey") String configKey);

    /**
     * 更新配置值
     */
    int updateConfigValue(@Param("configKey") String configKey, @Param("configValue") String configValue);

    /**
     * 插入配置（如果不存在）
     */
    int insert(AiConfig aiConfig);
}
