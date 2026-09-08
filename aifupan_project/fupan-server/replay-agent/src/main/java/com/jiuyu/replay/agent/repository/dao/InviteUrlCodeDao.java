package com.jiuyu.replay.agent.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.agent.entity.InviteUrlCodeEntity;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeAndPromotionName;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 邀请链接的code
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Mapper
public interface InviteUrlCodeDao extends BaseMapper<InviteUrlCodeEntity> {

    /**
     * 获取邀请code和对应的渠道明细
     *
     * @param inviteUrlCodes 邀请码
     * @return 数据
     */
    List<InviteUrlCodeAndPromotionName> inviteCodeAndPromotionName(@Param("inviteUrlCodes") List<String> inviteUrlCodes);
}
