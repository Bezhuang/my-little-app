package com.bezhuang.my_little_app_backend.mapper;

import com.bezhuang.my_little_app_backend.entity.Thought;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 想法 Mapper
 */
@Mapper
public interface ThoughtMapper {

    /**
     * 根据ID查询
     */
    Thought selectById(Long id);

    /**
     * 查询所有（按时间倒序）
     */
    List<Thought> selectAll();

    /**
     * 查询公开想法
     */
    List<Thought> selectPublic();

    /**
     * 查询所有想法（包括私密）
     */
    List<Thought> selectAllForAdmin();

    /**
     * 分页查询
     */
    List<Thought> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 分页查询公开想法
     */
    List<Thought> selectPublicPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 分页查询所有想法（管理员）
     */
    List<Thought> selectAllPageForAdmin(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 插入想法
     */
    int insert(Thought thought);

    /**
     * 更新想法
     */
    int update(Thought thought);

    /**
     * 删除想法
     */
    int deleteById(Long id);

    /**
     * 更新点赞数
     */
    int updateLikeCount(@Param("id") Long id, @Param("likeCount") int likeCount);

    /**
     * 更新评论数
     */
    int updateCommentCount(@Param("id") Long id, @Param("commentCount") int commentCount);

    /**
     * 统计总数
     */
    int count();

    /**
     * 统计公开想法数
     */
    int countPublic();

    /**
     * 获取最新想法
     *
     * @param limit 数量限制
     * @return 最新想法列表
     */
    List<Thought> selectLatest(@Param("limit") int limit);
}
