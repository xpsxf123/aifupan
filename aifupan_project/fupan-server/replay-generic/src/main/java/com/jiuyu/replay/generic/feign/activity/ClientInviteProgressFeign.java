package com.jiuyu.replay.generic.feign.activity;

import com.jiuyu.replay.generic.dto.activity.ClientInviteProgressDto;

import java.util.List;

/**
 * @author RayChou
 * @date 2025/6/3 19:07
 */
public interface ClientInviteProgressFeign {

    /**
     * 获取活动进度根据活动id和进度类型
     *
     * @param activityId         活动id
     * @param inviteProgressType 进度类型 0：被邀请人进度 1：邀请人进度
     * @return
     */
    List<ClientInviteProgressDto> listClientInviteProgressByActivityIdAndInviteProgressType(Long activityId, Integer inviteProgressType);
}
