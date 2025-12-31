package com.bezhuang.my_little_app_backend.controller;

import com.bezhuang.my_little_app_backend.dto.PageResult;
import com.bezhuang.my_little_app_backend.dto.Result;
import com.bezhuang.my_little_app_backend.entity.Admin;
import com.bezhuang.my_little_app_backend.entity.User;
import com.bezhuang.my_little_app_backend.service.AdminService;
import com.bezhuang.my_little_app_backend.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 * 所有接口需要 ADMIN 角色
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final AdminService adminService;

    public AdminController(UserService userService, AdminService adminService) {
        this.userService = userService;
        this.adminService = adminService;
    }

    // ==================== 用户管理接口 ====================

    /**
     * 获取用户列表
     */
    @GetMapping("/user/list")
    public Result<PageResult<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        PageResult<User> result = userService.getPage(page, pageSize, keyword, status);
        return Result.success(result);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/user/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.notFound("用户不存在");
        }
        // 清除密码
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 新增用户
     * 密码会在 Service 层进行 MD5 加密
     */
    @PostMapping("/user/add")
    public Result<User> addUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return Result.success("用户创建成功", createdUser);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户信息
     * 注意：Admin 不能修改用户密码，password 字段会被忽略
     */
    @PutMapping("/user/update")
    public Result<Void> updateUser(@RequestBody User user) {
        try {
            // 重要：Admin 更新用户时，密码字段会被强制忽略
            boolean success = userService.updateUserByAdmin(user);
            if (success) {
                return Result.success("更新成功", null);
            }
            return Result.error("更新失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/user/delete/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        try {
            boolean success = userService.deleteUser(id);
            if (success) {
                return Result.success("删除成功", null);
            }
            return Result.error("删除失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重置用户密码为默认密码 (123456)
     * 这是 Admin 能够影响用户密码的唯一方式
     */
    @PostMapping("/user/reset-password/{id}")
    public Result<Void> resetUserPassword(@PathVariable Long id) {
        try {
            boolean success = userService.resetPassword(id);
            if (success) {
                return Result.success("密码已重置为默认密码", null);
            }
            return Result.error("密码重置失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/user/status/{id}")
    public Result<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        try {
            boolean success = userService.updateStatus(id, status);
            if (success) {
                return Result.success("状态更新成功", null);
            }
            return Result.error("状态更新失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 管理员管理接口 ====================

    /**
     * 获取管理员列表
     */
    @GetMapping("/admin/list")
    public Result<List<Admin>> getAdminList() {
        List<Admin> admins = adminService.getAll();
        return Result.success(admins);
    }

    /**
     * 获取管理员详情
     */
    @GetMapping("/admin/{id}")
    public Result<Admin> getAdminById(@PathVariable Long id) {
        Admin admin = adminService.getById(id);
        if (admin == null) {
            return Result.notFound("管理员不存在");
        }
        // 清除密码
        admin.setPassword(null);
        return Result.success(admin);
    }

    /**
     * 新增管理员（仅超级管理员可操作）
     */
    @PostMapping("/admin/add")
    public Result<Admin> addAdmin(@RequestBody Admin admin) {
        try {
            Admin createdAdmin = adminService.createAdmin(admin);
            return Result.success("管理员创建成功", createdAdmin);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新管理员信息
     * 注意：不能修改密码，password 字段会被忽略
     */
    @PutMapping("/admin/update")
    public Result<Void> updateAdmin(@RequestBody Admin admin) {
        try {
            boolean success = adminService.updateAdmin(admin);
            if (success) {
                return Result.success("更新成功", null);
            }
            return Result.error("更新失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除管理员（仅超级管理员可操作，不能删除自己）
     */
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> deleteAdmin(@PathVariable Long id) {
        try {
            boolean success = adminService.deleteAdmin(id);
            if (success) {
                return Result.success("删除成功", null);
            }
            return Result.error("删除失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重置管理员密码
     */
    @PostMapping("/admin/reset-password/{id}")
    public Result<Void> resetAdminPassword(@PathVariable Long id) {
        try {
            boolean success = adminService.resetPassword(id);
            if (success) {
                return Result.success("密码已重置为默认密码", null);
            }
            return Result.error("密码重置失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新管理员状态
     */
    @PutMapping("/admin/status/{id}")
    public Result<Void> updateAdminStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        try {
            boolean success = adminService.updateStatus(id, status);
            if (success) {
                return Result.success("状态更新成功", null);
            }
            return Result.error("状态更新失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改自己的密码
     */
    @PostMapping("/admin/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            return Result.error("参数不完整");
        }

        if (newPassword.length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }

        try {
            // 这里需要获取当前登录管理员的ID
            // 由于在 SecurityContext 中，我们可以通过另一种方式获取
            // 但为了简单起见，让前端传入管理员ID
            Long adminId = Long.valueOf(params.get("adminId"));
            boolean success = adminService.changePassword(adminId, oldPassword, newPassword);
            if (success) {
                return Result.success("密码修改成功", null);
            }
            return Result.error("密码修改失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}





