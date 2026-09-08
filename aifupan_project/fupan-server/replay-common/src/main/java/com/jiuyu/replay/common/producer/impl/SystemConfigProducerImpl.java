package com.jiuyu.replay.common.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.bo.SystemConfigBo;
import com.jiuyu.replay.common.bo.SystemConfigListBo;
import com.jiuyu.replay.common.entity.SystemConfigEntity;
import com.jiuyu.replay.common.producer.SystemConfigProducer;
import com.jiuyu.replay.common.repository.service.SystemConfigService;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.vo.SystemConfigInfoVo;
import com.jiuyu.replay.common.vo.SystemConfigListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 系统配置
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-24 18:55:01
 */
@Service
public class SystemConfigProducerImpl implements SystemConfigProducer {

    @Resource
    private SystemConfigService systemConfigService;


    @Override
    public PageUtils<SystemConfigListVo> queryPage(SystemConfigListBo systemConfigListBo) {
        QueryWrapper<SystemConfigEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(systemConfigListBo.getKeyword())){
            wrapper.like("name", systemConfigListBo.getKeyword());
        }

        IPage<SystemConfigEntity> iPage = systemConfigService.page(new Query<SystemConfigEntity>().getPage(systemConfigListBo.getPage(), systemConfigListBo.getLimit()), wrapper);

        PageUtils<SystemConfigListVo> pageUtils = new PageUtils<>(systemConfigListBo.getPage(), systemConfigListBo.getLimit(), iPage);

        List<SystemConfigEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<SystemConfigListVo> vos = records.stream().map(item -> {
                SystemConfigListVo systemConfigVo = new SystemConfigListVo();
                BeanUtils.copyProperties(item, systemConfigVo);
                return systemConfigVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public SystemConfigInfoVo info(Long id) {

        SystemConfigEntity systemConfigEntity = systemConfigService.getById(id);
        if(systemConfigEntity != null) {
            SystemConfigInfoVo systemConfigInfoVo = new SystemConfigInfoVo();
            BeanUtils.copyProperties(systemConfigEntity, systemConfigInfoVo);
            return systemConfigInfoVo;
        }

        return null;
    }

    @Override
    public SystemConfigInfoVo save(SystemConfigBo systemConfigBo) {

         SystemConfigEntity systemConfigEntity = new SystemConfigEntity();
         BeanUtils.copyProperties(systemConfigBo, systemConfigEntity);
         systemConfigEntity.setId(SnowflakeManager.nextValue());
         systemConfigEntity.setCreateDate(new Date());
         systemConfigEntity.setUpdateDate(new Date());

         systemConfigService.save(systemConfigEntity);

         SystemConfigInfoVo systemConfigInfoVo = new SystemConfigInfoVo();
         BeanUtils.copyProperties(systemConfigEntity, systemConfigInfoVo);

         return systemConfigInfoVo;
     }

    @Override
    public void update(SystemConfigBo systemConfigBo) {

        SystemConfigEntity systemConfigEntity = new SystemConfigEntity();
        BeanUtils.copyProperties(systemConfigBo, systemConfigEntity);
        systemConfigEntity.setUpdateDate(new Date());

        systemConfigService.updateById(systemConfigEntity);
    }

    @Override
    public void deleteById(Long id) {

        systemConfigService.removeById(id);
    }


}

