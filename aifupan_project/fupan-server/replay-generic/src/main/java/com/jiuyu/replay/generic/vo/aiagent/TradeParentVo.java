package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 行业层级链节点（自身 + 各级父行业）。
 *
 * @author fupan-server
 */
@Data
public class TradeParentVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 行业 ID
     */
    private Long id;

    /**
     * 行业名称
     */
    private String name;

    /**
     * 父行业 ID（0 表示顶级）
     */
    private Long parentId;
}
