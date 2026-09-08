package com.jiuyu.replay.power.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.power.repository.dao.SalesDao;
import com.jiuyu.replay.power.entity.SalesEntity;
import com.jiuyu.replay.power.repository.service.SalesService;


@Service("salesService")
public class SalesServiceImpl extends ServiceImpl<SalesDao, SalesEntity> implements SalesService {



}