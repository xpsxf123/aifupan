package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bo.InvitationCodeBatchBo;
import com.jiuyu.replay.order.bo.InvitationCodeBatchListBo;
import com.jiuyu.replay.order.vo.InvitationCodeBatchInfoVo;
import com.jiuyu.replay.order.vo.InvitationCodeBatchListVo;
import com.jiuyu.replay.order.vo.InvitationCodeListVo;

import java.util.Collection;
import java.util.List;


/**
 * 邀请码-批次
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
public interface InvitationCodeBatchProducer {


    /**
     * 邀请码-批次列表
     * @param invitationCodeBatchListBo 邀请码-批次列表查询参数
     * @return
     */
    PageUtils<InvitationCodeBatchListVo> queryPage(InvitationCodeBatchListBo invitationCodeBatchListBo);

    /**
    * 邀请码-批次信息
    * @param id 邀请码-批次id
    * @return
    */
    InvitationCodeBatchInfoVo info(Long id);

    /**
     * 新增邀请码-批次
     * @param invitationCodeBatchBo 邀请码-批次对象
     * @return
     */
     InvitationCodeBatchInfoVo save(InvitationCodeBatchBo invitationCodeBatchBo);

    /**
     * 修改邀请码-批次
     * @param invitationCodeBatchBo 邀请码-批次对象
     * @return
     */
    void update(InvitationCodeBatchBo invitationCodeBatchBo);

    /**
     * 删除邀请码-批次
     * @param id 邀请码-批次id
     * @return
     */
    void deleteById(Long id);

    /**
     * 根据批次id集合获取批次列表
     * @param codeBatchIds 批次id集合
     * @return
     */
    List<InvitationCodeBatchInfoVo> listByIds(Collection<Long> codeBatchIds);

    /**
     * 根据邀请码批次导出该批次的所有邀请码
     * @param batchId
     * @return
     */
    R<List<InvitationCodeListVo>> invitationByBatchId(Long batchId);
}

