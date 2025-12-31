package com.bezhuang.my_little_app_backend.mapper;

import com.bezhuang.my_little_app_backend.entity.ApiUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * API使用配额 Mapper
 */
@Mapper
public interface ApiUsageMapper {

    /**
     * 根据用户ID查询
     */
    ApiUsage selectByUserId(@Param("userId") Long userId);

    /**
     * 插入新记录
     */
    int insert(ApiUsage apiUsage);

    /**
     * 更新Token剩余数
     */
    int updateTokensRemaining(@Param("id") Long id, @Param("tokensRemaining") Long tokensRemaining);

    /**
     * 更新搜索剩余数
     */
    int updateSearchRemaining(@Param("id") Long id, @Param("searchRemaining") Integer searchRemaining);
}
