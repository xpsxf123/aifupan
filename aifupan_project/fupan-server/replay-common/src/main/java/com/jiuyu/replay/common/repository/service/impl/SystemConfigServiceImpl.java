package com.jiuyu.replay.common.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.common.repository.dao.SystemConfigDao;
import com.jiuyu.replay.common.entity.SystemConfigEntity;
import com.jiuyu.replay.common.repository.service.SystemConfigService;


@Service("systemConfigService")
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigDao, SystemConfigEntity> implements SystemConfigService {



}