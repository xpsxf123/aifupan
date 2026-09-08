package com.jiuyu.replay.agent.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.agent.entity.ChannelEntity;
import com.jiuyu.replay.agent.repository.dao.ChannelDao;
import com.jiuyu.replay.agent.repository.service.ChannelService;
import org.springframework.stereotype.Service;


@Service("channelService")
public class ChannelServiceImpl extends ServiceImpl<ChannelDao, ChannelEntity> implements ChannelService {



}