package com.jiuyu.replay.activity.api;

import com.jiuyu.replay.activity.bll.UserInviteBll;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.generic.bo.activity.UserInviteBo;
import com.jiuyu.replay.generic.dto.activity.UserInviteDto;
import com.jiuyu.replay.generic.feign.activity.UserInviteFeign;
import com.jiuyu.replay.generic.vo.activity.UserInviteInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserInviteApi implements UserInviteFeign {

    @Resource
    private UserInviteBll userInviteBll;

    @Override
    public R<List<UserInviteInfoVo>> listByCode(String urlCode) {

        return userInviteBll.listByCode(urlCode);
    }

    /**
     * 获取总邀请人数根据邀请用户id
     *
     * @param inviteUserId 邀请用户id
     * @return
     */
    @Override
    public Long getInviteUserNumberByInviteUserId(Long inviteUserId) {
        return userInviteBll.getInviteUserNumberByInviteUserId(inviteUserId);
    }

    /**
     * 判断用户邀请奖励是否生效
     *
     * @param acceptUserId 被邀请用户id
     * @return
     */
    @Override
    public UserInviteDto judgeUserInviteEffective(Long acceptUserId) {
        UserInviteBo userInviteBo = userInviteBll.judgeUserInviteEffective(acceptUserId);
        return BeanConvertUtils.convert(userInviteBo, UserInviteDto.class);
    }
}
