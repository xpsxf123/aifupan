package com.jiuyu.replay.common.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.common.repository.dao.ClientUpdateDao;
import com.jiuyu.replay.common.entity.ClientUpdateEntity;
import com.jiuyu.replay.common.repository.service.ClientUpdateService;


@Service("clientUpdateService")
public class ClientUpdateServiceImpl extends ServiceImpl<ClientUpdateDao, ClientUpdateEntity> implements ClientUpdateService {



}