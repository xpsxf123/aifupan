package com.jiuyu.replay.words.bo.oceanEngine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 巨量引擎文件里的实时格式
 *
 * @author liaoxin
 * @date 2025-06-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "巨量引擎文件里的实时格式")
public class OceanEngineProcessBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成交件数
     */
    @Schema(description = "成交件数")
    private Integer payComboCnt;

    /**
     * 成交金额 单位：分
     */
    @Schema(description = "成交金额")
    private Integer payAmt;

    /**
     * 新增直播团人数
     */
    @Schema(description = "新增直播团人数")
    private Integer fansClubJoinUcnt;

    /**
     * 新增粉丝数
     */
    @Schema(description = "新增粉丝数")
    private Integer followAnchorUcnt;

    /**
     * 总观看人次
     */
    @Schema(description = "总观看人次")
    private Integer watchNum;

    /**
     * 采集时间时间戳
     */
    @Schema(description = "采集时间时间戳")
    private Long gatherTimeStamp;

    /**
     * 采集时间
     */
    @Schema(description = "采集时间")
    private String gatherDateTime;

    /**
     * 整体支付ROI
     */
    @Schema(description = "整体支付ROI")
    private BigDecimal totalRoi;

    /**
     * 千川消耗（投放金额）
     */
    @Schema(description = "千川消耗（投放金额）")
    private BigDecimal qianchuanCost;

    /**
     * 退款金额（单位：分）
     */
    @Schema(description = "退款金额（单位：分）")
    private BigDecimal refundAmt;
}