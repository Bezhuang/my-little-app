package com.bezhuang.my_little_app_backend.controller;

import com.bezhuang.my_little_app_backend.dto.Result;
import com.bezhuang.my_little_app_backend.entity.Admin;
import com.bezhuang.my_little_app_backend.entity.User;
import com.bezhuang.my_little_app_backend.mapper.AdminMapper;
import com.bezhuang.my_little_app_backend.service.EmailService;
import com.bezhuang.my_little_app_backend.service.UserService;
import com.bezhuang.my_little_app_backend.service.impl.EmailServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公开接口控制器
 * 无需登录即可访问
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private static final Logger logger = LoggerFactory.getLogger(PublicController.class);

    private final UserService userService;
    private final EmailService emailService;
    private final EmailServiceImpl emailServiceImpl;
    private final AdminMapper adminMapper;

    public PublicController(UserService userService, EmailService emailService,
                           EmailServiceImpl emailServiceImpl, AdminMapper adminMapper) {
        this.userService = userService;
        this.emailService = emailService;
        this.emailServiceImpl = emailServiceImpl;
        this.adminMapper = adminMapper;
    }

    /**
     * 用户注册
     * 只能注册为普通用户
     */
    @PostMapping("/register")
    public Result<User> register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String email = params.get("email");
        String phone = params.get("phone");
        String password = params.get("password");

        // 参数验证
        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (username.length() < 4 || username.length() > 20) {
            return Result.error("用户名长度为4-20位");
        }
        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return Result.error("邮箱格式不正确");
        }
        if (password == null || password.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }

        try {
            User user = new User();
            user.setUsername(username.trim());
            user.setEmail(email.trim());
            user.setPhone(phone);
            user.setPassword(password);
            user.setStatus(1);

            User createdUser = userService.createUser(user);
            return Result.success("注册成功", createdUser);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 发送注册验证码
     */
    @PostMapping("/send-register-code")
    public Result<Void> sendRegisterCode(@RequestBody Map<String, String> params) {
        String email = params.get("email");

        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return Result.error("邮箱格式不正确");
        }

        try {
            emailService.sendRegisterCode(email.trim());
            return Result.success("验证码已发送", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 验证注册验证码
     */
    @PostMapping("/verify-register-code")
    public Result<Void> verifyRegisterCode(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        String code = params.get("code");

        if (email == null || code == null) {
            return Result.error("参数不完整");
        }

        boolean valid = emailService.verifyRegisterCode(email.trim(), code.trim());
        if (valid) {
            return Result.success("验证成功", null);
        } else {
            return Result.error("验证码错误或已过期");
        }
    }

    /**
     * 发送用户密码重置验证码
     */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@RequestBody Map<String, String> params) {
        String email = params.get("email");

        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }

        // 检查邮箱是否存在
        User user = userService.getByEmail(email.trim());
        if (user == null) {
            return Result.error("该邮箱未注册");
        }

        try {
            emailService.sendResetCode(email.trim());
            return Result.success("验证码已发送", null);
        } catch (Exception e) {
            return Result.error("验证码发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送管理员密码重置验证码
     */
    @PostMapping("/admin/forgot-password")
    public Result<Void> adminForgotPassword(@RequestBody Map<String, String> params) {
        String email = params.get("email");

        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }

        // 检查邮箱是否存在
        Admin admin = adminMapper.selectByEmail(email.trim());
        if (admin == null) {
            return Result.error("该邮箱未注册管理员");
        }

        try {
            emailServiceImpl.sendAdminResetCode(email.trim());
            return Result.success("验证码已发送", null);
        } catch (Exception e) {
            return Result.error("验证码发送失败: " + e.getMessage());
        }
    }

    /**
     * 验证用户重置码
     */
    @PostMapping("/verify-reset-code")
    public Result<Void> verifyResetCode(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        String code = params.get("code");

        if (email == null || code == null) {
            return Result.error("参数不完整");
        }

        boolean valid = emailService.verifyResetCode(email.trim(), code.trim());
        if (valid) {
            return Result.success("验证成功", null);
        } else {
            return Result.error("验证码错误或已过期");
        }
    }

    /**
     * 验证管理员重置码
     */
    @PostMapping("/admin/verify-reset-code")
    public Result<Void> verifyAdminResetCode(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        String code = params.get("code");

        if (email == null || code == null) {
            return Result.error("参数不完整");
        }

        boolean valid = emailServiceImpl.verifyAdminResetCode(email.trim(), code.trim());
        if (valid) {
            return Result.success("验证成功", null);
        } else {
            return Result.error("验证码错误或已过期");
        }
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        String code = params.get("code");
        String newPassword = params.get("newPassword");

        if (email == null || code == null || newPassword == null) {
            return Result.error("参数不完整");
        }

        if (newPassword.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }

        try {
            boolean success = emailService.resetPasswordWithCode(email.trim(), code.trim(), newPassword);

            if (success) {
                return Result.success("密码重置成功", null);
            } else {
                return Result.error("验证码错误或已过期");
            }
        } catch (Exception e) {
            return Result.error("密码重置失败: " + e.getMessage());
        }
    }

    /**
     * 重置管理员密码
     */
    @PostMapping("/admin/reset-password")
    public Result<Void> resetAdminPassword(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        String code = params.get("code");
        String newPassword = params.get("newPassword");

        if (email == null || code == null || newPassword == null) {
            return Result.error("参数不完整");
        }

        if (newPassword.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }

        try {
            boolean success = emailServiceImpl.resetAdminPasswordWithCode(email.trim(), code.trim(), newPassword);

            if (success) {
                return Result.success("密码重置成功", null);
            } else {
                return Result.error("验证码错误或已过期");
            }
        } catch (Exception e) {
            return Result.error("密码重置失败: " + e.getMessage());
        }
    }
}
