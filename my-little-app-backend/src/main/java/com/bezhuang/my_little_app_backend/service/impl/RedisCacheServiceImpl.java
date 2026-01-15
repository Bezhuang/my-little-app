package com.bezhuang.my_little_app_backend.service.impl;

import com.bezhuang.my_little_app_backend.service.CacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务实现
 * 使用 Redis 作为缓存存储
 */
@Service
@ConditionalOnBean(RedisTemplate.class)
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisCacheServiceImpl implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        // 注册 JavaTimeModule 支持 LocalDateTime 序列化
        this.objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写成时间戳
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        logger.info("Redis 缓存服务已启用");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            if (type.isInstance(value)) {
                return (T) value;
            }
            // JSON 反序列化
            String json = objectMapper.writeValueAsString(value);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            logger.warn("获取缓存失败 key={}: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public <T> void set(String key, T value, long timeoutSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("设置缓存失败 key={}: {}", key, e.getMessage());
        }
    }

    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            logger.warn("删除缓存失败 key={}: {}", key, e.getMessage());
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.warn("检查缓存是否存在失败 key={}: {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, DataLoader<T> dataLoader) {
        T cached = get(key, type);
        if (cached != null) {
            logger.debug("缓存命中 key={}", key);
            return cached;
        }
        logger.debug("缓存未命中，加载数据 key={}", key);
        T data = dataLoader.load();
        if (data != null) {
            set(key, data, 300); // 默认5分钟
        }
        return data;
    }

    @Override
    public boolean isAvailable() {
        try {
            return "PONG".equals(redisTemplate.getConnectionFactory()
                    .getConnection().ping());
        } catch (Exception e) {
            logger.warn("Redis 不可用: {}", e.getMessage());
            return false;
        }
    }
}
