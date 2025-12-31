package com.bezhuang.my_little_app_backend.service;

import com.bezhuang.my_little_app_backend.dto.PageResult;
import com.bezhuang.my_little_app_backend.entity.Image;

import java.util.List;

/**
 * 图片 Service 接口
 */
public interface ImageService {

    /**
     * 分页获取图片列表
     */
    PageResult<Image> getPage(int page, int pageSize);

    /**
     * 获取所有图片
     */
    List<Image> getAll();

    /**
     * 根据ID获取图片
     */
    Image getById(Long id);

    /**
     * 上传图片
     */
    Image uploadImage(String filename, String contentType, byte[] data);

    /**
     * 删除图片
     */
    boolean deleteById(Long id);

    /**
     * 获取图片数据
     */
    byte[] getImageData(Long id);
}
