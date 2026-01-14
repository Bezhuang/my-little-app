package com.bezhuang.my_little_app_backend.service.impl;

import com.bezhuang.my_little_app_backend.dto.PageResult;
import com.bezhuang.my_little_app_backend.entity.Thought;
import com.bezhuang.my_little_app_backend.mapper.ThoughtMapper;
import com.bezhuang.my_little_app_backend.service.CacheService;
import com.bezhuang.my_little_app_backend.service.ThoughtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 想法 Service 实现类
 */
@Service
public class ThoughtServiceImpl implements ThoughtService {

    private final ThoughtMapper thoughtMapper;
    private final CacheService cacheService;

    private static final String THOUGHT_CACHE_PREFIX = "thought:";
    private static final long THOUGHT_CACHE_TTL = 600; // 10分钟

    public ThoughtServiceImpl(ThoughtMapper thoughtMapper, CacheService cacheService) {
        this.thoughtMapper = thoughtMapper;
        this.cacheService = cacheService;
    }

    @Override
    public PageResult<Thought> getPublicPage(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Thought> list = thoughtMapper.selectPublicPage(offset, pageSize);
        int total = thoughtMapper.countPublic();

        // 处理图片URL
        for (Thought thought : list) {
            thought.setImageIds(thought.getImageIds());
        }

        return new PageResult<>(page, pageSize, (long) total, list);
    }

    @Override
    public PageResult<Thought> getAllPage(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Thought> list = thoughtMapper.selectAllPageForAdmin(offset, pageSize);
        int total = thoughtMapper.count();

        // 处理图片URL
        for (Thought thought : list) {
            thought.setImageIds(thought.getImageIds());
        }

        return new PageResult<>(page, pageSize, (long) total, list);
    }

    @Override
    public List<Thought> getAll() {
        List<Thought> list = thoughtMapper.selectAllForAdmin();
        for (Thought thought : list) {
            thought.setImageIds(thought.getImageIds());
        }
        return list;
    }

    @Override
    public Thought getById(Long id) {
        String cacheKey = THOUGHT_CACHE_PREFIX + id;
        return cacheService.getOrLoad(cacheKey, Thought.class, () -> {
            Thought thought = thoughtMapper.selectById(id);
            if (thought != null) {
                thought.setImageIds(thought.getImageIds());
            }
            return thought;
        });
    }

    @Override
    @Transactional
    public Thought createThought(Thought thought) {
        if (thought.getVisibility() == null) {
            thought.setVisibility(1); // 默认公开
        }
        if (thought.getLikeCount() == null) {
            thought.setLikeCount(0);
        }
        if (thought.getCommentCount() == null) {
            thought.setCommentCount(0);
        }

        thoughtMapper.insert(thought);
        return thought;
    }

    @Override
    @Transactional
    public boolean updateThought(Thought thought) {
        if (thought.getId() == null) {
            throw new RuntimeException("想法ID不能为空");
        }

        Thought existing = thoughtMapper.selectById(thought.getId());
        if (existing == null) {
            throw new RuntimeException("想法不存在");
        }

        boolean result = thoughtMapper.update(thought) > 0;
        if (result) {
            // 清除想法缓存
            cacheService.delete(THOUGHT_CACHE_PREFIX + thought.getId());
        }
        return result;
    }

    @Override
    @Transactional
    public boolean deleteThought(Long id) {
        Thought thought = thoughtMapper.selectById(id);
        if (thought == null) {
            throw new RuntimeException("想法不存在");
        }
        boolean result = thoughtMapper.deleteById(id) > 0;
        if (result) {
            // 清除想法缓存
            cacheService.delete(THOUGHT_CACHE_PREFIX + id);
        }
        return result;
    }

    @Override
    @Transactional
    public boolean likeThought(Long id) {
        Thought thought = thoughtMapper.selectById(id);
        if (thought == null) {
            throw new RuntimeException("想法不存在");
        }
        int newCount = (thought.getLikeCount() == null ? 0 : thought.getLikeCount()) + 1;
        boolean result = thoughtMapper.updateLikeCount(id, newCount) > 0;
        if (result) {
            // 清除想法缓存（因为点赞数变了）
            cacheService.delete(THOUGHT_CACHE_PREFIX + id);
        }
        return result;
    }

    @Override
    @Transactional
    public boolean unlikeThought(Long id) {
        Thought thought = thoughtMapper.selectById(id);
        if (thought == null) {
            throw new RuntimeException("想法不存在");
        }
        int current = thought.getLikeCount() == null ? 0 : thought.getLikeCount();
        int newCount = Math.max(0, current - 1);
        boolean result = thoughtMapper.updateLikeCount(id, newCount) > 0;
        if (result) {
            cacheService.delete(THOUGHT_CACHE_PREFIX + id);
        }
        return result;
    }

    @Override
    @Transactional
    public void incrementCommentCount(Long id) {
        Thought thought = thoughtMapper.selectById(id);
        if (thought != null) {
            int newCount = (thought.getCommentCount() == null ? 0 : thought.getCommentCount()) + 1;
            thoughtMapper.updateCommentCount(id, newCount);
            cacheService.delete(THOUGHT_CACHE_PREFIX + id);
        }
    }

    @Override
    @Transactional
    public void decrementCommentCount(Long id) {
        Thought thought = thoughtMapper.selectById(id);
        if (thought != null) {
            int current = thought.getCommentCount() == null ? 0 : thought.getCommentCount();
            int newCount = Math.max(0, current - 1);
            thoughtMapper.updateCommentCount(id, newCount);
            cacheService.delete(THOUGHT_CACHE_PREFIX + id);
        }
    }
}
