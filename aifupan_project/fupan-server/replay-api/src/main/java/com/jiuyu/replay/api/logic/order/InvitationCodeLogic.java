package com.jiuyu.replay.api.logic.order;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.order.vo.InvitationCodeListVo;
import com.jiuyu.replay.order.vo.InvitationCodeInfoVo;
import com.jiuyu.replay.order.bo.InvitationCodeBo;
import com.jiuyu.replay.order.bo.InvitationCodeListBo;

import java.util.List;


/**
 * 邀请码
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
public interface InvitationCodeLogic {


    /**
     * 邀请码列表
     * @param invitationCodeListBo 邀请码列表查询参数
     * @return
     */
    R<PageUtils<InvitationCodeListVo>> queryPage(InvitationCodeListBo invitationCodeListBo);

    /**
    * 邀请码信息
    * @param id 邀请码id
    * @return
    */
    R<InvitationCodeInfoVo> info(Long id);

    /**
     * 新增邀请码
     * @param invitationCodeBo 邀请码对象
     * @return
     */
    R<String> save(InvitationCodeBo invitationCodeBo);

    /**
     * 修改邀请码
     * @param invitationCodeBo 邀请码对象
     * @return
     */
    R<String> update(InvitationCodeBo invitationCodeBo);

    /**
     * 删除邀请码
     * @param id 邀请码id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 使用邀请码
     * @param code 邀请码
     * @return
     */
    R<String> exchange(String code);



    /**
     * 邀请码导出
     * @param invitationCodeBo
     * @return
     */
    R<List<InvitationCodeListVo>> exportInvitation(List<InvitationCodeBo> invitationCodeBo);

    /**
     * 修改已经导出的邀请码状态
     * @param ids
     * @return
     */
    R<String> updateIsLssued(List<Long> ids);
}

