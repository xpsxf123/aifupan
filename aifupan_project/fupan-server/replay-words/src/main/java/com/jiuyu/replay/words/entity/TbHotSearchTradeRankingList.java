package com.jiuyu.replay.words.entity;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 热搜行业榜单-榜单主播核心表
 */
@Getter
@Setter
@TableName(value = "tb_hot_search_trade_ranking_list")
public class TbHotSearchTradeRankingList {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 创建人ID
     */
    @TableField(value = "created_by")
    private Long createdBy;

    /**
     * 更新人ID
     */
    @TableField(value = "updated_by")
    private Long updatedBy;

    /**
     * 关联tb_similar_anchor的相似达人ID
     */
    @TableField(value = "similar_anchor_id")
    private Long similarAnchorId;

    /**
     * 关联tb_anchor_url的系统主播ID
     */
    @TableField(value = "system_anchor_id")
    private Long systemAnchorId;

    /**
     * 原始行业ID（录入/采集时的初始行业）
     */
    @TableField(value = "trade_id")
    private Long tradeId;

    /**
     * 关联主播的系统行业ID（取自tb_anchor_url）
     */
    @TableField(value = "system_trade_id")
    private Long systemTradeId;

    /**
     * 最后更改系统行业时间
     */
    @TableField(value = "last_update_system_trade_time")
    private LocalDateTime lastUpdateSystemTradeTime;

    /**
     * 数据来源：1-系统主播相似达人，2-后台录入，3-第三方榜单
     */
    @TableField(value = "source_type")
    private Integer sourceType;

    /**
     * 采集状态：0-待采集，1-采集中（仅针对系统主播相似达人），2-采集完成
     */
    @TableField(value = "collect_status")
    private Integer collectStatus;

    /**
     * 是否上榜：0-否，1-是
     */
    @TableField(value = "up_ranking")
    private Boolean upRanking;

    /**
     * 创建时间
     */
    @TableField(value = "create_date")
    private LocalDateTime createDate;

    /**
     * 最后修改时间
     */
    @TableField(value = "update_date")
    private LocalDateTime updateDate;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableField(value = "is_deleted")
    private Boolean isDeleted;

    /**
     * 权重得分, 由渠道 热度,平均场观,总销售额计算得出
     */
    @TableField(value = "weight_score")
    private Long weightScore;


    /**
     * 前置系统主播账户ID
     */
    @TableField(value = "pre_anchor_id")
    private Long preAnchorId;



    /**
     * 计算权重得分
     */
    public void calcWeightScore(SimilarAnchorEntity similarAnchor) {
        Integer accountHeat = similarAnchor.getAccountHeat();
        if (accountHeat == null) {
            return;
        }
        boolean totalAmountQualified = similarAnchor.getTotalAmountNumeric() != null && similarAnchor.getTotalAmountNumeric() >= 50000;
        boolean liveAverageUserQualified = similarAnchor.getLiveAverageUser() != null;
        if (liveAverageUserQualified && NumberUtil.isNumber(similarAnchor.getLiveAverageUser())) {
            liveAverageUserQualified = new BigDecimal(similarAnchor.getLiveAverageUser()).compareTo(BigDecimal.valueOf(5000)) >= 0;
        }
        // 根据上面是否合格来来根据 accountHeat 加权或者降权重 以实现不合格的永远低于合格的
        weightScore = accountHeat * (totalAmountQualified && liveAverageUserQualified ? 1L : -1L);
    }
}
