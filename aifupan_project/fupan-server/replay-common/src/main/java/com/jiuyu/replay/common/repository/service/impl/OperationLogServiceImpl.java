package com.jiuyu.replay.common.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.common.repository.dao.OperationLogDao;
import com.jiuyu.replay.common.entity.OperationLogEntity;
import com.jiuyu.replay.common.repository.service.OperationLogService;


@Service("operationLogService")
public class OperationLogServiceImpl extends ServiceImpl<OperationLogDao, OperationLogEntity> implements OperationLogService {



}