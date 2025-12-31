package com.bezhuang.my_little_app_backend.service.impl;

import com.bezhuang.my_little_app_backend.dto.PageResult;
import com.bezhuang.my_little_app_backend.entity.Thought;
import com.bezhuang.my_little_app_backend.mapper.ThoughtMapper;
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

    public ThoughtServiceImpl(ThoughtMapper thoughtMapper) {
        this.thoughtMapper = thoughtMapper;
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
        Thought thought = thoughtMapper.selectById(id);
        if (thought != null) {
            thought.setImageIds(thought.getImageIds());
        }
        return thought;
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

        return thoughtMapper.update(thought) > 0;
    }

    @Override
    @Transactional
    public boolean deleteThought(Long id) {
        Thought thought = thoughtMapper.selectById(id);
        if (thought == null) {
            throw new RuntimeException("想法不存在");
        }
        return thoughtMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean likeThought(Long id) {
        Thought thought = thoughtMapper.selectById(id);
        if (thought == null) {
            throw new RuntimeException("想法不存在");
        }
        int newCount = (thought.getLikeCount() == null ? 0 : thought.getLikeCount()) + 1;
        return thoughtMapper.updateLikeCount(id, newCount) > 0;
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
        return thoughtMapper.updateLikeCount(id, newCount) > 0;
    }

    @Override
    @Transactional
    public void incrementCommentCount(Long id) {
        Thought thought = thoughtMapper.selectById(id);
        if (thought != null) {
            int newCount = (thought.getCommentCount() == null ? 0 : thought.getCommentCount()) + 1;
            thoughtMapper.updateCommentCount(id, newCount);
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
        }
    }
}
