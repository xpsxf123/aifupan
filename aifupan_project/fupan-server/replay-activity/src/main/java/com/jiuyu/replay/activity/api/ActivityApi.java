package com.jiuyu.replay.activity.api;

import com.jiuyu.replay.activity.bll.ClientInviteActivityBll;
import com.jiuyu.replay.activity.constant.ActivityProperties;
import com.jiuyu.replay.generic.feign.activity.ActivityFeign;
import com.jiuyu.replay.generic.vo.activity.ClientInviteActivityInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ActivityApi implements ActivityFeign {

    @Resource
    private ClientInviteActivityBll clientInviteActivityBll;
    @Resource
    private ActivityProperties activityProperties;

    @Override
    public R<ClientInviteActivityInfoVo> infoActivateById(Long id) {

        if(id == null) {
            id = activityProperties.getClientDefaultInviteActivityId();
        }

        return clientInviteActivityBll.infoActivateById(id);
    }
}
