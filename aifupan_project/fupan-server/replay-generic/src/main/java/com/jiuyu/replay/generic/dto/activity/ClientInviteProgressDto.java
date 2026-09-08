package com.jiuyu.replay.generic.dto.activity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author RayChou
 * @date 2025/6/3 19:08
 */
@Data
public class ClientInviteProgressDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;
    /**
     * 关联的邀请活动id
     */
    private Long inviteActivityId;
    /**
     * 进度类型 0：被邀请人进度 1：邀请人进度
     */
    private Integer inviteProgressType;
    /**
     * 邀请进度code
     */
    private String inviteProgressCode;
    /**
     * 邀请进度值
     */
    private Integer inviteProgressValue;
    /**
     * 邀请进度标题
     */
    private String inviteProgressTitle;
    /**
     * 邀请进度要求
     */
    private String inviteProgressRequire;
    /**
     * 启用状态 0：停用 1：启用中
     */
    private Integer inviteProgressStatus;
}
