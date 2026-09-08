package com.jiuyu.replay.order.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.order.repository.dao.OrderDao;
import com.jiuyu.replay.order.entity.OrderEntity;
import com.jiuyu.replay.order.repository.service.OrderService;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {



}