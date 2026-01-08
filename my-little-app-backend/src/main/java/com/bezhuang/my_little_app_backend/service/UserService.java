package com.bezhuang.my_little_app_backend.service;

import com.bezhuang.my_little_app_backend.dto.PageResult;
import com.bezhuang.my_little_app_backend.entity.User;

/**
 * 用户 Service 接口
 */
public interface UserService {

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户实体
     */
    User getById(Long id);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    User getByUsername(String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户实体
     */
    User getByEmail(String email);

    /**
     * 分页查询用户
     *
     * @param page     页码
     * @param pageSize 每页数量
     * @param keyword  关键词
     * @param status   状态
     * @return 分页结果
     */
    PageResult<User> getPage(Integer page, Integer pageSize, String keyword, Integer status);

    /**
     * 创建用户（Admin调用）
     * 密码会在Service层进行MD5加密
     *
     * @param user 用户实体
     * @return 创建成功的用户
     */
    User createUser(User user);

    /**
     * 更新用户信息（Admin调用，不能修改密码）
     *
     * @param user 用户实体
     * @return 更新是否成功
     */
    boolean updateUserByAdmin(User user);

    /**
     * 用户自己更新个人信息（不包含密码）
     *
     * @param userId 用户ID
     * @param user   用户实体
     * @return 更新是否成功
     */
    boolean updateProfile(Long userId, User user);

    /**
     * 用户自己修改密码
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     * @return 修改是否成功
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 重置用户密码为默认密码（Admin调用）
     *
     * @param userId 用户ID
     * @return 重置是否成功
     */
    boolean resetPassword(Long userId);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 删除是否成功
     */
    boolean deleteUser(Long id);

    /**
     * 更新用户状态
     *
     * @param id     用户ID
     * @param status 状态
     * @return 更新是否成功
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 检查用户名是否存在
     *
     * @param username  用户名
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean existsByUsername(String username, Long excludeId);

    /**
     * 检查邮箱是否存在
     *
     * @param email     邮箱
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean existsByEmail(String email, Long excludeId);

    /**
     * 验证密码
     *
     * @param plainPassword   明文密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    boolean verifyPassword(String plainPassword, String encodedPassword);
}







