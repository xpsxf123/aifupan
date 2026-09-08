package com.jiuyu.replay.agent.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.agent.bo.ChannelBo;
import com.jiuyu.replay.agent.bo.ChannelListBo;
import com.jiuyu.replay.agent.entity.ChannelEntity;
import com.jiuyu.replay.agent.producer.ChannelProducer;
import com.jiuyu.replay.agent.repository.service.ChannelService;
import com.jiuyu.replay.agent.vo.ChannelInfoVo;
import com.jiuyu.replay.agent.vo.ChannelListVo;
import com.jiuyu.replay.agent.vo.ChannelTreeVo;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 用户来源渠道表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:14
 */
@Service
public class ChannelProducerImpl implements ChannelProducer {

    @Resource
    private ChannelService channelService;


    @Override
    public PageUtils<ChannelListVo> queryPage(ChannelListBo channelListBo) {
        QueryWrapper<ChannelEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(channelListBo.getKeyword())){
            wrapper.like("name", channelListBo.getKeyword());
        }

        IPage<ChannelEntity> iPage = channelService.page(new Query<ChannelEntity>().getPage(channelListBo.getPage(), channelListBo.getLimit()), wrapper);

        PageUtils<ChannelListVo> pageUtils = new PageUtils<>(channelListBo.getPage(), channelListBo.getLimit(), iPage);

        List<ChannelEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<ChannelListVo> vos = records.stream().map(item -> {
                ChannelListVo channelVo = new ChannelListVo();
                BeanUtils.copyProperties(item, channelVo);
                return channelVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ChannelInfoVo info(Long id) {

        ChannelEntity channelEntity = channelService.getById(id);
        if(channelEntity != null) {
            ChannelInfoVo channelInfoVo = new ChannelInfoVo();
            BeanUtils.copyProperties(channelEntity, channelInfoVo);
            return channelInfoVo;
        }

        return null;
    }

    @Override
    public ChannelInfoVo save(ChannelBo channelBo) {

         ChannelEntity channelEntity = new ChannelEntity();
         BeanUtils.copyProperties(channelBo, channelEntity);
         channelEntity.setId(SnowflakeManager.nextValue());
         channelEntity.setCreateDate(new Date());
         channelEntity.setUpdateDate(new Date());

         channelService.save(channelEntity);

         ChannelInfoVo channelInfoVo = new ChannelInfoVo();
         BeanUtils.copyProperties(channelEntity, channelInfoVo);

         return channelInfoVo;
     }

    @Override
    public void update(ChannelBo channelBo) {

        ChannelEntity channelEntity = new ChannelEntity();
        BeanUtils.copyProperties(channelBo, channelEntity);
        channelEntity.setUpdateDate(new Date());

        channelService.updateById(channelEntity);
    }

    @Override
    public void deleteById(Long id) {

        channelService.removeById(id);
    }

    /**
     * 获取来源渠道列表(树型结构)
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    @Override
    public List<ChannelTreeVo> listTree(Integer childrenNotNull) {
        List<ChannelEntity> channelEntities = this.channelService.list();

        if (channelEntities != null && channelEntities.size() > 0) {
            List<ChannelTreeVo> channelTreeVos = channelEntities.stream().filter((item) -> item.getParentId() == 0).map(item ->{
                ChannelTreeVo channelTreeVo = new ChannelTreeVo();
                BeanUtils.copyProperties(item,channelTreeVo);
                // 封装子来源渠道
                channelTreeVo.setChildren(getChildren(channelTreeVo,channelEntities,childrenNotNull));
                return channelTreeVo;
            }).toList();
            return channelTreeVos;
        }

        return null;
    }

    /**
     * 查询当前渠道是否有子渠道
     * @param id 渠道ID
     * @return
     */
    @Override
    public Long haveChildren(Long id) {
        List<ChannelEntity> channelEntities = channelService.list(new QueryWrapper<ChannelEntity>().eq("parent_id", id));
        long count = channelEntities.stream().count();
        return count;
    }

    @Override
    public List<ChannelInfoVo> listByIds(Collection<Long> channelIds) {
        if(channelIds != null && channelIds.size() > 0) {
            List<ChannelEntity> channelEntities = channelService.listByIds(channelIds);
            if(channelEntities != null && channelEntities.size() > 0) {
                List<ChannelInfoVo> channelInfoVos = channelEntities.stream().map(item -> {
                    ChannelInfoVo channelInfoVo = new ChannelInfoVo();
                    BeanUtils.copyProperties(item, channelInfoVo);
                    return channelInfoVo;
                }).collect(Collectors.toList());

                return channelInfoVos;
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<Long> getChannelAllChildId(Long channelId) {
        List<Long> ids = new LinkedList<>();
        ids.add(channelId);

        List<ChannelEntity> channelEntities = this.channelService.list();
        if(channelEntities != null && channelEntities.size() > 0) {
            for (ChannelEntity channelEntity : channelEntities) {
                if(channelEntity.getParentId().equals(channelId)) {
                    ids.add(channelEntity.getId());
                    recursionChannelChild(channelEntities, channelEntity.getId(), ids);
                }
            }
        }

        return ids;
    }


    /**
     * 根据渠道ID查询渠道信息
     * @param list
     * @return
     */
    @Override
    public List<ChannelInfoVo> selectByChannelIds(List<Long> list) {
        if (ObjectUtil.isNotEmpty( list)){
            List<ChannelEntity> channelEntities = channelService.listByIds(list);
            if (ObjectUtil.isNotEmpty(channelEntities)){
                return BeanUtil.copyToList(channelEntities, ChannelInfoVo.class);
            }
        }
        return List.of();
    }

    @Override
    public Map<Long, String> getChannelParentNameByIds(List<Long> channelIds) {
        if (ObjectUtil.isEmpty(channelIds)) {
            return new HashMap<>();
        }

        // 查询全部数据
        Map<Long, ChannelEntity> map = channelService.lambdaQuery()
                .select(ChannelEntity::getId, ChannelEntity::getParentId, ChannelEntity::getChannelName)
                .last("limit 10000")
                .list()
                .stream()
                .collect(Collectors.toMap(ChannelEntity::getId, Function.identity(), (a, b) -> a));

        if (ObjectUtil.isEmpty(map)) {
            return new HashMap<>();
        }

        return channelIds.stream()
                .filter(ObjectUtil::isNotEmpty)
                .collect(Collectors.toMap(item -> item, item -> getChannelParentName(map, item, "", 0)));
    }

    private String getChannelParentName(Map<Long, ChannelEntity> map, Long id, String name, int num) {
        if (ObjectUtil.isEmpty(map)) {
            return "";
        }
        if (num >= 20) {
            return name;
        }
        if (name == null) {
            name = "";
        }
        ChannelEntity channelEntity = map.get(id);
        if (channelEntity == null) {
            return name;
        }
        if (ObjectUtil.isNotEmpty(name)) {
            name = "/" + name;
        }
        name = channelEntity.getChannelName() + name;
        if (ObjectUtil.isNotEmpty(channelEntity.getParentId()) && map.get(channelEntity.getParentId()) != null) {
            name = getChannelParentName(map, channelEntity.getParentId(), name, num + 1);
        }
        return name;
    }

    private void recursionChannelChild(List<ChannelEntity> channelEntities, Long currentChannelId, List<Long> ids) {
        for (ChannelEntity channelEntity : channelEntities) {
            if(channelEntity.getParentId().equals(currentChannelId)) {
                ids.add(channelEntity.getId());
                recursionChannelChild(channelEntities, channelEntity.getId(), ids);
            }
        }
    }

    /**
     * 返回当前渠道的子渠道
     * @param channelTreeVo 当前渠道
     * @param channelEntities 全部渠道列表
     * @param childrenNotNull 当没有子渠道时，子渠道列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    private List<ChannelTreeVo> getChildren(ChannelTreeVo channelTreeVo,List<ChannelEntity> channelEntities,Integer childrenNotNull){
        List<ChannelTreeVo> channelTreeVoList = channelEntities.stream().filter(item -> item.getParentId().equals(channelTreeVo.getId())).map(item ->{
            ChannelTreeVo childrenChannelTreeVo = new ChannelTreeVo();
            BeanUtils.copyProperties(item,childrenChannelTreeVo);
            childrenChannelTreeVo.setChildren(getChildren(childrenChannelTreeVo,channelEntities,childrenNotNull));

            return childrenChannelTreeVo;
        }).toList();

        if (channelTreeVoList.size() == 0){
            if (!StringUtils.isEmpty(childrenNotNull) && childrenNotNull == 1){
                return channelTreeVoList;
            }else{
                return null;
            }
        }

        return channelTreeVoList;
    }

}

