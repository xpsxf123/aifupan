package com.jiuyu.replay.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/13 下午6:58
 */
@Data
@Schema(description = "佣金结算记录Vo")
public class CommissionRecordsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "开始时间")
    private String startDate;

    @Schema(description = "结束时间")
    private String endDate;

    @Schema(description = "新签佣金比例")
    private Double commissionRate;

    @Schema(description = "新签实付总额")
    private Double commissionRateAmount;

    @Schema(description = "续费佣金比例")
    private Double renewalCommissionRate;

    @Schema(description = "续费实付总额")
    private Double renewalCommissionRateAmount;

    @Schema(description = "佣金结算总额（分）")
    private Double settlementAmount;

    @Schema(description = "佣金结算状态 0：使用中 1：已结束")
    private Integer status;
}
