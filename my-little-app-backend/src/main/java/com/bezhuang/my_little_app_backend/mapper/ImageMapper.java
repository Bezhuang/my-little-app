package com.bezhuang.my_little_app_backend.mapper;

import com.bezhuang.my_little_app_backend.entity.Image;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 图片 Mapper
 */
@Mapper
public interface ImageMapper {

    /**
     * 查询所有图片
     */
    List<Image> selectAll();

    /**
     * 分页查询图片
     */
    List<Image> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计图片数量
     */
    int count();

    /**
     * 根据ID查询图片
     */
    Image selectById(Long id);

    /**
     * 插入图片
     */
    int insert(Image image);

    /**
     * 删除图片
     */
    int deleteById(Long id);

    /**
     * 获取最新上传的图片
     *
     * @param limit 数量限制
     * @return 最新图片列表
     */
    List<Image> selectLatest(@Param("limit") int limit);
}
