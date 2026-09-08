package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.WordRuleDao;
import com.jiuyu.replay.words.entity.WordRuleEntity;
import com.jiuyu.replay.words.repository.service.WordRuleService;


@Service("wordRuleService")
public class WordRuleServiceImpl extends ServiceImpl<WordRuleDao, WordRuleEntity> implements WordRuleService {



}