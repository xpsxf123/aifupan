package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.vo.InvitationCodeBatchListVo;
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
public interface InvitationCodeProducer {


    /**
     * 邀请码列表
     * @param invitationCodeListBo 邀请码列表查询参数
     * @return
     */
    PageUtils<InvitationCodeListVo> queryPage(InvitationCodeListBo invitationCodeListBo);

    /**
    * 邀请码信息
    * @param id 邀请码id
    * @return
    */
    InvitationCodeInfoVo info(Long id);

    /**
     * 新增邀请码
     * @param invitationCodeBo 邀请码对象
     * @return
     */
     InvitationCodeInfoVo save(InvitationCodeBo invitationCodeBo);

    /**
     * 修改邀请码
     * @param invitationCodeBo 邀请码对象
     * @return
     */
    void update(InvitationCodeBo invitationCodeBo);

    /**
     * 删除邀请码
     * @param id 邀请码id
     * @return
     */
    void deleteById(Long id);


    /**
     * 批量保存邀请码
     * @param invitationCodeBos 邀请码列表
     */
    void saveBatch(List<InvitationCodeBo> invitationCodeBos);

    /**
     * 根据批次id获取邀请码列表
     * @param codeBatchId
     * @return
     */
    List<InvitationCodeInfoVo> listByCodeBatchId(Long codeBatchId);

    /**
     * 根据批次id集合获取邀请码列表
     * @param codeBatchIds 批次id集合
     * @return
     */
    List<InvitationCodeInfoVo> listByCodeBatchIds(List<Long> codeBatchIds);

    /**
     * 根据邀请码获取邀请码信息
     * @param invitationCode 邀请码
     * @return
     */
    InvitationCodeInfoVo infoByCode(String invitationCode);

    /**
     * 根据用户id获取邀请码列表
     * @param userId 用户id
     * @return
     */
    List<InvitationCodeInfoVo> listByUserId(Long userId);

    /**
     * 使用邀请码
     * @param userId 用户id
     * @param orderId 订单id
     * @param codeId 邀请码id
     */
    void useInvitation(Long userId, Long orderId, Long codeId);


    /**
     * 邀请码导出
     * @param invitationCodeBo
     * @return
     */
    R<List<InvitationCodeListVo>> exportInvitation(List<InvitationCodeBo> invitationCodeBo);

    /**
     * 修改已经导出的邀请码状态
     * @param ids
     */
    void updateIsLssued(List<Long> ids);

    /**
     * 根据邀请码批次id集合获取邀请码数量
     *
     * @param codeBatchIds 邀请码批次id集合
     * @return 邀请码数量
     */
    List<InvitationCodeBatchListVo> codeCountByBatchId(List<Long> codeBatchIds);
}

