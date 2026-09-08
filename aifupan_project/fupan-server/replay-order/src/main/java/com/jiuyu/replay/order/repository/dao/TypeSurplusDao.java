package com.jiuyu.replay.order.repository.dao;

import com.jiuyu.replay.order.entity.TypeSurplusEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品类型资产剩余表
 * 
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Mapper
public interface TypeSurplusDao extends BaseMapper<TypeSurplusEntity> {


    /**
     * 获取当前用户剩余数量
     *
     * @param propertyId      资产ID
     * @param commodityTypeId 套餐资产类型ID
     *
     * @return 总剩余数量
     */
    Long currentSurplusCount(@Param("propertyId") Long propertyId, @Param("commodityTypeId") Long commodityTypeId);
}
