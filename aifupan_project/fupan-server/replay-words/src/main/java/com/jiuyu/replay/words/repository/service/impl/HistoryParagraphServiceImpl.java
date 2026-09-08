package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.HistoryParagraphDao;
import com.jiuyu.replay.words.entity.HistoryParagraphEntity;
import com.jiuyu.replay.words.repository.service.HistoryParagraphService;


@Service("historyParagraphService")
public class HistoryParagraphServiceImpl extends ServiceImpl<HistoryParagraphDao, HistoryParagraphEntity> implements HistoryParagraphService {



}