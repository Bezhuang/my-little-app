package com.bezhuang.my_little_app_backend.controller;

import com.bezhuang.my_little_app_backend.entity.AiConfig;
import com.bezhuang.my_little_app_backend.entity.ApiUsage;
import com.bezhuang.my_little_app_backend.entity.Admin;
import com.bezhuang.my_little_app_backend.entity.User;
import com.bezhuang.my_little_app_backend.mapper.AiConfigMapper;
import com.bezhuang.my_little_app_backend.mapper.ApiUsageMapper;
import com.bezhuang.my_little_app_backend.mapper.AdminMapper;
import com.bezhuang.my_little_app_backend.mapper.UserMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import com.bezhuang.my_little_app_backend.service.ApiUsageService;

/**
 * AI 管理控制器（Admin）
 * 管理系统配置，如 System Prompt
 */
@RestController
@RequestMapping("/api/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
public class AiAdminController {

    private final AiConfigMapper aiConfigMapper;
    private final ApiUsageService apiUsageService;
    private final ApiUsageMapper apiUsageMapper;
    private final AdminMapper adminMapper;
    private final UserMapper userMapper;

    public AiAdminController(AiConfigMapper aiConfigMapper, ApiUsageService apiUsageService,
                            ApiUsageMapper apiUsageMapper, AdminMapper adminMapper, UserMapper userMapper) {
        this.aiConfigMapper = aiConfigMapper;
        this.apiUsageService = apiUsageService;
        this.apiUsageMapper = apiUsageMapper;
        this.adminMapper = adminMapper;
        this.userMapper = userMapper;
    }

    /**
     * 获取 System Prompt
     */
    @GetMapping("/config/prompt")
    public ResponseEntity<Map<String, Object>> getSystemPrompt() {
        AiConfig config = aiConfigMapper.selectByConfigKey("system_prompt");
        if (config != null) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "id", config.getId(),
                            "configKey", config.getConfigKey(),
                            "configValue", config.getConfigValue(),
                            "description", config.getDescription()
                    )
            ));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "configValue", "你是 Bezhuang AI，一个智能助手。请用简洁清晰的语言回答用户的问题，保持对话的连贯性。"
                )
        ));
    }

    /**
     * 更新 System Prompt
     */
    @PutMapping("/config/prompt")
    public ResponseEntity<Map<String, Object>> updateSystemPrompt(@RequestBody Map<String, String> request) {
        String prompt = request.get("configValue");
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "系统提示词不能为空"));
        }

        try {
            // 先检查是否存在
            AiConfig existing = aiConfigMapper.selectByConfigKey("system_prompt");
            if (existing != null) {
                aiConfigMapper.updateConfigValue("system_prompt", prompt);
            } else {
                // 不存在则插入
                AiConfig newConfig = new AiConfig();
                newConfig.setConfigKey("system_prompt");
                newConfig.setConfigValue(prompt);
                newConfig.setDescription("AI助手系统提示词");
                aiConfigMapper.insert(newConfig);
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "更新成功"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "更新失败: " + e.getMessage()));
        }
    }

    /**
     * 获取模型温度配置
     */
    @GetMapping("/config/temperature")
    public ResponseEntity<Map<String, Object>> getTemperature() {
        AiConfig config = aiConfigMapper.selectByConfigKey("temperature");
        if (config != null) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "configKey", config.getConfigKey(),
                            "configValue", config.getConfigValue(),
                            "description", config.getDescription()
                    )
            ));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "configKey", "temperature",
                        "configValue", "0.7",
                        "description", "AI模型温度参数 (0.0-2.0)"
                )
        ));
    }

    /**
     * 更新模型温度配置
     */
    @PutMapping("/config/temperature")
    public ResponseEntity<Map<String, Object>> updateTemperature(@RequestBody Map<String, String> request) {
        String tempStr = request.get("configValue");
        if (tempStr == null || tempStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "温度值不能为空"));
        }

        try {
            double tempValue = Double.parseDouble(tempStr);
            if (tempValue < 0 || tempValue > 2) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "温度值必须在 0.0 到 2.0 之间"));
            }

            // 先检查是否存在
            AiConfig existing = aiConfigMapper.selectByConfigKey("temperature");
            if (existing != null) {
                aiConfigMapper.updateConfigValue("temperature", tempStr);
            } else {
                // 不存在则插入
                AiConfig newConfig = new AiConfig();
                newConfig.setConfigKey("temperature");
                newConfig.setConfigValue(tempStr);
                newConfig.setDescription("AI模型温度参数 (0.0-2.0)");
                aiConfigMapper.insert(newConfig);
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "更新成功"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "温度值必须是有效的数字"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "更新失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有用户和管理员的配额列表
     */
    @GetMapping("/quota/list")
    public ResponseEntity<Map<String, Object>> getQuotaList() {
        try {
            List<Map<String, Object>> quotaList = new ArrayList<>();

            // 1. 查询管理员配额 (ID 1-4)
            List<Admin> admins = adminMapper.selectAll();
            for (Admin admin : admins) {
                ApiUsage usage = apiUsageService.getOrCreate(admin.getId());
                Map<String, Object> item = new HashMap<>();
                item.put("userId", admin.getId());
                item.put("username", admin.getUsername());
                item.put("role", admin.getRole());
                item.put("tokensRemaining", usage.getTokensRemaining());
                item.put("searchRemaining", usage.getSearchRemaining());
                quotaList.add(item);
            }

            // 2. 查询普通用户配额 (ID >= 5)
            List<User> users = userMapper.selectAll();
            for (User user : users) {
                // 跳过管理员ID范围的用户（防止重复）
                if (user.getId() >= ApiUsage.MIN_ADMIN_ID && user.getId() <= ApiUsage.MAX_ADMIN_ID) {
                    continue;
                }
                // 只显示正常状态的用户
                if (user.getStatus() != null && user.getStatus() == 0) {
                    continue;
                }
                ApiUsage usage = apiUsageService.getOrCreate(user.getId());
                Map<String, Object> item = new HashMap<>();
                item.put("userId", user.getId());
                item.put("username", user.getUsername());
                item.put("role", "user");
                item.put("tokensRemaining", usage.getTokensRemaining());
                item.put("searchRemaining", usage.getSearchRemaining());
                quotaList.add(item);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", quotaList
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "获取配额列表失败: " + e.getMessage()));
        }
    }

    /**
     * 更新指定管理员的配额
     */
    @PutMapping("/quota/{userId}")
    public ResponseEntity<Map<String, Object>> updateQuota(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request) {
        try {
            Long tokensRemaining = request.get("tokensRemaining") != null ?
                    Long.parseLong(request.get("tokensRemaining").toString()) : null;
            Integer searchRemaining = request.get("searchRemaining") != null ?
                    Integer.parseInt(request.get("searchRemaining").toString()) : null;

            if (tokensRemaining == null && searchRemaining == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "请提供要更新的配额值"));
            }

            // 获取或创建配额记录
            ApiUsage usage = apiUsageService.getOrCreate(userId);

            // 更新配额
            if (tokensRemaining != null) {
                apiUsageMapper.updateTokensRemaining(usage.getId(), tokensRemaining);
            }
            if (searchRemaining != null) {
                apiUsageMapper.updateSearchRemaining(usage.getId(), searchRemaining);
            }

            // 重新获取更新后的数据
            ApiUsage updatedUsage = apiUsageService.getOrCreate(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "配额更新成功",
                    "data", Map.of(
                            "userId", userId,
                            "tokensRemaining", updatedUsage.getTokensRemaining(),
                            "searchRemaining", updatedUsage.getSearchRemaining()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "更新配额失败: " + e.getMessage()));
        }
    }
}
