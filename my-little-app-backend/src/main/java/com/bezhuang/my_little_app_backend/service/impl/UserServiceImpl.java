package com.bezhuang.my_little_app_backend.service.impl;

import com.bezhuang.my_little_app_backend.dto.PageResult;
import com.bezhuang.my_little_app_backend.entity.User;
import com.bezhuang.my_little_app_backend.mapper.UserMapper;
import com.bezhuang.my_little_app_backend.service.UserService;
import com.bezhuang.my_little_app_backend.util.MD5Util;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 Service 实现类
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public User getByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    @Override
    public PageResult<User> getPage(Integer page, Integer pageSize, String keyword, Integer status) {
        int offset = (page - 1) * pageSize;
        List<User> list = userMapper.selectByCondition(keyword, status, offset, pageSize);
        long total = userMapper.countByCondition(keyword, status);
        
        // 清除密码字段
        for (User user : list) {
            user.setPassword(null);
        }
        
        return PageResult.of(page, pageSize, total, list);
    }

    @Override
    @Transactional
    public User createUser(User user) {
        // 检查用户名是否已存在
        if (existsByUsername(user.getUsername(), null)) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (user.getEmail() != null && existsByEmail(user.getEmail(), null)) {
            throw new RuntimeException("邮箱已存在");
        }
        
        // 对密码进行MD5加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String encryptedPassword = MD5Util.md5(user.getPassword());
            user.setPassword(encryptedPassword);
        } else {
            // 如果没有设置密码，使用默认密码
            user.setPassword(MD5Util.getDefaultPasswordMD5());
        }
        
        // 设置默认状态
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        
        // 设置时间
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        
        userMapper.insert(user);
        
        // 返回时清除密码
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional
    public boolean updateUserByAdmin(User user) {
        // Admin更新用户信息，必须忽略密码字段
        // 这里通过调用 updateWithoutPassword 来确保不会更新密码
        
        if (user.getId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        
        // 检查用户是否存在
        User existingUser = userMapper.selectById(user.getId());
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户名是否重复
        if (user.getUsername() != null && existsByUsername(user.getUsername(), user.getId())) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查邮箱是否重复
        if (user.getEmail() != null && existsByEmail(user.getEmail(), user.getId())) {
            throw new RuntimeException("邮箱已存在");
        }
        
        // 强制清除密码字段，确保Admin不能修改用户密码
        user.setPassword(null);
        
        return userMapper.updateWithoutPassword(user) > 0;
    }

    @Override
    @Transactional
    public boolean updateProfile(Long userId, User user) {
        // 用户只能更新自己的信息
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 设置用户ID，确保只更新自己的信息
        user.setId(userId);
        
        // 用户不能通过此接口修改密码
        user.setPassword(null);
        
        // 用户不能修改状态
        user.setStatus(null);
        
        // 检查用户名是否重复
        if (user.getUsername() != null && existsByUsername(user.getUsername(), userId)) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查邮箱是否重复
        if (user.getEmail() != null && existsByEmail(user.getEmail(), userId)) {
            throw new RuntimeException("邮箱已存在");
        }
        
        return userMapper.updateWithoutPassword(user) > 0;
    }

    @Override
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证旧密码
        if (!verifyPassword(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        
        // 加密新密码
        String encryptedNewPassword = MD5Util.md5(newPassword);
        
        return userMapper.updatePassword(userId, encryptedNewPassword) > 0;
    }

    @Override
    @Transactional
    public boolean resetPassword(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 重置为默认密码 (123456 的 MD5)
        String defaultPassword = MD5Util.getDefaultPasswordMD5();
        
        return userMapper.updatePassword(userId, defaultPassword) > 0;
    }

    @Override
    @Transactional
    public boolean deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return userMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return userMapper.updateStatus(id, status) > 0;
    }

    @Override
    public boolean existsByUsername(String username, Long excludeId) {
        return userMapper.existsByUsername(username, excludeId) > 0;
    }

    @Override
    public boolean existsByEmail(String email, Long excludeId) {
        return userMapper.existsByEmail(email, excludeId) > 0;
    }

    @Override
    public boolean verifyPassword(String plainPassword, String encodedPassword) {
        return MD5Util.verifyPassword(plainPassword, encodedPassword);
    }
}







