package com.jiuyu.replay.agent.api;

import com.jiuyu.replay.agent.producer.ChannelProducer;
import com.jiuyu.replay.generic.feign.agent.ChannelFeign;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/2 16:47
 */
@Service
@AllArgsConstructor
public class ChannelApi implements ChannelFeign {

    private final ChannelProducer channelProducer;

    @Override
    public Map<Long, String> getChannelParentNameByIds(List<Long> channelIds) {
        return channelProducer.getChannelParentNameByIds(channelIds);
    }
}
