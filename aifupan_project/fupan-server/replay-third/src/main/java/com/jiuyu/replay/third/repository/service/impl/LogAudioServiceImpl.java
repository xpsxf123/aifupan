package com.jiuyu.replay.third.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.third.repository.dao.LogAudioDao;
import com.jiuyu.replay.third.entity.LogAudioEntity;
import com.jiuyu.replay.third.repository.service.LogAudioService;


@Service("logAudioService")
public class LogAudioServiceImpl extends ServiceImpl<LogAudioDao, LogAudioEntity> implements LogAudioService {



}