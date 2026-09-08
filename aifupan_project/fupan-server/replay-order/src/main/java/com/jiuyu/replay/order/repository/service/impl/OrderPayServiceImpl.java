package com.jiuyu.replay.order.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.order.repository.dao.OrderPayDao;
import com.jiuyu.replay.order.entity.OrderPayEntity;
import com.jiuyu.replay.order.repository.service.OrderPayService;


@Service("orderPayService")
public class OrderPayServiceImpl extends ServiceImpl<OrderPayDao, OrderPayEntity> implements OrderPayService {



}