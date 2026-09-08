package com.jiuyu.replay.agent.bll;

import com.jiuyu.replay.agent.bo.ChannelBo;
import com.jiuyu.replay.agent.bo.ChannelListBo;
import com.jiuyu.replay.agent.producer.ChannelProducer;
import com.jiuyu.replay.agent.vo.ChannelInfoVo;
import com.jiuyu.replay.agent.vo.ChannelListVo;
import com.jiuyu.replay.agent.vo.ChannelTreeVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;


/**
 * 用户来源渠道表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:14
 */
@Component
public class ChannelBll {

    @Resource
    private ChannelProducer channelProducer;


    /**
     * 用户来源渠道表列表
     * @param channelListBo 用户来源渠道表列表查询参数
     * @return
     */
    public R<PageUtils<ChannelListVo>> queryPage(ChannelListBo channelListBo) {

        return R.ok("获取成功", channelProducer.queryPage(channelListBo));
    }

    /**
    * 用户来源渠道表信息
    * @param id 用户来源渠道表id
    * @return
    */
    public R<ChannelInfoVo> info(Long id) {

        ChannelInfoVo channelInfoVo = channelProducer.info(id);
        return R.ok("获取成功", channelInfoVo);
    }

    /**
     * 新增用户来源渠道表
     * @param channelBo 用户来源渠道表对象
     * @return
     */
    public R<String> save(ChannelBo channelBo) {

        ChannelInfoVo channelInfoVo = channelProducer.save(channelBo);
        return R.ok("添加成功");
    }

    /**
     * 修改用户来源渠道表
     * @param channelBo 用户来源渠道表对象
     * @return
     */
    public R<String> update(ChannelBo channelBo) {

        channelProducer.update(channelBo);
        return R.ok("修改成功");
    }

    /**
     * 删除用户来源渠道表
     * @param id 用户来源渠道表id
     * @return
     */
    public R<String> delete(Long id) {

        channelProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 获取来源渠道列表(树型结构)
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    public R<List<ChannelTreeVo>> listTree(Integer childrenNotNull) {
        List<ChannelTreeVo> channelTreeVos = channelProducer.listTree(childrenNotNull);
        return R.ok("获取成功",channelTreeVos);
    }

    /**
     * 查询当前渠道是否有子渠道
     * @param id 渠道ID
     * @return
     */
    public Long haveChildren(Long id) {
        return channelProducer.haveChildren(id);
    }

    /**
     * 根据渠道id集合获取用户来源渠道列表
     * @param channelIds 渠道id集合
     * @return
     */
    public R<List<ChannelInfoVo>> listByIds(Collection<Long> channelIds) {

        List<ChannelInfoVo> channelInfoVos = this.channelProducer.listByIds(channelIds);

        return R.ok(channelInfoVos);
    }

    /**
     * 获取渠道和渠道下的所有子渠道id
     * @param channelId 渠道id
     * @return
     */
    public R<List<Long>> getChannelAllChildId(Long channelId) {
        List<Long> channelIds = this.channelProducer.getChannelAllChildId(channelId);

        return R.ok(channelIds);
    }

    /**
     * 根据渠道id集合获取用户来源渠道列表
     * @param list
     * @return
     */
    public List<ChannelInfoVo> selectByChannelIds(List<Long> list) {
        return this.channelProducer.selectByChannelIds(list);
    }
}

