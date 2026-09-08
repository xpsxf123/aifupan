package com.jiuyu.replay.power.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.power.repository.dao.BindingAccountDao;
import com.jiuyu.replay.power.entity.BindingAccountEntity;
import com.jiuyu.replay.power.repository.service.BindingAccountService;


@Service("bindingAccountService")
public class BindingAccountServiceImpl extends ServiceImpl<BindingAccountDao, BindingAccountEntity> implements BindingAccountService {



}