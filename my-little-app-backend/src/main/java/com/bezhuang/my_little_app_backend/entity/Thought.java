package com.bezhuang.my_little_app_backend.entity;

import java.time.LocalDateTime;

/**
 * 想法实体
 */
public class Thought {

    private Long id;
    private String content;
    private String imageIds;
    private Integer visibility;
    private Integer likeCount;
    private Integer commentCount;
    private Long adminId;
    private String adminUsername;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 临时字段（不存入数据库）
    private Long[] imageIdList;

    public Thought() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageIds() {
        return imageIds;
    }

    public void setImageIds(String imageIds) {
        this.imageIds = imageIds;
        if (imageIds != null && !imageIds.isEmpty()) {
            String[] ids = imageIds.split(",");
            this.imageIdList = new Long[ids.length];
            for (int i = 0; i < ids.length; i++) {
                try {
                    this.imageIdList[i] = Long.parseLong(ids[i].trim());
                } catch (NumberFormatException e) {
                    // ignore invalid id
                }
            }
        }
    }

    public Integer getVisibility() {
        return visibility;
    }

    public void setVisibility(Integer visibility) {
        this.visibility = visibility;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long[] getImageIdList() {
        return imageIdList;
    }

    public void setImageIdList(Long[] imageIdList) {
        this.imageIdList = imageIdList;
    }

    public String getVisibilityText() {
        if (visibility == null) return "未知";
        return visibility == 0 ? "私密" : "公开";
    }
}
