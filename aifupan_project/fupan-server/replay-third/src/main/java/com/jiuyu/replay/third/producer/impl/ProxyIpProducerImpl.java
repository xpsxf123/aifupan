package com.jiuyu.replay.third.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.third.bo.ProxyIpBo;
import com.jiuyu.replay.third.bo.ProxyIpListBo;
import com.jiuyu.replay.third.entity.ProxyIpEntity;
import com.jiuyu.replay.third.producer.ProxyIpProducer;
import com.jiuyu.replay.third.repository.service.ProxyIpService;
import com.jiuyu.replay.third.vo.ProxyIpInfoVo;
import com.jiuyu.replay.third.vo.ProxyIpListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


/**
 * 代理ip提取
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
@Service
public class ProxyIpProducerImpl implements ProxyIpProducer {

    @Resource
    private ProxyIpService proxyIpService;

    private final Lock lock = new ReentrantLock();

    @Override
    public PageUtils<ProxyIpListVo> queryPage(ProxyIpListBo proxyIpListBo) {
        QueryWrapper<ProxyIpEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(proxyIpListBo.getKeyword())){
            wrapper.like("name", proxyIpListBo.getKeyword());
        }

        IPage<ProxyIpEntity> iPage = proxyIpService.page(new Query<ProxyIpEntity>().getPage(proxyIpListBo.getPage(), proxyIpListBo.getLimit()), wrapper);

        PageUtils<ProxyIpListVo> pageUtils = new PageUtils<>(proxyIpListBo.getPage(), proxyIpListBo.getLimit(), iPage);

        List<ProxyIpEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<ProxyIpListVo> vos = records.stream().map(item -> {
                ProxyIpListVo proxyIpVo = new ProxyIpListVo();
                BeanUtils.copyProperties(item, proxyIpVo);
                return proxyIpVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ProxyIpInfoVo info(Long id) {

        ProxyIpEntity proxyIpEntity = proxyIpService.getById(id);
        if(proxyIpEntity != null) {
            ProxyIpInfoVo proxyIpInfoVo = new ProxyIpInfoVo();
            BeanUtils.copyProperties(proxyIpEntity, proxyIpInfoVo);
            return proxyIpInfoVo;
        }

        return null;
    }

    @Override
    public ProxyIpInfoVo save(ProxyIpBo proxyIpBo) {

        ProxyIpEntity proxyIpEntity = new ProxyIpEntity();
        BeanUtils.copyProperties(proxyIpBo, proxyIpEntity);
        proxyIpEntity.setId(SnowflakeManager.nextValue());
        proxyIpEntity.setCreateDate(new Date());
        proxyIpEntity.setUpdateDate(new Date());
        proxyIpEntity.setRemainingNum(proxyIpEntity.getTotalNum());
        proxyIpEntity.setActiveStatus(0);

        proxyIpService.save(proxyIpEntity);

        ProxyIpInfoVo proxyIpInfoVo = new ProxyIpInfoVo();
        BeanUtils.copyProperties(proxyIpEntity, proxyIpInfoVo);

        return proxyIpInfoVo;
     }

    @Override
    public void update(ProxyIpBo proxyIpBo) {

        ProxyIpEntity proxyIpEntity = new ProxyIpEntity();
        BeanUtils.copyProperties(proxyIpBo, proxyIpEntity);
        proxyIpEntity.setUpdateDate(new Date());

        proxyIpService.updateById(proxyIpEntity);
    }

    @Override
    public void deleteById(Long id) {

        proxyIpService.removeById(id);
    }

    @Override
    public ProxyIpInfoVo getUsableProxyIp(Integer validityType) {
        QueryWrapper<ProxyIpEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("active_status", 1);
        wrapper.gt("remaining_num", 0);
        wrapper.last(" limit 1 ");
        if(!StringUtils.isEmpty(validityType)) {
            wrapper.eq("validity_type", validityType);
        }

        ProxyIpEntity proxyIpEntity = this.proxyIpService.getOne(wrapper);

        if(proxyIpEntity != null) {
            ProxyIpInfoVo proxyIpInfoVo = new ProxyIpInfoVo();
            BeanUtils.copyProperties(proxyIpEntity, proxyIpInfoVo);
            return proxyIpInfoVo;
        }

        return null;
    }

    @Override
    public void updateRemainingNum(Long id){
        ProxyIpEntity proxy = this.proxyIpService.getById(id);
        RRException.isNotEmpty(proxy, "代理不存在");
        if (proxy != null){
            if (proxy.getRemainingNum() <= 0) RRException.create("代理IP已用完");
            this.proxyIpService.update(new LambdaUpdateWrapper<ProxyIpEntity>()
                    .eq(ProxyIpEntity::getId, id)
                    .set(ProxyIpEntity::getRemainingNum, proxy.getRemainingNum() - 1)
                    .set(ProxyIpEntity::getUpdateDate, new Date())
            );
        }
    }


}

