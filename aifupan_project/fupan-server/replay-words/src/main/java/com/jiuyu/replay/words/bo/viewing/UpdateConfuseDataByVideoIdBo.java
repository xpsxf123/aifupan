package com.jiuyu.replay.words.bo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 根据视频ID批量修改混淆数据请求参数
 *
 * @author jxy
 * @date 2025-04-12
 */
@Data
@Schema(description = "根据视频ID批量修改混淆数据请求参数")
public class UpdateConfuseDataByVideoIdBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 视频id
     */
    @Schema(description = "视频id", required = true)
    @NotBlank(message = "视频id不能为空")
    private String videoId;

    /**
     * 总观看人次
     */
    @Schema(description = "总观看人次")
    private Integer totalWatchNum;

    /**
     * 平均在线人数
     */
    @Schema(description = "平均在线人数")
    private Integer averageOnlineNum;

    /**
     * 平均停留时间(秒)
     */
    @Schema(description = "平均停留时间(秒)")
    private Integer averageResidenceTime;

    /**
     * 新增粉丝数
     */
    @Schema(description = "新增粉丝数")
    private Integer incrementFollowerCount;

    /**
     * 粉丝转化率
     */
    @Schema(description = "粉丝转化率")
    private Double convertFanRate;

    /**
     * 互动率
     */
    @Schema(description = "互动率")
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
     * 千次观看成交金额范围-起始 （单位：元）
     */
    @Schema(description = "千次观看成交金额范围-起始 （单位：元）")
    private Double gpmStart;

    /**
     * 千次观看成交金额范围-结束 （单位：元）
     */
    @Schema(description = "千次观看成交金额范围-结束 （单位：元）")
    private Double gpmEnd;

    /**
     * 曝光-观看率
     */
    @Schema(description = "曝光-观看率")
    private Double showWatchCntRatio;
}
