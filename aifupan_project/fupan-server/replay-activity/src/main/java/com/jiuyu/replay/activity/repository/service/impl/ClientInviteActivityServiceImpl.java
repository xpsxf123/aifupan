package com.jiuyu.replay.activity.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.activity.repository.dao.ClientInviteActivityDao;
import com.jiuyu.replay.activity.entity.ClientInviteActivityEntity;
import com.jiuyu.replay.activity.repository.service.ClientInviteActivityService;


@Service("clientInviteActivityService")
public class ClientInviteActivityServiceImpl extends ServiceImpl<ClientInviteActivityDao, ClientInviteActivityEntity> implements ClientInviteActivityService {



}