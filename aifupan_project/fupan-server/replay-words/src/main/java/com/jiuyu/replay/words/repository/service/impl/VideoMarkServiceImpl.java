package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.VideoMarkDao;
import com.jiuyu.replay.words.entity.VideoMarkEntity;
import com.jiuyu.replay.words.repository.service.VideoMarkService;


@Service("videoMarkService")
public class VideoMarkServiceImpl extends ServiceImpl<VideoMarkDao, VideoMarkEntity> implements VideoMarkService {



}