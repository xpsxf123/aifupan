package com.jiuyu.replay.order.repository.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.order.bo.CommodityBo;
import com.jiuyu.replay.order.entity.CommodityEntity;
import com.jiuyu.replay.order.repository.dao.CommodityDao;
import com.jiuyu.replay.order.repository.service.CommodityService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service("commodityService")
public class CommodityServiceImpl extends ServiceImpl<CommodityDao, CommodityEntity> implements CommodityService {


    @Override
    public List<CommodityBo> listAll() {
        List<CommodityEntity> commodityEntityList = list(new LambdaQueryWrapper<>(CommodityEntity.class).select(CommodityEntity::getId, CommodityEntity::getName));
        if (CollectionUtil.isNotEmpty(commodityEntityList)) {
            List<CommodityBo> commodityBoList = new ArrayList<>();
            for (CommodityEntity commodityEntity : commodityEntityList) {
                CommodityBo commodityBo = new CommodityBo();
                BeanUtils.copyProperties(commodityEntity, commodityBo);
                commodityBoList.add(commodityBo);
            }
            return commodityBoList;
        }
        return null;
    }
}