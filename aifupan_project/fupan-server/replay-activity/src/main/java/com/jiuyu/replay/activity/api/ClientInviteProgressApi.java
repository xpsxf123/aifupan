package com.jiuyu.replay.activity.api;

import com.jiuyu.replay.activity.bll.ClientInviteProgressBll;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressBo;
import com.jiuyu.replay.generic.dto.activity.ClientInviteProgressDto;
import com.jiuyu.replay.generic.feign.activity.ClientInviteProgressFeign;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author RayChou
 * @date 2025/6/3 19:09
 */
@Service
public class ClientInviteProgressApi implements ClientInviteProgressFeign {

    @Resource
    private ClientInviteProgressBll clientInviteProgressBll;


    @Override
    public List<ClientInviteProgressDto> listClientInviteProgressByActivityIdAndInviteProgressType(Long activityId, Integer inviteProgressType) {
        List<ClientInviteProgressBo> clientInviteProgressBoList = clientInviteProgressBll.listClientInviteProgressByActivityIdAndInviteProgressType(activityId,inviteProgressType);
        return BeanConvertUtils.convertList(clientInviteProgressBoList, ClientInviteProgressDto.class);
    }
}
