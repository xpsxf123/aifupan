package com.jiuyu.replay.third.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.third.repository.dao.AudioLogDao;
import com.jiuyu.replay.third.entity.AudioLogEntity;
import com.jiuyu.replay.third.repository.service.AudioLogService;


@Service("audioLogService")
public class AudioLogServiceImpl extends ServiceImpl<AudioLogDao, AudioLogEntity> implements AudioLogService {



}