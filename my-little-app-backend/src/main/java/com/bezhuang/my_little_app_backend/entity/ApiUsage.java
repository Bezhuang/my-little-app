package com.bezhuang.my_little_app_backend.entity;

/**
 * 用户API配额实体
 */
public class ApiUsage {
    private Long id;
    private Long userId;
    private Long tokensRemaining;
    private Integer searchRemaining;
    private String createdAt;
    private String updatedAt;

    // 普通用户默认配额（1万 tokens 可进行约 10-15 次对话，搜索 3 次）
    public static final long DEFAULT_TOKENS = 10000L;
    public static final int DEFAULT_SEARCH_COUNT = 3;

    // 管理员默认配额（100万 tokens，搜索 100 次）
    public static final long ADMIN_DEFAULT_TOKENS = 1000000L;
    public static final int ADMIN_DEFAULT_SEARCH_COUNT = 100;

    // 管理员ID范围
    public static final long MIN_ADMIN_ID = 1L;
    public static final long MAX_ADMIN_ID = 4L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTokensRemaining() {
        return tokensRemaining;
    }

    public void setTokensRemaining(Long tokensRemaining) {
        this.tokensRemaining = tokensRemaining;
    }

    public Integer getSearchRemaining() {
        return searchRemaining;
    }

    public void setSearchRemaining(Integer searchRemaining) {
        this.searchRemaining = searchRemaining;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
