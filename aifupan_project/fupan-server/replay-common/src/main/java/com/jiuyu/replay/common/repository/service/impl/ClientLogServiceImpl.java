package com.jiuyu.replay.common.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.entity.ClientLogEntity;
import com.jiuyu.replay.common.repository.dao.ClientLogDao;
import com.jiuyu.replay.common.repository.service.ClientLogService;
import org.springframework.stereotype.Service;

@Service
public class ClientLogServiceImpl extends ServiceImpl<ClientLogDao, ClientLogEntity> implements ClientLogService {
}
