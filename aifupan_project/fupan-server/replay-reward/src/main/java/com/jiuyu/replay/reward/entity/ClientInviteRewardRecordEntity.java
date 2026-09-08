package com.jiuyu.replay.reward.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 进度奖励记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 17:03:13
 */
@Data
@TableName("tb_client_invite_reward_record")
public class ClientInviteRewardRecordEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    /**
     * 活动id
     */
    private Long activityId;
    /**
     * 进度id
     */
    private Long progressId;
    /**
     * 进度奖励id
     */
    private Long progressRewardId;
    /**
     * 奖励对象类型 0：邀请人 1：被邀请人
     */
    private Integer rewardTargetType;
    /**
     * 奖励给哪个用户
     */
    private Long rewardUserId;
    /**
     * 奖励来源于哪个用户
     */
    private Long rewardSourceUserId;
    /**
     * 限制奖励给哪个租户
     */
    private Long rewardTenantId;
    /**
     * 进度code，用于区分是哪个进度的奖励，如注册为：register
     */
    private String progressCode;
    /**
     * 奖励的状态 0：待发放 1：已发放
     */
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
     * 是否已删除
     */
    private Integer isDeleted;


}
