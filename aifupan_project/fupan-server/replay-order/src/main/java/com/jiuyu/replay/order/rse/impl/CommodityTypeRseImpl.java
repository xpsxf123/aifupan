package com.jiuyu.replay.order.rse.impl;

import com.jiuyu.replay.generic.vo.order.CommodityTypeInfoVo;
import com.jiuyu.replay.order.entity.CommodityTypeEntity;
import com.jiuyu.replay.order.repository.service.CommodityTypeService;
import com.jiuyu.replay.order.rse.CommodityTypeRse;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommodityTypeRseImpl implements CommodityTypeRse {

    @Resource
    private CommodityTypeService commodityTypeService;

    @Override
    public List<CommodityTypeInfoVo> listAll() {
        List<CommodityTypeEntity> commodityTypeEntities = this.commodityTypeService.list();
        if(commodityTypeEntities != null && commodityTypeEntities.size() > 0) {
            List<CommodityTypeInfoVo> commodityTypeInfoVos = commodityTypeEntities.stream().map(item -> {
                CommodityTypeInfoVo commodityTypeInfoVo = new CommodityTypeInfoVo();
                BeanUtils.copyProperties(item, commodityTypeInfoVo);
                return commodityTypeInfoVo;
            }).collect(Collectors.toList());

            return commodityTypeInfoVos;
        }
        return null;
    }
}
