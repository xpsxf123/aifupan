package com.jiuyu.replay.generic.feign.agent;

import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/2 16:46
 */
public interface ChannelFeign {


    /**
     * 根据渠道id集合获取渠道名称
     *
     * @param channelIds 渠道id集合
     * @return 集合获取渠道名称
     */
    Map<Long, String> getChannelParentNameByIds(List<Long> channelIds);

}
