package com.jiuyu.replay.order.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.order.repository.dao.TypeSurplusDao;
import com.jiuyu.replay.order.entity.TypeSurplusEntity;
import com.jiuyu.replay.order.repository.service.TypeSurplusService;


@Service("typeSurplusService")
public class TypeSurplusServiceImpl extends ServiceImpl<TypeSurplusDao, TypeSurplusEntity> implements TypeSurplusService {


    /**
     * 获取当前用户剩余数量
     *
     * @param propertyId      资产ID
     * @param commodityTypeId 套餐资产类型ID
     *
     * @return 总剩余数量
     */
    @Override
    public Long currentSurplusCount(Long propertyId, Long commodityTypeId) {
        Long count = super.baseMapper.currentSurplusCount(propertyId, commodityTypeId);
        if (count == null) {
            return 0L;
        }
        return count;
    }
}