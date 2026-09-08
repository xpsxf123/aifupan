package com.jiuyu.replay.api.logic.order;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.order.vo.InvitationCodeBatchListVo;
import com.jiuyu.replay.order.vo.InvitationCodeBatchInfoVo;
import com.jiuyu.replay.order.bo.InvitationCodeBatchBo;
import com.jiuyu.replay.order.bo.InvitationCodeBatchListBo;
import com.jiuyu.replay.order.vo.InvitationCodeListVo;
import com.jiuyu.replay.order.vo.TypeConsumptionInfoVo;

import java.util.List;


/**
 * 邀请码-批次
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
public interface InvitationCodeBatchLogic {


    /**
     * 邀请码-批次列表
     * @param invitationCodeBatchListBo 邀请码-批次列表查询参数
     * @return
     */
    R<PageUtils<InvitationCodeBatchListVo>> queryPage(InvitationCodeBatchListBo invitationCodeBatchListBo);

    /**
    * 邀请码-批次信息
    * @param id 邀请码-批次id
    * @return
    */
    R<InvitationCodeBatchInfoVo> info(Long id);

    /**
     * 新增邀请码-批次
     * @param invitationCodeBatchBo 邀请码-批次对象
     * @return
     */
    R<String> save(InvitationCodeBatchBo invitationCodeBatchBo);

    /**
     * 修改邀请码-批次
     * @param invitationCodeBatchBo 邀请码-批次对象
     * @return
     */
    R<String> update(InvitationCodeBatchBo invitationCodeBatchBo);

    /**
     * 删除邀请码-批次
     * @param id 邀请码-批次id
     * @return
     */
    R<String> delete(Long id);

    /**
     * 根据邀请码批次导出该批次的所有邀请码
     * @param batchId
     * @return
     */
    R<List<InvitationCodeListVo>> invitationByBatchId(Long batchId);

    /**
     * 只查询激活码信息
     * @return
     */
    R<InvitationCodeBatchInfoVo> checkOnlyActivationCode();

    /**
     * 获取邀请中的资源列表
     * @param id
     * @return
     */
    R<List<TypeConsumptionInfoVo>> getTypeConsumptionById(Long id);
}

