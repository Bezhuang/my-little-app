package com.bezhuang.my_little_app_backend.mapper;

import com.bezhuang.my_little_app_backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface UserMapper {

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户实体
     */
    User selectById(@Param("id") Long id);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户实体
     */
    User selectByEmail(@Param("email") String email);

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    List<User> selectAll();

    /**
     * 分页查询用户
     *
     * @param offset 偏移量
     * @param limit  每页数量
     * @return 用户列表
     */
    List<User> selectByPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 根据条件查询用户
     *
     * @param keyword 关键词（用户名/邮箱/手机号）
     * @param status  状态
     * @param offset  偏移量
     * @param limit   每页数量
     * @return 用户列表
     */
    List<User> selectByCondition(@Param("keyword") String keyword,
                                  @Param("status") Integer status,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    /**
     * 统计用户总数
     *
     * @return 用户总数
     */
    long countAll();

    /**
     * 根据条件统计用户数量
     *
     * @param keyword 关键词
     * @param status  状态
     * @return 用户数量
     */
    long countByCondition(@Param("keyword") String keyword, @Param("status") Integer status);

    /**
     * 插入用户
     *
     * @param user 用户实体
     * @return 影响行数
     */
    int insert(User user);

    /**
     * 更新用户信息（不包含密码）
     *
     * @param user 用户实体
     * @return 影响行数
     */
    int updateWithoutPassword(User user);

    /**
     * 更新用户密码
     *
     * @param id       用户ID
     * @param password 新密码（已加密）
     * @return 影响行数
     */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /**
     * 更新用户状态
     *
     * @param id     用户ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 批量删除用户
     *
     * @param ids 用户ID列表
     * @return 影响行数
     */
    int deleteByIds(@Param("ids") List<Long> ids);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @param excludeId 排除的用户ID（用于更新时检查）
     * @return 存在返回1，不存在返回0
     */
    int existsByUsername(@Param("username") String username, @Param("excludeId") Long excludeId);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @param excludeId 排除的用户ID
     * @return 存在返回1，不存在返回0
     */
    int existsByEmail(@Param("email") String email, @Param("excludeId") Long excludeId);

    /**
     * 获取最新注册的用户
     *
     * @param limit 数量限制
     * @return 最新用户列表
     */
    List<User> selectLatest(@Param("limit") int limit);
}







