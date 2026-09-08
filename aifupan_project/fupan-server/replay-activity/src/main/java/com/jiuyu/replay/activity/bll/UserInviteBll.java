package com.jiuyu.replay.activity.bll;

import com.jiuyu.replay.activity.rse.UserInviteRse;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.bo.activity.UserInviteBo;
import com.jiuyu.replay.generic.bo.activity.UserInviteListBo;
import com.jiuyu.replay.generic.vo.activity.UserInviteInfoVo;
import com.jiuyu.replay.generic.vo.activity.UserInviteListVo;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 用户-邀请关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-27 15:15:49
 */
@Component
public class UserInviteBll {

    @Resource
    private UserInviteRse userInviteRse;


    /**
     * 用户-邀请关联列表
     *
     * @param userInviteListBo 用户-邀请关联列表查询参数
     * @return
     */
    public R<PageUtils<UserInviteListVo>> queryPage(UserInviteListBo userInviteListBo) {

        return R.ok("获取成功", userInviteRse.queryPage(userInviteListBo));
    }

    /**
     * 用户-邀请关联信息
     *
     * @param id 用户-邀请关联id
     * @return
     */
    public R<UserInviteInfoVo> info(Long id) {

        UserInviteInfoVo userInviteInfoVo = userInviteRse.info(id);
        return R.ok("获取成功", userInviteInfoVo);
    }

    /**
     * 新增用户-邀请关联
     *
     * @param userInviteBo 用户-邀请关联对象
     * @return
     */
    public R<String> save(UserInviteBo userInviteBo) {

        UserInviteInfoVo userInviteInfoVo = userInviteRse.save(userInviteBo);
        return R.ok("添加成功");
    }

    /**
     * 修改用户-邀请关联
     *
     * @param userInviteBo 用户-邀请关联对象
     * @return
     */
    public R<String> update(UserInviteBo userInviteBo) {

        userInviteRse.update(userInviteBo);
        return R.ok("修改成功");
    }

    /**
     * 删除用户-邀请关联
     *
     * @param id 用户-邀请关联id
     * @return
     */
    public R<String> delete(Long id) {

        userInviteRse.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 根据链接code获取邀请记录列表
     *
     * @param urlCode 链接code
     * @return
     */
    public R<List<UserInviteInfoVo>> listByCode(String urlCode) {

        List<UserInviteInfoVo> userInviteInfoVoList = this.userInviteRse.listByCode(urlCode);

        return R.ok(userInviteInfoVoList);
    }

    /**
     * 获取总邀请人数根据邀请用户id
     *
     * @param inviteUserId 邀请用户id
     * @return
     */
    public Long getInviteUserNumberByInviteUserId(Long inviteUserId) {
        return userInviteRse.getInviteUserNumberByInviteUserId(inviteUserId);
    }

    /**
     * 判断用户邀请奖励是否生效
     *
     * @param acceptUserId 被邀请用户id
     * @return
     */
    public UserInviteBo judgeUserInviteEffective(Long acceptUserId) {
        return userInviteRse.judgeUserInviteEffective(acceptUserId);
    }
}

