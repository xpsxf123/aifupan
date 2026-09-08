package com.jiuyu.replay.third.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.third.repository.dao.ProxyIpRecordDao;
import com.jiuyu.replay.third.entity.ProxyIpRecordEntity;
import com.jiuyu.replay.third.repository.service.ProxyIpRecordService;


@Service("proxyIpRecordService")
public class ProxyIpRecordServiceImpl extends ServiceImpl<ProxyIpRecordDao, ProxyIpRecordEntity> implements ProxyIpRecordService {



}