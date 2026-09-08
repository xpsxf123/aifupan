package com.jiuyu.replay.power.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.power.repository.dao.UserRemarkDao;
import com.jiuyu.replay.power.entity.UserRemarkEntity;
import com.jiuyu.replay.power.repository.service.UserRemarkService;


@Service("userRemarkService")
public class UserRemarkServiceImpl extends ServiceImpl<UserRemarkDao, UserRemarkEntity> implements UserRemarkService {



}