package com.bezhuang.my_little_app_backend.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页数量
     */
    private Integer pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 数据列表
     */
    private List<T> list;

    public PageResult() {
    }

    public PageResult(Integer page, Integer pageSize, Long total, List<T> list) {
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.list = list;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
    }

    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(Integer page, Integer pageSize, Long total, List<T> list) {
        return new PageResult<>(page, pageSize, total, list);
    }

    // ==================== Getter 和 Setter ====================

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return "PageResult{" +
                "page=" + page +
                ", pageSize=" + pageSize +
                ", total=" + total +
                ", totalPages=" + totalPages +
                ", list=" + list +
                '}';
    }
}







