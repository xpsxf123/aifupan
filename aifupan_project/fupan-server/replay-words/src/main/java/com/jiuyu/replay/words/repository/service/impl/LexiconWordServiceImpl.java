package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.LexiconWordDao;
import com.jiuyu.replay.words.entity.LexiconWordEntity;
import com.jiuyu.replay.words.repository.service.LexiconWordService;


@Service("lexiconWordService")
public class LexiconWordServiceImpl extends ServiceImpl<LexiconWordDao, LexiconWordEntity> implements LexiconWordService {



}