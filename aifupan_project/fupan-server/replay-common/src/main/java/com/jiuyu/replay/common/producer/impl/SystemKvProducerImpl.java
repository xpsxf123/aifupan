package com.jiuyu.replay.common.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.bo.SystemKvBo;
import com.jiuyu.replay.common.bo.SystemKvListBo;
import com.jiuyu.replay.common.entity.SystemKvEntity;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.common.vo.SystemKvListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 系统配置的键值对
 *
 * @author lj
 * @email 
 * @date 2025-05-13 16:59:53
 */
@Service
public class SystemKvProducerImpl implements SystemKvProducer {

    @Resource
    private SystemKvService systemKvService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public PageUtils<SystemKvListVo> queryPage(SystemKvListBo systemKvListBo) {
        LambdaQueryWrapper<SystemKvEntity> wrapper = new LambdaQueryWrapper<>();
        if(!StringUtils.isEmpty(systemKvListBo.getKeyword())){
            wrapper.and(w -> w
                    .like(SystemKvEntity::getKvKey, systemKvListBo.getKeyword())
                    .or()
                    .like(SystemKvEntity::getKvValue, systemKvListBo.getKeyword())
                    .or()
                    .like(SystemKvEntity::getRemarks, systemKvListBo.getKeyword())
            );
        }

        IPage<SystemKvEntity> iPage = systemKvService.page(new Query<SystemKvEntity>().getPage(systemKvListBo.getPage(), systemKvListBo.getLimit()), wrapper);

        PageUtils<SystemKvListVo> pageUtils = new PageUtils<>(systemKvListBo.getPage(), systemKvListBo.getLimit(), iPage);

        List<SystemKvEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<SystemKvListVo> vos = records.stream().map(item -> {
                SystemKvListVo systemKvVo = new SystemKvListVo();
                BeanUtils.copyProperties(item, systemKvVo);
                return systemKvVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public SystemKvInfoVo info(Long id) {

        SystemKvEntity systemKvEntity = systemKvService.getById(id);
        if(systemKvEntity != null) {
            SystemKvInfoVo systemKvInfoVo = new SystemKvInfoVo();
            BeanUtils.copyProperties(systemKvEntity, systemKvInfoVo);
            return systemKvInfoVo;
        }

        return null;
    }

    /**
     * 新增系统配置的键值对
     * @param systemKvBo 系统配置的键值对对象
     * @return
     */
     public SystemKvInfoVo save(SystemKvBo systemKvBo) {

         SystemKvEntity byKey = systemKvService.getByKey(systemKvBo.getKvKey());
         if(byKey != null) {
             RRException.create("键已存在");
         }

         SystemKvEntity systemKvEntity = new SystemKvEntity();
         BeanUtils.copyProperties(systemKvBo, systemKvEntity);
         systemKvEntity.setId(SnowflakeManager.nextValue());
         systemKvEntity.setCreateDate(new Date());
         systemKvEntity.setUpdateDate(new Date());

         systemKvService.save(systemKvEntity);

         SystemKvInfoVo systemKvInfoVo = new SystemKvInfoVo();
         BeanUtils.copyProperties(systemKvEntity, systemKvInfoVo);

         this.redisTemplate.delete("replay:system-kv:all");

         return systemKvInfoVo;
     }

    /**
     * 修改系统配置的键值对
     * @param systemKvBo 系统配置的键值对对象
     * @return
     */
    public void update(SystemKvBo systemKvBo) {

        SystemKvEntity byKey = systemKvService.getByKey(systemKvBo.getKvKey());
        if(byKey != null && !byKey.getId().equals(systemKvBo.getId())) {
            RRException.create("键已存在");
        }

        SystemKvEntity systemKvEntity = new SystemKvEntity();
        BeanUtils.copyProperties(systemKvBo, systemKvEntity);
        systemKvEntity.setUpdateDate(new Date());

        systemKvService.updateById(systemKvEntity);
    }

    /**
     * 删除系统配置的键值对
     * @param id 系统配置的键值对id
     * @return
     */
    public void deleteById(Long id) {

        systemKvService.removeById(id);
    }

    @Override
    public SystemKvInfoVo getByKey(String key) {

        SystemKvEntity systemKvEntity = systemKvService.getByKey(key);
        if(systemKvEntity != null) {
            SystemKvInfoVo systemKvInfoVo = new SystemKvInfoVo();
            BeanUtils.copyProperties(systemKvEntity, systemKvInfoVo);
            return systemKvInfoVo;
        }

        return null;
    }

    @Override
    public String getValueByKey(String key, String defaultValue) {
        return systemKvService.getValueByKey(key, defaultValue);
    }

    @Override
    public Integer getValueByKey(String key, Integer defaultValue) {
        return systemKvService.getValueByKey(key, defaultValue);
    }

    @Override
    public Long getValueByKey(String key, Long defaultValue) {
        return systemKvService.getValueByKey(key, defaultValue);
    }
}

