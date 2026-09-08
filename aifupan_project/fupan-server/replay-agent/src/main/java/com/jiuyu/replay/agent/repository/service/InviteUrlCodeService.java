package com.jiuyu.replay.agent.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.agent.entity.InviteUrlCodeEntity;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeAndPromotionName;

import java.util.List;

/**
 * 邀请链接的code
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
public interface InviteUrlCodeService extends IService<InviteUrlCodeEntity> {

    /**
     * 获取邀请code和对应的渠道明细
     *
     * @param inviteUrlCodes 邀请码
     * @return 数据
     */
    List<InviteUrlCodeAndPromotionName> inviteCodeAndPromotionName(List<String> inviteUrlCodes);
}

