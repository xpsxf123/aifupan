package com.jiuyu.replay.words.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.words.vo.WordRuleRelevanceListVo;
import com.jiuyu.replay.words.vo.WordRuleRelevanceInfoVo;
import com.jiuyu.replay.words.bo.WordRuleRelevanceBo;
import com.jiuyu.replay.words.bo.WordRuleRelevanceListBo;
import com.jiuyu.replay.words.repository.service.WordRuleRelevanceService;
import com.jiuyu.replay.words.entity.WordRuleRelevanceEntity;
import com.jiuyu.replay.words.producer.WordRuleRelevanceProducer;

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
 * 规则关联词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@Service
public class WordRuleRelevanceProducerImpl implements WordRuleRelevanceProducer {

    @Resource
    private WordRuleRelevanceService wordRuleRelevanceService;


    @Override
    public PageUtils<WordRuleRelevanceListVo> queryPage(WordRuleRelevanceListBo wordRuleRelevanceListBo) {
        QueryWrapper<WordRuleRelevanceEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(wordRuleRelevanceListBo.getKeyword())){
            wrapper.like("name", wordRuleRelevanceListBo.getKeyword());
        }

        IPage<WordRuleRelevanceEntity> iPage = wordRuleRelevanceService.page(new Query<WordRuleRelevanceEntity>().getPage(wordRuleRelevanceListBo.getPage(), wordRuleRelevanceListBo.getLimit()), wrapper);

        PageUtils<WordRuleRelevanceListVo> pageUtils = new PageUtils<>(wordRuleRelevanceListBo.getPage(), wordRuleRelevanceListBo.getLimit(), iPage);

        List<WordRuleRelevanceEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<WordRuleRelevanceListVo> vos = records.stream().map(item -> {
                WordRuleRelevanceListVo wordRuleRelevanceVo = new WordRuleRelevanceListVo();
                BeanUtils.copyProperties(item, wordRuleRelevanceVo);
                return wordRuleRelevanceVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public WordRuleRelevanceInfoVo info(Long id) {

        WordRuleRelevanceEntity wordRuleRelevanceEntity = wordRuleRelevanceService.getById(id);
        if(wordRuleRelevanceEntity != null) {
            WordRuleRelevanceInfoVo wordRuleRelevanceInfoVo = new WordRuleRelevanceInfoVo();
            BeanUtils.copyProperties(wordRuleRelevanceEntity, wordRuleRelevanceInfoVo);
            return wordRuleRelevanceInfoVo;
        }

        return null;
    }

    /**
     * 新增规则关联词
     * @param wordRuleRelevanceBo 规则关联词对象
     * @return
     */
     public WordRuleRelevanceInfoVo save(WordRuleRelevanceBo wordRuleRelevanceBo) {

         WordRuleRelevanceEntity wordRuleRelevanceEntity = new WordRuleRelevanceEntity();
         BeanUtils.copyProperties(wordRuleRelevanceBo, wordRuleRelevanceEntity);
         wordRuleRelevanceEntity.setId(SnowflakeManager.nextValue());
         wordRuleRelevanceEntity.setCreateDate(new Date());
         wordRuleRelevanceEntity.setUpdateDate(new Date());

         wordRuleRelevanceService.save(wordRuleRelevanceEntity);

         WordRuleRelevanceInfoVo wordRuleRelevanceInfoVo = new WordRuleRelevanceInfoVo();
         BeanUtils.copyProperties(wordRuleRelevanceEntity, wordRuleRelevanceInfoVo);

         return wordRuleRelevanceInfoVo;
     }

    /**
     * 修改规则关联词
     * @param wordRuleRelevanceBo 规则关联词对象
     * @return
     */
    public void update(WordRuleRelevanceBo wordRuleRelevanceBo) {

        WordRuleRelevanceEntity wordRuleRelevanceEntity = new WordRuleRelevanceEntity();
        BeanUtils.copyProperties(wordRuleRelevanceBo, wordRuleRelevanceEntity);
        wordRuleRelevanceEntity.setUpdateDate(new Date());

        wordRuleRelevanceService.updateById(wordRuleRelevanceEntity);
    }

    /**
     * 删除规则关联词
     * @param id 规则关联词id
     * @return
     */
    public void deleteById(Long id) {

        wordRuleRelevanceService.removeById(id);
    }


}

