package com.jiuyu.replay.generic.vo.words.viewing;

import com.jiuyu.replay.generic.dto.words.viewing.FlowSourceDto;
import com.jiuyu.replay.generic.dto.words.viewing.UserPortraitDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 巨量引擎数据表VO
 *
 * @author liaoxin
 * @date 2025-06-13
 */
@Data
@Schema(description = "巨量引擎数据表")
public class OceanEngineDataInfoVo {

    /**
     * 视频id
     */
    @Schema(description = "视频id")
    private String videoId;

    /**
     * 主播id
     */
    @Schema(description = "主播id")
    private String secUid;

    /**
     * 批次号
     */
    @Schema(description = "批次号")
    private String batchNumber;

    /**
     * 看播流量结构
     */
    private List<FlowSourceDto> watchFlowList;

    /**
     * 成交流量结构
     */
    private List<FlowSourceDto> payFlowList;

    /**
     * 看播用户画像
     */
    private UserPortraitDto watchUserPortrait;

    /**
     * 成交用户画像
     */
    private UserPortraitDto payUserPortrait;

    /**
     * 是否带货 0：否 1：是
     */
    private Integer isTakeProduct;
    /**
     * 主播账号
     */
    private String anchorNumber;
    /**
     * 总观看人次
     */
    private Integer totalWatchNum;
    /**
     * 平均在线人数
     */
    private Integer averageOnlineNum;
    /**
     * 平均停留时间(秒)
     */
    private Integer averageResidenceTime;
    /**
     * 新增粉丝数
     */
    private Integer incrementFollowerCount;
    /**
     * 粉丝转化率
     */
    private Double convertFanRate;
    /**
     * 互动率
     */
    private Double interactionPercent;

    /**
     * 销售额区间范围-起始(单位:元)
     */
    @Schema(description = "销售额区间范围-起始(单位:元)")
    private Integer volumeStart;
    /**
     * 销售额区间范围-结束(单位:元)
     */
    @Schema(description = "销售额区间范围-结束(单位:元)")
    private Integer volumeEnd;

    /**
     * 销量区间范围-起始
     */
    @Schema(description = "销量区间范围-起始")
    private Integer purchaseCountStart;
    /**
     * 销量区间范围-结束
     */
    @Schema(description = "销量区间范围-结束")
    private Integer purchaseCountEnd;
    /**
     * 客单价区间范围-起始（单位：元）
     */
    @Schema(description = "客单价区间范围-起始（单位：元）")
    private Double customerUnitPriceStart;
    /**
     * 客单价区间范围-结束（单位：元）
     */
    @Schema(description = "客单价区间范围-结束（单位：元）")
    private Double customerUnitPriceEnd;
    /**
     * uv价值区间范围-起始
     */
    @Schema(description = "uv价值区间范围-起始")
    private Double uvValueStart;
    /**
     * uv价值区间范围-结束
     */
    @Schema(description = "uv价值区间范围-结束")
    private Double uvValueEnd;
    /**
     * 带货转换率区间范围-起始
     */
    @Schema(description = "带货转换率区间范围-起始")
    private Double goodsConvertRateStart;
    /**
     * 带货转换率区间范围-结束
     */
    @Schema(description = "带货转换率区间范围-结束")
    private Double goodsConvertRateEnd;
    /**
     * 销售额
     */
    private Integer volume;
    /**
     * 销量
     */
    private Integer purchaseCount;
    /**
     * 客单价
     */
    private Double customerUnitPrice;
    /**
     * uv价值
     */
    private Double uvValue;
    /**
     * 带货转换率
     */
    private Double goodsConvertRate;

    /**
     * 曝光-观看率
     */
    @Schema(description = "曝光-观看率")
    private Double showWatchCntRatio;
    /**
     * ROI
     */
    @Schema(description = "ROI")
    private Double roi;
    /**
     * 投放ROI金额（单位：元）
     */
    @Schema(description = "投放ROI金额（单位：元）")
    private Double launchRoiAmount;
    /**
     * 退款金额（单位：元）
     */
    @Schema(description = "退款金额（单位：元）")
    private Double refundAmount;
    /**
     * 整体消耗ROI
     */
    @Schema(description = "整体消耗ROI")
    private Double overallCostRoi;
    /**
     * 净成交ROI
     */
    @Schema(description = "净成交ROI")
    private Double netTransactionRoi;

}
