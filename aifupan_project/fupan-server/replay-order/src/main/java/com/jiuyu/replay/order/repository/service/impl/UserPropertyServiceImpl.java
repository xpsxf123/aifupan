package com.jiuyu.replay.order.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.order.repository.dao.UserPropertyDao;
import com.jiuyu.replay.order.entity.UserPropertyEntity;
import com.jiuyu.replay.order.repository.service.UserPropertyService;


@Service("userPropertyService")
public class UserPropertyServiceImpl extends ServiceImpl<UserPropertyDao, UserPropertyEntity> implements UserPropertyService {



}