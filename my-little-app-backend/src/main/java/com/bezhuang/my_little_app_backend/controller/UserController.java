package com.bezhuang.my_little_app_backend.controller;

import com.bezhuang.my_little_app_backend.config.security.CustomUserDetails;
import com.bezhuang.my_little_app_backend.dto.Result;
import com.bezhuang.my_little_app_backend.entity.Admin;
import com.bezhuang.my_little_app_backend.entity.User;
import com.bezhuang.my_little_app_backend.service.AdminService;
import com.bezhuang.my_little_app_backend.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 * 普通用户只能操作自己的数据
 */
@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserController {

    private final UserService userService;
    private final AdminService adminService;

    public UserController(UserService userService, AdminService adminService) {
        this.userService = userService;
        this.adminService = adminService;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile() {
        CustomUserDetails currentUser = getCurrentUser();
        if (currentUser == null) {
            return Result.unauthorized("请先登录");
        }

        // 根据用户类型从不同的表查询
        if ("ADMIN".equals(currentUser.getUserType())) {
            // Admin 用户从 t_admin 表查询
            Admin admin = adminService.getById(currentUser.getId());
            if (admin == null) {
                return Result.notFound("用户不存在");
            }
            admin.setPassword(null);
            Map<String, Object> data = buildAdminData(admin);
            data.put("userType", "ADMIN");
            return Result.success(data);
        } else {
            // 普通用户从 t_user 表查询
            User user = userService.getById(currentUser.getId());
            if (user == null) {
                return Result.notFound("用户不存在");
            }
            user.setPassword(null);
            Map<String, Object> data = buildUserData(user);
            data.put("userType", "USER");
            return Result.success(data);
        }
    }

    /**
     * 更新个人信息
     * 用户只能修改自己的信息，且不能修改密码和状态
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody Map<String, Object> params) {
        CustomUserDetails currentUser = getCurrentUser();
        if (currentUser == null) {
            return Result.unauthorized("请先登录");
        }

        try {
            if ("ADMIN".equals(currentUser.getUserType())) {
                // Admin 用户更新自己的信息
                Admin admin = new Admin();
                admin.setId(currentUser.getId());
                admin.setEmail((String) params.get("email"));
                admin.setPhone((String) params.get("phone"));
                admin.setAvatar((String) params.get("avatar"));
                
                boolean success = adminService.updateAdmin(admin);
                if (success) {
                    return Result.success("更新成功", null);
                }
                return Result.error("更新失败");
            } else {
                // 普通用户更新
                User user = new User();
                user.setId(currentUser.getId());
                user.setEmail((String) params.get("email"));
                user.setPhone((String) params.get("phone"));
                user.setAvatar((String) params.get("avatar"));
                
                boolean success = userService.updateProfile(currentUser.getId(), user);
                if (success) {
                    return Result.success("更新成功", null);
                }
                return Result.error("更新失败");
            }
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改密码
     * 用户只能修改自己的密码，需要验证原密码
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        
        CustomUserDetails currentUser = getCurrentUser();
        if (currentUser == null) {
            return Result.unauthorized("请先登录");
        }

        // 验证新密码长度
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }

        try {
            boolean success;
            if ("ADMIN".equals(currentUser.getUserType())) {
                success = adminService.changePassword(currentUser.getId(), oldPassword, newPassword);
            } else {
                success = userService.changePassword(currentUser.getId(), oldPassword, newPassword);
            }
            
            if (success) {
                return Result.success("密码修改成功", null);
            }
            return Result.error("密码修改失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前登录用户
     */
    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    /**
     * 构建用户数据Map
     */
    private Map<String, Object> buildUserData(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("avatar", user.getAvatar());
        data.put("status", user.getStatus());
        data.put("createTime", user.getCreateTime());
        data.put("updateTime", user.getUpdateTime());
        return data;
    }

    /**
     * 构建管理员数据Map
     */
    private Map<String, Object> buildAdminData(Admin admin) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", admin.getId());
        data.put("username", admin.getUsername());
        data.put("email", admin.getEmail());
        data.put("phone", admin.getPhone());
        data.put("avatar", admin.getAvatar());
        data.put("status", admin.getStatus());
        data.put("role", admin.getRole());
        data.put("createTime", admin.getCreateTime());
        data.put("updateTime", admin.getUpdateTime());
        return data;
    }
}
