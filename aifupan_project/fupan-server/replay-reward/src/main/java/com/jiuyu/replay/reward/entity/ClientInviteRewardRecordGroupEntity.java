package com.jiuyu.replay.reward.entity;

import lombok.Data;

/**
 * @author RayChou
 * @date 2025/6/5 17:16
 */
@Data
public class ClientInviteRewardRecordGroupEntity extends ClientInviteRewardRecordEntity {

    /**
     * 多个进度奖励id拼接
     */
    private String progressRewardIdStr;
}
