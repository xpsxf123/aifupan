package com.jiuyu.replay.api.logic.agent;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.agent.vo.ChannelListVo;
import com.jiuyu.replay.agent.vo.ChannelInfoVo;
import com.jiuyu.replay.agent.bo.ChannelBo;
import com.jiuyu.replay.agent.bo.ChannelListBo;
import com.jiuyu.replay.agent.vo.ChannelTreeVo;

import java.util.List;


/**
 * 用户来源渠道表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:14
 */
public interface ChannelLogic {


    /**
     * 用户来源渠道表列表
     * @param channelListBo 用户来源渠道表列表查询参数
     * @return
     */
    R<PageUtils<ChannelListVo>> queryPage(ChannelListBo channelListBo);

    /**
    * 用户来源渠道表信息
    * @param id 用户来源渠道表id
    * @return
    */
    R<ChannelInfoVo> info(Long id);

    /**
     * 新增用户来源渠道表
     * @param channelBo 用户来源渠道表对象
     * @return
     */
    R<String> save(ChannelBo channelBo);

    /**
     * 修改用户来源渠道表
     * @param channelBo 用户来源渠道表对象
     * @return
     */
    R<String> update(ChannelBo channelBo);

    /**
     * 删除用户来源渠道表
     * @param id 用户来源渠道表id
     * @return
     */
    R<String> delete(Long id);

    /**
     * 获取来源渠道列表(树型结构)
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    R<List<ChannelTreeVo>> listTree(Integer childrenNotNull);
}

