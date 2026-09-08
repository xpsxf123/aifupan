package com.jiuyu.replay.words.producer.impl;

import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.words.vo.TotalSocketMessageInfoVo;
import com.jiuyu.replay.words.bo.TotalSocketMessageBo;
import com.jiuyu.replay.words.repository.service.TotalSocketMessageService;
import com.jiuyu.replay.words.entity.TotalSocketMessageEntity;
import com.jiuyu.replay.words.producer.TotalSocketMessageProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;

import java.util.Date;


/**
 * 直播场次的websocket记录统计
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-01-04 15:15:15
 */
@Service
public class TotalSocketMessageProducerImpl implements TotalSocketMessageProducer {

    @Resource
    private TotalSocketMessageService totalSocketMessageService;


    @Override
    public TotalSocketMessageInfoVo info(Long id) {

        TotalSocketMessageEntity totalSocketMessageEntity = totalSocketMessageService.getById(id);
        if(totalSocketMessageEntity != null) {
            TotalSocketMessageInfoVo totalSocketMessageInfoVo = new TotalSocketMessageInfoVo();
            BeanUtils.copyProperties(totalSocketMessageEntity, totalSocketMessageInfoVo);
            return totalSocketMessageInfoVo;
        }

        return null;
    }

    /**
     * 新增直播场次的websocket记录统计
     * @param totalSocketMessageBo 直播场次的websocket记录统计对象
     * @return
     */
     public TotalSocketMessageInfoVo save(TotalSocketMessageBo totalSocketMessageBo) {

         TotalSocketMessageEntity totalSocketMessageEntity = new TotalSocketMessageEntity();
         BeanUtils.copyProperties(totalSocketMessageBo, totalSocketMessageEntity);
         totalSocketMessageEntity.setId(SnowflakeManager.nextValue());
         totalSocketMessageEntity.setCreateDate(new Date());
         totalSocketMessageEntity.setUpdateDate(new Date());

         totalSocketMessageService.save(totalSocketMessageEntity);

         TotalSocketMessageInfoVo totalSocketMessageInfoVo = new TotalSocketMessageInfoVo();
         BeanUtils.copyProperties(totalSocketMessageEntity, totalSocketMessageInfoVo);

         return totalSocketMessageInfoVo;
     }

    /**
     * 修改直播场次的websocket记录统计
     * @param totalSocketMessageBo 直播场次的websocket记录统计对象
     * @return
     */
    public void update(TotalSocketMessageBo totalSocketMessageBo) {

        TotalSocketMessageEntity totalSocketMessageEntity = new TotalSocketMessageEntity();
        BeanUtils.copyProperties(totalSocketMessageBo, totalSocketMessageEntity);
        totalSocketMessageEntity.setUpdateDate(new Date());

        totalSocketMessageService.updateById(totalSocketMessageEntity);
    }

    /**
     * 删除直播场次的websocket记录统计
     * @param id 直播场次的websocket记录统计id
     * @return
     */
    public void deleteById(Long id) {

        totalSocketMessageService.removeById(id);
    }


}

