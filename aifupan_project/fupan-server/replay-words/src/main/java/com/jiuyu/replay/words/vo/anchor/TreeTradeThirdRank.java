package com.jiuyu.replay.words.vo.anchor;


import com.jiuyu.framework.shandard.TreeNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 树型第三方榜单响应
 *
 * @author HeHui
 * @date 2026-02-02 09:57
 */
@Getter
@Setter
public class TreeTradeThirdRank implements TreeNode<Long, Integer, TreeTradeThirdRank> {

    /**
     * 榜单ID
     */
    private Long id;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 榜单名称
     */
    private String name;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 子级
     */
    private List<TreeTradeThirdRank> children;



}
