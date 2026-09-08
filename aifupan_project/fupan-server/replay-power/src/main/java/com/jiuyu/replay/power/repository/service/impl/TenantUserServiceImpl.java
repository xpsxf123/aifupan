package com.jiuyu.replay.power.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.power.repository.dao.TenantUserDao;
import com.jiuyu.replay.power.entity.TenantUserEntity;
import com.jiuyu.replay.power.repository.service.TenantUserService;


@Service("tenantUserService")
public class TenantUserServiceImpl extends ServiceImpl<TenantUserDao, TenantUserEntity> implements TenantUserService {



}