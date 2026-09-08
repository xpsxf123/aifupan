package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 第三方直播录制记录表
 *
 * @author System
 * @date 2026-04-09
 */
@Data
@TableName("tb_thirdparty_live_record")
public class ThirdpartyLiveRecordEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 行业ID，关联tb_trade.id
     */
    private Long tradeId;

    /**
     * 主播抖音昵称
     */
    private String anchorNickname;

    /**
     * 抖音号
     */
    private String douyinId;

    /**
     * 云空间链接
     */
    private String cloudUrl;

    /**
     * 场观
     */
    private Long viewers;

    /**
     * 月销售额（区间值，如100万-500万）
     */
    private String monthlySales;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;

    /**
     * 是否删除 0否 1是
     */
    private Integer isDeleted;
}
