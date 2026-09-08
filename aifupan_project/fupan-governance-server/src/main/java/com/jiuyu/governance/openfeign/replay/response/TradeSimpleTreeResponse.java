package com.jiuyu.governance.openfeign.replay.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 简化版 行业树结构
 *
 * @author HeHui
 * @date 2026-03-26 16:42
 */
@Getter
@Setter
public class TradeSimpleTreeResponse {

    /**
     * 行业ID
     */
    private Long id;

    /**
     * 行业名称
     */
    private String name;

    /**
     * 父行业ID
     */
    private Long parentId;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 子行业
     */
    private List<TradeSimpleTreeResponse> children;
}
