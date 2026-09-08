package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.VideoDataViewingDao;
import com.jiuyu.replay.words.entity.VideoDataViewingEntity;
import com.jiuyu.replay.words.repository.service.VideoDataViewingService;


@Service("videoDataViewingService")
public class VideoDataViewingServiceImpl extends ServiceImpl<VideoDataViewingDao, VideoDataViewingEntity> implements VideoDataViewingService {



}