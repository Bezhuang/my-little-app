package com.bezhuang.my_little_app_backend.controller;

import com.bezhuang.my_little_app_backend.config.security.CustomUserDetails;
import com.bezhuang.my_little_app_backend.dto.PageResult;
import com.bezhuang.my_little_app_backend.dto.Result;
import com.bezhuang.my_little_app_backend.entity.Thought;
import com.bezhuang.my_little_app_backend.service.ThoughtService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * 想法管理控制器（仅管理员）
 */
@RestController
@RequestMapping("/api/admin/thoughts")
@PreAuthorize("hasRole('ADMIN')")
public class ThoughtAdminController {

    private final ThoughtService thoughtService;

    public ThoughtAdminController(ThoughtService thoughtService) {
        this.thoughtService = thoughtService;
    }

    /**
     * 获取所有想法列表（分页）
     */
    @GetMapping("/list")
    public Result<PageResult<Thought>> getAllThoughts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Thought> result = thoughtService.getAllPage(page, pageSize);
        return Result.success(result);
    }

    /**
     * 获取所有想法列表（不分页）
     */
    @GetMapping("/all")
    public Result<List<Thought>> getAllThoughts() {
        List<Thought> list = thoughtService.getAll();
        return Result.success(list);
    }

    /**
     * 获取想法详情
     */
    @GetMapping("/{id}")
    public Result<Thought> getThoughtById(@PathVariable Long id) {
        Thought thought = thoughtService.getById(id);
        if (thought == null) {
            return Result.notFound("想法不存在");
        }
        return Result.success(thought);
    }

    /**
     * 发布想法
     */
    @PostMapping("/add")
    public Result<Thought> createThought(
            @RequestBody Map<String, Object> params,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Thought thought = new Thought();
            thought.setContent((String) params.get("content"));
            thought.setImageIds((String) params.get("imageIds"));

            Object visibility = params.get("visibility");
            if (visibility != null) {
                thought.setVisibility(Integer.parseInt(visibility.toString()));
            } else {
                thought.setVisibility(1); // 默认公开
            }

            // 设置发布者信息
            if (userDetails != null) {
                thought.setAdminId(userDetails.getId());
                thought.setAdminUsername(userDetails.getUsername());
            }

            Thought created = thoughtService.createThought(thought);
            return Result.success("发布成功", created);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新想法
     */
    @PutMapping("/update")
    public Result<Void> updateThought(@RequestBody Thought thought) {
        try {
            boolean success = thoughtService.updateThought(thought);
            if (success) {
                return Result.success("更新成功", null);
            }
            return Result.error("更新失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除想法
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteThought(@PathVariable Long id) {
        try {
            boolean success = thoughtService.deleteThought(id);
            if (success) {
                return Result.success("删除成功", null);
            }
            return Result.error("删除失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 切换想法可见性
     */
    @PutMapping("/visibility/{id}")
    public Result<Void> updateVisibility(
            @PathVariable Long id,
            @RequestParam Integer visibility) {
        try {
            Thought thought = new Thought();
            thought.setId(id);
            thought.setVisibility(visibility);
            boolean success = thoughtService.updateThought(thought);
            if (success) {
                return Result.success("更新成功", null);
            }
            return Result.error("更新失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
