package com.jiuyu.replay.power.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.power.vo.TagListVo;
import com.jiuyu.replay.power.vo.TagInfoVo;
import com.jiuyu.replay.power.bo.TagBo;
import com.jiuyu.replay.power.bo.TagListBo;
import com.jiuyu.replay.power.repository.service.TagService;
import com.jiuyu.replay.power.entity.TagEntity;
import com.jiuyu.replay.power.producer.TagProducer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 用户标签
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-20 10:21:25
 */
@Service
public class TagProducerImpl implements TagProducer {

    @Resource
    private TagService tagService;


    @Override
    public PageUtils<TagListVo> queryPage(TagListBo tagListBo) {
        QueryWrapper<TagEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(tagListBo.getKeyword())){
            wrapper.like("name", tagListBo.getKeyword());
        }

        IPage<TagEntity> iPage = tagService.page(new Query<TagEntity>().getPage(tagListBo.getPage(), tagListBo.getLimit()), wrapper);

        PageUtils<TagListVo> pageUtils = new PageUtils<>(tagListBo.getPage(), tagListBo.getLimit(), iPage);

        List<TagEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<TagListVo> vos = records.stream().map(item -> {
                TagListVo tagVo = new TagListVo();
                BeanUtils.copyProperties(item, tagVo);
                return tagVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public TagInfoVo info(Long id) {

        TagEntity tagEntity = tagService.getById(id);
        if(tagEntity != null) {
            TagInfoVo tagInfoVo = new TagInfoVo();
            BeanUtils.copyProperties(tagEntity, tagInfoVo);
            return tagInfoVo;
        }

        return null;
    }

    /**
     * 新增用户标签
     * @param tagBo 用户标签对象
     * @return
     */
     public TagInfoVo save(TagBo tagBo) {

         TagEntity tagEntity = new TagEntity();
         BeanUtils.copyProperties(tagBo, tagEntity);
         tagEntity.setId(SnowflakeManager.nextValue());
         tagEntity.setCreateDate(new Date());
         tagEntity.setUpdateDate(new Date());

         tagService.save(tagEntity);

         TagInfoVo tagInfoVo = new TagInfoVo();
         BeanUtils.copyProperties(tagEntity, tagInfoVo);

         return tagInfoVo;
     }

    /**
     * 修改用户标签
     * @param tagBo 用户标签对象
     * @return
     */
    public void update(TagBo tagBo) {

        TagEntity tagEntity = new TagEntity();
        BeanUtils.copyProperties(tagBo, tagEntity);
        tagEntity.setUpdateDate(new Date());

        tagService.updateById(tagEntity);
    }

    /**
     * 删除用户标签
     * @param id 用户标签id
     * @return
     */
    public void deleteById(Long id) {

        tagService.removeById(id);
    }


}

