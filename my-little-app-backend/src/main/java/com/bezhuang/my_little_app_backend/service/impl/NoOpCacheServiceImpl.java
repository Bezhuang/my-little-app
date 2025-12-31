package com.bezhuang.my_little_app_backend.service.impl;

import com.bezhuang.my_little_app_backend.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * 无操作缓存服务实现
 * 当 Redis 不可用或未配置时，使用此实现
 * 直接通过 DataLoader 加载数据，不进行缓存
 */
@Service
@ConditionalOnMissingBean(CacheService.class)
public class NoOpCacheServiceImpl implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(NoOpCacheServiceImpl.class);

    public NoOpCacheServiceImpl() {
        logger.info("Redis 未配置或不可用，使用无缓存模式（优雅降级）");
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        // 无缓存模式，直接返回 null
        return null;
    }

    @Override
    public <T> void set(String key, T value, long timeoutSeconds) {
        // 无缓存模式，不做任何操作
    }

    @Override
    public void delete(String key) {
        // 无缓存模式，不做任何操作
    }

    @Override
    public boolean exists(String key) {
        // 无缓存模式，始终返回 false
        return false;
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, DataLoader<T> dataLoader) {
        // 无缓存模式，直接加载数据
        logger.debug("无缓存模式，直接加载数据 key={}", key);
        return dataLoader.load();
    }

    @Override
    public boolean isAvailable() {
        // 无缓存模式，返回不可用
        return false;
    }
}
