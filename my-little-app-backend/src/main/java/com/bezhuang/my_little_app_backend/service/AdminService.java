package com.bezhuang.my_little_app_backend.service;

import com.bezhuang.my_little_app_backend.entity.Admin;

import java.util.List;

/**
 * 管理员 Service 接口
 */
public interface AdminService {

    /**
     * 根据ID查询管理员
     *
     * @param id 管理员ID
     * @return 管理员实体
     */
    Admin getById(Long id);

    /**
     * 根据用户名查询管理员
     *
     * @param username 用户名
     * @return 管理员实体
     */
    Admin getByUsername(String username);

    /**
     * 根据邮箱查询管理员
     *
     * @param email 邮箱
     * @return 管理员实体
     */
    Admin getByEmail(String email);

    /**
     * 查询所有管理员
     *
     * @return 管理员列表
     */
    List<Admin> getAll();

    /**
     * 创建管理员
     *
     * @param admin 管理员实体
     * @return 创建成功的管理员
     */
    Admin createAdmin(Admin admin);

    /**
     * 更新管理员信息
     *
     * @param admin 管理员实体
     * @return 更新是否成功
     */
    boolean updateAdmin(Admin admin);

    /**
     * 修改密码
     *
     * @param adminId     管理员ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     * @return 修改是否成功
     */
    boolean changePassword(Long adminId, String oldPassword, String newPassword);

    /**
     * 删除管理员
     *
     * @param id 管理员ID
     * @return 删除是否成功
     */
    boolean deleteAdmin(Long id);

    /**
     * 更新管理员状态
     *
     * @param id     管理员ID
     * @param status 状态
     * @return 更新是否成功
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 验证密码
     *
     * @param plainPassword   明文密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    boolean verifyPassword(String plainPassword, String encodedPassword);

    /**
     * 重置密码为默认密码
     *
     * @param id 管理员ID
     * @return 重置是否成功
     */
    boolean resetPassword(Long id);
}







