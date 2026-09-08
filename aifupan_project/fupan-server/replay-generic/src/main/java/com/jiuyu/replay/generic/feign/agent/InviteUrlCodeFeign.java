package com.jiuyu.replay.generic.feign.agent;

import com.jiuyu.replay.generic.dto.activity.InviteUrlCodeInfoDto;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeAndPromotionName;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import com.jiuyu.replay.generic.vo.common.R;

import java.util.List;

public interface InviteUrlCodeFeign {

    /**
     * 获取用户邀请码信息
     *
     * @param activityId 活动id
     * @param userId     邀请用户id
     * @return
     */
    InviteUrlCodeInfoDto getUserInviteUrlCodeByActivityIdUserId(Long activityId, Long userId);


    /**
     * 获取当前用户的邀请链接信息
     *
     * @return
     */
    R<InviteUrlCodeInfoVo> getUserInviteUrlCodeInfo();

    /**
     * 获取邀请code和对应的渠道明细
     *
     * @param inviteUrlCodes 邀请码
     * @return 数据
     */
    List<InviteUrlCodeAndPromotionName> inviteCodeAndPromotionName(List<String> inviteUrlCodes);
}
