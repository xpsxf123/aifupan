/**
 * Copyright (c) 2016-2019 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.jiuyu.replay.generic.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页工具类
 *
 * @author Mark sunlightcs@gmail.com
 */
@Schema(description = "分页数据响应对象")
@Data
public class PageUtils<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 总记录数
     */
    @Schema(description = "总记录数")
    private int totalCount;
    /**
     * 每页记录数
     */
    @Schema(description = "每页记录数")
    private int pageSize;
    /**
     * 总页数
     */
    @Schema(description = "总页数")
    private int totalPage;
    /**
     * 当前页数
     */
    @Schema(description = "当前页数")
    private int currPage;
    /**
     * 列表数据
     */
    @Schema(description = "列表数据")
    private List<T> list;

    public PageUtils() {
        list = new ArrayList<>();
    }

    public PageUtils(Integer page, Integer limit) {
        this.totalCount = 0;
        this.pageSize = limit == null ? 10 : limit;
        this.currPage = page == null ? 1 : page;
        this.totalPage = 1;
    }

    public PageUtils(Integer page, Integer limit, IPage iPage) {
        this.totalCount = (int) iPage.getTotal();
        this.pageSize = limit == null ? 10 : limit;
        this.currPage = page == null ? 1 : page;
        this.totalPage = (int) iPage.getPages();
    }

    /**
     * 分页
     *
     * @param list       列表数据
     * @param totalCount 总记录数
     * @param pageSize   每页记录数
     * @param currPage   当前页数
     */
    public PageUtils(List<T> list, int totalCount, int pageSize, int currPage) {
        this.list = list;
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.currPage = currPage;
        this.totalPage = (int) Math.ceil((double) totalCount / pageSize);
    }

    /**
     * 分页
     */
    public PageUtils(IPage<T> page) {
        this.list = page.getRecords();
        this.totalCount = (int) page.getTotal();
        this.pageSize = (int) page.getSize();
        this.currPage = (int) page.getCurrent();
        this.totalPage = (int) page.getPages();
    }

    public PageUtils(IPage page, List<T> list) {
        this.totalCount = (int) page.getTotal();
        this.pageSize = (int) page.getSize();
        this.currPage = (int) page.getCurrent();
        this.totalPage = (int) page.getPages();
        this.list = list;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public int getCurrPage() {
        return currPage;
    }

    public void setCurrPage(int currPage) {
        this.currPage = currPage;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

}
