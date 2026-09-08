package com.jiuyu.replay.third.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.third.repository.dao.AiModelDao;
import com.jiuyu.replay.third.entity.AiModelEntity;
import com.jiuyu.replay.third.repository.service.AiModelService;


@Service("aiModelService")
public class AiModelServiceImpl extends ServiceImpl<AiModelDao, AiModelEntity> implements AiModelService {



}