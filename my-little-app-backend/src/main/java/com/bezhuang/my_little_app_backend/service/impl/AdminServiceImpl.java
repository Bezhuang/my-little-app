package com.bezhuang.my_little_app_backend.service.impl;

import com.bezhuang.my_little_app_backend.entity.Admin;
import com.bezhuang.my_little_app_backend.mapper.AdminMapper;
import com.bezhuang.my_little_app_backend.service.AdminService;
import com.bezhuang.my_little_app_backend.util.MD5Util;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员 Service 实现类
 */
@Service
public class AdminServiceImpl implements AdminService {

    /**
     * 管理员最大数量限制（ID 1-4）
     */
    private static final int MAX_ADMIN_COUNT = 4;

    private final AdminMapper adminMapper;

    public AdminServiceImpl(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    @Override
    public Admin getById(Long id) {
        return adminMapper.selectById(id);
    }

    @Override
    public Admin getByUsername(String username) {
        return adminMapper.selectByUsername(username);
    }

    @Override
    public Admin getByEmail(String email) {
        return adminMapper.selectByEmail(email);
    }

    @Override
    public List<Admin> getAll() {
        List<Admin> admins = adminMapper.selectAll();
        // 清除密码字段
        for (Admin admin : admins) {
            admin.setPassword(null);
        }
        return admins;
    }

    @Override
    @Transactional
    public Admin createAdmin(Admin admin) {
        // 检查管理员数量是否已达上限
        if (adminMapper.count() >= MAX_ADMIN_COUNT) {
            throw new RuntimeException("管理员数量已达上限（最多" + MAX_ADMIN_COUNT + "个）");
        }

        // 检查用户名是否已存在
        if (adminMapper.existsByUsername(admin.getUsername(), null) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (admin.getEmail() != null && adminMapper.existsByEmail(admin.getEmail(), null) > 0) {
            throw new RuntimeException("邮箱已存在");
        }
        
        // 对密码进行MD5加密
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            String encryptedPassword = MD5Util.md5(admin.getPassword());
            admin.setPassword(encryptedPassword);
        } else {
            // 如果没有设置密码，使用默认密码
            admin.setPassword(MD5Util.getDefaultPasswordMD5());
        }
        
        // 设置默认状态
        if (admin.getStatus() == null) {
            admin.setStatus(1);
        }
        
        // 设置默认角色
        if (admin.getRole() == null || admin.getRole().isEmpty()) {
            admin.setRole("admin");
        }
        
        // 设置时间
        LocalDateTime now = LocalDateTime.now();
        admin.setCreateTime(now);
        admin.setUpdateTime(now);
        
        adminMapper.insert(admin);
        
        // 返回时清除密码
        admin.setPassword(null);
        return admin;
    }

    @Override
    @Transactional
    public boolean updateAdmin(Admin admin) {
        if (admin.getId() == null) {
            throw new RuntimeException("管理员ID不能为空");
        }
        
        // 检查管理员是否存在
        Admin existingAdmin = adminMapper.selectById(admin.getId());
        if (existingAdmin == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        // 检查用户名是否重复
        if (admin.getUsername() != null && adminMapper.existsByUsername(admin.getUsername(), admin.getId()) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查邮箱是否重复
        if (admin.getEmail() != null && adminMapper.existsByEmail(admin.getEmail(), admin.getId()) > 0) {
            throw new RuntimeException("邮箱已存在");
        }
        
        // 清除密码字段，使用专门的方法修改密码
        admin.setPassword(null);
        
        return adminMapper.updateWithoutPassword(admin) > 0;
    }

    @Override
    @Transactional
    public boolean changePassword(Long adminId, String oldPassword, String newPassword) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        // 验证旧密码
        if (!verifyPassword(oldPassword, admin.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        
        // 加密新密码
        String encryptedNewPassword = MD5Util.md5(newPassword);
        
        return adminMapper.updatePassword(adminId, encryptedNewPassword) > 0;
    }

    @Override
    @Transactional
    public boolean deleteAdmin(Long id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        // 不能删除超级管理员
        if ("super_admin".equals(admin.getRole())) {
            throw new RuntimeException("不能删除超级管理员");
        }
        
        return adminMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        // 不能禁用超级管理员
        if ("super_admin".equals(admin.getRole()) && status == 0) {
            throw new RuntimeException("不能禁用超级管理员");
        }
        
        return adminMapper.updateStatus(id, status) > 0;
    }

    @Override
    public boolean verifyPassword(String plainPassword, String encodedPassword) {
        return MD5Util.verifyPassword(plainPassword, encodedPassword);
    }

    @Override
    @Transactional
    public boolean resetPassword(Long id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }

        String defaultPasswordMD5 = MD5Util.getDefaultPasswordMD5();
        return adminMapper.updatePassword(id, defaultPasswordMD5) > 0;
    }
}







