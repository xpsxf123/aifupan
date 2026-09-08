package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.alibaba.AiOssUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.producer.SensitiveWordsProducer;
import com.jiuyu.replay.words.repository.service.*;
import com.jiuyu.replay.words.vo.*;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * 敏感词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:20:06
 */
@Service
public class SensitiveWordsProducerImpl implements SensitiveWordsProducer {

    @Resource
    private SensitiveWordsService sensitiveWordsService;
    @Resource
    private TradeService tradeService;
    @Resource
    private WordRuleService wordRuleService;
    @Resource
    private WordRuleRelevanceService wordRuleRelevanceService;
    @Resource
    private WordsProperties wordsProperties;
    @Resource
    private CruxTypeService cruxTypeService;
    @Resource
    private AiOssUtils aiOssUtils;


    @Override
    public PageUtils<SensitiveWordsListVo> queryPage(SensitiveWordsListBo sensitiveWordsListBo) {
        QueryWrapper<SensitiveWordsEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(sensitiveWordsListBo.getKeyword())) {
            List<SensitiveWordsEntity> sensitiveWordsEntities = this.sensitiveWordsService.list(new QueryWrapper<SensitiveWordsEntity>().like("name", sensitiveWordsListBo.getKeyword()));
            if(sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {
                List<Long> ids = new LinkedList<>();
                for (SensitiveWordsEntity sensitiveWordsEntity : sensitiveWordsEntities) {
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
            List<SensitiveWordsEntity> sensitiveWordsEntities = this.sensitiveWordsService.list(new QueryWrapper<SensitiveWordsEntity>().apply(" CHAR_LENGTH(name) = " + sensitiveWordsListBo.getWordLength()));
            if(sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {
                List<Long> ids = new LinkedList<>();
                for (SensitiveWordsEntity sensitiveWordsEntity : sensitiveWordsEntities) {
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

        IPage<SensitiveWordsEntity> iPage = sensitiveWordsService.page(new Query<SensitiveWordsEntity>().getPage(sensitiveWordsListBo.getPage(), sensitiveWordsListBo.getLimit()), wrapper);

        PageUtils<SensitiveWordsListVo> pageUtils = new PageUtils<>(sensitiveWordsListBo.getPage(), sensitiveWordsListBo.getLimit(), iPage);

        List<SensitiveWordsEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {

            List<Long> cruxTypeIds = records.stream().map(SensitiveWordsEntity::getCruxTypeId).collect(Collectors.toList());
            List<CruxTypeEntity> cruxTypeEntities = this.cruxTypeService.listByIds(cruxTypeIds);

            // 获取所有相似词
            List<Long> wordIds = records.stream().map(SensitiveWordsEntity::getId).toList();
            List<SensitiveWordsEntity> similarList = this.sensitiveWordsService.list(new QueryWrapper<SensitiveWordsEntity>().in("parent_id", wordIds));

            List<SensitiveWordsListVo> vos = records.stream().map(item -> {
                SensitiveWordsListVo sensitiveWordsVo = new SensitiveWordsListVo();
                BeanUtils.copyProperties(item, sensitiveWordsVo);

                // 封装关键词分类
                if(cruxTypeEntities != null && cruxTypeEntities.size() > 0) {
                    if(sensitiveWordsVo.getWordsType() == 1) {
                        for (CruxTypeEntity cruxTypeEntity : cruxTypeEntities) {
                            if(cruxTypeEntity.getId().equals(sensitiveWordsVo.getCruxTypeId())) {
                                sensitiveWordsVo.setCruxTypeName(cruxTypeEntity.getName());
                                break;
                            }
                        }
                    }
                }

                // 封装相似词
                List<SensitiveWordsListVo> similarWordList = new LinkedList<>();
                if(similarList != null && similarList.size() > 0) {
                    for (SensitiveWordsEntity sensitiveWordsEntity : similarList) {
                        if(sensitiveWordsEntity.getParentId().equals(item.getId())) {
                            SensitiveWordsListVo similarWord = new SensitiveWordsListVo();
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
    public SensitiveWordsInfoVo info(Long id) {

        SensitiveWordsEntity sensitiveWordsEntity = sensitiveWordsService.getById(id);
        if (sensitiveWordsEntity != null) {
            SensitiveWordsInfoVo sensitiveWordsInfoVo = new SensitiveWordsInfoVo();
            BeanUtils.copyProperties(sensitiveWordsEntity, sensitiveWordsInfoVo);
            return sensitiveWordsInfoVo;
        }

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(SensitiveWordsBo sensitiveWordsBo, List<String> existList) {

        // 保存父词语
        SensitiveWordsEntity sensitiveWordsEntity = new SensitiveWordsEntity();
        BeanUtils.copyProperties(sensitiveWordsBo, sensitiveWordsEntity);
        sensitiveWordsEntity.setId(SnowflakeManager.nextValue());
        sensitiveWordsEntity.setCreateDate(new Date());
        sensitiveWordsEntity.setUpdateDate(new Date());
        this.sensitiveWordsService.save(sensitiveWordsEntity);

        // 保存子词语
        if (sensitiveWordsBo.getSimilarWordList() != null && sensitiveWordsBo.getSimilarWordList().size() > 0) {
            LinkedList<SensitiveWordsBo> sensitiveWordsBos = new LinkedList<>();
            if (existList == null) {
                // 没有已存在的词语，添加全部子词语
                sensitiveWordsBos.addAll(sensitiveWordsBo.getSimilarWordList());
            } else {
                for (SensitiveWordsBo child : sensitiveWordsBo.getSimilarWordList()) {
                    if (existList.contains(child.getName())) {
                        sensitiveWordsBos.add(child);
                    }
                }
            }

            List<SensitiveWordsEntity> sensitiveWordsEntities = sensitiveWordsBos.stream().map(item -> {
                SensitiveWordsEntity children = new SensitiveWordsEntity();
                BeanUtils.copyProperties(sensitiveWordsBo, children);
                children.setId(SnowflakeManager.nextValue());
                children.setCreateDate(new Date());
                children.setUpdateDate(new Date());
                children.setName(item.getName());
                children.setRemarks(item.getRemarks());
                children.setParentId(sensitiveWordsEntity.getId());
                return children;
            }).toList();

            sensitiveWordsService.saveBatch(sensitiveWordsEntities);

        }

        return sensitiveWordsEntity.getId();

    }

    /**
     * 修改词语
     *
     * @param sensitiveWordsBo 词语对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(SensitiveWordsBo sensitiveWordsBo) {

        SensitiveWordsEntity sensitiveWordsEntity = new SensitiveWordsEntity();
        BeanUtils.copyProperties(sensitiveWordsBo, sensitiveWordsEntity);
        sensitiveWordsEntity.setUpdateDate(new Date());

        sensitiveWordsService.updateById(sensitiveWordsEntity);


    }

    /**
     * 删除敏感词
     *
     * @param id 敏感词id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(Long id) {
        // 删除词语
        sensitiveWordsService.removeById(id);

        // 删除相似词
        sensitiveWordsService.remove(new QueryWrapper<SensitiveWordsEntity>().eq("parent_id", id));
    }

    @Override
    public SensitiveWordsVo exist(SensitiveWordsBo sensitiveWordsBo) {
        QueryWrapper<SensitiveWordsEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("name", sensitiveWordsBo.getName());
        wrapper.eq("trade_id", sensitiveWordsBo.getTradeId());
        wrapper.eq("resource_type", sensitiveWordsBo.getResourceType());
        wrapper.eq("platform_type", sensitiveWordsBo.getPlatformType());
        wrapper.eq("words_type", sensitiveWordsBo.getWordsType());

        SensitiveWordsEntity sensitiveWordsEntity = this.sensitiveWordsService.getOne(wrapper);

        if(sensitiveWordsEntity != null) {
            SensitiveWordsVo sensitiveWordsVo = new SensitiveWordsVo();
            BeanUtils.copyProperties(sensitiveWordsEntity, sensitiveWordsVo);
            return sensitiveWordsVo;
        }

        return null;
    }

    @Override
    public List<SensitiveWordsVo> listByUidAndPidAndTid(Long userId, Integer platformType, Long tradeId, Integer wordsType) {

        QueryWrapper<SensitiveWordsEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("words_type", wordsType);
        wrapper.eq("status", 0);
//        wrapper.and(w -> {
//            w.eq("resource_type", 0).or(w1 -> {
//                w1.eq("resource_type", 1).eq("user_id", userId);
//            });
//        });
        wrapper.eq("resource_type", 0);
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

        List<SensitiveWordsEntity> sensitiveWordsEntities = this.sensitiveWordsService.list(wrapper);

        if (sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {
            List<SensitiveWordsVo> sensitiveWordsVos = sensitiveWordsEntities.stream().map(item -> {
                SensitiveWordsVo sensitiveWordsVo = new SensitiveWordsVo();
                BeanUtils.copyProperties(item, sensitiveWordsVo);
                return sensitiveWordsVo;
            }).collect(Collectors.toList());

            return sensitiveWordsVos;
        }

        return null;
    }

    @Override
    public List<SensitiveWordsVo> listRepeat(SensitiveWordsBatchBo sensitiveWordsBatchBo) {

        List<WordsBatchItemBo> wordsList = sensitiveWordsBatchBo.getWordsList();
        if (wordsList != null && wordsList.size() > 0) {
            List<String> sensitiveWords = wordsList.stream().map(WordsBatchItemBo::getWords).toList();
            QueryWrapper<SensitiveWordsEntity> wrapper = new QueryWrapper<>();

            wrapper.in("name", sensitiveWords);
            wrapper.eq("trade_id", sensitiveWordsBatchBo.getTradeId());
            wrapper.eq("resource_type", sensitiveWordsBatchBo.getResourceType());
            wrapper.eq("type", sensitiveWordsBatchBo.getType());
            wrapper.eq("level", sensitiveWordsBatchBo.getLevel());
            wrapper.eq("platform_type", sensitiveWordsBatchBo.getPlatformType());

            List<SensitiveWordsEntity> sensitiveWordsEntities = this.sensitiveWordsService.list(wrapper);

            if (sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {
                List<SensitiveWordsVo> sensitiveWordsVos = sensitiveWordsEntities.stream().map(item -> {
                    SensitiveWordsVo sensitiveWordsVo = new SensitiveWordsVo();
                    BeanUtils.copyProperties(item, sensitiveWordsVo);
                    return sensitiveWordsVo;
                }).toList();

                return sensitiveWordsVos;
            }
        }
        return null;
    }

    @Override
    public void saveBatch(SensitiveWordsBatchBo sensitiveWordsBatchBo) {

        List<WordsBatchItemBo> wordsList = sensitiveWordsBatchBo.getWordsList();

        if (wordsList != null && wordsList.size() > 0) {
            List<SensitiveWordsEntity> sensitiveWordsEntities = wordsList.stream().map(item -> {
                SensitiveWordsEntity sensitiveWordsEntity = new SensitiveWordsEntity();
                BeanUtils.copyProperties(sensitiveWordsBatchBo, sensitiveWordsEntity);
                sensitiveWordsEntity.setId(SnowflakeManager.nextValue());
                sensitiveWordsEntity.setCreateDate(new Date());
                sensitiveWordsEntity.setUpdateDate(new Date());
                sensitiveWordsEntity.setName(item.getWords());
                List<String> similarWords = item.getSimilarWords();
                if (similarWords != null && similarWords.size() > 0) {
                    sensitiveWordsEntity.setSimilarWords(String.join("_", similarWords));
                }
                return sensitiveWordsEntity;
            }).filter(item -> !StringUtils.isEmpty(item.getName())).toList();

            if (sensitiveWordsEntities.size() > 0) {
                this.sensitiveWordsService.saveBatch(sensitiveWordsEntities);
            }
        }
    }

    @Override
    public List<String> existSimilarList(SensitiveWordsBo sensitiveWordsBo) {

        List<String> nameList = new LinkedList<>();
//        nameList.add(sensitiveWordsBo.getName());

        if (sensitiveWordsBo.getSimilarWordList() != null && sensitiveWordsBo.getSimilarWordList().size() > 0) {

            List<String> similarNameList = sensitiveWordsBo.getSimilarWordList().stream().map(SensitiveWordsBo::getName).toList();

            nameList.addAll(similarNameList);

            QueryWrapper<SensitiveWordsEntity> wrapper = new QueryWrapper<>();
            wrapper.in("name", nameList);
            wrapper.eq("trade_id", sensitiveWordsBo.getTradeId());
            wrapper.eq("resource_type", sensitiveWordsBo.getResourceType());
            wrapper.eq("platform_type", sensitiveWordsBo.getPlatformType());
            wrapper.eq("words_type", sensitiveWordsBo.getWordsType());

            List<SensitiveWordsEntity> sensitiveWordsEntities = this.sensitiveWordsService.list(wrapper);
            if (sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {
                return sensitiveWordsEntities.stream().map(SensitiveWordsEntity::getName).toList();
            }

        }

        return null;
    }

    @Override
    public SensitiveWordsVo saveWord(SensitiveWordsBo sensitiveWordsBo) {

        SensitiveWordsEntity sensitiveWordsEntity = new SensitiveWordsEntity();
        BeanUtils.copyProperties(sensitiveWordsBo, sensitiveWordsEntity);
        sensitiveWordsEntity.setId(SnowflakeManager.nextValue());
        sensitiveWordsEntity.setCreateDate(new Date());
        sensitiveWordsEntity.setUpdateDate(new Date());
        this.sensitiveWordsService.save(sensitiveWordsEntity);

        SensitiveWordsVo sensitiveWordsVo = new SensitiveWordsVo();
        BeanUtils.copyProperties(sensitiveWordsEntity, sensitiveWordsVo);

        return sensitiveWordsVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateSimilar(SensitiveWordsBo sensitiveWordsBo, List<DictDataListVo> platformList) {

        // 删除原相似词
        this.sensitiveWordsService.remove(new QueryWrapper<SensitiveWordsEntity>().eq("parent_id", sensitiveWordsBo.getId()));

        StringBuilder resultMsg = new StringBuilder("修改成功");

        // 添加新相似词
        List<SensitiveWordsBo> similarWordList = sensitiveWordsBo.getSimilarWordList();
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
    public void addSimilar(SensitiveWordsBo sensitiveWordsBo, SensitiveWordsVo sensitiveWordsVo, List<String> existList) {

        List<SensitiveWordsBo> similarWordList = sensitiveWordsBo.getSimilarWordList();

        if(similarWordList != null && similarWordList.size() > 0) {
            this.addNewSimilar(sensitiveWordsBo, sensitiveWordsVo.getId(), similarWordList, existList);
        }

    }

    @Override
    public List<SensitiveWordsInfoVo> listByParentId(Long id) {

        List<SensitiveWordsEntity> sensitiveWordsEntities = this.sensitiveWordsService.list(new QueryWrapper<SensitiveWordsEntity>().eq("parent_id", id));
        if(sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {

            // 获取规则列表
            List<Long> wordIds = sensitiveWordsEntities.stream().map(SensitiveWordsEntity::getId).toList();
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

            List<SensitiveWordsInfoVo> sensitiveWordsVos = sensitiveWordsEntities.stream().map(item -> {
                SensitiveWordsInfoVo sensitiveWordsVo = new SensitiveWordsInfoVo();
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

    @Override
    public String saveAnalysisDataToFile(String uuid, int type, Long tradeId, int versionNum, String jsonStr, Long recordId) {

        // 定义路径和文件名
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(new Date());

        String zipFileName = versionNum + "th-" + tradeId + "-" + recordId + ".zip";
        String storePath = "";
        if(type == 0) {
            storePath = wordsProperties.getVideoAnalysisStorePath() + dateStr + "/" + uuid + "/";
        } else {
            storePath = wordsProperties.getFileAnalysisStorePath() + dateStr + "/" + uuid + "/";
        }


        // 判断文件夹存不存在
        File directory = new File(storePath);
        if(!directory.exists()) {
            directory.mkdirs();
        }

        try(FileOutputStream fos = new FileOutputStream(storePath + zipFileName);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {

            // 创建一个新的ZIP条目
            ZipEntry entry = new ZipEntry("analysis.txt");
            zos.putNextEntry(entry);

            // 将字符串转换为字节并写入当前ZIP条目
            byte[] data = jsonStr.getBytes("UTF-8");
            zos.write(data, 0, data.length);

            // 完成当前条目的写入
            zos.closeEntry();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return dateStr + "/" + uuid + "/" + zipFileName;
    }

    @Override
    public String saveAnalysisDataToFile2_0(String version, String uuid, int type, Long tradeId, int versionNum, String jsonStr, Long recordId) {

        // 定义路径和文件名
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(new Date());

        String zipFileName = versionNum + "th-" + tradeId + "-" + recordId + ".zip";
        String storePath = "";
        if(type == 0) {
            storePath = wordsProperties.getVideoAnalysisStorePath() + dateStr + "/" + uuid + "/" + version + "/";
        } else {
            storePath = wordsProperties.getFileAnalysisStorePath() + dateStr + "/" + uuid + "/" + version + "/";
        }


        // 判断文件夹存不存在
        File directory = new File(storePath);
        if(!directory.exists()) {
            directory.mkdirs();
        }

        try(FileOutputStream fos = new FileOutputStream(storePath + zipFileName);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {

            // 创建一个新的ZIP条目
            ZipEntry entry = new ZipEntry("analysis.txt");
            zos.putNextEntry(entry);

            // 将字符串转换为字节并写入当前ZIP条目
            byte[] data = jsonStr.getBytes("UTF-8");
            zos.write(data, 0, data.length);

            // 完成当前条目的写入
            zos.closeEntry();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return dateStr + "/" + uuid + "/" + version + "/" + zipFileName;
    }

    @Override
    public List<SensitiveWordsInfoVo> listByCruxTypeId(Long cruxTypeId) {
        List<SensitiveWordsEntity> sensitiveWordsEntities = this.sensitiveWordsService.list(new QueryWrapper<SensitiveWordsEntity>().eq("crux_type_id", cruxTypeId));
        if(sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {
            List<SensitiveWordsInfoVo> sensitiveWordsInfoVos = sensitiveWordsEntities.stream().map(item -> {
                SensitiveWordsInfoVo sensitiveWordsInfoVo = new SensitiveWordsInfoVo();
                BeanUtils.copyProperties(item, sensitiveWordsInfoVo);
                return sensitiveWordsInfoVo;
            }).toList();

            return sensitiveWordsInfoVos;
        }
        return null;
    }


    /**
     * 将AI分析的内容保存进文件
     * @param storePath 文件路径
     * @param jsonString 文件内容
     * @return
     */
    @Override
    public String AiContentFileName(String storePath,String jsonString) {

        // 判断文件夹存不存在
        File directory = new File(storePath.substring(0, storePath.lastIndexOf("\\")));
        if (!directory.exists()){
            directory.mkdirs();
        }

        try(FileOutputStream fos = new FileOutputStream(storePath)) {
            byte[] data = jsonString.getBytes("UTF-8");

            fos.write(data,0,data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return storePath;
    }

    /**
     * 根据AI分析出来的关键词内容，去查询关键词是否存在于词库中
     * @param aiContentList Ai分析出来的内容
     * @return
     */
    @Override
    public List<String> aiAnswerAndSensitiveList(List<String> aiContentList) {
        // 获取词库所有的词语
        List<SensitiveWordsEntity> wordsEntities = sensitiveWordsService.list(new QueryWrapper<SensitiveWordsEntity>().eq("words_type",1));
        Map<String,SensitiveWordsEntity> wordsMap = wordsEntities.stream().collect(Collectors.toMap(SensitiveWordsEntity::getName,sensitiveWordsEntity -> sensitiveWordsEntity,(a,b) -> a));
        // 查出所有行业
        List<TradeEntity> tradeEntities = tradeService.list();
        Map<Long,TradeEntity> tradeMap = tradeEntities.stream().collect(Collectors.toMap(TradeEntity::getId,tradeEntity -> tradeEntity,(a,b) -> a));
        // 查出所有关键词类型
        List<CruxTypeEntity> cruxTypeEntities = cruxTypeService.list();
        Map<Long,CruxTypeEntity> cruxTypeMap =  cruxTypeEntities.stream().collect(Collectors.toMap(CruxTypeEntity::getId,cruxTypeEntity -> cruxTypeEntity,(a,b) -> a));

        // 用来接收新格式的数组
        List<String> newAnswerList = new ArrayList<>();
        // 将关键词从AI分析出来的内容里根据条件截取出来组成新的组合
        // AI分析出来的内容是已 |关键词|出现次数|阐述观点| 的形式呈现（仅表格形式）
        int index = 0;
        List<String> modifiableList = new ArrayList<>(aiContentList);
        // 移除豆包固定传回来的第二条数据  |---|---|---
        if (modifiableList.size() > 2) {
            modifiableList.remove(1);
        }
        for (String words : modifiableList) {
            if (index == 0){
                words += "是否在词库|";
                words += "分类|";
                words += "行业|";
            }else{
                String[] parts = words.split("\\|");
                if (parts.length >= 4){
                    String word = parts[1];
                    SensitiveWordsEntity wordsEntity = wordsMap.get(word);
                    if (wordsEntity != null) {
                        TradeEntity trade = tradeMap.get(wordsEntity.getTradeId());
                        words += "是|";
                        // 判断当前词语是敏感词还是关键词
                        if (wordsEntity.getWordsType() == 0){
                            // 敏感词：根据type判断属于什么类型
                            words += switch (wordsEntity.getType()){
                                case 0 -> "广告|";
                                case 1 -> "品牌|";
                                case 2 -> "国家|";
                                case 3 -> "限制词|";
                                case 4 -> "其他|";
                                default -> "";
                            };
                        }else if (wordsEntity.getWordsType() == 1){
                            // 关键词：根据cruxTypeId判断属于什么类型
                            CruxTypeEntity cruxTypeEntity = cruxTypeMap.get(wordsEntity.getCruxTypeId());
                            if (cruxTypeEntity != null){
                                words += cruxTypeEntity.getName() + "|";
                            }else{
                                words += " |";
                            }
                        }
                        if (trade != null){
                            words += trade.getName() + "|";
                        }
                    }else{
                        words += "否|";
                        words += " |";
                        words += " |";
                    }
                }
            }
            newAnswerList.add(words);
            index++;
        }
        return newAnswerList;
    }

    /**
     * 根据AI分析出来的关键词内容，去查询关键词是否存在于词库中
     * @param aiContentList Ai分析出来的内容
     * @return
     */
    @Override
    public AiAnalysisRecordVo aiAnswerAndSensitiveTotal(List<String> aiContentList, Long tradeId) {
        // 获取词库所有的词语
        List<SensitiveWordsEntity> wordsEntities = sensitiveWordsService.list(new QueryWrapper<SensitiveWordsEntity>().eq("words_type",1).eq("trade_id", tradeId));
        Map<String,SensitiveWordsEntity> wordsMap = wordsEntities.stream().collect(Collectors.toMap(SensitiveWordsEntity::getName,sensitiveWordsEntity -> sensitiveWordsEntity,(a,b) -> a));
        // 查出所有行业
        List<TradeEntity> tradeEntities = tradeService.list();
        Map<Long,TradeEntity> tradeMap = tradeEntities.stream().collect(Collectors.toMap(TradeEntity::getId,tradeEntity -> tradeEntity,(a,b) -> a));
        // 查出所有关键词类型
        List<CruxTypeEntity> cruxTypeEntities = cruxTypeService.list();
        Map<Long,CruxTypeEntity> cruxTypeMap =  cruxTypeEntities.stream().collect(Collectors.toMap(CruxTypeEntity::getId,cruxTypeEntity -> cruxTypeEntity,(a,b) -> a));

        // 用来接收新格式的数组
        List<String> newAnswerList = new ArrayList<>();
        // 将关键词从AI分析出来的内容里根据条件截取出来组成新的组合
        // AI分析出来的内容是已 |关键词|出现次数|阐述观点| 的形式呈现（仅表格形式）
        int index = 0;
        List<String> modifiableList = new ArrayList<>(aiContentList);
        // 移除豆包固定传回来的第二条数据  |---|---|---
        if (modifiableList.size() > 2) {
            modifiableList.remove(1);
        }
        AiAnalysisRecordVo aiAnalysisRecordVo = new AiAnalysisRecordVo();
        List<AiAnalysisSensitiveRelaBo> notMarkWordList = new LinkedList<>();

        int markWords = 0;
        int totalWords = 0;
        for (String words : modifiableList) {
            if (index == 0){
                words += "是否在词库|";
                words += "分类|";
                words += "行业|";
            }else{
                totalWords++;
                String[] parts = words.split("\\|");
                if (parts.length >= 4){
                    // 匹配关键词库
                    String word = parts[1];
                    SensitiveWordsEntity wordsEntity = wordsMap.get(word);
                    if (wordsEntity != null) {
                        TradeEntity trade = tradeMap.get(wordsEntity.getTradeId());
                        words += "是|";
                        // 判断当前词语是敏感词还是关键词
                        if (wordsEntity.getWordsType() == 0){
                            // 敏感词：根据type判断属于什么类型
                            words += switch (wordsEntity.getType()){
                                case 0 -> "广告|";
                                case 1 -> "品牌|";
                                case 2 -> "国家|";
                                case 3 -> "限制词|";
                                case 4 -> "其他|";
                                default -> "";
                            };
                        }else if (wordsEntity.getWordsType() == 1){
                            // 关键词：根据cruxTypeId判断属于什么类型
                            CruxTypeEntity cruxTypeEntity = cruxTypeMap.get(wordsEntity.getCruxTypeId());
                            if (cruxTypeEntity != null){
                                words += cruxTypeEntity.getName() + "|";
                            }else{
                                words += " |";
                            }
                        }
                        if (trade != null){
                            words += trade.getName() + "|";
                        }
                        // 匹配成功后+1
                        markWords++;
                    }else{
                        words += "否|";
                        words += " |";
                        words += " |";

                        // 将词语增加进词库未存在列表
                        AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo = new AiAnalysisSensitiveRelaBo();
                        aiAnalysisSensitiveRelaBo.setSensitiveWord(word);
                        notMarkWordList.add(aiAnalysisSensitiveRelaBo);
                    }
                }
            }
            newAnswerList.add(words);
            index++;
        }
        aiAnalysisRecordVo.setSensitiveWordTotal(totalWords);
        aiAnalysisRecordVo.setSensitiveWordMark(totalWords - markWords);
        aiAnalysisRecordVo.setAiAnswerList(newAnswerList);
        aiAnalysisRecordVo.setNotMarkWordList(notMarkWordList);
        return aiAnalysisRecordVo;
    }

    /**
     * 添加新的相似词
     * @param wordsId 词语id
     * @param similarWordList 要添加的相似词列表
     * @param existList 已存在的列表
     */
    private void addNewSimilar(SensitiveWordsBo sensitiveWordsBo, Long wordsId, List<SensitiveWordsBo> similarWordList, List<String> existList) {
        // 不存在的相似词
        List<SensitiveWordsBo> notExistSimilarList = new LinkedList<>();

        if(existList != null && existList.size() > 0) {
            for (SensitiveWordsBo wordsBo : similarWordList) {
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
            List<SensitiveWordsEntity> wordsEntities = notExistSimilarList.stream().map(item -> {
                SensitiveWordsEntity similar = new SensitiveWordsEntity();
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
            this.sensitiveWordsService.saveBatch(wordsEntities);

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

