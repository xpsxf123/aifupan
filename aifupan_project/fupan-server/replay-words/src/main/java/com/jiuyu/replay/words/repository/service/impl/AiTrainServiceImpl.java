package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.AiTrainDao;
import com.jiuyu.replay.words.entity.AiTrainEntity;
import com.jiuyu.replay.words.repository.service.AiTrainService;


@Service("aiTrainService")
public class AiTrainServiceImpl extends ServiceImpl<AiTrainDao, AiTrainEntity> implements AiTrainService {



}