package com.bezhuang.my_little_app_backend.controller;

import com.bezhuang.my_little_app_backend.dto.PageResult;
import com.bezhuang.my_little_app_backend.dto.Result;
import com.bezhuang.my_little_app_backend.entity.Thought;
import com.bezhuang.my_little_app_backend.service.ThoughtService;
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
 * 想法控制器（公开接口）
 * 所有用户可见公开想法，管理员可管理所有想法
 */
@RestController
@RequestMapping("/api/thoughts")
public class ThoughtController {

    private final ThoughtService thoughtService;

    public ThoughtController(ThoughtService thoughtService) {
        this.thoughtService = thoughtService;
    }

    /**
     * 获取公开想法列表（分页）
     */
    @GetMapping("/public")
    public Result<PageResult<Thought>> getPublicThoughts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Thought> result = thoughtService.getPublicPage(page, pageSize);
        return Result.success(result);
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
        // 非公开想法只能管理员查看
        if (thought.getVisibility() == 0) {
            return Result.forbidden("无权查看此想法");
        }
        return Result.success(thought);
    }

    /**
     * 点赞想法
     */
    @PostMapping("/{id}/like")
    public Result<Void> likeThought(@PathVariable Long id) {
        try {
            boolean success = thoughtService.likeThought(id);
            if (success) {
                return Result.success("点赞成功", null);
            }
            return Result.error("点赞失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消点赞
     */
    @DeleteMapping("/{id}/like")
    public Result<Void> unlikeThought(@PathVariable Long id) {
        try {
            boolean success = thoughtService.unlikeThought(id);
            if (success) {
                return Result.success("取消点赞成功", null);
            }
            return Result.error("取消点赞失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
