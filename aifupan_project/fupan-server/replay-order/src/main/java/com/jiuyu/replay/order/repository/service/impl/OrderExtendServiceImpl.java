package com.jiuyu.replay.order.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.order.repository.dao.OrderExtendDao;
import com.jiuyu.replay.order.entity.OrderExtendEntity;
import com.jiuyu.replay.order.repository.service.OrderExtendService;


@Service("orderExtendService")
public class OrderExtendServiceImpl extends ServiceImpl<OrderExtendDao, OrderExtendEntity> implements OrderExtendService {



}