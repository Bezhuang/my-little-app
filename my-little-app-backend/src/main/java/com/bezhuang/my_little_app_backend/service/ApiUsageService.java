package com.bezhuang.my_little_app_backend.service;

import com.bezhuang.my_little_app_backend.entity.ApiUsage;
import com.bezhuang.my_little_app_backend.mapper.ApiUsageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API使用配额服务
 */
@Service
public class ApiUsageService {

    private static final Logger logger = LoggerFactory.getLogger(ApiUsageService.class);

    private final ApiUsageMapper apiUsageMapper;

    public ApiUsageService(ApiUsageMapper apiUsageMapper) {
        this.apiUsageMapper = apiUsageMapper;
    }

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin(Long userId) {
        return userId >= ApiUsage.MIN_ADMIN_ID && userId <= ApiUsage.MAX_ADMIN_ID;
    }

    /**
     * 获取用户配额记录，不存在则创建
     */
    @Transactional
    public ApiUsage getOrCreate(Long userId) {
        ApiUsage usage = apiUsageMapper.selectByUserId(userId);
        if (usage == null) {
            usage = new ApiUsage();
            usage.setUserId(userId);
            // 管理员有更高配额
            if (isAdmin(userId)) {
                usage.setTokensRemaining(ApiUsage.ADMIN_DEFAULT_TOKENS);
                usage.setSearchRemaining(ApiUsage.ADMIN_DEFAULT_SEARCH_COUNT);
                logger.info("创建管理员 {} 配额记录: {} tokens, {} 次搜索",
                        userId, ApiUsage.ADMIN_DEFAULT_TOKENS, ApiUsage.ADMIN_DEFAULT_SEARCH_COUNT);
            } else {
                usage.setTokensRemaining(ApiUsage.DEFAULT_TOKENS);
                usage.setSearchRemaining(ApiUsage.DEFAULT_SEARCH_COUNT);
                logger.info("创建用户 {} 配额记录: {} tokens, {} 次搜索",
                        userId, ApiUsage.DEFAULT_TOKENS, ApiUsage.DEFAULT_SEARCH_COUNT);
            }
            apiUsageMapper.insert(usage);
        }
        return usage;
    }

    /**
     * 获取当月配额记录（兼容旧方法名）
     */
    @Transactional
    public ApiUsage getOrCreateCurrentMonth(Long userId) {
        return getOrCreate(userId);
    }

    /**
     * 检查是否可以使用Token
     */
    public boolean canUseTokens(Long userId, long estimatedTokens) {
        ApiUsage usage = getOrCreate(userId);
        return usage.getTokensRemaining() != null && usage.getTokensRemaining() >= estimatedTokens;
    }

    /**
     * 检查是否可以使用搜索
     */
    public boolean canSearch(Long userId) {
        ApiUsage usage = getOrCreate(userId);
        return usage.getSearchRemaining() != null && usage.getSearchRemaining() > 0;
    }

    /**
     * 消耗Token配额
     * @return 是否成功消耗
     */
    @Transactional
    public boolean consumeTokens(Long userId, long inputTokens, long outputTokens) {
        ApiUsage usage = getOrCreate(userId);
        Long remaining = usage.getTokensRemaining();
        if (remaining == null || remaining < inputTokens + outputTokens) {
            logger.warn("用户 {} Token不足，需要 {}，剩余 {}", userId, inputTokens + outputTokens, remaining);
            return false;
        }

        long newRemaining = remaining - inputTokens - outputTokens;
        apiUsageMapper.updateTokensRemaining(usage.getId(), newRemaining);

        logger.info("用户 {} 消耗 {} tokens，剩余 {}", userId, inputTokens + outputTokens, newRemaining);
        return true;
    }

    /**
     * 消耗搜索次数
     * @return 是否成功消耗
     */
    @Transactional
    public boolean consumeSearch(Long userId) {
        ApiUsage usage = getOrCreate(userId);
        Integer remaining = usage.getSearchRemaining();
        if (remaining == null || remaining <= 0) {
            logger.warn("用户 {} 搜索次数已用尽", userId);
            return false;
        }

        int newRemaining = remaining - 1;
        apiUsageMapper.updateSearchRemaining(usage.getId(), newRemaining);

        logger.info("用户 {} 消耗1次搜索，剩余 {}", userId, newRemaining);
        return true;
    }

    /**
     * 获取用户当前配额信息
     */
    public ApiUsage getUserQuota(Long userId) {
        return getOrCreate(userId);
    }

    /**
     * 检查用户是否还有可用配额
     */
    public boolean hasQuota(Long userId) {
        ApiUsage usage = getOrCreate(userId);
        boolean hasTokens = usage.getTokensRemaining() != null && usage.getTokensRemaining() > 0;
        boolean hasSearch = usage.getSearchRemaining() != null && usage.getSearchRemaining() > 0;
        return hasTokens || hasSearch;
    }

    /**
     * 获取配额不足的提示信息
     * @param userId 用户ID
     * @param checkSearch 是否检查搜索次数（仅在启用联网搜索时检查）
     */
    public String getQuotaWarning(Long userId, boolean checkSearch) {
        ApiUsage usage = getOrCreate(userId);
        StringBuilder sb = new StringBuilder();
        boolean hasQuota = false;

        if (usage.getTokensRemaining() != null && usage.getTokensRemaining() <= 0) {
            sb.append("Token已用尽");
            hasQuota = true;
        }
        // 仅在启用联网搜索时检查搜索次数
        if (checkSearch && usage.getSearchRemaining() != null && usage.getSearchRemaining() <= 0) {
            if (sb.length() > 0) sb.append("，");
            sb.append("搜索次数已用尽");
            hasQuota = true;
        }

        if (hasQuota) {
            sb.append("，请联系管理员充值。");
            return sb.toString();
        }
        return null;
    }

    /**
     * 获取配额不足的提示信息（默认不检查搜索次数，用于兼容旧代码）
     */
    public String getQuotaWarning(Long userId) {
        return getQuotaWarning(userId, false);
    }
}
