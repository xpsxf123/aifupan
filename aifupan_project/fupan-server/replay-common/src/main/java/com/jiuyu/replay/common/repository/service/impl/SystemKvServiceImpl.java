package com.jiuyu.replay.common.repository.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.entity.SystemKvEntity;
import com.jiuyu.replay.common.repository.dao.SystemKvDao;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;


@Service("systemKvService")
public class SystemKvServiceImpl extends ServiceImpl<SystemKvDao, SystemKvEntity> implements SystemKvService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public SystemKvEntity getById(Serializable id) {

        String redisKey = "replay:system-kv:id:" + id;
        Object obj = this.redisTemplate.opsForValue().get(redisKey);
        if(obj != null) {
            return (SystemKvEntity) obj;
        }

        SystemKvEntity systemKvEntity = super.getById(id);
        if(systemKvEntity != null) {
            // 存到缓存
            this.redisTemplate.opsForValue().set(redisKey, systemKvEntity, Duration.ofDays(5));
        }

        return systemKvEntity;
    }

    @Override
    public SystemKvEntity getByKey(String key) {

        String redisKey = "replay:system-kv:key:" + key;
        Object obj = this.redisTemplate.opsForValue().get(redisKey);
        if(obj != null) {
            return (SystemKvEntity) obj;
        }

        SystemKvEntity systemKvEntity = getOne(new LambdaQueryWrapper<SystemKvEntity>().eq(SystemKvEntity::getKvKey, key));
        if(systemKvEntity != null) {
            // 存到缓存
            this.redisTemplate.opsForValue().set(redisKey, systemKvEntity, Duration.ofDays(5));
        }

        return systemKvEntity;
    }

    @Override
    public String getValueByKey(String key, String defaultValue) {
        SystemKvEntity systemKvEntity = getByKey(key);
        if (systemKvEntity != null) {
            return systemKvEntity.getKvValue();
        }
        return defaultValue;
    }

    @Override
    public Integer getValueByKey(String key, Integer defaultValue) {
        SystemKvEntity systemKvEntity = getByKey(key);
        if (systemKvEntity != null) {
            return NumberUtil.parseInt(systemKvEntity.getKvValue(), defaultValue);
        }
        return defaultValue;
    }

    @Override
    public Long getValueByKey(String key, Long defaultValue) {
        SystemKvEntity systemKvEntity = getByKey(key);
        if (systemKvEntity != null) {
            return NumberUtil.parseLong(systemKvEntity.getKvValue(), defaultValue);
        }
        return defaultValue;
    }

    @Override
    public List<SystemKvEntity> list() {

        String redisKey = "replay:system-kv:all";
        Object obj = this.redisTemplate.opsForValue().get(redisKey);
        if(obj != null) {
            return JSON.parseArray((String) obj, SystemKvEntity.class);
        }

        List<SystemKvEntity> systemKvEntities = super.list();
        if(systemKvEntities != null && systemKvEntities.size() > 0) {
            // 存到缓存
            this.redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(systemKvEntities), Duration.ofDays(5));
        }

        return systemKvEntities;
    }

    @Override
    public boolean removeById(Serializable id) {

        SystemKvEntity systemKvEntity = super.getById(id);
        if(systemKvEntity != null) {
            this.redisTemplate.delete(List.of(
                    "replay:system-kv:id:" + systemKvEntity.getId(),
                    "replay:system-kv:key:" + systemKvEntity.getKvKey(),
                    "replay:system-kv:all"));
        }

        return super.removeById(id);
    }

    @Override
    public boolean removeById(SystemKvEntity entity) {

        SystemKvEntity systemKvEntity = super.getById(entity.getId());
        if(systemKvEntity != null) {
            this.redisTemplate.delete(List.of(
                    "replay:system-kv:id:" + systemKvEntity.getId(),
                    "replay:system-kv:key:" + systemKvEntity.getKvKey(),
                    "replay:system-kv:all"));
        }

        return super.removeById(entity);
    }

    @Override
    public boolean updateById(SystemKvEntity entity) {

        SystemKvEntity systemKvEntity = super.getById(entity.getId());
        if(systemKvEntity != null) {
            this.redisTemplate.delete(List.of(
                    "replay:system-kv:id:" + systemKvEntity.getId(),
                    "replay:system-kv:key:" + systemKvEntity.getKvKey(),
                    "replay:system-kv:all"));
        }

        return super.updateById(entity);
    }

    @Override
    public boolean saveOrUpdate(SystemKvEntity entity) {

        if(entity != null && entity.getId() != null) {
            SystemKvEntity systemKvEntity = super.getById(entity.getId());
            if(systemKvEntity != null) {
                this.redisTemplate.delete(List.of(
                        "replay:system-kv:id:" + systemKvEntity.getId(),
                        "replay:system-kv:key:" + systemKvEntity.getKvKey(),
                        "replay:system-kv:all"));
            }
        }

        return super.saveOrUpdate(entity);
    }
}