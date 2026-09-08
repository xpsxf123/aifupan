package com.jiuyu.replay.generic.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author RayChou
 * @date 2025/6/3 14:23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInviteMqDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 奖励规则code
     */
    private String rewardRuleCode;

    /**
     * 被邀请人
     */
    private Long acceptUserId;

    /**
     * 指纹信息
     */
    private String fingerprint;

}
