package com.jiuyu.replay.api.logic.agent.impl;

import com.jiuyu.replay.agent.producer.AgentProducer;
import com.jiuyu.replay.api.logic.agent.ChannelLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.power.bll.UserDetailsBll;
import com.jiuyu.replay.agent.vo.ChannelListVo;
import com.jiuyu.replay.agent.vo.ChannelInfoVo;
import com.jiuyu.replay.agent.bo.ChannelBo;
import com.jiuyu.replay.agent.bo.ChannelListBo;
import com.jiuyu.replay.agent.bll.ChannelBll;

import com.jiuyu.replay.agent.vo.ChannelTreeVo;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

import java.util.List;


/**
 * 用户来源渠道表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:14
 */
@Service
public class ChannelLogicImpl implements ChannelLogic {

    @Resource
    private ChannelBll channelBll;
    @Resource
    private UserDetailsBll userDetailsBll;
    @Resource
    private AgentProducer agentProducer;


    @Override
    public R<PageUtils<ChannelListVo>> queryPage(ChannelListBo channelListBo) {

        return channelBll.queryPage(channelListBo);
    }

    @Override
    public R<ChannelInfoVo> info(Long id) {

        return channelBll.info(id);
    }

    @Override
    public R<String> save(ChannelBo channelBo) {

        return channelBll.save(channelBo);
    }

    @Override
    public R<String> update(ChannelBo channelBo) {

        R<String> update = channelBll.update(channelBo);
        if (update.getCode() == StatusCode.SUCCESS.getCode()) {
            // 修改代理商的名称
            agentProducer.updateAgentNameByChannelId(channelBo.getId(), channelBo.getChannelName());
        }
        return update;
    }

    @Override
    public R<String> delete(Long id) {

        // 查询当前渠道是否有绑定用户
        Long userDetailsInfoCounts= userDetailsBll.getByChannelId(id);
        if (userDetailsInfoCounts > 0){
            return R.error(40001,"当前渠道有绑定的用户,请先取消绑定后再删除");
        }
        // 查询当前渠道是否有子渠道
        Long haveChildrenCount = channelBll.haveChildren(id);
        if (haveChildrenCount > 0){
            return R.error(40001,"当前渠道有子渠道,请先处理子渠道后再删除");
        }

        return channelBll.delete(id);
    }

    /**
     * 获取来源渠道列表(树型结构)
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    @Override
    public R<List<ChannelTreeVo>> listTree(Integer childrenNotNull) {
        return channelBll.listTree(childrenNotNull);
    }


}

