package com.jiuyu.replay.generic.feign.activity;

import com.jiuyu.replay.generic.vo.activity.ClientInviteActivityInfoVo;
import com.jiuyu.replay.generic.vo.common.R;

public interface ActivityFeign {

    /**
     * 根据活动id获取启用中的活动
     * @param id 活动id
     * @return
     */
    R<ClientInviteActivityInfoVo> infoActivateById(Long id);
}
