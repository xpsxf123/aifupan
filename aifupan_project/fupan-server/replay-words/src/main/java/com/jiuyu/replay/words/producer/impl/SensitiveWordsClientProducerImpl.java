package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.producer.SensitiveWordsClientProducer;
import com.jiuyu.replay.words.repository.service.SensitiveWordsClientService;
import com.jiuyu.replay.words.repository.service.TradeService;
import com.jiuyu.replay.words.repository.service.WordRuleRelevanceService;
import com.jiuyu.replay.words.repository.service.WordRuleService;
import com.jiuyu.replay.words.vo.*;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 客户端自定义词语
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:20:06
 */
@Service
public class SensitiveWordsClientProducerImpl implements SensitiveWordsClientProducer {

    @Resource
    private SensitiveWordsClientService sensitiveWordsClientService;
    @Resource
    private TradeService tradeService;
    @Resource
    private WordRuleService wordRuleService;
    @Resource
    private WordRuleRelevanceService wordRuleRelevanceService;


    @Override
    public PageUtils<SensitiveWordsClientListVo> queryPage(SensitiveWordsClientListBo sensitiveWordsListBo) {
        QueryWrapper<SensitiveWordsClientEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(sensitiveWordsListBo.getKeyword())) {
            List<SensitiveWordsClientEntity> sensitiveWordsEntities = this.sensitiveWordsClientService.list(new QueryWrapper<SensitiveWordsClientEntity>().like("name", sensitiveWordsListBo.getKeyword()));
            if(sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {
                List<Long> ids = new LinkedList<>();
                for (SensitiveWordsClientEntity sensitiveWordsEntity : sensitiveWordsEntities) {
                    if(!StringUtils.isEmpty(sensitiveWordsListBo.getWordLength())) {
                        // 如果长度不为空，则只查长度符合要求的词语
                        if(sensitiveWordsEntity.getName().length() == sensitiveWordsListBo.getWordLength()) {
                            if(sensitiveWordsEntity.getParentId().equals(0L)) {
                                ids.add(sensitiveWordsEntity.getId());
                            }else {
                                ids.add(sensitiveWordsEntity.getParentId());
                            }
                        }
                    }else {
                        if(sensitiveWordsEntity.getParentId().equals(0L)) {
                            ids.add(sensitiveWordsEntity.getId());
                        }else {
                            ids.add(sensitiveWordsEntity.getParentId());
                        }
                    }
                }
                if(ids.size() > 0) {
                    wrapper.in("id", ids);
                }else {
                    wrapper.eq("id", 0);
                }

            }else {
                wrapper.eq("id", 0);
            }
        }
        if (!StringUtils.isEmpty(sensitiveWordsListBo.getResourceType())) {
            wrapper.eq("resource_type", sensitiveWordsListBo.getResourceType());
        }
        if (!StringUtils.isEmpty(sensitiveWordsListBo.getType())) {
            wrapper.eq("type", sensitiveWordsListBo.getType());
        }
        if (!StringUtils.isEmpty(sensitiveWordsListBo.getPlatformType())) {
            wrapper.eq("platform_type", sensitiveWordsListBo.getPlatformType());
        }
        if (!StringUtils.isEmpty(sensitiveWordsListBo.getLevel())) {
            wrapper.eq("level", sensitiveWordsListBo.getLevel());
        }
        if (!StringUtils.isEmpty(sensitiveWordsListBo.getTradeId())) {
            wrapper.eq("trade_id", sensitiveWordsListBo.getTradeId());
        }
        if (!StringUtils.isEmpty(sensitiveWordsListBo.getGroupStr())) {
            wrapper.like("group_str", sensitiveWordsListBo.getGroupStr());
        }
        if (!StringUtils.isEmpty(sensitiveWordsListBo.getWordsType())) {
            wrapper.eq("words_type", sensitiveWordsListBo.getWordsType());
        }
        if (!StringUtils.isEmpty(sensitiveWordsListBo.getWordLength())) {
            List<SensitiveWordsClientEntity> sensitiveWordsEntities = this.sensitiveWordsClientService.list(new QueryWrapper<SensitiveWordsClientEntity>().apply(" CHAR_LENGTH(name) = " + sensitiveWordsListBo.getWordLength()));
            if(sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {
                List<Long> ids = new LinkedList<>();
                for (SensitiveWordsClientEntity sensitiveWordsEntity : sensitiveWordsEntities) {
                    if(sensitiveWordsEntity.getParentId().equals(0L)) {
                        ids.add(sensitiveWordsEntity.getId());
                    }else {
                        ids.add(sensitiveWordsEntity.getParentId());
                    }
                }
                wrapper.in("id", ids);
            }else {
                wrapper.eq("id", 0);
            }
        }
        if (sensitiveWordsListBo.getCruxTypeIds() != null && sensitiveWordsListBo.getCruxTypeIds().size() > 0) {
            wrapper.in("crux_type_id", sensitiveWordsListBo.getCruxTypeIds());
        }
        wrapper.eq("parent_id", 0L);

        IPage<SensitiveWordsClientEntity> iPage = sensitiveWordsClientService.page(new Query<SensitiveWordsClientEntity>().getPage(sensitiveWordsListBo.getPage(), sensitiveWordsListBo.getLimit()), wrapper);

        PageUtils<SensitiveWordsClientListVo> pageUtils = new PageUtils<>(sensitiveWordsListBo.getPage(), sensitiveWordsListBo.getLimit(), iPage);

        List<SensitiveWordsClientEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {

            // 获取所有相似词
            List<Long> wordIds = records.stream().map(SensitiveWordsClientEntity::getId).toList();
            List<SensitiveWordsClientEntity> similarList = this.sensitiveWordsClientService.list(new QueryWrapper<SensitiveWordsClientEntity>().in("parent_id", wordIds));

            List<SensitiveWordsClientListVo> vos = records.stream().map(item -> {
                SensitiveWordsClientListVo sensitiveWordsVo = new SensitiveWordsClientListVo();
                BeanUtils.copyProperties(item, sensitiveWordsVo);
                List<SensitiveWordsClientListVo> similarWordList = new LinkedList<>();
                if(similarList != null && similarList.size() > 0) {
                    for (SensitiveWordsClientEntity sensitiveWordsEntity : similarList) {
                        if(sensitiveWordsEntity.getParentId().equals(item.getId())) {
                            SensitiveWordsClientListVo similarWord = new SensitiveWordsClientListVo();
                            BeanUtils.copyProperties(sensitiveWordsEntity, similarWord);
                            boolean isMark = true;
                            if(!StringUtils.isEmpty(sensitiveWordsListBo.getKeyword())) {
                                if(!similarWord.getName().contains(sensitiveWordsListBo.getKeyword())) {
                                    isMark = false;
                                }
                            }
                            if(!StringUtils.isEmpty(sensitiveWordsListBo.getWordLength())) {
                                if(similarWord.getName().length() != sensitiveWordsListBo.getWordLength()) {
                                    isMark = false;
                                }
                            }
                            if(!StringUtils.isEmpty(sensitiveWordsListBo.getLevel())) {
                                if(!similarWord.getLevel().equals(sensitiveWordsListBo.getLevel())) {
                                    isMark = false;
                                }
                            }
                            if(isMark) {
                                similarWord.setIsMark(1);
                            }
                            similarWordList.add(similarWord);
                        }
                    }
                }
                sensitiveWordsVo.setSimilarWordList(similarWordList);
                return sensitiveWordsVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public SensitiveWordsClientInfoVo info(Long id) {

        SensitiveWordsClientEntity sensitiveWordsEntity = sensitiveWordsClientService.getById(id);
        if (sensitiveWordsEntity != null) {
            SensitiveWordsClientInfoVo sensitiveWordsInfoVo = new SensitiveWordsClientInfoVo();
            BeanUtils.copyProperties(sensitiveWordsEntity, sensitiveWordsInfoVo);
            return sensitiveWordsInfoVo;
        }

        return null;
    }

    /**
     * 修改词语
     *
     * @param sensitiveWordsBo 词语对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(SensitiveWordsClientBo sensitiveWordsBo) {

        SensitiveWordsClientEntity sensitiveWordsEntity = new SensitiveWordsClientEntity();
        BeanUtils.copyProperties(sensitiveWordsBo, sensitiveWordsEntity);
        sensitiveWordsEntity.setUpdateDate(new Date());

        sensitiveWordsClientService.updateById(sensitiveWordsEntity);


    }

    /**
     * 删除客户端自定义词语
     *
     * @param id 客户端自定义词语id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(Long id) {
        // 删除词语
        sensitiveWordsClientService.removeById(id);

        // 删除相似词
        sensitiveWordsClientService.remove(new QueryWrapper<SensitiveWordsClientEntity>().eq("parent_id", id));
    }

    @Override
    public SensitiveWordsClientVo exist(SensitiveWordsClientBo sensitiveWordsBo) {
        QueryWrapper<SensitiveWordsClientEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("name", sensitiveWordsBo.getName());
        wrapper.eq("trade_id", sensitiveWordsBo.getTradeId());
        wrapper.eq("resource_type", sensitiveWordsBo.getResourceType());
        wrapper.eq("platform_type", sensitiveWordsBo.getPlatformType());
        wrapper.eq("words_type", sensitiveWordsBo.getWordsType());

        SensitiveWordsClientEntity sensitiveWordsEntity = this.sensitiveWordsClientService.getOne(wrapper);

        if(sensitiveWordsEntity != null) {
            SensitiveWordsClientVo sensitiveWordsVo = new SensitiveWordsClientVo();
            BeanUtils.copyProperties(sensitiveWordsEntity, sensitiveWordsVo);
            return sensitiveWordsVo;
        }

        return null;
    }

    @Override
    public List<SensitiveWordsClientVo> listByUidAndPidAndTid(Long userId, Integer platformType, Long tradeId, Integer wordsType) {

        QueryWrapper<SensitiveWordsClientEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("words_type", wordsType);
        wrapper.eq("status", 0);
//        wrapper.and(w -> {
//            w.eq("resource_type", 0).or(w1 -> {
//                w1.eq("resource_type", 1).eq("user_id", userId);
//            });
//        });
        wrapper.eq("resource_type", 1);
        wrapper.eq("user_id", userId);
        wrapper.and(w -> {
            w.in("platform_type", 0, platformType);
        });

        // 查找行业以及全行业以及当前行业id所有父级行业id
        List<Long> tradeIds = new LinkedList<>();
        tradeIds.add(1L);
        tradeIds.add(tradeId);
        TradeEntity tradeEntity = this.tradeService.getById(tradeId);
        if(!tradeEntity.getParentId().equals(0L)) {
            Long parentId = tradeEntity.getParentId();
            while (!parentId.equals(0L)) {
                tradeIds.add(parentId);
                TradeEntity parentTradeEntity = this.tradeService.getById(parentId);
                parentId = parentTradeEntity.getParentId();
            }
        }
        wrapper.and(w -> {
            w.in("trade_id", tradeIds);
        });

        List<SensitiveWordsClientEntity> sensitiveWordsEntities = this.sensitiveWordsClientService.list(wrapper);

        if (sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {
            List<SensitiveWordsClientVo> sensitiveWordsVos = sensitiveWordsEntities.stream().map(item -> {
                SensitiveWordsClientVo sensitiveWordsVo = new SensitiveWordsClientVo();
                BeanUtils.copyProperties(item, sensitiveWordsVo);
                return sensitiveWordsVo;
            }).collect(Collectors.toList());

            return sensitiveWordsVos;
        }

        return null;
    }

    @Override
    public List<String> existSimilarList(SensitiveWordsClientBo sensitiveWordsBo) {

        List<String> nameList = new LinkedList<>();
//        nameList.add(sensitiveWordsBo.getName());

        if (sensitiveWordsBo.getSimilarWordList() != null && sensitiveWordsBo.getSimilarWordList().size() > 0) {

            List<String> similarNameList = sensitiveWordsBo.getSimilarWordList().stream().map(SensitiveWordsClientBo::getName).toList();

            nameList.addAll(similarNameList);

            QueryWrapper<SensitiveWordsClientEntity> wrapper = new QueryWrapper<>();
            wrapper.in("name", nameList);
            wrapper.eq("trade_id", sensitiveWordsBo.getTradeId());
            wrapper.eq("resource_type", sensitiveWordsBo.getResourceType());
            wrapper.eq("platform_type", sensitiveWordsBo.getPlatformType());
            wrapper.eq("words_type", sensitiveWordsBo.getWordsType());

            List<SensitiveWordsClientEntity> sensitiveWordsEntities = this.sensitiveWordsClientService.list(wrapper);
            if (sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {
                return sensitiveWordsEntities.stream().map(SensitiveWordsClientEntity::getName).toList();
            }

        }

        return null;
    }

    @Override
    public SensitiveWordsClientVo saveWord(SensitiveWordsClientBo sensitiveWordsBo) {

        SensitiveWordsClientEntity sensitiveWordsEntity = new SensitiveWordsClientEntity();
        BeanUtils.copyProperties(sensitiveWordsBo, sensitiveWordsEntity);
        sensitiveWordsEntity.setId(SnowflakeManager.nextValue());
        sensitiveWordsEntity.setCreateDate(new Date());
        sensitiveWordsEntity.setUpdateDate(new Date());
        this.sensitiveWordsClientService.save(sensitiveWordsEntity);

        SensitiveWordsClientVo sensitiveWordsVo = new SensitiveWordsClientVo();
        BeanUtils.copyProperties(sensitiveWordsEntity, sensitiveWordsVo);

        return sensitiveWordsVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateSimilar(SensitiveWordsClientBo sensitiveWordsBo, List<DictDataListVo> platformList) {

        // 删除原相似词
        this.sensitiveWordsClientService.remove(new QueryWrapper<SensitiveWordsClientEntity>().eq("parent_id", sensitiveWordsBo.getId()));

        StringBuilder resultMsg = new StringBuilder("修改成功");

        // 添加新相似词
        List<SensitiveWordsClientBo> similarWordList = sensitiveWordsBo.getSimilarWordList();
        if(similarWordList != null && similarWordList.size() > 0) {
            List<String> existList =  this.existSimilarList(sensitiveWordsBo);

            if(existList != null && existList.size() > 0) {
                resultMsg.append("，");
                if(platformList != null && platformList.size() > 0) {
                    resultMsg.append(platformList.stream().filter(item -> item.getValue().equals(sensitiveWordsBo.getPlatformType() + "")).toList().get(0).getLabel());
                }
//                resultMsg.append(platformList.get(sensitiveWordsBo.getPlatformType()).getLabel());
                resultMsg.append("因重复而被忽略的相似词：");
                for (String wordsName : existList) {
                    resultMsg.append("-").append(wordsName);
                }
            }

            this.addNewSimilar(sensitiveWordsBo, sensitiveWordsBo.getId(), similarWordList, existList);
        }

        return resultMsg.toString();

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSimilar(SensitiveWordsClientBo sensitiveWordsBo, SensitiveWordsClientVo sensitiveWordsVo, List<String> existList) {

        List<SensitiveWordsClientBo> similarWordList = sensitiveWordsBo.getSimilarWordList();

        if(similarWordList != null && similarWordList.size() > 0) {
            this.addNewSimilar(sensitiveWordsBo, sensitiveWordsVo.getId(), similarWordList, existList);
        }

    }

    @Override
    public List<SensitiveWordsClientInfoVo> listByParentId(Long id) {

        List<SensitiveWordsClientEntity> sensitiveWordsEntities = this.sensitiveWordsClientService.list(new QueryWrapper<SensitiveWordsClientEntity>().eq("parent_id", id));
        if(sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {

            // 获取规则列表
            List<Long> wordIds = sensitiveWordsEntities.stream().map(SensitiveWordsClientEntity::getId).toList();
            List<WordRuleEntity> wordRuleEntities = this.wordRuleService.list(new QueryWrapper<WordRuleEntity>().in("word_id", wordIds));

            List<WordRuleInfoVo> wordRuleInfoVos = new LinkedList<>();
            if(wordRuleEntities != null && wordRuleEntities.size() > 0) {
                // 获取规则关联词
                List<Long> ruleIds = wordRuleEntities.stream().map(WordRuleEntity::getId).toList();
                List<WordRuleRelevanceEntity> wordRuleRelevanceEntities = this.wordRuleRelevanceService.list(new QueryWrapper<WordRuleRelevanceEntity>().in("rule_id", ruleIds));
                List<WordRuleInfoVo> tempRuleInfoVos = wordRuleEntities.stream().map(item -> {
                    WordRuleInfoVo wordRuleInfoVo = new WordRuleInfoVo();
                    BeanUtils.copyProperties(item, wordRuleInfoVo);
                    // 设置规则关联词
                    List<String> relevanceWordArr = new LinkedList<>();
                    for (WordRuleRelevanceEntity wordRuleRelevanceEntity : wordRuleRelevanceEntities) {
                        if (wordRuleRelevanceEntity.getRuleId().equals(wordRuleInfoVo.getId())) {
                            relevanceWordArr.add(wordRuleRelevanceEntity.getRelevanceWord());
                        }
                    }
                    wordRuleInfoVo.setContainerWords(String.join("#", relevanceWordArr));
                    return wordRuleInfoVo;
                }).toList();
                if(tempRuleInfoVos.size() > 0) {
                    wordRuleInfoVos.addAll(tempRuleInfoVos);
                }
            }

            List<SensitiveWordsClientInfoVo> sensitiveWordsVos = sensitiveWordsEntities.stream().map(item -> {
                SensitiveWordsClientInfoVo sensitiveWordsVo = new SensitiveWordsClientInfoVo();
                BeanUtils.copyProperties(item, sensitiveWordsVo);
                // 设置词语规则列表
                List<WordRuleInfoVo> ruleList = new LinkedList<>();
                for (WordRuleInfoVo wordRuleInfoVo : wordRuleInfoVos) {
                    if(wordRuleInfoVo.getWordId().equals(sensitiveWordsVo.getId())) {
                        ruleList.add(wordRuleInfoVo);
                    }
                }
                sensitiveWordsVo.setRuleList(ruleList);
                return sensitiveWordsVo;
            }).toList();

            return sensitiveWordsVos;
        }

        return null;
    }


    /**
     * 添加新的相似词
     * @param wordsId 词语id
     * @param similarWordList 要添加的相似词列表
     * @param existList 已存在的列表
     */
    private void addNewSimilar(SensitiveWordsClientBo sensitiveWordsBo, Long wordsId, List<SensitiveWordsClientBo> similarWordList, List<String> existList) {
        // 不存在的相似词
        List<SensitiveWordsClientBo> notExistSimilarList = new LinkedList<>();

        if(existList != null && existList.size() > 0) {
            for (SensitiveWordsClientBo wordsBo : similarWordList) {
                boolean existFlag = false;
                for (String name : existList) {
                    if(wordsBo.getName().equals(name)) {
                        existFlag = true;
                    }
                }
                if(!existFlag) {
                    notExistSimilarList.add(wordsBo);
                }
            }
        }else {
            // 原本没有相似词，全部添加
            notExistSimilarList.addAll(similarWordList);
        }

        // 相似词规则列表
        List<WordRuleEntity> wordRuleEntities = new LinkedList<>();
        // 规则关联词列表
        List<WordRuleRelevanceEntity> wordRuleRelevanceEntities = new LinkedList<>();

        // 保存相似词
        if(notExistSimilarList.size() > 0) {
            List<SensitiveWordsClientEntity> wordsEntities = notExistSimilarList.stream().map(item -> {
                SensitiveWordsClientEntity similar = new SensitiveWordsClientEntity();
                BeanUtils.copyProperties(sensitiveWordsBo, similar);
                similar.setId(SnowflakeManager.nextValue());
                similar.setCreateDate(new Date());
                similar.setUpdateDate(new Date());
                similar.setName(item.getName());
                similar.setRemarks(item.getRemarks());
                similar.setParentId(wordsId);
                if(!StringUtils.isEmpty(item.getLevel())) {
                    similar.setLevel(item.getLevel());
                }
                // 将相似词规则添加到列表
                if(item.getRuleList() != null && item.getRuleList().size() > 0) {
                    for (WordRuleBo wordRuleBo : item.getRuleList()) {
                        WordRuleEntity wordRuleEntity = new WordRuleEntity();
                        BeanUtils.copyProperties(wordRuleBo, wordRuleEntity);
                        wordRuleEntity.setId(SnowflakeManager.nextValue());
                        wordRuleEntity.setWordId(similar.getId());
                        wordRuleEntity.setCreateDate(new Date());
                        wordRuleEntity.setUpdateDate(new Date());
                        wordRuleEntities.add(wordRuleEntity);
                        // 添加规则关联词到列表
                        if(!StringUtils.isEmpty(wordRuleBo.getContainerWords())) {
                            String containerWords = wordRuleBo.getContainerWords().replaceAll("[ \\t\\n\\r\\f\\v]", "");
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
                    }
                }
                return similar;
            }).toList();

            // 保存相似词
            this.sensitiveWordsClientService.saveBatch(wordsEntities);

            // 保存相似词的规则
            if(wordRuleEntities.size() > 0) {
                this.wordRuleService.saveBatch(wordRuleEntities);
            }

            // 保存规则的关联词
            if(wordRuleRelevanceEntities.size() > 0) {
                this.wordRuleRelevanceService.saveBatch(wordRuleRelevanceEntities);
            }
        }
    }
}

