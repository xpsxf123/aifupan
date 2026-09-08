package com.jiuyu.replay.generic.feign.activity;

import com.jiuyu.replay.generic.dto.activity.UserInviteDto;
import com.jiuyu.replay.generic.vo.activity.UserInviteInfoVo;
import com.jiuyu.replay.generic.vo.common.R;

import java.util.List;

public interface UserInviteFeign {

    /**
     * 根据链接code获取邀请记录列表
     *
     * @param urlCode 链接code
     * @return
     */
    R<List<UserInviteInfoVo>> listByCode(String urlCode);

    /**
     * 获取总邀请人数根据邀请用户id
     *
     * @param inviteUserId 邀请用户id
     * @return
     */
    Long getInviteUserNumberByInviteUserId(Long inviteUserId);

    /**
     * 判断用户邀请奖励是否生效
     *
     * @param acceptUserId 被邀请用户id
     * @return
     */
    UserInviteDto judgeUserInviteEffective(Long acceptUserId);

}
