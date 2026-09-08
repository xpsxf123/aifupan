package com.jiuyu.replay.words.bo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "直播流量信息")
public class LiveTrafficBo implements Serializable {
    private static final long serialVersionUID = 1L;



    /**
     * 互动率百分比
     */
    @Schema(description = "互动率百分比")
    private Double interactionPercent;

    /**
     * 粉丝转化率
     */
    @Schema(description = "粉丝转化率")
    private Double convertFanRate;

    /**
     * 抖加引流进入次数
     */
    @Schema(description = "抖加引流进入次数")
    private Integer douPlusEnterCnt;

    /**
     * 新增关注数
     */
    @Schema(description = "新增关注数")
    private Integer incrementFollowerCount;

    /**
     * 转化率百分比
     */
    @Schema(description = "转化率百分比")
    private Float conversionRatePercent;

    /**
     * UV成本超过百分比
     */
    @Schema(description = "UV成本超过百分比")
    private Float uvCostMoreThanPercent;
}
