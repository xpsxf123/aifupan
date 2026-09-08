package com.jiuyu.replay.generic.feign.activity;

import com.jiuyu.replay.generic.dto.activity.ClientInviteProgressRewardDto;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressRewardInfoVo;
import com.jiuyu.replay.generic.vo.common.R;

import java.util.Collection;
import java.util.List;

public interface ClientInviteProgressRewardFeign {

    /**
     * 根据进度奖励id集合获取进度奖励列表
     * @param progressRewardIds 进度奖励id集合
     * @return
     */
    R<List<ClientInviteProgressRewardInfoVo>> listByProgressRewardIds(Collection<Long> progressRewardIds);

    /**
     * 获取奖励规则根据进度ids
     * @param progressIds 进度ids
     * @return
     */
    List<ClientInviteProgressRewardDto> listByProgressIds(List<Long> progressIds);
}
