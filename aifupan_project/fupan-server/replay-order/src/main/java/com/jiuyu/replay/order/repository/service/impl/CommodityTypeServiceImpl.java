package com.jiuyu.replay.order.repository.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.order.entity.CommodityTypeEntity;
import com.jiuyu.replay.order.repository.dao.CommodityTypeDao;
import com.jiuyu.replay.order.repository.service.CommodityTypeService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;
import java.util.List;


@Service("commodityTypeService")
public class CommodityTypeServiceImpl extends ServiceImpl<CommodityTypeDao, CommodityTypeEntity> implements CommodityTypeService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public CommodityTypeEntity getById(Serializable id) {

        String redisKey = "replay:commodity-type:id:" + id;
        Object obj = this.redisTemplate.opsForValue().get(redisKey);
        if(obj != null) {
            return (CommodityTypeEntity) obj;
        }

        CommodityTypeEntity commodityTypeEntity = super.getById(id);
        if(commodityTypeEntity != null) {
            // 存到缓存
            this.redisTemplate.opsForValue().set(redisKey, commodityTypeEntity, Duration.ofDays(5));
        }

        return commodityTypeEntity;
    }

    @Override
    public boolean updateById(CommodityTypeEntity entity) {

        CommodityTypeEntity commodityTypeEntity = super.getById(entity.getId());
        if(commodityTypeEntity != null) {
            this.redisTemplate.delete(List.of(
                    "replay:commodity-type:id:" + commodityTypeEntity.getId(),
                    "replay:commodity-type:all"));
        }

        return super.updateById(entity);
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<CommodityTypeEntity> entityList) {

        if(entityList != null && entityList.size() > 0) {
            for (CommodityTypeEntity commodityTypeEntity : entityList) {
                if(commodityTypeEntity != null && commodityTypeEntity.getId() != null) {
                    CommodityTypeEntity commodityType = super.getById(commodityTypeEntity.getId());
                    if(commodityType != null) {
                        this.redisTemplate.delete("replay:commodity-type:id:" + commodityType.getId());
                    }
                }
            }
            this.redisTemplate.delete("replay:commodity-type:all");
        }

        return super.saveOrUpdateBatch(entityList);
    }

    @Override
    public boolean save(CommodityTypeEntity entity) {

        this.redisTemplate.delete("replay:commodity-type:all");

        return super.save(entity);
    }

    @Override
    public boolean removeById(CommodityTypeEntity entity) {

        CommodityTypeEntity commodityTypeEntity = super.getById(entity.getId());
        if(commodityTypeEntity != null) {
            this.redisTemplate.delete(List.of(
                    "replay:commodity-type:id:" + commodityTypeEntity.getId(),
                    "replay:commodity-type:all"));
        }

        return super.removeById(entity);
    }

    @Override
    public List<CommodityTypeEntity> listAllByCache() {

        String redisKey = "replay:commodity-type:all";
        Object obj = this.redisTemplate.opsForValue().get(redisKey);
        if(obj != null) {
            return JSON.parseArray((String) obj, CommodityTypeEntity.class);
        }

        List<CommodityTypeEntity> commodityTypeEntities = list();
        if(commodityTypeEntities != null && commodityTypeEntities.size() > 0) {
            // 存到缓存
            this.redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(commodityTypeEntities), Duration.ofDays(5));
        }

        return commodityTypeEntities;
    }
}