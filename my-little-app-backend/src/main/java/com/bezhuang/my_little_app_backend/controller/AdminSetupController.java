package com.bezhuang.my_little_app_backend.controller;

import com.bezhuang.my_little_app_backend.dto.Result;
import com.bezhuang.my_little_app_backend.entity.Admin;
import com.bezhuang.my_little_app_backend.entity.SystemConfig;
import com.bezhuang.my_little_app_backend.mapper.AdminMapper;
import com.bezhuang.my_little_app_backend.mapper.SystemConfigMapper;
import com.bezhuang.my_little_app_backend.util.MD5Util;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理员初始化控制器
 * 仅在系统未初始化管理员时使用
 */
@RestController
@RequestMapping("/api/setup")
public class AdminSetupController {

    private final AdminMapper adminMapper;
    private final SystemConfigMapper systemConfigMapper;

    public AdminSetupController(AdminMapper adminMapper, SystemConfigMapper systemConfigMapper) {
        this.adminMapper = adminMapper;
        this.systemConfigMapper = systemConfigMapper;
    }

    /**
     * 检查系统是否需要初始化
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> checkSetupStatus() {
        // 双重检查：同时检查管理员数量和配置表
        long adminCount = adminMapper.count();
        SystemConfig config = systemConfigMapper.selectByConfigKey("admin_initialized");
        boolean isInitialized = config != null && "true".equals(config.getConfigValue());
        boolean needsSetup = adminCount == 0 && !isInitialized;
        return Result.success(Map.of(
            "needsSetup", needsSetup,
            "adminCount", adminCount,
            "configInitialized", isInitialized
        ));
    }

    /**
     * 初始化管理员
     */
    @PostMapping("/admin")
    public Result<Admin> setupAdmin(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");
        String appName = request.get("appName");

        // 参数校验
        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        if (password == null || password.length() < 6) {
            return Result.error("密码长度至少6位");
        }
        if (appName == null || appName.trim().isEmpty()) {
            return Result.error("应用名称不能为空");
        }

        // 检查是否已有管理员
        if (adminMapper.count() > 0) {
            return Result.error("系统已存在管理员，无法初始化");
        }

        // 检查用户名是否已存在
        if (adminMapper.selectByUsername(username.trim()) != null) {
            return Result.error("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (adminMapper.selectByEmail(email.trim()) != null) {
            return Result.error("邮箱已被使用");
        }

        // 创建管理员
        Admin admin = new Admin();
        admin.setUsername(username.trim());
        admin.setEmail(email.trim());
        admin.setPassword(MD5Util.md5(password));
        admin.setStatus(1);
        admin.setRole("super_admin");
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());

        adminMapper.insert(admin);

        // 保存应用名称（先检查是否存在，不存在则插入）
        SystemConfig existingAppName = systemConfigMapper.selectByConfigKey("app_name");
        if (existingAppName != null) {
            systemConfigMapper.updateConfigValue("app_name", appName.trim());
        } else {
            SystemConfig newAppName = new SystemConfig();
            newAppName.setConfigKey("app_name");
            newAppName.setConfigValue(appName.trim());
            newAppName.setDescription("应用名称");
            systemConfigMapper.insert(newAppName);
        }

        // 标记系统已初始化（先检查是否存在）
        SystemConfig existingInitialized = systemConfigMapper.selectByConfigKey("admin_initialized");
        if (existingInitialized != null) {
            systemConfigMapper.updateConfigValue("admin_initialized", "true");
        } else {
            SystemConfig newInitialized = new SystemConfig();
            newInitialized.setConfigKey("admin_initialized");
            newInitialized.setConfigValue("true");
            newInitialized.setDescription("系统是否已初始化");
            systemConfigMapper.insert(newInitialized);
        }

        // 返回管理员信息（不包含密码）
        admin.setPassword(null);
        return Result.success(admin);
    }

    /**
     * 获取应用名称
     */
    @GetMapping("/app-name")
    public Result<String> getAppName() {
        SystemConfig config = systemConfigMapper.selectByConfigKey("app_name");
        String appName = config != null ? config.getConfigValue() : "My Little App";
        return Result.success(appName);
    }
}
