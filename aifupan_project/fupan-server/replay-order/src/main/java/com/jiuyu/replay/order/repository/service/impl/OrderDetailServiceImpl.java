package com.jiuyu.replay.order.repository.service.impl;

import com.jiuyu.replay.order.vo.ClintGetDataVo;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.order.repository.dao.OrderDetailDao;
import com.jiuyu.replay.order.entity.OrderDetailEntity;
import com.jiuyu.replay.order.repository.service.OrderDetailService;

import java.util.List;


@Service("orderDetailService")
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailDao, OrderDetailEntity> implements OrderDetailService {

    @Override
    public List<OrderDetailEntity> statisticsTotalNumberByUserId(Long userId) {
        return baseMapper.statisticsTotalNumberByUserId(userId);
    }


    @Override
    public List<ClintGetDataVo> getDetailByOrderIds(List<Long> orderIds) {
        return baseMapper.getDetailByOrderIds(orderIds);
    }


}