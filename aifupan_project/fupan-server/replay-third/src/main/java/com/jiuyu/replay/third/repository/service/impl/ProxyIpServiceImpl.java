package com.jiuyu.replay.third.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.third.repository.dao.ProxyIpDao;
import com.jiuyu.replay.third.entity.ProxyIpEntity;
import com.jiuyu.replay.third.repository.service.ProxyIpService;


@Service("proxyIpService")
public class ProxyIpServiceImpl extends ServiceImpl<ProxyIpDao, ProxyIpEntity> implements ProxyIpService {



}