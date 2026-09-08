package com.jiuyu.replay.api.logic.agent;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeBo;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeListBo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeListVo;
import com.jiuyu.replay.generic.vo.common.R;


/**
 * 邀请链接的code
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
public interface InviteUrlCodeLogic {


    /**
     * 邀请链接的code列表
     * @param inviteUrlCodeListBo 邀请链接的code列表查询参数
     * @return
     */
    R<PageUtils<InviteUrlCodeListVo>> queryPage(InviteUrlCodeListBo inviteUrlCodeListBo);

    /**
    * 邀请链接的code信息
    * @param id 邀请链接的codeid
    * @return
    */
    R<InviteUrlCodeInfoVo> info(Long id);

    /**
     * 新增邀请链接的code
     * @param inviteUrlCodeBo 邀请链接的code对象
     * @return
     */
    R<String> save(InviteUrlCodeBo inviteUrlCodeBo);

    /**
     * 修改邀请链接的code
     * @param inviteUrlCodeBo 邀请链接的code对象
     * @return
     */
    R<String> update(InviteUrlCodeBo inviteUrlCodeBo);

    /**
     * 删除邀请链接的code
     * @param id 邀请链接的codeid
     * @return
     */
    R<String> delete(Long id);


    /**
     * 根据邀请链接的code获取信息
     * @param code 邀请链接的code
     * @return
     */
    R<InviteUrlCodeInfoVo> infoByCode(String code);

    /**
     * 获取当前用户的邀请链接
     * @return
     */
    R<String> getUserInviteUrl();
}

