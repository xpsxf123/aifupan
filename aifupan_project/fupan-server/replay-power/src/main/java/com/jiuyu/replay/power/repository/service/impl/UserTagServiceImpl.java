package com.jiuyu.replay.power.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.power.repository.dao.UserTagDao;
import com.jiuyu.replay.power.entity.UserTagEntity;
import com.jiuyu.replay.power.repository.service.UserTagService;


@Service("userTagService")
public class UserTagServiceImpl extends ServiceImpl<UserTagDao, UserTagEntity> implements UserTagService {



}