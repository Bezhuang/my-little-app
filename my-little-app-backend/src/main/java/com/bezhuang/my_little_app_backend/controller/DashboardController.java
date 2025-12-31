package com.bezhuang.my_little_app_backend.controller;

import com.bezhuang.my_little_app_backend.dto.Result;
import com.bezhuang.my_little_app_backend.entity.Thought;
import com.bezhuang.my_little_app_backend.entity.User;
import com.bezhuang.my_little_app_backend.mapper.ImageMapper;
import com.bezhuang.my_little_app_backend.mapper.ThoughtMapper;
import com.bezhuang.my_little_app_backend.mapper.UserMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制台统计控制器
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final UserMapper userMapper;
    private final ThoughtMapper thoughtMapper;
    private final ImageMapper imageMapper;

    public DashboardController(UserMapper userMapper, ThoughtMapper thoughtMapper, ImageMapper imageMapper) {
        this.userMapper = userMapper;
        this.thoughtMapper = thoughtMapper;
        this.imageMapper = imageMapper;
    }

    /**
     * 获取控制台统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 用户总数
        long userCount = userMapper.countAll();
        stats.put("userCount", userCount);

        // 想法总数
        int thoughtCount = thoughtMapper.count();
        stats.put("thoughtCount", thoughtCount);

        // 图片总数
        int imageCount = imageMapper.count();
        stats.put("imageCount", imageCount);

        return Result.success(stats);
    }

    /**
     * 获取最新用户列表
     */
    @GetMapping("/latest-users")
    public Result<List<User>> getLatestUsers() {
        List<User> users = userMapper.selectLatest(5);
        return Result.success(users);
    }

    /**
     * 获取最新想法列表
     */
    @GetMapping("/latest-thoughts")
    public Result<List<Thought>> getLatestThoughts() {
        List<Thought> thoughts = thoughtMapper.selectLatest(5);
        return Result.success(thoughts);
    }
}
