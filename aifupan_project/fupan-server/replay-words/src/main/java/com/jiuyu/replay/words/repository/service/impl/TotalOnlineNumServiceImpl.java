package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.TotalOnlineNumDao;
import com.jiuyu.replay.words.entity.TotalOnlineNumEntity;
import com.jiuyu.replay.words.repository.service.TotalOnlineNumService;


@Service("totalOnlineNumService")
public class TotalOnlineNumServiceImpl extends ServiceImpl<TotalOnlineNumDao, TotalOnlineNumEntity> implements TotalOnlineNumService {



}