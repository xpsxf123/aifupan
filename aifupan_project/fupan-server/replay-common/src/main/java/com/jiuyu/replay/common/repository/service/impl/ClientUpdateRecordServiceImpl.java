package com.jiuyu.replay.common.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.common.repository.dao.ClientUpdateRecordDao;
import com.jiuyu.replay.common.entity.ClientUpdateRecordEntity;
import com.jiuyu.replay.common.repository.service.ClientUpdateRecordService;


@Service("clientUpdateRecordService")
public class ClientUpdateRecordServiceImpl extends ServiceImpl<ClientUpdateRecordDao, ClientUpdateRecordEntity> implements ClientUpdateRecordService {



}