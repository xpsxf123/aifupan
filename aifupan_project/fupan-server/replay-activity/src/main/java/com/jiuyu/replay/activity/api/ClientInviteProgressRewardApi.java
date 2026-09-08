package com.jiuyu.replay.activity.api;

import com.jiuyu.replay.activity.bll.ClientInviteProgressRewardBll;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.generic.dto.activity.ClientInviteProgressRewardDto;
import com.jiuyu.replay.generic.feign.activity.ClientInviteProgressRewardFeign;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressRewardInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class ClientInviteProgressRewardApi implements ClientInviteProgressRewardFeign {

    @Resource
    private ClientInviteProgressRewardBll clientInviteProgressRewardBll;

    @Override
    public R<List<ClientInviteProgressRewardInfoVo>> listByProgressRewardIds(Collection<Long> progressRewardIds) {

        return clientInviteProgressRewardBll.listByProgressRewardIds(progressRewardIds);
    }

    @Override
    public List<ClientInviteProgressRewardDto> listByProgressIds(List<Long> progressIds) {
        return BeanConvertUtils.convertList(clientInviteProgressRewardBll.listByProgressIds(progressIds), ClientInviteProgressRewardDto.class);
    }
}
