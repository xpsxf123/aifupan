package com.jiuyu.replay.reward.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author RayChou
 * @date 2025/5/29 19:17
 */
@Data
public class ClientInviteRewardRecordInfoBo implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 活动id
     */
    @Schema(description = "活动id")
    private Long activityId;
    /**
     * 进度id
     */
    @Schema(description = "进度id")
    private Long progressId;
    /**
     * 进度奖励id
     */
    @Schema(description = "进度奖励id")
    private Long progressRewardId;
    /**
     * 奖励对象类型 0：邀请人 1：被邀请人
     */
    @Schema(description = "奖励对象类型 0：邀请人 1：被邀请人")
    private Integer rewardTargetType;
    /**
     * 奖励给哪个用户
     */
    @Schema(description = "奖励给哪个用户")
    private Long rewardUserId;
    /**
     * 奖励来源于哪个用户
     */
    @Schema(description = "奖励来源于哪个用户")
    private Long rewardSourceUserId;
    /**
     * 限制奖励给哪个租户
     */
    @Schema(description = "限制奖励给哪个租户")
    private Long rewardTenantId;
    /**
     * 进度code，用于区分是哪个进度的奖励，如注册为：register
     */
    @Schema(description = "进度code，用于区分是哪个进度的奖励，如注册为：register")
    private String progressCode;
    /**
     * 奖励的状态 0：待发放 1：已发放
     */
    @Schema(description = "奖励的状态 0：待发放 1：已发放")
    private Long rewardStatus;

    /**
     * 奖励发放时间
     */
    private Date sendDate;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 最后修改时间
     */
    private Date updateDate;

    /**
     * 商品类型id
     */
    private Long commodityTypeId;

    /**
     * 商品数据量
     */
    private Long commodityNumber;

    /**
     * 版本id（当奖励类型为版本时有）
     */
    private Long packageId;

    /**
     * 版本价格id（当奖励类型为版本时有）
     */
    private Long packagePriceId;
}
