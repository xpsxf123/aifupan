package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.TotalSocketMessageDao;
import com.jiuyu.replay.words.entity.TotalSocketMessageEntity;
import com.jiuyu.replay.words.repository.service.TotalSocketMessageService;


@Service("totalSocketMessageService")
public class TotalSocketMessageServiceImpl extends ServiceImpl<TotalSocketMessageDao, TotalSocketMessageEntity> implements TotalSocketMessageService {



}