package com.jiuyu.replay.order.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.order.entity.TypeSurplusEntity;

/**
 * 商品类型资产剩余表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
public interface TypeSurplusService extends IService<TypeSurplusEntity> {

    /**
     * 获取当前用户剩余数量
     *
     * @param propertyId      资产ID
     * @param commodityTypeId 套餐资产类型ID
     *
     * @return 总剩余数量
     */
    Long currentSurplusCount(Long propertyId, Long commodityTypeId);
}

