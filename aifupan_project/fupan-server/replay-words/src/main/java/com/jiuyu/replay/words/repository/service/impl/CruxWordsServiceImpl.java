package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.CruxWordsDao;
import com.jiuyu.replay.words.entity.CruxWordsEntity;
import com.jiuyu.replay.words.repository.service.CruxWordsService;


@Service("cruxWordsService")
public class CruxWordsServiceImpl extends ServiceImpl<CruxWordsDao, CruxWordsEntity> implements CruxWordsService {



}