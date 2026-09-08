package com.jiuyu.replay.generic.vo.power.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/30 11:10
 */
@Data
public class EachSalesFollowStatisticsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "销售id")
    private Long salesId;

    @Schema(description = "销售名称")
    private String salesName;

    @Schema(description = "注册用户数")
    private int registerUserNum;

    @Schema(description = "试用用户数")
    private int trialUserNum;

    @Schema(description = "试用率")
    private String trialRate;

    @Schema(description = "登录用户数")
    private int loginUserNum;

    @Schema(description = "试用登录用户数")
    private int trialLoginUserNum;

    @Schema(description = "试用登录率")
    private String trialLoginRate;

    @Schema(description = "演示数")
    private int demoNum;

    @Schema(description = "演示率")
    private String demoRate;

    @Schema(description = "成交客户数")
    private int customerNum;

    @Schema(description = "成交率")
    private String customerRate;

    @Schema(description = "成交金额")
    private int customerAmount;

    @Schema(description = "UV价值")
    private String uvValue;

}
