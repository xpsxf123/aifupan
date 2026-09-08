package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.AiTrainBo;
import com.jiuyu.replay.words.bo.AiTrainListBo;
import com.jiuyu.replay.words.entity.AiTrainEntity;
import com.jiuyu.replay.words.producer.AiTrainProducer;
import com.jiuyu.replay.words.repository.service.AiTrainService;
import com.jiuyu.replay.words.vo.AiTrainInfoVo;
import com.jiuyu.replay.words.vo.AiTrainListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * AI训练
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-20 18:47:39
 */
@Service
public class AiTrainProducerImpl implements AiTrainProducer {

    @Resource
    private AiTrainService aiTrainService;


    @Override
    public PageUtils<AiTrainListVo> queryPage(AiTrainListBo aiTrainListBo) {
        QueryWrapper<AiTrainEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(aiTrainListBo.getAiStatus())){
            wrapper.eq("ai_status", aiTrainListBo.getAiStatus());
        }

        IPage<AiTrainEntity> iPage = aiTrainService.page(new Query<AiTrainEntity>().getPage(aiTrainListBo.getPage(), aiTrainListBo.getLimit()), wrapper);

        PageUtils<AiTrainListVo> pageUtils = new PageUtils<>(aiTrainListBo.getPage(), aiTrainListBo.getLimit(), iPage);

        List<AiTrainEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {

            List<AiTrainListVo> vos = records.stream().map(item -> {
                AiTrainListVo aiTrainVo = new AiTrainListVo();
                BeanUtils.copyProperties(item, aiTrainVo);
                return aiTrainVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AiTrainInfoVo info(Long id) {

        AiTrainEntity aiTrainEntity = aiTrainService.getById(id);
        if(aiTrainEntity != null) {
            AiTrainInfoVo aiTrainInfoVo = new AiTrainInfoVo();
            BeanUtils.copyProperties(aiTrainEntity, aiTrainInfoVo);
            return aiTrainInfoVo;
        }

        return null;
    }

    @Override
    public AiTrainInfoVo save(AiTrainBo aiTrainBo) {

         AiTrainEntity aiTrainEntity = new AiTrainEntity();
         BeanUtils.copyProperties(aiTrainBo, aiTrainEntity);
         aiTrainEntity.setId(SnowflakeManager.nextValue());
         aiTrainEntity.setCreateDate(new Date());
         aiTrainEntity.setUpdateDate(new Date());
         aiTrainEntity.setAiStatus(0);

         aiTrainService.save(aiTrainEntity);

         AiTrainInfoVo aiTrainInfoVo = new AiTrainInfoVo();
         BeanUtils.copyProperties(aiTrainEntity, aiTrainInfoVo);

         return aiTrainInfoVo;
     }

    @Override
    public void update(AiTrainBo aiTrainBo) {

        AiTrainEntity aiTrainEntity = new AiTrainEntity();
        BeanUtils.copyProperties(aiTrainBo, aiTrainEntity);
        aiTrainEntity.setUpdateDate(new Date());

        aiTrainService.updateById(aiTrainEntity);
    }

    @Override
    public void deleteById(Long id) {

        aiTrainService.removeById(id);
    }

    @Override
    public AiTrainInfoVo getByVideoId(String videoId) {

        AiTrainEntity aiTrainEntity = this.aiTrainService.getOne(new QueryWrapper<AiTrainEntity>().eq("video_id", videoId));

        if(aiTrainEntity != null) {
            AiTrainInfoVo aiTrainInfoVo = new AiTrainInfoVo();
            BeanUtils.copyProperties(aiTrainEntity, aiTrainInfoVo);
            return aiTrainInfoVo;
        }

        return null;
    }

    @Override
    public void completeTrain(Long id) {

        AiTrainEntity aiTrainEntity = this.aiTrainService.getById(id);

        if(aiTrainEntity != null) {
            aiTrainEntity.setAiStatus(1);
            aiTrainEntity.setUpdateDate(new Date());
            double progressRange = 0.01 + (0.05 - 0.01) * Math.random();
            aiTrainEntity.setProgressRange(progressRange);

            this.aiTrainService.updateById(aiTrainEntity);
        }
    }


}

