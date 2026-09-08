package com.jiuyu.replay.generic.bo.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "查询单条邀请码参数")
public class InviteUrlCodeInfoConditionBo {

    /**
     * 代理商id
     */
    @Schema(description = "代理商id")
    private Long agentId;
    /**
     * 代理商推广渠道id
     */
    @Schema(description = "代理商推广渠道id")
    private Long promotionId;
    /**
     * 代理商销售id
     */
    @Schema(description = "代理商销售id")
    private Long agentSaleId;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;

    /**
     * 子账号
     */
    private Long subUserId;
    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;
    /**
     * 活动id
     */
    @Schema(description = "活动id")
    private Long activityId;
    /**
     * code类型 0：代理商链接 1：推广渠道链接 2：用户链接 3：代理商销售链接
     */
    @Schema(description = "code类型 0：代理商链接 1：推广渠道链接 2：用户链接 3：代理商销售链接")
    private Integer codeType;
}
