package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.UserVideoAppealDao;
import com.jiuyu.replay.words.entity.UserVideoAppealEntity;
import com.jiuyu.replay.words.repository.service.UserVideoAppealService;


@Service("userVideoAppealService")
public class UserVideoAppealServiceImpl extends ServiceImpl<UserVideoAppealDao, UserVideoAppealEntity> implements UserVideoAppealService {



}