package com.bezhuang.my_little_app_backend.service.impl;

import com.bezhuang.my_little_app_backend.dto.PageResult;
import com.bezhuang.my_little_app_backend.entity.Image;
import com.bezhuang.my_little_app_backend.mapper.ImageMapper;
import com.bezhuang.my_little_app_backend.service.ImageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 图片 Service 实现类
 */
@Service
public class ImageServiceImpl implements ImageService {

    private final ImageMapper imageMapper;

    public ImageServiceImpl(ImageMapper imageMapper) {
        this.imageMapper = imageMapper;
    }

    @Override
    public PageResult<Image> getPage(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Image> list = imageMapper.selectPage(offset, pageSize);
        int total = imageMapper.count();

        return new PageResult<>(page, pageSize, (long) total, list);
    }

    @Override
    public List<Image> getAll() {
        return imageMapper.selectAll();
    }

    @Override
    public Image getById(Long id) {
        return imageMapper.selectById(id);
    }

    @Override
    @Transactional
    public Image uploadImage(String filename, String contentType, byte[] data) {
        Image image = new Image(filename, contentType, data);
        imageMapper.insert(image);
        return image;
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        return imageMapper.deleteById(id) > 0;
    }

    @Override
    public byte[] getImageData(Long id) {
        Image image = imageMapper.selectById(id);
        return image != null ? image.getData() : null;
    }
}
