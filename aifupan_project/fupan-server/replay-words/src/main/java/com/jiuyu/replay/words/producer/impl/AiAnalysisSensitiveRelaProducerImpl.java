package com.jiuyu.replay.words.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaListVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaInfoVo;
import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaBo;
import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaListBo;
import com.jiuyu.replay.words.repository.service.AiAnalysisSensitiveRelaService;
import com.jiuyu.replay.words.entity.AiAnalysisSensitiveRelaEntity;
import com.jiuyu.replay.words.producer.AiAnalysisSensitiveRelaProducer;

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
 * AI分析关键词与记录关联关系表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-03-03 18:49:53
 */
@Service
public class AiAnalysisSensitiveRelaProducerImpl implements AiAnalysisSensitiveRelaProducer {

    @Resource
    private AiAnalysisSensitiveRelaService aiAnalysisSensitiveRelaService;


    @Override
    public PageUtils<AiAnalysisSensitiveRelaListVo> queryPage(AiAnalysisSensitiveRelaListBo aiAnalysisSensitiveRelaListBo) {
        QueryWrapper<AiAnalysisSensitiveRelaEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(aiAnalysisSensitiveRelaListBo.getKeyword())){
            wrapper.like("name", aiAnalysisSensitiveRelaListBo.getKeyword());
        }

        IPage<AiAnalysisSensitiveRelaEntity> iPage = aiAnalysisSensitiveRelaService.page(new Query<AiAnalysisSensitiveRelaEntity>().getPage(aiAnalysisSensitiveRelaListBo.getPage(), aiAnalysisSensitiveRelaListBo.getLimit()), wrapper);

        PageUtils<AiAnalysisSensitiveRelaListVo> pageUtils = new PageUtils<>(aiAnalysisSensitiveRelaListBo.getPage(), aiAnalysisSensitiveRelaListBo.getLimit(), iPage);

        List<AiAnalysisSensitiveRelaEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AiAnalysisSensitiveRelaListVo> vos = records.stream().map(item -> {
                AiAnalysisSensitiveRelaListVo aiAnalysisSensitiveRelaVo = new AiAnalysisSensitiveRelaListVo();
                BeanUtils.copyProperties(item, aiAnalysisSensitiveRelaVo);
                return aiAnalysisSensitiveRelaVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AiAnalysisSensitiveRelaInfoVo info(Long id) {

        AiAnalysisSensitiveRelaEntity aiAnalysisSensitiveRelaEntity = aiAnalysisSensitiveRelaService.getById(id);
        if(aiAnalysisSensitiveRelaEntity != null) {
            AiAnalysisSensitiveRelaInfoVo aiAnalysisSensitiveRelaInfoVo = new AiAnalysisSensitiveRelaInfoVo();
            BeanUtils.copyProperties(aiAnalysisSensitiveRelaEntity, aiAnalysisSensitiveRelaInfoVo);
            return aiAnalysisSensitiveRelaInfoVo;
        }

        return null;
    }

    /**
     * 新增AI分析关键词与记录关联关系表
     * @param aiAnalysisSensitiveRelaBo AI分析关键词与记录关联关系表对象
     * @return
     */
     public AiAnalysisSensitiveRelaInfoVo save(AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo) {

         AiAnalysisSensitiveRelaEntity aiAnalysisSensitiveRelaEntity = new AiAnalysisSensitiveRelaEntity();
         BeanUtils.copyProperties(aiAnalysisSensitiveRelaBo, aiAnalysisSensitiveRelaEntity);
         aiAnalysisSensitiveRelaEntity.setId(SnowflakeManager.nextValue());
         aiAnalysisSensitiveRelaEntity.setCreateDate(new Date());
         aiAnalysisSensitiveRelaEntity.setUpdateDate(new Date());

         aiAnalysisSensitiveRelaService.save(aiAnalysisSensitiveRelaEntity);

         AiAnalysisSensitiveRelaInfoVo aiAnalysisSensitiveRelaInfoVo = new AiAnalysisSensitiveRelaInfoVo();
         BeanUtils.copyProperties(aiAnalysisSensitiveRelaEntity, aiAnalysisSensitiveRelaInfoVo);

         return aiAnalysisSensitiveRelaInfoVo;
     }

    /**
     * 修改AI分析关键词与记录关联关系表
     * @param aiAnalysisSensitiveRelaBo AI分析关键词与记录关联关系表对象
     * @return
     */
    public void update(AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo) {

        AiAnalysisSensitiveRelaEntity aiAnalysisSensitiveRelaEntity = new AiAnalysisSensitiveRelaEntity();
        BeanUtils.copyProperties(aiAnalysisSensitiveRelaBo, aiAnalysisSensitiveRelaEntity);
        aiAnalysisSensitiveRelaEntity.setUpdateDate(new Date());

        aiAnalysisSensitiveRelaService.updateById(aiAnalysisSensitiveRelaEntity);
    }

    /**
     * 删除AI分析关键词与记录关联关系表
     * @param id AI分析关键词与记录关联关系表id
     * @return
     */
    public void deleteById(Long id) {

        aiAnalysisSensitiveRelaService.removeById(id);
    }

    @Override
    public void saveBatch(List<AiAnalysisSensitiveRelaBo> notMarkWordList) {

        if(notMarkWordList != null && notMarkWordList.size() > 0) {
            List<AiAnalysisSensitiveRelaEntity> aiAnalysisSensitiveRelaEntities = notMarkWordList.stream().map(aiAnalysisSensitiveRelaBo -> {
                AiAnalysisSensitiveRelaEntity aiAnalysisSensitiveRelaEntity = new AiAnalysisSensitiveRelaEntity();
                BeanUtils.copyProperties(aiAnalysisSensitiveRelaBo, aiAnalysisSensitiveRelaEntity);
                aiAnalysisSensitiveRelaEntity.setId(SnowflakeManager.nextValue());
                aiAnalysisSensitiveRelaEntity.setCreateDate(new Date());
                aiAnalysisSensitiveRelaEntity.setUpdateDate(new Date());
                return aiAnalysisSensitiveRelaEntity;
            }).toList();

            this.aiAnalysisSensitiveRelaService.saveBatch(aiAnalysisSensitiveRelaEntities);
        }
    }

    @Override
    public void delByWordListAndTrade(List<String> wordNameList, Long tradeId) {
        if(wordNameList != null && wordNameList.size() > 0) {
            this.aiAnalysisSensitiveRelaService.remove(new QueryWrapper<AiAnalysisSensitiveRelaEntity>().in("sensitive_word", wordNameList).eq("trade_id", tradeId));
        }
    }


}

