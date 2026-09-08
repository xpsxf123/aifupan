package com.jiuyu.replay.order.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.order.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

    int getIncrementalSubNum(@Param("userId") Long userId, @Param("commodityTypeCode") String commodityTypeCode);

    /**
     * 判断用户当前有效订单是否为付费订单
     */
    Integer hasPaidUser(@Param("orderUserId") long orderUserId);

    /**
     * 查用户当前激活租户id（跨域读 power 域 tb_user，用于订单创建时固化 tenant_id）
     * 仅主账号下单，其 active_tenant_id 恒为自身租户
     */
    Long getActiveTenantIdByUserId(@Param("userId") Long userId);
}
