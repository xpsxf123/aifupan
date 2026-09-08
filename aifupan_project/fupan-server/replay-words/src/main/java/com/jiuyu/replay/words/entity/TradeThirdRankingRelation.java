package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 热搜行业榜单-行业与第三方榜单关联表
 */
@Getter
@Setter
@TableName(value = "tb_trade_third_ranking_relation")
public class TradeThirdRankingRelation {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 行业ID
     */
    @TableField(value = "trade_id")
    private Long tradeId;

    /**
     * 第三方榜单唯一ID（第三方平台的榜单ID）
     */
    @TableField(value = "third_ranking_id")
    private String thirdRankingId;

    /**
     * 第三方榜单名称
     */
    @TableField(value = "third_ranking_name")
    private String thirdRankingName;

    /**
     * 创建时间
     */
    @TableField(value = "create_date")
    private LocalDateTime createDate;

    /**
     * 添加人
     */
    @TableField(value = "created_by")
    private Long createdBy;

    /**
     * 最后修改时间
     */
    @TableField(value = "update_date")
    private LocalDateTime updateDate;

    /**
     * 修改人
     */
    @TableField(value = "updated_by")
    private Long updatedBy;

    /**
     * 上次采集时间
     */
    @TableField(value = "last_collect_time")
    private LocalDateTime lastCollectTime;

    /**
     * 是否采集
     */
    @TableField(value = "enable_collect")
    private Boolean enableCollect;

    /**
     * 每个周期的周几采集 1-7, 0表示不采集
     */
    @TableField(value = "day_of_week")
    private Integer dayOfWeek;

}
