package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 抖音同行直播基础数据平均值
 *
 * @author jy
 * @date 2026-06-25
 */
@Data
@TableName("tb_douyin_peer_avg")
public class DouyinPeerAvgEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private Long tradeId;
    private Date statDate;
    private Integer videoCount;

    private BigDecimal avgTotalWatchNum;
    private BigDecimal avgAverageOnlineNum;
    private BigDecimal avgAverageResidenceTime;
    private BigDecimal avgIncrementFollowerCount;
    private BigDecimal avgConvertFanRate;
    private BigDecimal avgInteractionPercent;

    private BigDecimal avgVolumeStart;
    private BigDecimal avgVolumeEnd;
    private BigDecimal avgPurchaseCountStart;
    private BigDecimal avgPurchaseCountEnd;
    private BigDecimal avgCustomerUnitPriceStart;
    private BigDecimal avgCustomerUnitPriceEnd;
    private BigDecimal avgUvValueStart;
    private BigDecimal avgUvValueEnd;
    private BigDecimal avgGoodsConvertRateStart;
    private BigDecimal avgGoodsConvertRateEnd;
    private BigDecimal avgGpmStart;
    private BigDecimal avgGpmEnd;

    private Date createDate;
    private Date updateDate;
    private Integer isDeleted;
}
