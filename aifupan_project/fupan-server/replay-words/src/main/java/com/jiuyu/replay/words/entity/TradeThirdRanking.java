package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jiuyu.replay.words.vo.anchor.TreeTradeThirdRank;
import lombok.Getter;
import lombok.Setter;

/**
 * 行业第三方榜单表
 */
@Getter
@Setter
@TableName(value = "tb_trade_third_ranking")
public class TradeThirdRanking {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 父类主键
     */
    @TableField(value = "parent_id")
    private Long parentId;

    /**
     * 层级
     */
    @TableField(value = "layer")
    private Integer layer;

    /**
     * 行业名称
     */
    @TableField(value = "category_name")
    private String categoryName;

    /**
     * 父类路径
     */
    @TableField(value = "parent_path")
    private String parentPath;



    public TreeTradeThirdRank toTree() {
        TreeTradeThirdRank tree = new TreeTradeThirdRank();
        tree.setId(this.id);
        tree.setParentId(this.parentId);
        tree.setName(this.categoryName);
        tree.setSort(this.layer);
        tree.setChildren(null);
        return tree;
    }
}
