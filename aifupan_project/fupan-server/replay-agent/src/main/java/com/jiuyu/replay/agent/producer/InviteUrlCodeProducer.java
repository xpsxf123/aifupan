package com.jiuyu.replay.agent.producer;

import com.jiuyu.replay.agent.vo.InviteUrlPromotionVo;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeBo;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeListVo;

import java.util.List;


/**
 * 邀请链接的code
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
public interface InviteUrlCodeProducer {


    /**
     * 邀请链接的code列表
     * @param inviteUrlCodeListBo 邀请链接的code列表查询参数
     * @return
     */
    PageUtils<InviteUrlCodeListVo> queryPage(InviteUrlCodeListBo inviteUrlCodeListBo);

    /**
    * 邀请链接的code信息
    * @param id 邀请链接的codeid
    * @return
    */
    InviteUrlCodeInfoVo info(Long id);

    /**
     * 新增邀请链接的code
     * @param inviteUrlCodeBo 邀请链接的code对象
     * @return
     */
     InviteUrlCodeInfoVo save(InviteUrlCodeBo inviteUrlCodeBo);

    /**
     * 修改邀请链接的code
     * @param inviteUrlCodeBo 邀请链接的code对象
     * @return
     */
    void update(InviteUrlCodeBo inviteUrlCodeBo);

    /**
     * 删除邀请链接的code
     * @param id 邀请链接的codeid
     * @return
     */
    void deleteById(Long id);

    /**
     * 根据code获取code信息
     * @param code code
     * @return
     */
    InviteUrlCodeInfoVo infoByCode(String code);

    /**
     * 根据code获取信息
     * @param inviteUrlCode
     * @return
     */
    InviteUrlCodeInfoVo getByInviteUrlCode(String inviteUrlCode);

    /**
     * 根据代理商销售id删除邀请链接的code
     * @param agentSaleId 代理商销售id
     */
    void deleteByAgentSaleId(Long agentSaleId);

    List<InviteUrlPromotionVo> getByCodes(List<String> inUrlCodes);

    /**
     * 根据用户id获取邀请链接的code信息
     * @param userId 用户id
     */
    InviteUrlCodeInfoVo infoByUserId(Long userId);

    List<String> selectQuery(List<Long> promotionIds);
}

