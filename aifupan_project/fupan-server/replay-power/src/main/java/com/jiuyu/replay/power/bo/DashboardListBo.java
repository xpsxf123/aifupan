package com.jiuyu.replay.power.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/24 16:54
 */
@Data
@Schema(description = "数据看板的分页参数")
public class DashboardListBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "到期维度 1：3日内到期，2:7日内到期，3：已过期3日内，4：已过期7日内，5：已过期超过7日")
    private Integer dimension;

    @Schema(description = "客户意向等级")
    private String userAmbition;

    @Schema(description = "渠道id")
    private Long channelId;

    @Schema(description = "销售id")
    private Long salesId;

    @Schema(description = "客户名称")
    private String keyword;

    @Schema(description = "是否是试用订单")
    private Integer trialOrder;

    @Schema(description = "用户版本等级")
    private Integer level;

    @Schema(description = "客户类型")
    private Integer userBelongType;
}
