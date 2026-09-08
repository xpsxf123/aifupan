package com.jiuyu.replay.order.repository.dao;

import com.jiuyu.replay.order.entity.OrderPayEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单-支付信息
 * 
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Mapper
public interface OrderPayDao extends BaseMapper<OrderPayEntity> {
	
}
