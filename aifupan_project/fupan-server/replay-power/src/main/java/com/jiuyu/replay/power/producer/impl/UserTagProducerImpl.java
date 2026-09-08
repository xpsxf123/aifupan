package com.jiuyu.replay.power.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.power.vo.UserTagListVo;
import com.jiuyu.replay.power.vo.UserTagInfoVo;
import com.jiuyu.replay.power.bo.UserTagBo;
import com.jiuyu.replay.power.bo.UserTagListBo;
import com.jiuyu.replay.power.repository.service.UserTagService;
import com.jiuyu.replay.power.entity.UserTagEntity;
import com.jiuyu.replay.power.producer.UserTagProducer;

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
 * 用户-标签-关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-20 10:21:25
 */
@Service
public class UserTagProducerImpl implements UserTagProducer {

    @Resource
    private UserTagService userTagService;


    @Override
    public PageUtils<UserTagListVo> queryPage(UserTagListBo userTagListBo) {
        QueryWrapper<UserTagEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(userTagListBo.getKeyword())){
            wrapper.like("name", userTagListBo.getKeyword());
        }

        IPage<UserTagEntity> iPage = userTagService.page(new Query<UserTagEntity>().getPage(userTagListBo.getPage(), userTagListBo.getLimit()), wrapper);

        PageUtils<UserTagListVo> pageUtils = new PageUtils<>(userTagListBo.getPage(), userTagListBo.getLimit(), iPage);

        List<UserTagEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<UserTagListVo> vos = records.stream().map(item -> {
                UserTagListVo userTagVo = new UserTagListVo();
                BeanUtils.copyProperties(item, userTagVo);
                return userTagVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public UserTagInfoVo info(Long id) {

        UserTagEntity userTagEntity = userTagService.getById(id);
        if(userTagEntity != null) {
            UserTagInfoVo userTagInfoVo = new UserTagInfoVo();
            BeanUtils.copyProperties(userTagEntity, userTagInfoVo);
            return userTagInfoVo;
        }

        return null;
    }

    /**
     * 新增用户-标签-关联
     * @param userTagBo 用户-标签-关联对象
     * @return
     */
     public UserTagInfoVo save(UserTagBo userTagBo) {

         UserTagEntity userTagEntity = new UserTagEntity();
         BeanUtils.copyProperties(userTagBo, userTagEntity);
         userTagEntity.setId(SnowflakeManager.nextValue());
         userTagEntity.setCreateDate(new Date());
         userTagEntity.setUpdateDate(new Date());

         userTagService.save(userTagEntity);

         UserTagInfoVo userTagInfoVo = new UserTagInfoVo();
         BeanUtils.copyProperties(userTagEntity, userTagInfoVo);

         return userTagInfoVo;
     }

    /**
     * 修改用户-标签-关联
     * @param userTagBo 用户-标签-关联对象
     * @return
     */
    public void update(UserTagBo userTagBo) {

        UserTagEntity userTagEntity = new UserTagEntity();
        BeanUtils.copyProperties(userTagBo, userTagEntity);
        userTagEntity.setUpdateDate(new Date());

        userTagService.updateById(userTagEntity);
    }

    /**
     * 删除用户-标签-关联
     * @param id 用户-标签-关联id
     * @return
     */
    public void deleteById(Long id) {

        userTagService.removeById(id);
    }


}

