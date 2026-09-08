package com.jiuyu.replay.common.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.common.repository.dao.ClientFileDao;
import com.jiuyu.replay.common.entity.ClientFileEntity;
import com.jiuyu.replay.common.repository.service.ClientFileService;


@Service("clientFileService")
public class ClientFileServiceImpl extends ServiceImpl<ClientFileDao, ClientFileEntity> implements ClientFileService {



}