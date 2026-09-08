package com.jiuyu.replay.activity.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.activity.repository.dao.ClientInviteProgressDao;
import com.jiuyu.replay.activity.entity.ClientInviteProgressEntity;
import com.jiuyu.replay.activity.repository.service.ClientInviteProgressService;


@Service("clientInviteProgressService")
public class ClientInviteProgressServiceImpl extends ServiceImpl<ClientInviteProgressDao, ClientInviteProgressEntity> implements ClientInviteProgressService {



}