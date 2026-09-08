package com.jiuyu.replay.order.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.order.entity.OrderDetailEntity;
import com.jiuyu.replay.order.vo.ClintGetDataVo;

import java.util.List;

/**
 * 
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
public interface OrderDetailService extends IService<OrderDetailEntity> {


    /**
     * 统计用户订单总数
     * @param userId
     * @return
     */
    List<OrderDetailEntity> statisticsTotalNumberByUserId(Long userId);

    /**
     * 通过订单id 获取所有订单详情 其类型为非版本的都归为增量包资产
     * @param orderIds
     * @return
     */
    List<ClintGetDataVo> getDetailByOrderIds(List<Long> orderIds);
}

