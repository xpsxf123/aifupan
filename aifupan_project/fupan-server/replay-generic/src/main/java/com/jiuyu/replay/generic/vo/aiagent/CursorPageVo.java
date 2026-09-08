package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 游标流式分页统一外壳。
 *
 * <p>Agent 反复翻页时把上一页返回的 {@code nextCursor} 原样回传即可，直到 {@code hasMore=false}。</p>
 *
 * @param <T> 列表元素类型
 * @author fupan-server
 */
@Data
public class CursorPageVo<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页数据
     */
    private List<T> list;

    /**
     * 下一页游标；null 表示已到末页
     */
    private Long nextCursor;

    /**
     * 是否还有下一页
     */
    private Boolean hasMore;

    public static <T> CursorPageVo<T> of(List<T> list, Long nextCursor, boolean hasMore) {
        CursorPageVo<T> vo = new CursorPageVo<>();
        vo.setList(list == null ? new ArrayList<>() : list);
        vo.setNextCursor(nextCursor);
        vo.setHasMore(hasMore);
        return vo;
    }

    public static <T> CursorPageVo<T> empty() {
        return of(new ArrayList<>(), null, false);
    }
}
