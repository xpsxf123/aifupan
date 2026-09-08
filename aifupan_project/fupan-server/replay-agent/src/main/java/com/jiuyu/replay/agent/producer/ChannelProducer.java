package com.jiuyu.replay.agent.producer;

import com.jiuyu.replay.agent.bo.ChannelBo;
import com.jiuyu.replay.agent.bo.ChannelListBo;
import com.jiuyu.replay.agent.vo.ChannelInfoVo;
import com.jiuyu.replay.agent.vo.ChannelListVo;
import com.jiuyu.replay.agent.vo.ChannelTreeVo;
import com.jiuyu.replay.generic.utils.PageUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * 用户来源渠道表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:14
 */
public interface ChannelProducer {


    /**
     * 用户来源渠道表列表
     * @param channelListBo 用户来源渠道表列表查询参数
     * @return
     */
    PageUtils<ChannelListVo> queryPage(ChannelListBo channelListBo);

    /**
    * 用户来源渠道表信息
    * @param id 用户来源渠道表id
    * @return
    */
    ChannelInfoVo info(Long id);

    /**
     * 新增用户来源渠道表
     * @param channelBo 用户来源渠道表对象
     * @return
     */
     ChannelInfoVo save(ChannelBo channelBo);

    /**
     * 修改用户来源渠道表
     * @param channelBo 用户来源渠道表对象
     * @return
     */
    void update(ChannelBo channelBo);

    /**
     * 删除用户来源渠道表
     * @param id 用户来源渠道表id
     * @return
     */
    void deleteById(Long id);

    /**
     * 获取来源渠道列表(树型结构)
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    List<ChannelTreeVo> listTree(Integer childrenNotNull);

    /**
     * 查询当前渠道是否有子渠道
     * @param id 渠道ID
     * @return
     */
    Long haveChildren(Long id);

    /**
     * 根据渠道id集合获取用户来源渠道列表
     * @param channelIds 渠道id集合
     * @return
     */
    List<ChannelInfoVo> listByIds(Collection<Long> channelIds);

    /**
     * 获取渠道和渠道下的所有子渠道id
     * @param channelId 渠道id
     * @return
     */
    List<Long> getChannelAllChildId(Long channelId);

    List<ChannelInfoVo> selectByChannelIds(List<Long> list);

    /**
     * 根据渠道id集合获取渠道名称
     *
     * @param channelIds 渠道id集合
     * @return 集合获取渠道名称
     */
    Map<Long, String> getChannelParentNameByIds(List<Long> channelIds);
}

