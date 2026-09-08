package com.jiuyu.replay.common.repository.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.entity.ClientVersionEntity;
import com.jiuyu.replay.common.repository.dao.ClientVersionDao;
import com.jiuyu.replay.common.repository.service.ClientVersionService;
import org.springframework.stereotype.Service;

@Service("clientVersionService")
public class ClientVersionServiceImpl extends ServiceImpl<ClientVersionDao, ClientVersionEntity> implements ClientVersionService {



}