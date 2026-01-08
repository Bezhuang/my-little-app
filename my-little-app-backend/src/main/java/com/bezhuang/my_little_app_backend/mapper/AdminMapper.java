package com.bezhuang.my_little_app_backend.mapper;

import com.bezhuang.my_little_app_backend.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管理员 Mapper 接口
 */
@Mapper
public interface AdminMapper {

    /**
     * 根据ID查询管理员
     *
     * @param id 管理员ID
     * @return 管理员实体
     */
    Admin selectById(@Param("id") Long id);

    /**
     * 根据用户名查询管理员
     *
     * @param username 用户名
     * @return 管理员实体
     */
    Admin selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询管理员
     *
     * @param email 邮箱
     * @return 管理员实体
     */
    Admin selectByEmail(@Param("email") String email);

    /**
     * 查询所有管理员
     *
     * @return 管理员列表
     */
    List<Admin> selectAll();

    /**
     * 插入管理员
     *
     * @param admin 管理员实体
     * @return 影响行数
     */
    int insert(Admin admin);

    /**
     * 更新管理员信息（不包含密码）
     *
     * @param admin 管理员实体
     * @return 影响行数
     */
    int updateWithoutPassword(Admin admin);

    /**
     * 更新管理员密码
     *
     * @param id       管理员ID
     * @param password 新密码（已加密）
     * @return 影响行数
     */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /**
     * 更新管理员状态
     *
     * @param id     管理员ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 删除管理员
     *
     * @param id 管理员ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 检查用户名是否存在
     *
     * @param username  用户名
     * @param excludeId 排除的ID
     * @return 存在返回1，不存在返回0
     */
    int existsByUsername(@Param("username") String username, @Param("excludeId") Long excludeId);

    /**
     * 检查邮箱是否存在
     *
     * @param email     邮箱
     * @param excludeId 排除的ID
     * @return 存在返回1，不存在返回0
     */
    int existsByEmail(@Param("email") String email, @Param("excludeId") Long excludeId);

    /**
     * 获取管理员总数
     *
     * @return 管理员数量
     */
    int count();
}







