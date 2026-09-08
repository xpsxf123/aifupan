package com.jiuyu.replay.generic.vo.power.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/30 10:38
 */
@Data
public class SalesCardStatisticsDataVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "新增注册数")
    private Integer registerNum;

    @Schema(description = "试用用户数")
    private Integer trialUserNum;

    @Schema(description = "试用登录用户数")
    private Integer trialLogoUserNum;

    @Schema(description = "累计注册数")
    private Integer totalRegisterNum;

    @Schema(description = "续费到期数")
    private Integer renewalExpiresNum;

    @Schema(description = "成交客户数")
    private Integer dealCustomersNum;
}
