package com.jiuyu.replay.agent.rse;

import com.jiuyu.replay.agent.bo.InviteUrlCodeInfoBo;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeBo;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeInfoConditionBo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeAndPromotionName;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;

import java.util.List;

public interface InviteUrlCodeRse {

    /**
     * 根据条件获取链接code信息
     * @param inviteUrlCodeInfoConditionBo 查询条件
     * @return
     */
    InviteUrlCodeInfoVo infoByCondition(InviteUrlCodeInfoConditionBo inviteUrlCodeInfoConditionBo);

    /**
     * 新增邀请链接的code
     * @param inviteUrlCodeBo 邀请链接的code对象
     * @return
     */
    InviteUrlCodeInfoVo save(InviteUrlCodeBo inviteUrlCodeBo);

    /**
     * 根据code获取code信息
     * @param code code
     * @return
     */
    InviteUrlCodeInfoVo infoByCode(String code);

    /**
     * 获取用户邀请码信息
     *
     * @param activityId 活动id
     * @param userId 邀请用户id
     * @return
     */
    InviteUrlCodeInfoBo getUserInviteUrlCodeByActivityIdUserId(Long activityId, Long userId);

    /**
     * 获取邀请code和对应的渠道明细
     *
     * @param inviteUrlCodes 邀请码
     * @return 数据
     */
    List<InviteUrlCodeAndPromotionName> inviteCodeAndPromotionName(List<String> inviteUrlCodes);
}
