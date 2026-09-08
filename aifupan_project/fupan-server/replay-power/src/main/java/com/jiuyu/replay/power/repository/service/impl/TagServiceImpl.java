package com.jiuyu.replay.power.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.power.repository.dao.TagDao;
import com.jiuyu.replay.power.entity.TagEntity;
import com.jiuyu.replay.power.repository.service.TagService;


@Service("tagService")
public class TagServiceImpl extends ServiceImpl<TagDao, TagEntity> implements TagService {



}