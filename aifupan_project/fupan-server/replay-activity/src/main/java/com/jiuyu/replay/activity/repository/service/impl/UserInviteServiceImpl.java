package com.jiuyu.replay.activity.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.activity.repository.dao.UserInviteDao;
import com.jiuyu.replay.activity.entity.UserInviteEntity;
import com.jiuyu.replay.activity.repository.service.UserInviteService;


@Service("userInviteService")
public class UserInviteServiceImpl extends ServiceImpl<UserInviteDao, UserInviteEntity> implements UserInviteService {



}