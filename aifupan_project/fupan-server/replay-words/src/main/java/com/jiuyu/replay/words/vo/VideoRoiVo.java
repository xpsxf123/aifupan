package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 视频ROI信息
 *
 * @author lujie
 * @date 2025-07-20
 */
@Data
public class VideoRoiVo {

    @Schema(description = "投放消耗（元）")
    private Double launchRoiAmount;

    @Schema(description = "销售额（元）")
    private Integer salesAmount;

    @Schema(description = "整体支付ROI")
    private Double overallCostRoi;

    @Schema(description = "净成交金额（元）")
    private Double netTransactionAmount;

    @Schema(description = "净成交ROI")
    private Double netTransactionRoi;

    @Schema(description = "巨量百应授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配")
    private Integer jlbyAuthStatus;

    @Schema(description = "千川授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配")
    private Integer qcAuthStatus;

    @Schema(description = "成交单量")
    private Integer payCount;

    @Schema(description = "千次观看成交金额（元）")
    private Double gpm;

    @Schema(description = "是否有数据")
    private Boolean hasData;
}
