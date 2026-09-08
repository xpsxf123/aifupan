package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.LexiconDao;
import com.jiuyu.replay.words.entity.LexiconEntity;
import com.jiuyu.replay.words.repository.service.LexiconService;


@Service("lexiconService")
public class LexiconServiceImpl extends ServiceImpl<LexiconDao, LexiconEntity> implements LexiconService {



}