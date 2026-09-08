package com.jiuyu.replay.activity.rse;


import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.bo.activity.UserInviteBo;
import com.jiuyu.replay.generic.bo.activity.UserInviteListBo;
import com.jiuyu.replay.generic.vo.activity.UserInviteInfoVo;
import com.jiuyu.replay.generic.vo.activity.UserInviteListVo;

import java.util.List;

/**
 * 用户-邀请关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-27 15:15:49
 */
public interface UserInviteRse {


    /**
     * 用户-邀请关联列表
     * @param userInviteListBo 用户-邀请关联列表查询参数
     * @return
     */
    PageUtils<UserInviteListVo> queryPage(UserInviteListBo userInviteListBo);

    /**
    * 用户-邀请关联信息
    * @param id 用户-邀请关联id
    * @return
    */
    UserInviteInfoVo info(Long id);

    /**
     * 新增用户-邀请关联
     * @param userInviteBo 用户-邀请关联对象
     * @return
     */
     UserInviteInfoVo save(UserInviteBo userInviteBo);

    /**
     * 修改用户-邀请关联
     * @param userInviteBo 用户-邀请关联对象
     * @return
     */
    void update(UserInviteBo userInviteBo);

    /**
     * 删除用户-邀请关联
     * @param id 用户-邀请关联id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据链接code获取邀请记录列表
     * @param urlCode 链接code
     * @return
     */
    List<UserInviteInfoVo> listByCode(String urlCode);

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
    UserInviteBo judgeUserInviteEffective(Long acceptUserId);
}

