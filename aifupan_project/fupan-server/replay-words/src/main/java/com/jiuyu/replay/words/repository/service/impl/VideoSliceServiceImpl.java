package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.VideoSliceDao;
import com.jiuyu.replay.words.entity.VideoSliceEntity;
import com.jiuyu.replay.words.repository.service.VideoSliceService;


@Service("videoSliceService")
public class VideoSliceServiceImpl extends ServiceImpl<VideoSliceDao, VideoSliceEntity> implements VideoSliceService {



}