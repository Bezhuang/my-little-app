package com.bezhuang.my_little_app_backend.service;

import com.bezhuang.my_little_app_backend.dto.PageResult;
import com.bezhuang.my_little_app_backend.entity.Thought;

import java.util.List;

/**
 * 想法 Service 接口
 */
public interface ThoughtService {

    /**
     * 分页获取公开想法（所有用户可见）
     */
    PageResult<Thought> getPublicPage(int page, int pageSize);

    /**
     * 分页获取所有想法（仅管理员）
     */
    PageResult<Thought> getAllPage(int page, int pageSize);

    /**
     * 获取所有想法列表（管理员）
     */
    List<Thought> getAll();

    /**
     * 根据ID获取想法
     */
    Thought getById(Long id);

    /**
     * 创建想法
     */
    Thought createThought(Thought thought);

    /**
     * 更新想法
     */
    boolean updateThought(Thought thought);

    /**
     * 删除想法
     */
    boolean deleteThought(Long id);

    /**
     * 点赞
     */
    boolean likeThought(Long id);

    /**
     * 取消点赞
     */
    boolean unlikeThought(Long id);

    /**
     * 增加评论数
     */
    void incrementCommentCount(Long id);

    /**
     * 减少评论数
     */
    void decrementCommentCount(Long id);
}
