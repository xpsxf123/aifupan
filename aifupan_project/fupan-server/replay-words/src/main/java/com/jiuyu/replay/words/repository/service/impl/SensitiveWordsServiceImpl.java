package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.SensitiveWordsDao;
import com.jiuyu.replay.words.entity.SensitiveWordsEntity;
import com.jiuyu.replay.words.repository.service.SensitiveWordsService;


@Service("sensitiveWordsService")
public class SensitiveWordsServiceImpl extends ServiceImpl<SensitiveWordsDao, SensitiveWordsEntity> implements SensitiveWordsService {



}