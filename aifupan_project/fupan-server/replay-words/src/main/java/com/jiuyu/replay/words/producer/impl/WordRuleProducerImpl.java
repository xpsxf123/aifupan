package com.jiuyu.replay.words.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.words.entity.WordRuleRelevanceEntity;
import com.jiuyu.replay.words.repository.service.WordRuleRelevanceService;
import com.jiuyu.replay.words.vo.WordRuleListVo;
import com.jiuyu.replay.words.vo.WordRuleInfoVo;
import com.jiuyu.replay.words.bo.WordRuleBo;
import com.jiuyu.replay.words.bo.WordRuleListBo;
import com.jiuyu.replay.words.repository.service.WordRuleService;
import com.jiuyu.replay.words.entity.WordRuleEntity;
import com.jiuyu.replay.words.producer.WordRuleProducer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 词语匹配规则
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@Service
public class WordRuleProducerImpl implements WordRuleProducer {

    @Resource
    private WordRuleService wordRuleService;
    @Resource
    private WordRuleRelevanceService wordRuleRelevanceService;


    @Override
    public PageUtils<WordRuleListVo> queryPage(WordRuleListBo wordRuleListBo) {
        QueryWrapper<WordRuleEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(wordRuleListBo.getKeyword())){
            wrapper.like("name", wordRuleListBo.getKeyword());
        }

        IPage<WordRuleEntity> iPage = wordRuleService.page(new Query<WordRuleEntity>().getPage(wordRuleListBo.getPage(), wordRuleListBo.getLimit()), wrapper);

        PageUtils<WordRuleListVo> pageUtils = new PageUtils<>(wordRuleListBo.getPage(), wordRuleListBo.getLimit(), iPage);

        List<WordRuleEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<WordRuleListVo> vos = records.stream().map(item -> {
                WordRuleListVo wordRuleVo = new WordRuleListVo();
                BeanUtils.copyProperties(item, wordRuleVo);
                return wordRuleVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public WordRuleInfoVo info(Long id) {

        WordRuleEntity wordRuleEntity = wordRuleService.getById(id);
        if(wordRuleEntity != null) {
            WordRuleInfoVo wordRuleInfoVo = new WordRuleInfoVo();
            BeanUtils.copyProperties(wordRuleEntity, wordRuleInfoVo);
            return wordRuleInfoVo;
        }

        return null;
    }

    /**
     * 新增词语匹配规则
     * @param wordRuleBo 词语匹配规则对象
     * @return
     */
     public WordRuleInfoVo save(WordRuleBo wordRuleBo) {

         WordRuleEntity wordRuleEntity = new WordRuleEntity();
         BeanUtils.copyProperties(wordRuleBo, wordRuleEntity);
         wordRuleEntity.setId(SnowflakeManager.nextValue());
         wordRuleEntity.setCreateDate(new Date());
         wordRuleEntity.setUpdateDate(new Date());

         wordRuleService.save(wordRuleEntity);

         WordRuleInfoVo wordRuleInfoVo = new WordRuleInfoVo();
         BeanUtils.copyProperties(wordRuleEntity, wordRuleInfoVo);

         return wordRuleInfoVo;
     }

    /**
     * 修改词语匹配规则
     * @param wordRuleBo 词语匹配规则对象
     * @return
     */
    public void update(WordRuleBo wordRuleBo) {

        WordRuleEntity wordRuleEntity = new WordRuleEntity();
        BeanUtils.copyProperties(wordRuleBo, wordRuleEntity);
        wordRuleEntity.setUpdateDate(new Date());

        wordRuleService.updateById(wordRuleEntity);
    }

    /**
     * 删除词语匹配规则
     * @param id 词语匹配规则id
     * @return
     */
    public void deleteById(Long id) {

        wordRuleService.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatchByWordId(Long wordId, List<WordRuleBo> ruleList) {
        if(ruleList != null && ruleList.size() > 0) {
            // 规则关联词列表
            List<WordRuleRelevanceEntity> wordRuleRelevanceEntities = new LinkedList<>();

            List<WordRuleEntity> wordRuleEntities = ruleList.stream().map(item -> {
                WordRuleEntity wordRuleEntity = new WordRuleEntity();
                BeanUtils.copyProperties(item, wordRuleEntity);
                wordRuleEntity.setId(SnowflakeManager.nextValue());
                wordRuleEntity.setWordId(wordId);
                wordRuleEntity.setCreateDate(new Date());
                wordRuleEntity.setUpdateDate(new Date());
                // 添加规则关联词到列表
                if(!StringUtils.isEmpty(item.getContainerWords())) {
                    String containerWords = item.getContainerWords().replaceAll("[ \\t\\n\\r\\f\\v]", "");
                    if(!StringUtils.isEmpty(containerWords)) {
                        String[] containerWordArr = containerWords.split("#");
                        for (String containerWord : containerWordArr) {
                            if(!StringUtils.isEmpty(containerWord)) {
                                WordRuleRelevanceEntity wordRuleRelevanceEntity = new WordRuleRelevanceEntity();
                                wordRuleRelevanceEntity.setId(SnowflakeManager.nextValue());
                                wordRuleRelevanceEntity.setRuleId(wordRuleEntity.getId());
                                wordRuleRelevanceEntity.setRelevanceWord(containerWord);
                                wordRuleRelevanceEntity.setCreateDate(new Date());
                                wordRuleRelevanceEntity.setUpdateDate(new Date());
                                wordRuleRelevanceEntities.add(wordRuleRelevanceEntity);
                            }
                        }
                    }
                }
                return wordRuleEntity;
            }).toList();

            // 保存规则
            this.wordRuleService.saveBatch(wordRuleEntities);

            // 保存规则关联词
            if(wordRuleRelevanceEntities.size() > 0) {
                this.wordRuleRelevanceService.saveBatch(wordRuleRelevanceEntities);
            }

        }
    }

    @Override
    public List<WordRuleInfoVo> listByWordId(Long wordId) {
        List<WordRuleEntity> wordRuleEntities = this.wordRuleService.list(new QueryWrapper<WordRuleEntity>().eq("word_id", wordId));
        if(wordRuleEntities != null && wordRuleEntities.size() > 0) {
            List<Long> ruleIds = wordRuleEntities.stream().map(WordRuleEntity::getId).toList();
            List<WordRuleRelevanceEntity> wordRuleRelevanceEntities = this.wordRuleRelevanceService.list(new QueryWrapper<WordRuleRelevanceEntity>().in("rule_id", ruleIds));

            List<WordRuleInfoVo> wordRuleInfoVos = wordRuleEntities.stream().map(item -> {
                WordRuleInfoVo wordRuleInfoVo = new WordRuleInfoVo();
                BeanUtils.copyProperties(item, wordRuleInfoVo);
                List<String> relevanceWordArr = new LinkedList<>();
                // 设置规则关联词
                for (WordRuleRelevanceEntity wordRuleRelevanceEntity : wordRuleRelevanceEntities) {
                    if(wordRuleRelevanceEntity.getRuleId().equals(wordRuleInfoVo.getId())) {
                        relevanceWordArr.add(wordRuleRelevanceEntity.getRelevanceWord());
                    }
                }
                wordRuleInfoVo.setContainerWordList(relevanceWordArr);
                wordRuleInfoVo.setContainerWords(String.join("#", relevanceWordArr));
                return wordRuleInfoVo;
            }).toList();

            return wordRuleInfoVos;
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatchByWordId(Long wordId, List<WordRuleBo> ruleList) {
        // 删除原规则
        this.removeByWordId(wordId);
        // 添加新规则
        if(ruleList != null && ruleList.size() > 0) {
            this.saveBatchByWordId(wordId, ruleList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByWordId(Long wordId) {

        List<WordRuleEntity> wordRuleEntities = this.wordRuleService.list(new QueryWrapper<WordRuleEntity>().eq("word_id", wordId));
        if(wordRuleEntities != null && wordRuleEntities.size() > 0) {
            List<Long> ruleIds = wordRuleEntities.stream().map(WordRuleEntity::getId).toList();
            // 删除规则
            this.wordRuleService.removeBatchByIds(ruleIds);
            // 删除规则关联词
            this.wordRuleRelevanceService.remove(new QueryWrapper<WordRuleRelevanceEntity>().in("rule_id", ruleIds));
        }
    }

    @Override
    public List<WordRuleInfoVo> listByWordIds(List<Long> wordIds) {

        List<WordRuleEntity> wordRuleEntities = this.wordRuleService.list(new QueryWrapper<WordRuleEntity>().in("word_id", wordIds));
        if(wordRuleEntities != null && wordRuleEntities.size() > 0) {
            List<Long> ruleIds = wordRuleEntities.stream().map(WordRuleEntity::getId).toList();
            List<WordRuleRelevanceEntity> wordRuleRelevanceEntities = this.wordRuleRelevanceService.list(new QueryWrapper<WordRuleRelevanceEntity>().in("rule_id", ruleIds));

            List<WordRuleInfoVo> wordRuleInfoVos = wordRuleEntities.stream().map(item -> {
                WordRuleInfoVo wordRuleInfoVo = new WordRuleInfoVo();
                BeanUtils.copyProperties(item, wordRuleInfoVo);
                List<String> relevanceWordArr = new LinkedList<>();
                // 设置规则关联词
                for (WordRuleRelevanceEntity wordRuleRelevanceEntity : wordRuleRelevanceEntities) {
                    if (wordRuleRelevanceEntity.getRuleId().equals(wordRuleInfoVo.getId())) {
                        relevanceWordArr.add(wordRuleRelevanceEntity.getRelevanceWord());
                    }
                }
                wordRuleInfoVo.setContainerWordList(relevanceWordArr);
                wordRuleInfoVo.setContainerWords(String.join("#", relevanceWordArr));
                return wordRuleInfoVo;
            }).toList();

            return wordRuleInfoVos;
        }

        return null;
    }


}

