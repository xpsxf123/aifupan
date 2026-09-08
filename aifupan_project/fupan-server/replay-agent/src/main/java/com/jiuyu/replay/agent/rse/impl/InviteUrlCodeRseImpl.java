package com.jiuyu.replay.agent.rse.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.agent.bo.InviteUrlCodeInfoBo;
import com.jiuyu.replay.agent.entity.InviteUrlCodeEntity;
import com.jiuyu.replay.agent.repository.service.InviteUrlCodeService;
import com.jiuyu.replay.agent.rse.InviteUrlCodeRse;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeBo;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeInfoConditionBo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeAndPromotionName;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InviteUrlCodeRseImpl implements InviteUrlCodeRse {

    @Resource
    private InviteUrlCodeService inviteUrlCodeService;

    @Override
    public InviteUrlCodeInfoVo infoByCondition(InviteUrlCodeInfoConditionBo inviteUrlCodeInfoConditionBo) {

        QueryWrapper<InviteUrlCodeEntity> wrapper = new QueryWrapper<>();
        if (inviteUrlCodeInfoConditionBo.getAgentId() != null) {
            wrapper.eq("agent_id", inviteUrlCodeInfoConditionBo.getAgentId());
        }
        if (inviteUrlCodeInfoConditionBo.getPromotionId() != null) {
            wrapper.eq("promotion_id", inviteUrlCodeInfoConditionBo.getPromotionId());
        }
        if (inviteUrlCodeInfoConditionBo.getAgentSaleId() != null) {
            wrapper.eq("agent_sale_id", inviteUrlCodeInfoConditionBo.getAgentSaleId());
        }
        if (inviteUrlCodeInfoConditionBo.getUserId() != null) {
            wrapper.eq("user_id", inviteUrlCodeInfoConditionBo.getUserId());
        }
        if (inviteUrlCodeInfoConditionBo.getActivityId() != null) {
            wrapper.eq("activity_id", inviteUrlCodeInfoConditionBo.getActivityId());
        }
        if (inviteUrlCodeInfoConditionBo.getCodeType() != null) {
            wrapper.eq("code_type", inviteUrlCodeInfoConditionBo.getCodeType());
        }
        if (inviteUrlCodeInfoConditionBo.getTenantId() != null) {
            wrapper.eq("tenant_id", inviteUrlCodeInfoConditionBo.getTenantId());
        }

        InviteUrlCodeEntity inviteUrlCodeEntity = this.inviteUrlCodeService.getOne(wrapper);
        if (inviteUrlCodeEntity != null) {
            InviteUrlCodeInfoVo inviteUrlCodeInfoVo = new InviteUrlCodeInfoVo();
            BeanUtils.copyProperties(inviteUrlCodeEntity, inviteUrlCodeInfoVo);
            return inviteUrlCodeInfoVo;
        }

        return null;
    }

    @Override
    public InviteUrlCodeInfoVo save(InviteUrlCodeBo inviteUrlCodeBo) {

        String code = createUrlCode();
        while (true) {
            InviteUrlCodeInfoVo inviteUrlCodeInfoVo = this.infoByCode(code);
            if (inviteUrlCodeInfoVo == null) {
                break;
            }
            code = createUrlCode();
        }

        InviteUrlCodeEntity inviteUrlCodeEntity = new InviteUrlCodeEntity();
        BeanUtils.copyProperties(inviteUrlCodeBo, inviteUrlCodeEntity);
        inviteUrlCodeEntity.setId(SnowflakeManager.nextValue());
        inviteUrlCodeEntity.setUrlCode(code);
        inviteUrlCodeEntity.setCreateDate(new Date());
        inviteUrlCodeEntity.setUpdateDate(new Date());

        inviteUrlCodeService.save(inviteUrlCodeEntity);

        InviteUrlCodeInfoVo inviteUrlCodeInfoVo = new InviteUrlCodeInfoVo();
        BeanUtils.copyProperties(inviteUrlCodeEntity, inviteUrlCodeInfoVo);

        return inviteUrlCodeInfoVo;
    }

    @Override
    public InviteUrlCodeInfoVo infoByCode(String code) {
        InviteUrlCodeEntity inviteUrlCodeEntity = this.inviteUrlCodeService.getOne(new QueryWrapper<InviteUrlCodeEntity>().eq("url_code", code));
        if (inviteUrlCodeEntity != null) {
            InviteUrlCodeInfoVo inviteUrlCodeInfoVo = new InviteUrlCodeInfoVo();
            BeanUtils.copyProperties(inviteUrlCodeEntity, inviteUrlCodeInfoVo);
            return inviteUrlCodeInfoVo;
        }
        return null;
    }

    /**
     * 获取用户邀请码信息
     *
     * @param activityId 活动id
     * @param userId     邀请用户id
     * @return
     */
    @Override
    public InviteUrlCodeInfoBo getUserInviteUrlCodeByActivityIdUserId(Long activityId, Long userId) {

        InviteUrlCodeEntity inviteUrlCodeEntity = inviteUrlCodeService.getOne(new LambdaQueryWrapper<InviteUrlCodeEntity>().select(InviteUrlCodeEntity::getId, InviteUrlCodeEntity::getUrlCode, InviteUrlCodeEntity::getUserId, InviteUrlCodeEntity::getSubUserId, InviteUrlCodeEntity::getActivityId, InviteUrlCodeEntity::getCodeType, InviteUrlCodeEntity::getTenantId).eq(InviteUrlCodeEntity::getActivityId, activityId).eq(InviteUrlCodeEntity::getUserId, userId));
        if (Objects.nonNull(inviteUrlCodeEntity)) {
            InviteUrlCodeInfoBo inviteUrlCodeInfoBo = new InviteUrlCodeInfoBo();
            BeanUtils.copyProperties(inviteUrlCodeEntity, inviteUrlCodeInfoBo);
            return inviteUrlCodeInfoBo;
        }
        return null;
    }

    /**
     * 生成随机的链接code
     *
     * @return
     */
    private String createUrlCode() {
        String allChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();

        StringBuilder sb = new StringBuilder(10);

        for (int i = 0; i < 10; i++) {
            int index = random.nextInt(allChars.length());
            sb.append(allChars.charAt(index));
        }

        return sb.toString();
    }

    @Override
    public List<InviteUrlCodeAndPromotionName> inviteCodeAndPromotionName(List<String> inviteUrlCodes) {
        if (ObjectUtil.isEmpty(inviteUrlCodes)) {
            return new ArrayList<>();
        }
        return inviteUrlCodeService.inviteCodeAndPromotionName(inviteUrlCodes);
    }
}
