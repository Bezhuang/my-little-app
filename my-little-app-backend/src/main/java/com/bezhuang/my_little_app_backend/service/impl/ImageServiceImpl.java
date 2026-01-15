package com.bezhuang.my_little_app_backend.service.impl;

import com.bezhuang.my_little_app_backend.dto.PageResult;
import com.bezhuang.my_little_app_backend.entity.Image;
import com.bezhuang.my_little_app_backend.mapper.ImageMapper;
import com.bezhuang.my_little_app_backend.service.CacheService;
import com.bezhuang.my_little_app_backend.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 图片 Service 实现类
 */
@Service
public class ImageServiceImpl implements ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageServiceImpl.class);
    private static final String IMAGE_CACHE_PREFIX = "image:data:";
    private static final long IMAGE_CACHE_TTL = 3600; // 1小时

    private final ImageMapper imageMapper;
    private final CacheService cacheService;

    public ImageServiceImpl(ImageMapper imageMapper, CacheService cacheService) {
        this.imageMapper = imageMapper;
        this.cacheService = cacheService;
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
        // 删除时清除缓存
        cacheService.delete(IMAGE_CACHE_PREFIX + id);
        return imageMapper.deleteById(id) > 0;
    }

    @Override
    public byte[] getImageData(Long id) {
        String cacheKey = IMAGE_CACHE_PREFIX + id;

        // 先从 Redis 缓存获取
        byte[] cachedData = cacheService.get(cacheKey, byte[].class);
        if (cachedData != null) {
            logger.debug("图片缓存命中 id={}", id);
            return cachedData;
        }

        // 缓存未命中，从数据库获取
        Image image = imageMapper.selectById(id);
        if (image == null || image.getData() == null) {
            return null;
        }

        // 存入 Redis 缓存
        if (cacheService.isAvailable()) {
            try {
                cacheService.set(cacheKey, image.getData(), IMAGE_CACHE_TTL);
                logger.debug("图片数据已缓存 id={}, size={}", id, image.getData().length);
            } catch (Exception e) {
                logger.warn("缓存图片数据失败 id={}: {}", id, e.getMessage());
            }
        }

        return image.getData();
    }
}
