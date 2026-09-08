package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流式（游标）分页外壳——用于按指标排序的 keyset 分页。
 *
 * <p>{@code nextCursor} 是不透明字符串令牌（内部编码"排序值+id"）。Agent 无需解析，
 * 把上一页的 {@code nextCursor} 原样回传即可，直到 {@code hasMore=false}。无总数、无页码，不支持跳页。</p>
 *
 * @param <T> 列表元素类型
 * @author fupan-server
 */
@Data
public class StreamPageVo<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页数据
     */
    private List<T> list;

    /**
     * 下一页游标（不透明令牌）；null 表示已到末页
     */
    private String nextCursor;

    /**
     * 是否还有下一页
     */
    private Boolean hasMore;

    public static <T> StreamPageVo<T> of(List<T> list, String nextCursor, boolean hasMore) {
        StreamPageVo<T> vo = new StreamPageVo<>();
        vo.setList(list == null ? new ArrayList<>() : list);
        vo.setNextCursor(nextCursor);
        vo.setHasMore(hasMore);
        return vo;
    }

    public static <T> StreamPageVo<T> empty() {
        return of(new ArrayList<>(), null, false);
    }
}
