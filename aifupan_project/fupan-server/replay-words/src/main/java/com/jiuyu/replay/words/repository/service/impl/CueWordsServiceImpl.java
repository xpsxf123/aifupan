package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.words.entity.CueWordsEntity;
import com.jiuyu.replay.words.repository.dao.CueWordsDao;
import com.jiuyu.replay.words.repository.service.CueWordsService;
import org.springframework.stereotype.Service;


@Service("cueWordsService")
public class CueWordsServiceImpl extends ServiceImpl<CueWordsDao, CueWordsEntity> implements CueWordsService {



}