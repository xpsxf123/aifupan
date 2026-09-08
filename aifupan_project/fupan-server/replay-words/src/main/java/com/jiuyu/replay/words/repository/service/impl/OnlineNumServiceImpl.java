package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.OnlineNumDao;
import com.jiuyu.replay.words.entity.OnlineNumEntity;
import com.jiuyu.replay.words.repository.service.OnlineNumService;


@Service("onlineNumService")
public class OnlineNumServiceImpl extends ServiceImpl<OnlineNumDao, OnlineNumEntity> implements OnlineNumService {



}