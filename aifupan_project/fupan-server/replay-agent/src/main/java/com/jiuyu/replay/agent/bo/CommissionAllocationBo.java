package com.jiuyu.replay.agent.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/13 下午2:06
 */
@Data
public class CommissionAllocationBo  implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "邀请链接code")
    private String inviteUrlCode;

    @Schema(description = "订单id")
    private Long orderId;

    @Schema(description = "订单总金额，单位：分")
    private Integer orderTotalMoney;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "分佣类型 0：新签佣金 1：续费佣金")
    private Integer commissionType;

    @Schema(description = "备注")
    private String remarks;
}
