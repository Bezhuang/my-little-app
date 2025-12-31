package com.bezhuang.my_little_app_backend.controller;

import com.bezhuang.my_little_app_backend.dto.PageResult;
import com.bezhuang.my_little_app_backend.dto.Result;
import com.bezhuang.my_little_app_backend.entity.Image;
import com.bezhuang.my_little_app_backend.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 图片管理 Controller
 */
@RestController
@RequestMapping("/api/admin/images")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * 分页获取图片列表（需要管理员权限）
     */
    @GetMapping("/list")
    public Result<PageResult<Image>> getList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<Image> result = imageService.getPage(page, pageSize);
        return Result.success(result);
    }

    /**
     * 获取所有图片（需要管理员权限）
     */
    @GetMapping("/all")
    public Result<List<Image>> getAll() {
        List<Image> images = imageService.getAll();
        return Result.success(images);
    }

    /**
     * 获取图片信息（不含数据，需要管理员权限）
     */
    @GetMapping("/{id}")
    public Result<Image> getById(@PathVariable Long id) {
        Image image = imageService.getById(id);
        if (image == null) {
            return Result.error("图片不存在");
        }
        // 不返回大数据
        image.setData(null);
        return Result.success(image);
    }

    /**
     * 公开获取图片数据（无需认证，用于前端显示）
     */
    @GetMapping("/public/{id}/data")
    public ResponseEntity<byte[]> getPublicImageData(@PathVariable Long id) {
        logger.info("获取公开图片数据, id: {}", id);
        byte[] data = imageService.getImageData(id);
        if (data == null) {
            logger.warn("图片不存在或数据为空, id: {}", id);
            return ResponseEntity.notFound().build();
        }

        logger.info("图片数据大小: {} bytes", data.length);

        Image image = imageService.getById(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                image != null && image.getContentType() != null
                        ? image.getContentType()
                        : "image/jpeg"));
        headers.setCacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofSeconds(3600)).cachePublic());

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    public Result<Image> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        try {
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            byte[] data = file.getBytes();

            Image image = imageService.uploadImage(filename, contentType, data);
            image.setData(null); // 不返回数据
            return Result.success(image);
        } catch (IOException e) {
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除图片
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = imageService.deleteById(id);
        if (success) {
            return Result.success();
        }
        return Result.error("删除失败");
    }
}
