package com.jiuyu.replay.agent.api;

import com.jiuyu.replay.agent.bll.InviteUrlCodeBll;
import com.jiuyu.replay.agent.bo.InviteUrlCodeInfoBo;
import com.jiuyu.replay.agent.rse.InviteUrlCodeRse;
import com.jiuyu.replay.generic.dto.activity.InviteUrlCodeInfoDto;
import com.jiuyu.replay.generic.feign.agent.InviteUrlCodeFeign;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeAndPromotionName;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class InviteUrlCodeApi implements InviteUrlCodeFeign {

    @Resource
    private InviteUrlCodeBll inviteUrlCodeBll;
    @Resource
    private InviteUrlCodeRse inviteUrlCodeRse;
    /**
     * 获取用户邀请码信息
     *
     * @param activityId 活动id
     * @param userId 邀请用户id
     * @return
     */
    @Override
    public InviteUrlCodeInfoDto getUserInviteUrlCodeByActivityIdUserId(Long activityId,Long userId) {
        InviteUrlCodeInfoBo inviteUrlCodeInfoBo = inviteUrlCodeRse.getUserInviteUrlCodeByActivityIdUserId(activityId, userId);
        if(Objects.nonNull(inviteUrlCodeInfoBo)){
            InviteUrlCodeInfoDto inviteUrlCodeInfoDto = new InviteUrlCodeInfoDto();
            BeanUtils.copyProperties(inviteUrlCodeInfoBo,inviteUrlCodeInfoDto);
            return inviteUrlCodeInfoDto;
        }
        return null;
    }

    @Override
    public R<InviteUrlCodeInfoVo> getUserInviteUrlCodeInfo() {

        return inviteUrlCodeBll.getUserInviteUrlCodeInfo();
    }

    @Override
    public List<InviteUrlCodeAndPromotionName> inviteCodeAndPromotionName(List<String> inviteUrlCodes) {
        return inviteUrlCodeRse.inviteCodeAndPromotionName(inviteUrlCodes);
    }
}
