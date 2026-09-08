package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.SocketCollectMessageDao;
import com.jiuyu.replay.words.entity.SocketCollectMessageEntity;
import com.jiuyu.replay.words.repository.service.SocketCollectMessageService;


@Service("socketCollectMessageService")
public class SocketCollectMessageServiceImpl extends ServiceImpl<SocketCollectMessageDao, SocketCollectMessageEntity> implements SocketCollectMessageService {



}