package com.jiuyu.replay.order.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.order.repository.dao.IncrementDao;
import com.jiuyu.replay.order.entity.IncrementEntity;
import com.jiuyu.replay.order.repository.service.IncrementService;


@Service("incrementService")
public class IncrementServiceImpl extends ServiceImpl<IncrementDao, IncrementEntity> implements IncrementService {



}