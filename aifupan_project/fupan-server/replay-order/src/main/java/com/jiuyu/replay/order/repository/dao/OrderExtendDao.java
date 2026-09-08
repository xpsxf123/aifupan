package com.jiuyu.replay.order.repository.dao;

import com.jiuyu.replay.order.entity.OrderExtendEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单的扩展表
 * 
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-07-05 09:56:24
 */
@Mapper
public interface OrderExtendDao extends BaseMapper<OrderExtendEntity> {
	
}
