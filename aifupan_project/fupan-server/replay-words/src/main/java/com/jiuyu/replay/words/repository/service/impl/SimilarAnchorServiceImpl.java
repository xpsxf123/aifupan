package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.SimilarAnchorDao;
import com.jiuyu.replay.words.entity.SimilarAnchorEntity;
import com.jiuyu.replay.words.repository.service.SimilarAnchorService;


@Service("similarAnchorService")
public class SimilarAnchorServiceImpl extends ServiceImpl<SimilarAnchorDao, SimilarAnchorEntity> implements SimilarAnchorService {



}