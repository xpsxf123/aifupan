package com.jiuyu.replay.agent.producer.impl;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.agent.entity.InviteUrlCodeEntity;
import com.jiuyu.replay.agent.producer.InviteUrlCodeProducer;
import com.jiuyu.replay.agent.repository.service.InviteUrlCodeService;
import com.jiuyu.replay.agent.vo.InviteUrlPromotionVo;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeBo;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeListVo;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 邀请链接的code
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Service
public class InviteUrlCodeProducerImpl implements InviteUrlCodeProducer {

    @Resource
    private InviteUrlCodeService inviteUrlCodeService;


    @Override
    public PageUtils<InviteUrlCodeListVo> queryPage(InviteUrlCodeListBo inviteUrlCodeListBo) {
        QueryWrapper<InviteUrlCodeEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(inviteUrlCodeListBo.getKeyword())){
            wrapper.like("name", inviteUrlCodeListBo.getKeyword());
        }

        IPage<InviteUrlCodeEntity> iPage = inviteUrlCodeService.page(new Query<InviteUrlCodeEntity>().getPage(inviteUrlCodeListBo.getPage(), inviteUrlCodeListBo.getLimit()), wrapper);

        PageUtils<InviteUrlCodeListVo> pageUtils = new PageUtils<>(inviteUrlCodeListBo.getPage(), inviteUrlCodeListBo.getLimit(), iPage);

        List<InviteUrlCodeEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<InviteUrlCodeListVo> vos = records.stream().map(item -> {
                InviteUrlCodeListVo inviteUrlCodeVo = new InviteUrlCodeListVo();
                BeanUtils.copyProperties(item, inviteUrlCodeVo);
                return inviteUrlCodeVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public InviteUrlCodeInfoVo info(Long id) {

        InviteUrlCodeEntity inviteUrlCodeEntity = inviteUrlCodeService.getById(id);
        if(inviteUrlCodeEntity != null) {
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
            if(inviteUrlCodeInfoVo == null) {
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

    /**
     * 生成随机的链接code
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
    public void update(InviteUrlCodeBo inviteUrlCodeBo) {

        InviteUrlCodeEntity inviteUrlCodeEntity = new InviteUrlCodeEntity();
        BeanUtils.copyProperties(inviteUrlCodeBo, inviteUrlCodeEntity);
        inviteUrlCodeEntity.setUpdateDate(new Date());

        inviteUrlCodeService.updateById(inviteUrlCodeEntity);
    }

    @Override
    public void deleteById(Long id) {

        inviteUrlCodeService.removeById(id);
    }

    @Override
    public InviteUrlCodeInfoVo infoByCode(String code) {
        InviteUrlCodeEntity inviteUrlCodeEntity = this.inviteUrlCodeService.getOne(new QueryWrapper<InviteUrlCodeEntity>().eq("url_code", code));
        if(inviteUrlCodeEntity != null) {
            InviteUrlCodeInfoVo inviteUrlCodeInfoVo = new InviteUrlCodeInfoVo();
            BeanUtils.copyProperties(inviteUrlCodeEntity, inviteUrlCodeInfoVo);
            return inviteUrlCodeInfoVo;
        }
        return null;
    }


    @Override
    public InviteUrlCodeInfoVo getByInviteUrlCode(String inviteUrlCode) {
        InviteUrlCodeEntity inviteUrlCodeEntity = inviteUrlCodeService.getOne(new LambdaQueryWrapper<InviteUrlCodeEntity>()
                .eq(InviteUrlCodeEntity::getUrlCode, inviteUrlCode)
        );
        if(inviteUrlCodeEntity != null) {
            InviteUrlCodeInfoVo inviteUrlCodeInfoVo = new InviteUrlCodeInfoVo();
            BeanUtils.copyProperties(inviteUrlCodeEntity, inviteUrlCodeInfoVo);
            return inviteUrlCodeInfoVo;
        }

        return null;
    }

    @Override
    public void deleteByAgentSaleId(Long agentSaleId) {

        this.inviteUrlCodeService.remove(new QueryWrapper<InviteUrlCodeEntity>().eq("agent_sale_id", agentSaleId));
    }

    @Override
    public InviteUrlCodeInfoVo infoByUserId(Long userId) {

        InviteUrlCodeEntity inviteUrlCodeEntity = this.inviteUrlCodeService.getOne(new QueryWrapper<InviteUrlCodeEntity>().eq("user_id", userId));
        if(inviteUrlCodeEntity != null) {
            InviteUrlCodeInfoVo inviteUrlCodeInfoVo = new InviteUrlCodeInfoVo();
            BeanUtils.copyProperties(inviteUrlCodeEntity, inviteUrlCodeInfoVo);
            return inviteUrlCodeInfoVo;
        }
        return null;
    }


    @Override
    public List<String> selectQuery(List<Long> promotionIds) {
         if (!promotionIds.isEmpty()){
             return inviteUrlCodeService.lambdaQuery().in(InviteUrlCodeEntity::getPromotionId, promotionIds)
                     .eq(InviteUrlCodeEntity::getCodeType, 1)
                     .select(InviteUrlCodeEntity::getUrlCode).list().stream().map(InviteUrlCodeEntity::getUrlCode)
                     .filter(StringUtil::isNotBlank).distinct().toList();
         }
        return List.of();
    }


    @Override
    public List<InviteUrlPromotionVo> getByCodes(List<String> inUrlCodes) {
        List<InviteUrlCodeEntity> list = inviteUrlCodeService.lambdaQuery()
                .in(InviteUrlCodeEntity::getUrlCode, inUrlCodes)
                .list();
        if (!list.isEmpty()){
            return Convert.toList(InviteUrlPromotionVo.class, list);
        }
        return new ArrayList<>();
    }
}

