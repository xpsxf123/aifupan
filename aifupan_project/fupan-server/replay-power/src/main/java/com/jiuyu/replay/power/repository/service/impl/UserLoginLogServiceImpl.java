package com.jiuyu.replay.power.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.power.repository.dao.UserLoginLogDao;
import com.jiuyu.replay.power.entity.UserLoginLogEntity;
import com.jiuyu.replay.power.repository.service.UserLoginLogService;


@Service("userLoginLogService")
public class UserLoginLogServiceImpl extends ServiceImpl<UserLoginLogDao, UserLoginLogEntity> implements UserLoginLogService {



}