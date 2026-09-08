package com.jiuyu.replay.third.producer.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.third.bo.ProxyIpRecordBo;
import com.jiuyu.replay.third.bo.ProxyIpRecordListBo;
import com.jiuyu.replay.third.entity.ProxyIpRecordEntity;
import com.jiuyu.replay.third.producer.ProxyIpRecordProducer;
import com.jiuyu.replay.third.repository.service.ProxyIpRecordService;
import com.jiuyu.replay.third.vo.ProxyIpRecordInfoVo;
import com.jiuyu.replay.third.vo.ProxyIpRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 代理ip提取记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
@Service
public class ProxyIpRecordProducerImpl implements ProxyIpRecordProducer {

    @Resource
    private ProxyIpRecordService proxyIpRecordService;


    @Override
    public PageUtils<ProxyIpRecordListVo> queryPage(ProxyIpRecordListBo proxyIpRecordListBo) {
        QueryWrapper<ProxyIpRecordEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(proxyIpRecordListBo.getKeyword())){
            if(proxyIpRecordListBo.getUserIds() != null && proxyIpRecordListBo.getUserIds().size() > 0) {
                wrapper.in("user_id", proxyIpRecordListBo.getUserIds());
            }else {
                wrapper.eq("user_id", 0);
            }
        }

        IPage<ProxyIpRecordEntity> iPage = proxyIpRecordService.page(new Query<ProxyIpRecordEntity>().getPage(proxyIpRecordListBo.getPage(), proxyIpRecordListBo.getLimit()), wrapper);

        PageUtils<ProxyIpRecordListVo> pageUtils = new PageUtils<>(proxyIpRecordListBo.getPage(), proxyIpRecordListBo.getLimit(), iPage);

        List<ProxyIpRecordEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<ProxyIpRecordListVo> vos = records.stream().map(item -> {
                ProxyIpRecordListVo proxyIpRecordVo = new ProxyIpRecordListVo();
                BeanUtils.copyProperties(item, proxyIpRecordVo);
                return proxyIpRecordVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ProxyIpRecordInfoVo info(Long id) {

        ProxyIpRecordEntity proxyIpRecordEntity = proxyIpRecordService.getById(id);
        if(proxyIpRecordEntity != null) {
            ProxyIpRecordInfoVo proxyIpRecordInfoVo = new ProxyIpRecordInfoVo();
            BeanUtils.copyProperties(proxyIpRecordEntity, proxyIpRecordInfoVo);
            return proxyIpRecordInfoVo;
        }

        return null;
    }

    @Override
    public ProxyIpRecordInfoVo save(ProxyIpRecordBo proxyIpRecordBo) {

         ProxyIpRecordEntity proxyIpRecordEntity = new ProxyIpRecordEntity();
         BeanUtils.copyProperties(proxyIpRecordBo, proxyIpRecordEntity);
         proxyIpRecordEntity.setId(SnowflakeManager.nextValue());
         proxyIpRecordEntity.setCreateDate(new Date());
         proxyIpRecordEntity.setUpdateDate(new Date());

         proxyIpRecordService.save(proxyIpRecordEntity);

         ProxyIpRecordInfoVo proxyIpRecordInfoVo = new ProxyIpRecordInfoVo();
         BeanUtils.copyProperties(proxyIpRecordEntity, proxyIpRecordInfoVo);

         return proxyIpRecordInfoVo;
     }

    @Override
    public void update(ProxyIpRecordBo proxyIpRecordBo) {

        ProxyIpRecordEntity proxyIpRecordEntity = new ProxyIpRecordEntity();
        BeanUtils.copyProperties(proxyIpRecordBo, proxyIpRecordEntity);
        proxyIpRecordEntity.setUpdateDate(new Date());

        proxyIpRecordService.updateById(proxyIpRecordEntity);
    }

    @Override
    public void deleteById(Long id) {

        proxyIpRecordService.removeById(id);
    }

    @Override
    public ProxyIpRecordInfoVo saveRecord(Long userId, Long tenantId, String ip, String port, Integer ipEffectiveTime, Long proxyId) {

        ProxyIpRecordEntity proxyIpRecordEntity = new ProxyIpRecordEntity();
        proxyIpRecordEntity.setId(SnowflakeManager.nextValue());
        proxyIpRecordEntity.setCreateDate(new Date());
        proxyIpRecordEntity.setUpdateDate(new Date());
        proxyIpRecordEntity.setUserId(userId);
        proxyIpRecordEntity.setTenantId(tenantId);
        proxyIpRecordEntity.setIpStr(ip);
        proxyIpRecordEntity.setPortStr(port);
        proxyIpRecordEntity.setIpEffectiveTime(ipEffectiveTime);
        proxyIpRecordEntity.setExtractDate(new Date());
        proxyIpRecordEntity.setProxyId(proxyId);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, ipEffectiveTime);
        proxyIpRecordEntity.setValidityDate(calendar.getTime());

        proxyIpRecordService.save(proxyIpRecordEntity);

        ProxyIpRecordInfoVo proxyIpRecordInfoVo = new ProxyIpRecordInfoVo();
        BeanUtils.copyProperties(proxyIpRecordEntity, proxyIpRecordInfoVo);

        // 设置有效期比实际有效期短一分钟，避免过期导致不能用
        calendar.add(Calendar.MINUTE, -1);
        proxyIpRecordInfoVo.setValidityDate(calendar.getTime());
        proxyIpRecordInfoVo.setExpireTime(calendar.getTimeInMillis());

        return proxyIpRecordInfoVo;
    }

    @Override
    public Integer countDayUserNum(Long userId, Long tenantId, Long proxyId) {
        ProxyIpRecordEntity one = proxyIpRecordService.getOne(new QueryWrapper<ProxyIpRecordEntity>()
                .select("ifnull(count(*), 0) as ipEffectiveTime")
                .eq("date_format(extract_date, '%Y-%m-%d')", DateUtil.today())
                .lambda()
                .eq(ProxyIpRecordEntity::getUserId, userId)
                .eq(ProxyIpRecordEntity::getTenantId, tenantId)
                .eq(ProxyIpRecordEntity::getProxyId, proxyId)
        );
        if (one != null){
            return  one.getIpEffectiveTime();
        }
        return null;
    }
}

