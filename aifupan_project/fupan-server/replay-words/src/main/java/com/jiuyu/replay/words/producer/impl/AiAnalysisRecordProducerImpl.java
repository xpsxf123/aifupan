package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.AiAnalysisRecordBo;
import com.jiuyu.replay.words.bo.AiAnalysisRecordListBo;
import com.jiuyu.replay.words.entity.AiAnalysisRecordEntity;
import com.jiuyu.replay.words.producer.AiAnalysisRecordProducer;
import com.jiuyu.replay.words.repository.service.AiAnalysisRecordService;
import com.jiuyu.replay.words.vo.AiAnalysisRecordInfoVo;
import com.jiuyu.replay.words.vo.AiAnalysisRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * AI分析出来的关键词总数和未匹配上词库的关键词个数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-21 17:06:53
 */
@Service
public class AiAnalysisRecordProducerImpl implements AiAnalysisRecordProducer {

    @Resource
    private AiAnalysisRecordService aiAnalysisRecordService;


    @Override
    public PageUtils<AiAnalysisRecordListVo> queryPage(AiAnalysisRecordListBo aiAnalysisRecordListBo) {
        QueryWrapper<AiAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(aiAnalysisRecordListBo.getKeyword())){
            wrapper.like("name", aiAnalysisRecordListBo.getKeyword());
        }

        IPage<AiAnalysisRecordEntity> iPage = aiAnalysisRecordService.page(new Query<AiAnalysisRecordEntity>().getPage(aiAnalysisRecordListBo.getPage(), aiAnalysisRecordListBo.getLimit()), wrapper);

        PageUtils<AiAnalysisRecordListVo> pageUtils = new PageUtils<>(aiAnalysisRecordListBo.getPage(), aiAnalysisRecordListBo.getLimit(), iPage);

        List<AiAnalysisRecordEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AiAnalysisRecordListVo> vos = records.stream().map(item -> {
                AiAnalysisRecordListVo aiAnalysisRecordVo = new AiAnalysisRecordListVo();
                BeanUtils.copyProperties(item, aiAnalysisRecordVo);
                return aiAnalysisRecordVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AiAnalysisRecordInfoVo info(Long id) {

        AiAnalysisRecordEntity aiAnalysisRecordEntity = aiAnalysisRecordService.getById(id);
        if(aiAnalysisRecordEntity != null) {
            AiAnalysisRecordInfoVo aiAnalysisRecordInfoVo = new AiAnalysisRecordInfoVo();
            BeanUtils.copyProperties(aiAnalysisRecordEntity, aiAnalysisRecordInfoVo);
            return aiAnalysisRecordInfoVo;
        }

        return null;
    }

    @Override
    public AiAnalysisRecordInfoVo save(AiAnalysisRecordBo aiAnalysisRecordBo) {

         AiAnalysisRecordEntity aiAnalysisRecordEntity = new AiAnalysisRecordEntity();
         BeanUtils.copyProperties(aiAnalysisRecordBo, aiAnalysisRecordEntity);
         aiAnalysisRecordEntity.setId(SnowflakeManager.nextValue());
         aiAnalysisRecordEntity.setCreateDate(new Date());
         aiAnalysisRecordEntity.setUpdateDate(new Date());

         aiAnalysisRecordService.save(aiAnalysisRecordEntity);

         AiAnalysisRecordInfoVo aiAnalysisRecordInfoVo = new AiAnalysisRecordInfoVo();
         BeanUtils.copyProperties(aiAnalysisRecordEntity, aiAnalysisRecordInfoVo);

         return aiAnalysisRecordInfoVo;
     }

    @Override
    public void update(AiAnalysisRecordBo aiAnalysisRecordBo) {

        AiAnalysisRecordEntity aiAnalysisRecordEntity = new AiAnalysisRecordEntity();
        BeanUtils.copyProperties(aiAnalysisRecordBo, aiAnalysisRecordEntity);
        aiAnalysisRecordEntity.setUpdateDate(new Date());

        aiAnalysisRecordService.updateById(aiAnalysisRecordEntity);
    }

    @Override
    public void deleteById(Long id) {

        aiAnalysisRecordService.removeById(id);
    }

    @Override
    public List<AiAnalysisRecordEntity> allList() {
        List<AiAnalysisRecordEntity> list = aiAnalysisRecordService.list();
        if (list != null && list.size() > 0) {
            return list;
        }

        return List.of();
    }

    @Override
    public List<AiAnalysisRecordEntity> allListByVideoIds(List<String> videoIds) {
        return aiAnalysisRecordService.list(new LambdaQueryWrapper<AiAnalysisRecordEntity>()
                .in(AiAnalysisRecordEntity::getUuid, videoIds)
        );
    }
}

