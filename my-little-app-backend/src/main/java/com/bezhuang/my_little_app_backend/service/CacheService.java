package com.bezhuang.my_little_app_backend.service;

/**
 * 缓存服务接口
 * 支持 Redis 缓存和优雅降级
 * 当 Redis 不可用时，自动降级为无缓存模式
 */
public interface CacheService {

    /**
     * 获取缓存
     * @param key 缓存键
     * @param <T> 缓存值类型
     * @return 缓存值，不存在返回 null
     */
    <T> T get(String key, Class<T> type);

    /**
     * 设置缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param timeoutSeconds 过期时间（秒）
     * @param <T> 缓存值类型
     */
    <T> void set(String key, T value, long timeoutSeconds);

    /**
     * 删除缓存
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 检查缓存是否存在
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 获取缓存，如果不存在则从数据源加载
     * @param key 缓存键
     * @param type 缓存值类型
     * @param dataLoader 数据加载器（当缓存不存在时调用）
     * @param <T> 缓存值类型
     * @return 缓存值或加载的数据
     */
    <T> T getOrLoad(String key, Class<T> type, DataLoader<T> dataLoader);

    /**
     * 数据加载器接口
     * @param <T> 数据类型
     */
    interface DataLoader<T> {
        T load();
    }

    /**
     * 检查缓存是否可用
     * @return 缓存是否可用
     */
    boolean isAvailable();
}
