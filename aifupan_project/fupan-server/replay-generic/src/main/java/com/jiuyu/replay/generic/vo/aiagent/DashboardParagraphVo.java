package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 视频看板分段（看盘）数据明细，源自 tb_video_data_viewing_paragraph。
 *
 * @author fupan-server
 */
@Data
public class DashboardParagraphVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 直播场次号
     */
    private String batchNumber;

    private Integer totalWatchNum;
    private Integer averageOnlineNum;
    private Integer averageResidenceTime;
    private Integer incrementFollowerCount;
    private Double convertFanRate;
    private Double interactionPercent;

    /**
     * 销售额区间（元）
     */
    private Integer volumeStart;
    private Integer volumeEnd;

    /**
     * 销量区间
     */
    private Integer purchaseCountStart;
    private Integer purchaseCountEnd;

    /**
     * 客单价区间（元）
     */
    private Double customerUnitPriceStart;
    private Double customerUnitPriceEnd;

    /**
     * UV 价值区间
     */
    private Double uvValueStart;
    private Double uvValueEnd;

    /**
     * 带货转换率区间
     */
    private Double goodsConvertRateStart;
    private Double goodsConvertRateEnd;

    /**
     * 是否带货 0否1是
     */
    private Integer isTakeProduct;

    /**
     * 数据状态（1=拉取成功，8=数据整理中）
     */
    private Integer dataStatus;
}
