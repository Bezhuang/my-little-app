package com.bezhuang.my_little_app_backend.config.security;

import com.bezhuang.my_little_app_backend.entity.Admin;
import com.bezhuang.my_little_app_backend.entity.User;
import com.bezhuang.my_little_app_backend.mapper.AdminMapper;
import com.bezhuang.my_little_app_backend.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义 UserDetailsService 实现类
 * 支持从 t_admin 和 t_user 两个表中查询用户
 * 查询顺序：先查 Admin，如果没有再查 User
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminMapper adminMapper;
    private final UserMapper userMapper;

    public CustomUserDetailsService(AdminMapper adminMapper, UserMapper userMapper) {
        this.adminMapper = adminMapper;
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 首先从 Admin 表查询
        Admin admin = adminMapper.selectByUsername(username);
        if (admin != null) {
            return new CustomUserDetails(
                    admin.getId(),
                    admin.getUsername(),
                    admin.getPassword(),
                    admin.getEmail(),
                    admin.getPhone(),
                    admin.getAvatar(),
                    "ADMIN",
                    admin.getRole(),
                    admin.getStatus()
            );
        }

        // 如果 Admin 表没有，再从 User 表查询
        User user = userMapper.selectByUsername(username);
        if (user != null) {
            return new CustomUserDetails(
                    user.getId(),
                    user.getUsername(),
                    user.getPassword(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getAvatar(),
                    "USER",
                    "user",
                    user.getStatus()
            );
        }

        throw new UsernameNotFoundException("用户不存在: " + username);
    }

    /**
     * 根据用户类型加载用户
     *
     * @param username 用户名
     * @param userType 用户类型：admin 或 user
     * @return UserDetails
     * @throws UsernameNotFoundException 用户不存在异常
     */
    public UserDetails loadUserByUsernameAndType(String username, String userType) throws UsernameNotFoundException {
        if ("admin".equalsIgnoreCase(userType)) {
            Admin admin = adminMapper.selectByUsername(username);
            if (admin != null) {
                return new CustomUserDetails(
                        admin.getId(),
                        admin.getUsername(),
                        admin.getPassword(),
                        admin.getEmail(),
                        admin.getPhone(),
                        admin.getAvatar(),
                        "ADMIN",
                        admin.getRole(),
                        admin.getStatus()
                );
            }
            throw new UsernameNotFoundException("管理员不存在: " + username);
        } else {
            User user = userMapper.selectByUsername(username);
            if (user != null) {
                return new CustomUserDetails(
                        user.getId(),
                        user.getUsername(),
                        user.getPassword(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getAvatar(),
                        "USER",
                        "user",
                        user.getStatus()
                );
            }
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
    }
}
