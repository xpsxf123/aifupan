package com.jiuyu.governance.openfeign.replay.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 复盘开放接口-游标分页响应外壳
 * <p>
 * 列表接口 {@code data} 的统一结构。把 {@link #nextCursor} 原样回传到下次请求的 cursor 即可翻页，
 * 直到 {@link #hasMore} 为 {@code false}。
 * </p>
 *
 * @param <T> 列表元素类型
 *
 * @author HeHui
 * @date 2026-06-14
 */
@Getter
@Setter
public class ReplayCursorPage<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页数据
     */
    private List<T> list;

    /**
     * 下一页游标；null 表示已到末页。
     * 序列化为字符串：游标会原样回传，值可能超出 JS Number 安全整数范围，精度丢失会静默错翻页。
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long nextCursor;

    /**
     * 是否还有下一页
     */
    private Boolean hasMore;
}
