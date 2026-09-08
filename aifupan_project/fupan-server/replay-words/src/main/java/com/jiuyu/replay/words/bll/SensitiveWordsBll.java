package com.jiuyu.replay.words.bll;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.common.alibaba.OssUtils;
import com.jiuyu.replay.common.constant.AliOssProperties;
import com.jiuyu.replay.common.tencent.TencentCosUtils;
import com.jiuyu.replay.common.utils.ReplayFileUtils;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.vo.TencentCosTokenVo;
import com.jiuyu.replay.generic.feign.third.TableStoreFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.system.producer.DictDataProducer;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineProcessBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.producer.*;
import com.jiuyu.replay.words.rse.TradeRse;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.vo.chart.CurveData;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.words.vo.oceanEngineData.JuLiangDataListVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * 敏感词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
@Service
@Slf4j
public class SensitiveWordsBll {

    @Resource
    private SensitiveWordsProducer sensitiveWordsProducer;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private WordsProperties wordsProperties;
    @Resource
    private DictDataProducer dictDataProducer;
    @Resource
    private WordRuleProducer wordRuleProducer;
    @Resource
    private AnchorVideoProducer anchorVideoProducer;
    @Resource
    private UploadFileProducer uploadFileProducer;
    @Resource
    private LexiconWordProducer lexiconWordProducer;
    @Resource
    private TradeProducer tradeProducer;
    @Resource
    private UploadFileAnalysisProducer uploadFileAnalysisProducer;
    @Resource
    private AnchorUrlProducer anchorUrlProducer;
    @Resource
    private SyncContrastProducer syncContrastProducer;
    @Resource
    private VideoAnalysisRecordProducer videoAnalysisRecordProducer;
    @Resource
    private UploadFileAnalysisRecordProducer uploadFileAnalysisRecordProducer;
    @Resource
    private SocketCollectMessageProducer socketCollectMessageProducer;
    @Resource
    private CruxTypeProducer cruxTypeProducer;
    @Resource
    private AiAnalysisSensitiveRelaProducer aiAnalysisSensitiveRelaProducer;
    @Resource
    private OssUtils ossUtils;
    @Resource
    private OceanEngineDataProducer oceanEngineDataProducer;
    @Resource
    private VideoDataViewingBll videoDataViewingBll;
    @Resource
    private VideoDataViewingConfuseProducer videoDataViewingConfuseProducer;
    @Resource
    private TableStoreFeign tableStoreFeign;
    @Resource
    private TradeRse tradeRse;
    @Autowired
    private AliOssProperties aliOssProperties;

    /**
     * 敏感词列表
     *
     * @param sensitiveWordsListBo 敏感词列表查询参数
     * @return
     */
    public R<PageUtils<SensitiveWordsListVo>> queryPage(SensitiveWordsListBo sensitiveWordsListBo) {

        if(!StringUtils.isEmpty(sensitiveWordsListBo.getCruxTypeId())) {
            List<Long> cruxTypeIds = this.cruxTypeProducer.getChildrenIdAndSelfIdList(sensitiveWordsListBo.getCruxTypeId());
            if(cruxTypeIds != null && cruxTypeIds.size() > 0) {
                sensitiveWordsListBo.setCruxTypeIds(cruxTypeIds);
            }
        }

        return R.ok("获取成功", sensitiveWordsProducer.queryPage(sensitiveWordsListBo));
    }

    /**
     * 敏感词信息
     *
     * @param id 敏感词id
     * @return
     */
    public R<SensitiveWordsInfoVo> info(Long id) {

        SensitiveWordsInfoVo sensitiveWordsInfoVo = sensitiveWordsProducer.info(id);

        if (sensitiveWordsInfoVo != null) {
            // 获取词语规则
            List<WordRuleInfoVo> ruleList = this.wordRuleProducer.listByWordId(sensitiveWordsInfoVo.getId());
            sensitiveWordsInfoVo.setRuleList(ruleList);
            // 获取相似词
            List<SensitiveWordsInfoVo> similarList = this.sensitiveWordsProducer.listByParentId(sensitiveWordsInfoVo.getId());
            sensitiveWordsInfoVo.setSimilarWordList(similarList);
            if(sensitiveWordsInfoVo.getWordsType() == 1) {
                // 获取关键词分类数组
                List<Long> cruxTypeIdArr = cruxTypeProducer.getParentIdContainerSelfArr(sensitiveWordsInfoVo.getCruxTypeId());
                sensitiveWordsInfoVo.setCruxTypeIdArr(cruxTypeIdArr);
            }
        }

        return R.ok("获取成功", sensitiveWordsInfoVo);
    }

    /**
     * 新增敏感词
     *
     * @param sensitiveWordsBo 敏感词对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<List<String>> save(SensitiveWordsBo sensitiveWordsBo) {

        if (StringUtils.isEmpty(sensitiveWordsBo.getName())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "词语不能为空");
        }

        // 检查相似词是否有重复的
        List<SensitiveWordsBo> similarWordList = sensitiveWordsBo.getSimilarWordList();
        if(similarWordList != null && similarWordList.size() > 0) {
            Set<String> names = new HashSet<>();
            for (SensitiveWordsBo wordsBo : similarWordList) {
                names.add(wordsBo.getName());
            }
            if(similarWordList.size() != names.size()) {
                StringBuilder msg = new StringBuilder("相似词列表存在相同的词语：");
                for (String name : names) {
                    int count = 0;
                    for (SensitiveWordsBo wordsBo : similarWordList) {
                        if(wordsBo.getName().equals(name)) {
                            count ++;
                        }
                    }
                    if(count > 1) {
                        msg.append(name).append("出现了").append(count).append("次。");
                    }
                }
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), msg.toString());
            }
        }

        List<String> resultMsgList = new LinkedList<>();

        // 查平台列表
        List<DictDataListVo> platformList = this.dictDataProducer.listByTypeLogo("words_platform_type");

        for (Integer platformType : sensitiveWordsBo.getPlatformTypeList()) {

            sensitiveWordsBo.setPlatformType(platformType);

            // 检查是否已经存在词语
            SensitiveWordsVo sensitiveWords = this.sensitiveWordsProducer.exist(sensitiveWordsBo);
            if (sensitiveWords != null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "词语已存在，添加失败");
            }

            // 保存词语
            sensitiveWords = this.sensitiveWordsProducer.saveWord(sensitiveWordsBo);

            // 保存词语规则
            if (sensitiveWordsBo.getRuleList() != null && sensitiveWordsBo.getRuleList().size() > 0) {
                this.wordRuleProducer.saveBatchByWordId(sensitiveWords.getId(), sensitiveWordsBo.getRuleList());
            }

            // 检查是否已经存在相似词，返回已存在的相似词列表
            List<String> existList = this.sensitiveWordsProducer.existSimilarList(sensitiveWordsBo);
            if (existList != null && existList.size() > 0) {
                StringBuilder resultMsg = new StringBuilder();
                // resultMsg.append(platformList.get(platformType).getLabel());
                if (platformList != null && platformList.size() > 0) {
                    resultMsg.append(platformList.stream().filter(item -> item.getValue().equals(platformType + "")).toList().get(0).getLabel());
                }
//                resultMsg.append(platformList.get(0).getLabel());
                resultMsg.append("因重复而被忽略的相似词：");
                for (String wordsName : existList) {
                    resultMsg.append("-").append(wordsName);
                }
                resultMsgList.add(resultMsg.toString());
            }

            // 保存相似词
            this.sensitiveWordsProducer.addSimilar(sensitiveWordsBo, sensitiveWords, existList);

            // 如果是客户端添加的，跟词库增加关联
            if(sensitiveWordsBo.getResourceType() == 1) {
                LexiconWordBo lexiconWordBo = new LexiconWordBo();
                lexiconWordBo.setLexiconId(sensitiveWordsBo.getLexiconId());
                lexiconWordBo.setWordId(sensitiveWords.getId());
                lexiconWordBo.setWordsType(sensitiveWordsBo.getWordsType());
                this.lexiconWordProducer.save(lexiconWordBo);
            }

        }


        // 从ai分析词库删除对应的词语
        List<String> wordNameList = new LinkedList<>();
        wordNameList.add(sensitiveWordsBo.getName());
        if(sensitiveWordsBo.getSimilarWordList() != null && sensitiveWordsBo.getSimilarWordList().size() > 0) {
            List<String> similarWordNameList = sensitiveWordsBo.getSimilarWordList().stream().map(SensitiveWordsBo::getName).toList();
            wordNameList.addAll(similarWordNameList);
        }
        this.aiAnalysisSensitiveRelaProducer.delByWordListAndTrade(wordNameList, sensitiveWordsBo.getTradeId());

        // 删除缓存
        if(sensitiveWordsBo.getPlatformType() != null) {
            List<TradeInfoVo> tradeInfoVos = this.tradeProducer.listChildrenByTradeId(sensitiveWordsBo.getTradeId(), true);
            if(tradeInfoVos != null && tradeInfoVos.size() > 0) {
                for (TradeInfoVo tradeInfoVo : tradeInfoVos) {
                    deleteWordCache(sensitiveWordsBo.getPlatformType(), tradeInfoVo.getId(), sensitiveWordsBo.getWordsType());
                }
            }
        }

        return R.ok(resultMsgList);
    }

    /**
     * 修改敏感词
     *
     * @param sensitiveWordsBo 敏感词对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> update(SensitiveWordsBo sensitiveWordsBo) {

        if (StringUtils.isEmpty(sensitiveWordsBo.getName())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "词语不能为空");
        }

        // 检查相似词是否有重复的
        List<SensitiveWordsBo> similarWordList = sensitiveWordsBo.getSimilarWordList();
        if(similarWordList != null && similarWordList.size() > 0) {
            Set<String> names = new HashSet<>();
            for (SensitiveWordsBo wordsBo : similarWordList) {
                names.add(wordsBo.getName());
            }
            if(similarWordList.size() != names.size()) {
                StringBuilder msg = new StringBuilder("相似词列表存在相同的词语：");
                for (String name : names) {
                    int count = 0;
                    for (SensitiveWordsBo wordsBo : similarWordList) {
                        if(wordsBo.getName().equals(name)) {
                            count ++;
                        }
                    }
                    if(count > 1) {
                        msg.append(name).append("出现了").append(count).append("次。");
                    }
                }
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), msg.toString());
            }
        }

        // 获取原词语
        SensitiveWordsVo sensitiveWordsVo = this.sensitiveWordsProducer.info(sensitiveWordsBo.getId());
        if (sensitiveWordsVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
        }
        sensitiveWordsBo.setUserId(sensitiveWordsVo.getUserId());

        if (!sensitiveWordsBo.getName().equals(sensitiveWordsBo.getOldName())) {
            // 检查是否已经存在
            SensitiveWordsVo exist = this.sensitiveWordsProducer.exist(sensitiveWordsBo);
            if (exist != null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), (sensitiveWordsBo.getWordsType() == 0 ? "敏感词" : "关键词") + "已存在，修改失败");
            }
        }

        // 更新词语
        this.sensitiveWordsProducer.update(sensitiveWordsBo);
        // 更新规则
        this.wordRuleProducer.updateBatchByWordId(sensitiveWordsBo.getId(), sensitiveWordsBo.getRuleList());

        // 查平台列表
        List<DictDataListVo> platformList = this.dictDataProducer.listByTypeLogo("words_platform_type");

        // 更新相似词
        String resultMsg = sensitiveWordsProducer.updateSimilar(sensitiveWordsBo, platformList);

        // 从ai分析词库删除对应的词语
        List<String> wordNameList = new LinkedList<>();
        wordNameList.add(sensitiveWordsBo.getName());
        if(sensitiveWordsBo.getSimilarWordList() != null && sensitiveWordsBo.getSimilarWordList().size() > 0) {
            List<String> similarWordNameList = sensitiveWordsBo.getSimilarWordList().stream().map(SensitiveWordsBo::getName).toList();
            wordNameList.addAll(similarWordNameList);
        }
        this.aiAnalysisSensitiveRelaProducer.delByWordListAndTrade(wordNameList, sensitiveWordsBo.getTradeId());

        // 删除缓存
        if(sensitiveWordsBo.getPlatformType() != null) {
            List<TradeInfoVo> tradeInfoVos = this.tradeProducer.listChildrenByTradeId(sensitiveWordsBo.getTradeId(), true);
            if(tradeInfoVos != null && tradeInfoVos.size() > 0) {
                for (TradeInfoVo tradeInfoVo : tradeInfoVos) {
                    deleteWordCache(sensitiveWordsBo.getPlatformType(), tradeInfoVo.getId(), sensitiveWordsBo.getWordsType());
                }
            }
        }


        return R.ok(resultMsg);
    }

    /**
     * 删除词语
     *
     * @param id 敏感词id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> delete(Long id) {

        SensitiveWordsInfoVo oldWordsInfoVo = this.sensitiveWordsProducer.info(id);
        if(oldWordsInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "词语不存在，删除失败");
        }

        SensitiveWordsInfoVo wordsInfoVo = this.sensitiveWordsProducer.info(id);
        if(wordsInfoVo != null && wordsInfoVo.getResourceType() == 1) {
            // 客户自定义的词语，删除词语跟词库的关联关系
            this.lexiconWordProducer.deleteByWordId(wordsInfoVo.getId());
        }

        // 删除词语
        sensitiveWordsProducer.deleteById(id);

        // 删除缓存
        List<TradeInfoVo> tradeInfoVos = this.tradeProducer.listChildrenByTradeId(oldWordsInfoVo.getTradeId(), true);
        if(tradeInfoVos != null && tradeInfoVos.size() > 0) {
            for (TradeInfoVo tradeInfoVo : tradeInfoVos) {
                deleteWordCache(oldWordsInfoVo.getPlatformType(), tradeInfoVo.getId(), oldWordsInfoVo.getWordsType());
            }
        }

        return R.ok("删除成功");
    }

    /**
     * 根据平台id和行业id获取系统和用户定义的所有敏感词
     *
     * @param userId       用户id
     * @param platformType 平台类型 0：全平台 1：抖音 2：快手 3：视频号
     * @param tradeId      行业id 1表示全行业
     * @param wordsType    词语类型 0：敏感词 1：关键词 2：白名单(只有客户自定义有)
     * @return
     */
    public List<SensitiveWordsVo> listByUidAndPidAndTid(Long userId, Integer platformType, Long tradeId, Integer wordsType) {

        // 缓存里面有则直接返回
        String redisKey = wordsProperties.getUserSensitiveRedisPrefix() + platformType + ":" + tradeId + ":" + wordsType;
        Object obj = redisTemplate.opsForValue().get(redisKey);
        if (obj != null) {
            return JSONArray.parseArray((String) obj, SensitiveWordsVo.class);
        }

        // 获取敏感词
        List<SensitiveWordsVo> sensitiveWordsVos = sensitiveWordsProducer.listByUidAndPidAndTid(userId, platformType, tradeId, wordsType);

        // 设置敏感词的规则列表
        if (sensitiveWordsVos != null && sensitiveWordsVos.size() > 0) {
            List<Long> wordIds = sensitiveWordsVos.stream().map(SensitiveWordsVo::getId).toList();
            List<WordRuleInfoVo> wordRuleList = this.wordRuleProducer.listByWordIds(wordIds);

            for (SensitiveWordsVo sensitiveWordsVo : sensitiveWordsVos) {
                List<WordRuleInfoVo> ruleList = new LinkedList<>();
                if (wordRuleList != null && wordRuleList.size() > 0) {
                    for (WordRuleInfoVo wordRuleInfoVo : wordRuleList) {
                        if (wordRuleInfoVo.getWordId().equals(sensitiveWordsVo.getId())) {
                            ruleList.add(wordRuleInfoVo);
                        }
                    }
                }
                sensitiveWordsVo.setRuleList(ruleList);
            }
        }

        // 存到缓存
        redisTemplate.opsForValue().set(redisKey, JSONObject.toJSONString(sensitiveWordsVos), Duration.ofDays(5));


        return sensitiveWordsVos;
    }

    /**
     * 批量新增敏感词
     *
     * @param sensitiveWordsBatchBo 敏感词对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<List<String>> saveBatch(SensitiveWordsBatchBo sensitiveWordsBatchBo) {

        List<WordsBatchItemBo> wordsList = sensitiveWordsBatchBo.getWordsList();

        if (wordsList == null || wordsList.size() < 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "内容不能为空");
        }

        List<String> resultMsg = new LinkedList<>();

        for (WordsBatchItemBo wordsBatchItemBo : wordsList) {
            // 封装词语
            SensitiveWordsBo sensitiveWordsBo = new SensitiveWordsBo();
            BeanUtils.copyProperties(sensitiveWordsBatchBo, sensitiveWordsBo);
            sensitiveWordsBo.setName(wordsBatchItemBo.getWords());
            sensitiveWordsBo.setParentId(0L);
            // 封装相似词
            if (wordsBatchItemBo.getSimilarWords() != null && wordsBatchItemBo.getSimilarWords().size() > 0) {
                List<SensitiveWordsBo> similarWordList = wordsBatchItemBo.getSimilarWords().stream().map(item -> {
                    SensitiveWordsBo similar = new SensitiveWordsBo();
                    similar.setName(item);
                    return similar;
                }).toList();
                sensitiveWordsBo.setSimilarWordList(similarWordList);
            }

            R<List<String>> saveMsg = this.save(sensitiveWordsBo);
            if(saveMsg.getCode() != 0) {
                return R.error(saveMsg.getCode(), saveMsg.getMsg());
            }
            List<String> saveMsgData = saveMsg.getData();
            if (saveMsgData != null && saveMsgData.size() > 0) {
                resultMsg.addAll(saveMsgData);
            }
        }

        // 删除缓存
        if(sensitiveWordsBatchBo.getPlatformType() != null) {
            List<TradeInfoVo> tradeInfoVos = this.tradeProducer.listChildrenByTradeId(sensitiveWordsBatchBo.getTradeId(), true);
            if(tradeInfoVos != null && tradeInfoVos.size() > 0) {
                for (TradeInfoVo tradeInfoVo : tradeInfoVos) {
                    deleteWordCache(sensitiveWordsBatchBo.getPlatformType(), tradeInfoVo.getId(), sensitiveWordsBatchBo.getWordsType());
                }
            }
        }

        return R.ok(resultMsg);
    }

    /**
     * 删除词语redis缓存
     * @param platformType 平台类型
     * @param tradeId 行业id
     * @param wordType 词语类型
     */
    private void deleteWordCache(Integer platformType, Long tradeId, Integer wordType) {
        if(platformType == 0) {
            List<DictDataListVo> platformTypeList = this.dictDataProducer.listByTypeLogo("replay_platform_type");
            if(platformTypeList != null && !platformTypeList.isEmpty()) {
                for (DictDataListVo platformTypeVo : platformTypeList) {
                    this.redisTemplate.delete(wordsProperties.getUserSensitiveRedisPrefix() + platformTypeVo.getValue() + ":" + tradeId + ":" + wordType);
                }
            }
        }else {
            this.redisTemplate.delete(wordsProperties.getUserSensitiveRedisPrefix() + platformType + ":" + tradeId + ":" + wordType);
        }
    }

    /**
     * 文字内容截取
     * @param wordsMarkBo 文字数据
     * @return
     */
    public List<SentenceMarkVo> subSentence(WordsMarkBo wordsMarkBo, Long userId) {

        if (StringUtils.isEmpty(wordsMarkBo.getContent()) || wordsMarkBo.getItems() == null) {
            return null;
        }

        // 如果是第一段，先清空缓存
        if (wordsMarkBo.getCurrentSort() == 1) {
            // 删除缓存
            if (Boolean.TRUE.equals(redisTemplate.hasKey(wordsProperties.getRedisRemainSentenceKeyPrefix() + userId + wordsMarkBo.getVideoId()))) {
                redisTemplate.delete(wordsProperties.getRedisRemainSentenceKeyPrefix() + userId + wordsMarkBo.getVideoId());
            }
        }

        // 去掉最后一个标点符号（二次分析不截）
        if (wordsMarkBo.getIsLast() != 1 && (StringUtils.isEmpty(wordsMarkBo.getIsReAnalysis()) || wordsMarkBo.getIsReAnalysis() == 0)) {
            String lastWord = wordsMarkBo.getContent().substring(wordsMarkBo.getContent().length() - 1);
            if (lastWord.endsWith("，") || lastWord.endsWith("。") || lastWord.endsWith("！") || lastWord.endsWith("？") || lastWord.endsWith(".") || lastWord.endsWith("!") || lastWord.endsWith(",")) {
                List<WordsMarkItemBo> items = wordsMarkBo.getItems();
                if(items.size() > 1) {
                    wordsMarkBo.setContent(wordsMarkBo.getContent().substring(0, wordsMarkBo.getContent().length() - 1));
                    items.remove(items.size() - 1);
                }

            }
        }


        // 计算出真正的词语时间戳(乘以段落)（二次分析不计算）
        if (StringUtils.isEmpty(wordsMarkBo.getIsReAnalysis()) || wordsMarkBo.getIsReAnalysis() == 0) {
            for (WordsMarkItemBo item : wordsMarkBo.getItems()) {
                item.setEndTime(item.getEndTime() + ((wordsMarkBo.getCurrentSort() - 1) * wordsProperties.getAudioLength()));
                item.setStartTime(item.getStartTime() + ((wordsMarkBo.getCurrentSort() - 1) * wordsProperties.getAudioLength()));
            }
        }

        // 当前段落没有文字，如果缓存里面有，将缓存里面的文字取出来当当前段落的内容
        if(wordsMarkBo.getItems().size() == 1 && wordsMarkBo.getItems().get(0).getWord().equals("-")) {
            List<SentenceMarkVo> sentenceMarkVoList = new LinkedList<>();
            SentenceMarkVo sentenceMarkVo = new SentenceMarkVo();
            sentenceMarkVo.setVideoId(wordsMarkBo.getVideoId());
            sentenceMarkVo.setCurrentSort(wordsMarkBo.getCurrentSort());
            sentenceMarkVo.setContent("");
            sentenceMarkVo.setItems(new LinkedList<WordListItemVo>());

            Object obj = redisTemplate.opsForValue().get(wordsProperties.getRedisRemainSentenceKeyPrefix() + userId + wordsMarkBo.getVideoId());
            if(obj != null) {
                WordsMarkBo remainSentence = (WordsMarkBo) obj;
                if (remainSentence.getItems() != null && remainSentence.getItems().size() > 0) {
                    sentenceMarkVo.setContent(remainSentence.getContent());
                    for (WordsMarkItemBo item : remainSentence.getItems()) {
                        WordListItemVo wordListItemVo = new WordListItemVo();
                        BeanUtils.copyProperties(item, wordListItemVo);
                        sentenceMarkVo.getItems().add(wordListItemVo);
                    }
                }else {
                    sentenceMarkVo.setContent("-");
                    WordListItemVo wordListItemVo = new WordListItemVo();
                    BeanUtils.copyProperties(wordsMarkBo.getItems().get(0), wordListItemVo);
                    sentenceMarkVo.getItems().add(wordListItemVo);
                }

            }else {
                sentenceMarkVo.setContent("-");
                WordListItemVo wordListItemVo = new WordListItemVo();
                BeanUtils.copyProperties(wordsMarkBo.getItems().get(0), wordListItemVo);
                sentenceMarkVo.getItems().add(wordListItemVo);
            }

            sentenceMarkVoList.add(sentenceMarkVo);

            redisTemplate.opsForValue().set(wordsProperties.getRedisRemainSentenceKeyPrefix() + userId + wordsMarkBo.getVideoId(), null, Duration.ofDays(5));

            return sentenceMarkVoList;
        }

        // 获取缓存里遗留的文字
        Object obj = redisTemplate.opsForValue().get(wordsProperties.getRedisRemainSentenceKeyPrefix() + userId + wordsMarkBo.getVideoId());
        // 将遗留的文字跟新的文字段落拼接
        if (obj != null) {
            WordsMarkBo remainSentence = (WordsMarkBo) obj;
            wordsMarkBo.setContent(remainSentence.getContent() + wordsMarkBo.getContent());
            if (remainSentence.getItems() != null && remainSentence.getItems().size() > 0) {
                List<WordsMarkItemBo> list = new LinkedList<>();
                list.addAll(remainSentence.getItems());
                list.addAll(wordsMarkBo.getItems());
                wordsMarkBo.setItems(list);
            }
        }
        // 从后面开始找到一个合适标点符号的索引(不能超过音频长度)
//        Long startTime = wordsMarkBo.getItems().get(0).getStartTime();
        int index = 0;
        for (int i = wordsMarkBo.getItems().size() - 1; i >= 0; i--) {
            WordsMarkItemBo item = wordsMarkBo.getItems().get(i);
            if (item.getWord().endsWith("，") || item.getWord().endsWith("。") || item.getWord().endsWith("！") || item.getWord().endsWith("？")
                    || item.getWord().endsWith(".") || item.getWord().endsWith("!") || item.getWord().endsWith(",")) {

//                if (item.getEndTime() - startTime <= wordsProperties.getAudioLength()) {
//                    index = i;
//                    break;
//                }
                index = i;
                break;
            }
        }

        List<SentenceMarkVo> sentenceMarkVoList = new LinkedList<>();
        SentenceMarkVo sentenceMarkVo = new SentenceMarkVo();
        sentenceMarkVo.setVideoId(wordsMarkBo.getVideoId());
        sentenceMarkVo.setCurrentSort(wordsMarkBo.getCurrentSort());
        sentenceMarkVo.setContent("");
        sentenceMarkVo.setItems(new LinkedList<WordListItemVo>());
        // 截取字符串
        for (int i = 0; i <= index; i++) {
            WordsMarkItemBo item = wordsMarkBo.getItems().get(i);
            sentenceMarkVo.setContent(sentenceMarkVo.getContent() + item.getWord());
            WordListItemVo wordListItemVo = new WordListItemVo();
            BeanUtils.copyProperties(item, wordListItemVo);
            sentenceMarkVo.getItems().add(wordListItemVo);
        }
        sentenceMarkVoList.add(sentenceMarkVo);

        // 如果当前是最后一次请求，直接新读取完剩余内容，否则将标点符号后面的文字放到缓存，等待下一次读取
        if (wordsMarkBo.getIsLast() == 1 && wordsMarkBo.getItems().size() - 1 != index) {
            SentenceMarkVo remainSentenceMarkVo = new SentenceMarkVo();
            remainSentenceMarkVo.setVideoId(wordsMarkBo.getVideoId());
            remainSentenceMarkVo.setCurrentSort(wordsMarkBo.getCurrentSort() + 1);
            remainSentenceMarkVo.setContent("");
            remainSentenceMarkVo.setItems(new LinkedList<WordListItemVo>());
            for (int i = index + 1; i < wordsMarkBo.getItems().size(); i++) {
                WordsMarkItemBo item = wordsMarkBo.getItems().get(i);
                remainSentenceMarkVo.setContent(remainSentenceMarkVo.getContent() + item.getWord());
                WordListItemVo wordListItemVo = new WordListItemVo();
                BeanUtils.copyProperties(item, wordListItemVo);
                remainSentenceMarkVo.getItems().add(wordListItemVo);
            }
            sentenceMarkVoList.add(remainSentenceMarkVo);

            // 删除缓存
            if (Boolean.TRUE.equals(redisTemplate.hasKey(wordsProperties.getRedisRemainSentenceKeyPrefix() + userId + wordsMarkBo.getVideoId()))) {
                redisTemplate.delete(wordsProperties.getRedisRemainSentenceKeyPrefix() + userId + wordsMarkBo.getVideoId());
            }
        } else {
            WordsMarkBo redisWordsMarkBo = new WordsMarkBo();
            redisWordsMarkBo.setVideoId(wordsMarkBo.getVideoId());
            redisWordsMarkBo.setContent("");
            redisWordsMarkBo.setItems(new LinkedList<WordsMarkItemBo>());
            for (int i = index + 1; i < wordsMarkBo.getItems().size(); i++) {
                WordsMarkItemBo item = wordsMarkBo.getItems().get(i);
                redisWordsMarkBo.setContent(redisWordsMarkBo.getContent() + item.getWord());
                WordsMarkItemBo wordsMarkItemBo = new WordsMarkItemBo();
                BeanUtils.copyProperties(item, wordsMarkItemBo);
                redisWordsMarkBo.getItems().add(wordsMarkItemBo);
            }
            redisTemplate.opsForValue().set(wordsProperties.getRedisRemainSentenceKeyPrefix() + userId + wordsMarkBo.getVideoId(), redisWordsMarkBo, Duration.ofDays(5));
        }

        return sentenceMarkVoList;

    }

    /**
     * 分析数据插入到数据库
     * @param jsonStr 分析结果数据
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识
     * @param tradeId 行业id
     * @param userId 用户id
     */
    public void saveCacheToDb(String jsonStr, Integer type, String uuid, Long tradeId, Long userId) {

        if(!StringUtils.isEmpty(jsonStr)) {

            if(type == 0) {
                // 视频

                // 获取视频分析最新的版本号
                int versionNum = this.videoAnalysisRecordProducer.getLastVersion(uuid);
                // 保存分析数据到本地文件
                Long id = SnowflakeManager.nextValue();
                String fileName = this.saveAnalysisDataToFile(uuid, type, tradeId, versionNum, jsonStr, id);
                // 保存分析记录
                VideoAnalysisRecordBo videoAnalysisRecordBo = new VideoAnalysisRecordBo();
                videoAnalysisRecordBo.setId(id);
                videoAnalysisRecordBo.setUserId(userId);
                videoAnalysisRecordBo.setVideoId(uuid);
                videoAnalysisRecordBo.setTradeId(tradeId);
                videoAnalysisRecordBo.setStoreFileName(fileName);
                videoAnalysisRecordBo.setVersion(versionNum);
                videoAnalysisRecordProducer.save(videoAnalysisRecordBo);
            }else {
                // 文件

                // 获取文件分析最新的版本号
                int versionNum = this.uploadFileAnalysisRecordProducer.getLastVersion(uuid);
                // 保存分析数据到本地文件
                Long id = SnowflakeManager.nextValue();
                String fileName = this.saveAnalysisDataToFile(uuid, type, tradeId, versionNum, jsonStr, id);
                // 保存分析记录
                UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo = new UploadFileAnalysisRecordBo();
                uploadFileAnalysisRecordBo.setId(id);
                uploadFileAnalysisRecordBo.setUserId(userId);
                uploadFileAnalysisRecordBo.setFileId(uuid);
                uploadFileAnalysisRecordBo.setTradeId(tradeId);
                uploadFileAnalysisRecordBo.setStoreFileName(fileName);
                uploadFileAnalysisRecordBo.setVersion(versionNum);
                uploadFileAnalysisRecordProducer.save(uploadFileAnalysisRecordBo);
            }
        }

    }

    /**
     * 分析数据插入到数据库2_0
     * @param oldJsonStr 旧的分析结果数据
     * @param newJsonStr 新的分析结果数据
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识
     * @param tradeId 行业id
     * @param userId 用户id
     * @param cruxWordNum 关键词数量
     * @param sensitiveWordNum 敏感词数量
     * @param contentNum 文本总字数
     */
    public void saveCacheToDb2_0(String oldJsonStr, String newJsonStr, Integer type, String uuid, Long tradeId, Long userId, Integer cruxWordNum, Integer sensitiveWordNum, Integer contentNum) {

        if(!StringUtils.isEmpty(oldJsonStr) || !StringUtils.isEmpty(newJsonStr)) {

            if(type == 0) {
                // 视频

                // 获取视频分析最新的版本号
                int versionNum = this.videoAnalysisRecordProducer.getLastVersion(uuid);
                Long id = SnowflakeManager.nextValue();

                // 上传分析数据到oss
                String ossSaveKey = uploadAnalysisFileToOSS(uuid, type, tradeId, versionNum, newJsonStr, id);

                // 保存分析数据到本地文件
//                String oldFileName = this.sensitiveWordsProducer.saveAnalysisDataToFile2_0("10", uuid, type, tradeId, versionNum, oldJsonStr, id);
//                String newFileName = this.sensitiveWordsProducer.saveAnalysisDataToFile2_0("20", uuid, type, tradeId, versionNum, newJsonStr, id);
                // 保存分析记录
                VideoAnalysisRecordBo videoAnalysisRecordBo = new VideoAnalysisRecordBo();
                videoAnalysisRecordBo.setId(id);
                videoAnalysisRecordBo.setUserId(userId);
                videoAnalysisRecordBo.setVideoId(uuid);
                videoAnalysisRecordBo.setTradeId(tradeId);
//                videoAnalysisRecordBo.setStoreFileName(oldFileName);
//                videoAnalysisRecordBo.setStoreFileNameNew(newFileName);
                videoAnalysisRecordBo.setStoreFileOssKey(ossSaveKey);
                videoAnalysisRecordBo.setVersion(versionNum);
                videoAnalysisRecordBo.setCruxWordNum(cruxWordNum);
                videoAnalysisRecordBo.setSensitiveWordNum(sensitiveWordNum);
                videoAnalysisRecordBo.setContentNum(contentNum);
                videoAnalysisRecordProducer.save(videoAnalysisRecordBo);
            }else {
                // 文件

                // 获取文件分析最新的版本号
                int versionNum = this.uploadFileAnalysisRecordProducer.getLastVersion(uuid);
                Long id = SnowflakeManager.nextValue();

                // 上传分析数据到oss
                String ossSaveKey = uploadAnalysisFileToOSS(uuid, type, tradeId, versionNum, newJsonStr, id);

                // 保存分析数据到本地文件
//                String oldFileName = this.sensitiveWordsProducer.saveAnalysisDataToFile2_0("10", uuid, type, tradeId, versionNum, oldJsonStr, id);
//                String newFileName = this.sensitiveWordsProducer.saveAnalysisDataToFile2_0("20", uuid, type, tradeId, versionNum, newJsonStr, id);

                // 保存分析记录
                UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo = new UploadFileAnalysisRecordBo();
                uploadFileAnalysisRecordBo.setId(id);
                uploadFileAnalysisRecordBo.setUserId(userId);
                uploadFileAnalysisRecordBo.setFileId(uuid);
                uploadFileAnalysisRecordBo.setTradeId(tradeId);
//                uploadFileAnalysisRecordBo.setStoreFileName(oldFileName);
//                uploadFileAnalysisRecordBo.setStoreFileNameNew(newFileName);
                uploadFileAnalysisRecordBo.setStoreFileOssKey(ossSaveKey);
                uploadFileAnalysisRecordBo.setVersion(versionNum);
                uploadFileAnalysisRecordBo.setCruxWordNum(cruxWordNum);
                uploadFileAnalysisRecordBo.setSensitiveWordNum(sensitiveWordNum);
                uploadFileAnalysisRecordBo.setContentNum(contentNum);
                uploadFileAnalysisRecordProducer.save(uploadFileAnalysisRecordBo);
            }
        }

    }

    /**
     * 上传分析数据文件到cos
     * @param uuid 视频/文件唯一标识
     * @param type 类型 0：视频 1：文件
     * @param tradeId 行业id
     * @param versionNum 版本号
     * @param jsonStr 分析数据
     * @param recordId 记录id
     */
    private String uploadAnalysisFileToCos(String uuid, int type, Long tradeId, int versionNum, String jsonStr, Long recordId) {
        // 获取上传凭证
        TencentCosTokenVo tencentCosTokenVo = TencentCosUtils.privateCosUploadTempToken();
        // 将内容转成输入流
        ByteArrayInputStream byteArrayInputStream = TencentCosUtils.convertToZipStream(jsonStr);
        if (byteArrayInputStream != null) {
            // 构建cos保存的key
            String videoOrFile = type == 0 ? "video" : "file";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String fileName = versionNum + "th-" + tradeId + "-" + recordId + ".zip";
            ;
            String cosSaveKey = "analysis/" + videoOrFile + "/" + sdf.format(new Date()) + "/" + uuid + "/20/" + fileName;
            // 上传到cos
            Boolean isSuccess = TencentCosUtils.putStreamObject(TencentCosUtils.getPrivateCosBucketName(), byteArrayInputStream, cosSaveKey);
            if (isSuccess) {
                // 上传成功
                return cosSaveKey;
            }
        }

        log.info("上传分析数据文件到cos发生错误{},{},{},{},{},{}", uuid, type, tradeId, versionNum, jsonStr, recordId);
        throw new RuntimeException();
    }

    /**
     * 上传分析数据文件到oss
     * @param uuid 视频/文件唯一标识
     * @param type 类型 0：视频 1：文件
     * @param tradeId 行业id
     * @param versionNum 版本号
     * @param jsonStr 分析数据
     * @param recordId 记录id
     */
    private String uploadAnalysisFileToOSS(String uuid, int type, Long tradeId, int versionNum, String jsonStr, Long recordId) {
        // 将内容转成输入流
        ByteArrayInputStream byteArrayInputStream = TencentCosUtils.convertToZipStream(jsonStr);
        if(byteArrayInputStream != null) {
            // 构建oss保存的key
            String videoOrFile = type == 0 ? "video" : "file";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String fileName = versionNum + "th-" + tradeId + "-" + recordId + ".zip";;
            String ossSaveKey = "analysis/" + videoOrFile + "/" + sdf.format(new Date()) + "/" + uuid + "/20/" + fileName;
            // 上传到oss
            log.info("上传前oss-key：{}", ossSaveKey);
            ossSaveKey = ossUtils.putObjectByStream("replay-analysis-data", ossSaveKey, byteArrayInputStream);
            log.info("上传后oss-key：{}", ossSaveKey);
            if(ObjectUtil.isNotEmpty(ossSaveKey)) {
                // 上传成功
                return ossSaveKey;
            }
        }

        log.info("上传分析数据文件到oss发生错误{},{},{},{},{},{}", uuid, type, tradeId, versionNum, jsonStr, recordId);
        throw new RuntimeException();
    }

    /**
     * 将分析数据存储到本地
     * @param uuid 视频/文件唯一标识
     * @param type 类型 0：视频 1：文件
     * @param tradeId 行业id
     * @param versionNum 版本号
     * @param jsonStr 分析数据
     * @param recordId 记录id
     */
    private String saveAnalysisDataToFile(String uuid, int type, Long tradeId, int versionNum, String jsonStr, Long recordId) {

        return this.sensitiveWordsProducer.saveAnalysisDataToFile(uuid, type, tradeId, versionNum, jsonStr, recordId);

    }


    /**
     * 敏感词导入
     * @param excel excel文件
     * @param userId 用户id
     * @return
     */
    public R<List<String>> importExcel(MultipartFile excel, Long userId) {

        try {

            List<SensitiveWordImportBo> sensitiveWordImportBos = new LinkedList<>();
            List<String> resultMsgList = new LinkedList<>();

            EasyExcel.read(excel.getInputStream(), SensitiveWordImportBo.class, new PageReadListener<SensitiveWordImportBo>(dataList -> {
                sensitiveWordImportBos.addAll(dataList);
            })).sheet().doRead();

            if(sensitiveWordImportBos.size() > 0) {

                // 获取行业列表
                List<TradeVo> tradeVoList = this.tradeProducer.listAll();
                // 获取敏感词类型
                List<DictDataListVo> sensitiveTypeList = this.dictDataProducer.listByTypeLogo("sensitive_words_type");
                // 获取敏感词等级
                List<DictDataListVo> sensitiveLevelList = this.dictDataProducer.listByTypeLogo("sensitive_words_level_type");

                for (int i = 0; i < sensitiveWordImportBos.size(); i++) {
                    SensitiveWordImportBo sensitiveWordImportBo = sensitiveWordImportBos.get(i);
                    // 检查示例词
                    if(StringUtils.isEmpty(sensitiveWordImportBo.getName()) || "".equals(sensitiveWordImportBo.getName().replaceAll("[ \\t\\n\\r\\f\\v]", ""))){
                        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "excel解析错误，" + (i+1) + "行的示例词没有值");
                    }
                    // 检查行业
                    if(StringUtils.isEmpty(sensitiveWordImportBo.getTradeStr()) || "".equals(sensitiveWordImportBo.getTradeStr().replaceAll("[ \\t\\n\\r\\f\\v]", ""))){
                        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "excel解析错误，" + (i+1) + "行的行业没有值");
                    }else {
                        String[] tradeArr = sensitiveWordImportBo.getTradeStr().split("#");
                        for (String trade : tradeArr) {
                            boolean existTrade = false;
                            for (TradeVo tradeVo : tradeVoList) {
                                if(tradeVo.getName().equals(trade)) {
                                    existTrade = true;
                                    break;
                                }
                            }
                            if(!existTrade) {
                                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "excel解析错误，请检查" + (i+1) + "行的行业" + trade + "是否存在");
                            }
                        }
                    }
                    // 检查敏感词类型
                    if(StringUtils.isEmpty(sensitiveWordImportBo.getTypeStr()) || "".equals(sensitiveWordImportBo.getTypeStr().replaceAll("[ \\t\\n\\r\\f\\v]", ""))){
                        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "excel解析错误，" + (i+1) + "行的违规类型没有值");
                    }else {
                        boolean exist = false;
                        for (DictDataListVo dictDataListVo : sensitiveTypeList) {
                            if(dictDataListVo.getLabel().equals(sensitiveWordImportBo.getTypeStr())) {
                                exist = true;
                                break;
                            }
                        }
                        if(!exist) {
                            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "excel解析错误，请检查" + (i+1) + "行的违规类型" + sensitiveWordImportBo.getTypeStr() + "是否存在");
                        }
                    }
                    // 检查敏感词等级
                    if(StringUtils.isEmpty(sensitiveWordImportBo.getLevelStr()) || "".equals(sensitiveWordImportBo.getLevelStr().replaceAll("[ \\t\\n\\r\\f\\v]", ""))){
                        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "excel解析错误，" + (i+1) + "行的违规等级没有值");
                    }else {
                        boolean exist = false;
                        for (DictDataListVo dictDataListVo : sensitiveLevelList) {
                            if(dictDataListVo.getLabel().equals(sensitiveWordImportBo.getLevelStr())) {
                                exist = true;
                                break;
                            }
                        }
                        if(!exist) {
                            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "excel解析错误，请检查" + (i+1) + "行的违规等级" + sensitiveWordImportBo.getTypeStr() + "是否存在");
                        }
                    }
                    // 检查相似词
                    if(!StringUtils.isEmpty(sensitiveWordImportBo.getSimilarWords()) && !"".equals(sensitiveWordImportBo.getSimilarWords().replaceAll("[ \\t\\n\\r\\f\\v]", ""))){
                        String[] similarArr = sensitiveWordImportBo.getSimilarWords().split("、");
                        for (String similar : similarArr) {
                            if(StringUtils.isEmpty(similar)) {
                                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "excel解析错误，请检查" + (i+1) + "行的相似词格式");
                            }
                        }
                    }
                }

                // 构建保存数据
                for (SensitiveWordImportBo sensitiveWordImportBo : sensitiveWordImportBos) {
                    SensitiveWordsBo sensitiveWordsBo = new SensitiveWordsBo();
                    sensitiveWordsBo.setUserId(userId);
                    sensitiveWordsBo.setResourceType(0);

                    for (DictDataListVo dictDataListVo : sensitiveTypeList) {
                        if(dictDataListVo.getLabel().equals(sensitiveWordImportBo.getTypeStr())) {
                            sensitiveWordsBo.setType(Integer.valueOf(dictDataListVo.getValue()));
                            break;
                        }
                    }

                    sensitiveWordsBo.setPlatformType(1);
                    List<Integer> platformTypeList = new LinkedList<>();
                    platformTypeList.add(1);
                    sensitiveWordsBo.setPlatformTypeList(platformTypeList);

                    for (DictDataListVo dictDataListVo : sensitiveLevelList) {
                        if(dictDataListVo.getLabel().equals(sensitiveWordImportBo.getLevelStr())) {
                            sensitiveWordsBo.setLevel(Integer.valueOf(dictDataListVo.getValue()));
                            break;
                        }
                    }

                    sensitiveWordsBo.setName(sensitiveWordImportBo.getName());

                    String tradeStr = sensitiveWordImportBo.getTradeStr();
                    String[] tradeStrArr = tradeStr.split("#");
                    List<Long> resultTradeIdList = new LinkedList<>();
                    for (String trade : tradeStrArr) {
                        for (TradeVo tradeVo : tradeVoList) {
                            if(trade.equals(tradeVo.getName())) {
                                resultTradeIdList.add(tradeVo.getId());
                                break;
                            }
                        }
                    }
                    sensitiveWordsBo.setTradeId(resultTradeIdList.get(resultTradeIdList.size() - 1));
                    sensitiveWordsBo.setTradeIdArr(JSONObject.toJSONString(resultTradeIdList));

                    if(!StringUtils.isEmpty(sensitiveWordImportBo.getSimilarWords())) {
                        List<SensitiveWordsBo> similarWordList = new LinkedList<>();
                        String[] similarWordsArr = sensitiveWordImportBo.getSimilarWords().split("、");
                        for (String similarWords : similarWordsArr) {
                            SensitiveWordsBo sensitiveBo = new SensitiveWordsBo();
                            sensitiveBo.setName(similarWords);
                            similarWordList.add(sensitiveBo);
                        }
                        sensitiveWordsBo.setSimilarWordList(similarWordList);
                    }

                    sensitiveWordsBo.setRemarks(sensitiveWordImportBo.getRemarks());
                    sensitiveWordsBo.setGroupStr(sensitiveWordImportBo.getGroupStr());
                    sensitiveWordsBo.setWordsType(0);
                    sensitiveWordsBo.setStatus(0);

                    R<List<String>> save = save(sensitiveWordsBo);
                    List<String> data = save.getData();
                    if(data != null && data.size() > 0) {
                        resultMsgList.addAll(data);
                    }
                }


            }

            return R.ok(resultMsgList);


        }catch (Exception e) {
            e.printStackTrace();

        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "excel解析错误，请检查文件");
    }

    /**
     * 获取在线复盘分析信息
     * @param fileId 文件唯一标识 uuid，传其中一个
     * @param videoId 视频唯一标识 uuid，传其中一个
     * @return
     */
    public R<OnlineAnalysisInfoVo> getOnlineAnalysisInfo(String fileId, String videoId) throws Exception {

        OnlineAnalysisInfoVo onlineAnalysisInfoVo = new OnlineAnalysisInfoVo();
        if(!StringUtils.isEmpty(fileId)) {
            // 文件
            UploadFileInfoVo uploadFileInfoVo = this.uploadFileProducer.getByFileId(fileId);
            onlineAnalysisInfoVo.setUploadFile(uploadFileInfoVo);
            if(uploadFileInfoVo != null) {
                // 设置播放地址
                if(uploadFileInfoVo.getFileType() == 0 || uploadFileInfoVo.getFileType() == 1) {
                    onlineAnalysisInfoVo.setPlayUrl(uploadFileInfoVo.getPlayUrl());
                }
                // 设置词语段落分析数据
                List<OnlineAnalysisItemVo> analysisItemVoList = this.uploadFileAnalysisProducer.listByFileIdAndTradeId(uploadFileInfoVo.getFileId(), uploadFileInfoVo.getTradeId());
                onlineAnalysisInfoVo.setAnalysisList(analysisItemVoList);
            }
        }else {
            // 视频
            AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoProducer.getByVideoId(videoId);
            onlineAnalysisInfoVo.setVideoInfo(anchorVideoInfoVo);
            if(anchorVideoInfoVo != null && !StringUtils.isEmpty(anchorVideoInfoVo.getShareUrl())) {
                // 获取主播
                AnchorUrlInfoVo anchorUrlInfoVo = this.anchorUrlProducer.infoBySecUid(anchorVideoInfoVo.getSecUid());
                onlineAnalysisInfoVo.setAnchorInfo(anchorUrlInfoVo);
                // 设置播放地址
                onlineAnalysisInfoVo.setPlayUrl(anchorVideoInfoVo.getPlayUrl());
                // 设置词语段落分析数据
                VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = this.videoAnalysisRecordProducer.infoLastByVideoIdAndTradeId(anchorVideoInfoVo.getVideoId(), anchorVideoInfoVo.getTradeId());
                if(videoAnalysisRecordInfoVo != null) {
                    String dataJsonStr = ReplayFileUtils.getFileContent(wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecordInfoVo.getStoreFileName());
                    if(!StringUtils.isEmpty(dataJsonStr)) {
                        List<SentenceMarkVo> sentenceMarkVos = JSON.parseArray(dataJsonStr, SentenceMarkVo.class);

                        List<OnlineAnalysisItemVo> onlineAnalysisItemVos = sentenceMarkVos.stream().map(item -> {
                            OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
                            onlineAnalysisItemVo.setFileUuid(videoAnalysisRecordInfoVo.getVideoId());
                            onlineAnalysisItemVo.setStatus(0);
                            onlineAnalysisItemVo.setDataJson(JSON.toJSONString(item));
                            onlineAnalysisItemVo.setTradeId(videoAnalysisRecordInfoVo.getTradeId());
                            onlineAnalysisItemVo.setParagraph(item.getCurrentSort());
                            return onlineAnalysisItemVo;
                        }).sorted(Comparator.comparingInt(OnlineAnalysisItemVo::getParagraph)).collect(Collectors.toList());

                        onlineAnalysisInfoVo.setAnalysisList(onlineAnalysisItemVos);
                    }
                }
                // 获取在线人数列表
                List<OnlineNumInfoVo> onlineNumInfoVoList = socketCollectMessageProducer.onlineNumByUserIdAndBatchNumber(anchorVideoInfoVo.getUserId(), anchorVideoInfoVo.getBatchNumber() + "", videoId);
                onlineAnalysisInfoVo.setOnlineNumList(onlineNumInfoVoList);
            }
        }

        return R.ok(onlineAnalysisInfoVo);
    }

    /**
     * 获取在线复盘分析信息2_0
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    public R<AnalysisResultClientCloudVo> getOnlineAnalysisInfo2_0(Integer type, String uuid) {

        // 从本地文件获取分析数据
//        String content = "";
//        String analysisFilePath = this.getAnalysisFilePath(type, uuid);
//        if(!StringUtils.isEmpty(analysisFilePath)) {
//            content = ReplayFileUtils.getFileContent(analysisFilePath);
//        }

        // 从oss获取分析数据
        String downloadUrl = this.getAnalysisDownloadUrl(type, uuid);
        if(!StringUtils.isEmpty(downloadUrl)) {
            String content = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);

            // 封装在线复盘信息
            if(!StringUtils.isEmpty(content)) {
                AnalysisResultClientCloudVo analysisResultCloudVo = packageOnlineAnalysisInfo2_0(type, uuid, content);

                if(analysisResultCloudVo != null) {
                    return R.ok(analysisResultCloudVo);
                }
            }
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据内容不存在");
    }

    /**
     * 获取在线复盘分析数据
     * @param type
     * @param uuid
     * @return
     */
    public R<AnalysisResultVo> getAnalysisData(Integer type, String uuid){
        String downloadUrl = this.getAnalysisDownloadUrl(type, uuid);
        if(ObjectUtil.isNotEmpty(downloadUrl)) {
            String content = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);

            // 封装在线复盘信息
            if(ObjectUtil.isNotEmpty(content)) {
                return R.ok(JSONObject.parseObject(content, AnalysisResultVo.class));
            }
        }
        return R.ok(null);
    }

    /**
     * 封装在线复盘信息
     * @param type 类型 0：视频 1：文件 2
     * @param uuid 视频或文件的唯一标识 uuid
     * @param content 分析数据内容
     */
    public AnalysisResultClientCloudVo packageOnlineAnalysisInfo2_0(Integer type, String uuid, String content) {


        if(type == 0) {
            // 视频
            AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoProducer.getByVideoId(uuid);
            if(anchorVideoInfoVo != null && anchorVideoInfoVo.getUploadStatus() == 1) {

                AnalysisResultClientCloudVo analysisResultCloudVo = new AnalysisResultClientCloudVo();

                analysisResultCloudVo.setVideoInfo(anchorVideoInfoVo);
                // 获取主播
                AnchorUrlInfoVo anchorUrlInfoVo = this.anchorUrlProducer.infoBySecUid(anchorVideoInfoVo.getSecUid());
                analysisResultCloudVo.setAnchorInfo(anchorUrlInfoVo);
                // 设置播放地址
                analysisResultCloudVo.setPlayUrl(anchorVideoInfoVo.getPlayUrl());
                // 设置行业信息
                if (anchorVideoInfoVo.getTradeId() != null) {
                    TradeVo tradeInfoVo = this.tradeRse.getById(anchorVideoInfoVo.getTradeId());
                    analysisResultCloudVo.setTradeInfo(tradeInfoVo);
                }
                // 设置词语段落分析数据
                if(!StringUtils.isEmpty(content)) {
                    AnalysisResultVo analysisResultVo = JSONObject.parseObject(content, AnalysisResultVo.class);
                    analysisResultCloudVo.setWordsCollect(analysisResultVo.getWordsCollect());
                    analysisResultCloudVo.setCruxTypeList(analysisResultVo.getCruxTypeList());
                    analysisResultCloudVo.setWordsTabList(analysisResultVo.getWordsTabList());
                    List<SentenceMarkVo> sentenceMarkVos = analysisResultVo.getSentenceMarkVos();
                    if(sentenceMarkVos != null && sentenceMarkVos.size() > 0) {
                        List<OnlineAnalysisItemVo> onlineAnalysisItemVos = sentenceMarkVos.stream().map(item -> {
                            OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
                            onlineAnalysisItemVo.setFileUuid(uuid);
                            onlineAnalysisItemVo.setStatus(0);
                            onlineAnalysisItemVo.setDataJson(JSON.toJSONString(item));
                            onlineAnalysisItemVo.setTradeId(anchorVideoInfoVo.getTradeId());
                            onlineAnalysisItemVo.setParagraph(item.getCurrentSort());
                            return onlineAnalysisItemVo;
                        }).sorted(Comparator.comparingInt(OnlineAnalysisItemVo::getParagraph)).collect(Collectors.toList());

                        analysisResultCloudVo.setAnalysisList(onlineAnalysisItemVos);
                    }
                }
                // 获取在线人数列表
                List<OnlineNumInfoVo> onlineNumInfoVoList = socketCollectMessageProducer.onlineNumByUserIdAndBatchNumber(anchorVideoInfoVo.getUserId(), anchorVideoInfoVo.getBatchNumber() + "", uuid);

                if (onlineNumInfoVoList == null) onlineNumInfoVoList = new ArrayList<>();
                if (onlineNumInfoVoList.isEmpty()){
                    OnlineNumInfoVo e = new OnlineNumInfoVo();
                    e.setRecordDate(DateUtil.format(anchorVideoInfoVo.getStartTime(), "yyyy-MM-dd HH:mm:ss"));
                    e.setPeopleNum("0");
                    onlineNumInfoVoList.add(e);
                }
                analysisResultCloudVo.setOnlineNumList(onlineNumInfoVoList);
                // 获取巨量数据
                processOceanEngineData(analysisResultCloudVo, anchorVideoInfoVo);

                return analysisResultCloudVo;
            }
        }else {
            // 文件
        }

        return null;
    }

    /**
     * 获取巨量数据
     *
     * @param analysisResultCloudVo 赋值对项
     * @param anchorVideoInfoVo     视频信息
     */
    private void processOceanEngineData(AnalysisResultClientCloudVo analysisResultCloudVo, AnchorVideoInfoVo anchorVideoInfoVo) {
        // 获取巨量的数据列表
        List<OnlineAnalysisItemVo> dataJson = analysisResultCloudVo.getAnalysisList();
        VideoDataViewingConfuseInfoVo oceanEngineData = this.videoDataViewingConfuseProducer.infoByVideoIdAndUser(anchorVideoInfoVo.getVideoId(), null, null);
        if (oceanEngineData != null && ObjectUtil.isNotEmpty(dataJson)) {
            // 互动率
            analysisResultCloudVo.setInteractionPercent(oceanEngineData.getInteractionPercent());
            // 成交量
            analysisResultCloudVo.setPurchaseCountStart(oceanEngineData.getPurchaseCountStart());
            analysisResultCloudVo.setPurchaseCountEnd(oceanEngineData.getPurchaseCountEnd());
            analysisResultCloudVo.setPurchaseCount(oceanEngineData.getPurchaseCountStart());
            // 总观看人次
            analysisResultCloudVo.setTotalWatchNum(oceanEngineData.getTotalWatchNum());

            // 数据看板的数据来源
            analysisResultCloudVo.setDataSourceType(oceanEngineData.getDataSourceType());

            analysisResultCloudVo.setUvValueStart(oceanEngineData.getUvValueStart());
            analysisResultCloudVo.setUvValueEnd(oceanEngineData.getUvValueEnd());

            analysisResultCloudVo.setVolumeStart(oceanEngineData.getVolumeStart());
            analysisResultCloudVo.setVolumeEnd(oceanEngineData.getVolumeEnd());
        }

        List<SentenceMarkVo> list = dataJson.stream().map(item -> {
            try {
                return JSON.parseObject(item.getDataJson(), SentenceMarkVo.class);
            } catch (Exception e) {
                return null;
            }
        }).filter(ObjectUtil::isNotEmpty).toList();
        if (ObjectUtil.isNotEmpty(list)) {
            // 对应分钟段落的成交数量列表
            analysisResultCloudVo.setJuLiangDataList(getJuLiangDataList(anchorVideoInfoVo.getVideoId(), list, anchorVideoInfoVo.getStartTime(), anchorVideoInfoVo.getEndTime()));
        }
    }

    private List<JuLiangDataListVo> getJuLiangDataList(String videoId, List<SentenceMarkVo> list, Date startDate, Date endDate) {
        VideoDataViewingConfuseInfoVo oceanEngineData = videoDataViewingBll.getOceanEngineDetailsByVideoId(videoId);
        long startTime = startDate.getTime();
        if (oceanEngineData != null) {
            List<CurveData> temp = list.stream().map(item -> {
                List<WordListItemVo> items = item.getItems();
                if (ObjectUtil.isNotEmpty(items)) {
                    CurveData result = new CurveData();
                    result.setDateTime(startTime + items.get(0).getStartTime());
                    return result;
                }
                return null;
            }).filter(ObjectUtil::isNotEmpty).collect(Collectors.toList());

            // 把数据转换为对应的时间的数据
            List<OceanEngineProcessBo> oceanEngineProcessBos = oceanEngineDataProducer.convertToIncrementalData(oceanEngineData.getOceanEngineProcessList(), temp, startDate, endDate);

            // 把增量数据转换为图表数据
            List<CurveData> payComboData = oceanEngineDataProducer.convertToChartData(oceanEngineProcessBos, OceanEngineProcessBo::getPayComboCnt, startDate);
            Map<Long, Integer> payAmtMap = oceanEngineDataProducer.convertToChartData(oceanEngineProcessBos, OceanEngineProcessBo::getPayAmt, startDate)
                    .stream()
                    .collect(Collectors.toMap(CurveData::getDateTime, CurveData::getValueNum, (key1, key2) -> key2));

            if (ObjectUtil.isNotEmpty(payComboData)) {
                // 封装数据
                return payComboData.stream().map(item -> {
                    JuLiangDataListVo data = new JuLiangDataListVo();
                    data.setDateTime(item.getDateTime().toString());
                    data.setPayComboCnt(item.getValueNum());
                    Integer fenAmt = payAmtMap.getOrDefault(item.getDateTime(), 0);
                    BigDecimal yuanAmt = BigDecimal.valueOf(fenAmt)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    data.setSalesCount(yuanAmt.doubleValue());
                    data.setDate(DateUtil.formatDateTime(new Date(item.getDateTime())));
                    return data;
                }).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    /**
     * 巨量引擎分钟数据 enrich（投放消耗 + 净成交ROI）
     */
    private void enrichOceanEngineMinuteData(String videoId, AnalysisResultCloudVo data) {
        VideoDataViewingConfuseInfoVo oceanEngineData = videoDataViewingBll.getOceanEngineDetailsByVideoId(videoId);
        if (oceanEngineData == null || ObjectUtil.isEmpty(oceanEngineData.getOceanEngineProcessList())) {
            return;
        }

        AnchorVideoInfoVoUpper videoInfo = data.getVideoInfo();
        if (videoInfo == null || videoInfo.getStartTime() == null || videoInfo.getEndTime() == null) {
            return;
        }

        List<VideoParagraphAnalysisVoUpper> audioaAlyses = data.getAudioaAlyses();
        if (ObjectUtil.isEmpty(audioaAlyses)) {
            return;
        }

        long startTimeMs = videoInfo.getStartTime().getTime();
        List<CurveData> timeAnchors = audioaAlyses.stream().map(item -> {
            try {
                SentenceMarkVo vo = JSON.parseObject(item.getDataJson(), SentenceMarkVo.class);
                if (vo != null && ObjectUtil.isNotEmpty(vo.getItems())) {
                    CurveData cd = new CurveData();
                    cd.setDateTime(startTimeMs + vo.getItems().get(0).getStartTime());
                    return cd;
                }
            } catch (Exception e) {
                log.warn("解析段落dataJson失败: {}", item.getDataJson(), e);
            }
            return null;
        }).filter(ObjectUtil::isNotEmpty).toList();

        if (ObjectUtil.isEmpty(timeAnchors)) {
            return;
        }

        List<OceanEngineProcessBo> incrementalData = oceanEngineDataProducer.convertToIncrementalData(
                oceanEngineData.getOceanEngineProcessList(), timeAnchors, videoInfo.getStartTime(), videoInfo.getEndTime());

        if (ObjectUtil.isEmpty(incrementalData)) {
            return;
        }

        List<Map<String, Object>> qianchuanCostDataList = new ArrayList<>();
        List<Map<String, Object>> netTransactionRoiDataList = new ArrayList<>();

        BigDecimal totalQianchuanCostFen = BigDecimal.ZERO;
        int totalPayAmtFen = 0;
        BigDecimal totalRefundAmtFen = BigDecimal.ZERO;

        for (OceanEngineProcessBo bo : incrementalData) {
            if (bo.getGatherTimeStamp() == null) {
                continue;
            }

            BigDecimal cost = bo.getQianchuanCost() != null ? bo.getQianchuanCost() : BigDecimal.ZERO;
            int payAmt = bo.getPayAmt() != null ? bo.getPayAmt() : 0;
            BigDecimal refund = bo.getRefundAmt() != null ? bo.getRefundAmt() : BigDecimal.ZERO;

            totalQianchuanCostFen = totalQianchuanCostFen.add(cost);
            totalPayAmtFen += payAmt;
            totalRefundAmtFen = totalRefundAmtFen.add(refund);

            Date gatherDate = new Date(bo.getGatherTimeStamp());
            Map<String, Object> costMap = new HashMap<>();
            costMap.put("dateTime", String.valueOf(bo.getGatherTimeStamp()));
            costMap.put("date", DateUtil.formatDateTime(gatherDate));
            costMap.put("value", cost.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            qianchuanCostDataList.add(costMap);

            Map<String, Object> roiMap = new HashMap<>();
            roiMap.put("dateTime", String.valueOf(bo.getGatherTimeStamp()));
            roiMap.put("date", DateUtil.formatDateTime(gatherDate));
            BigDecimal netAmt = BigDecimal.valueOf(payAmt).subtract(refund);
            double roiValue = 0;
            if (cost.compareTo(BigDecimal.ZERO) > 0 && netAmt.compareTo(BigDecimal.ZERO) > 0) {
                roiValue = netAmt.divide(cost, 4, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP).doubleValue();
            }
            roiMap.put("value", roiValue);
            netTransactionRoiDataList.add(roiMap);
        }

        data.setQianchuanCostDataList(qianchuanCostDataList);
        data.setTotalQianchuanCost(totalQianchuanCostFen.divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValue());

        data.setNetTransactionRoiDataList(netTransactionRoiDataList);
        BigDecimal netAmtFen = BigDecimal.valueOf(totalPayAmtFen).subtract(totalRefundAmtFen);
        double totalNetTransactionRoi = 0;
        if (totalQianchuanCostFen.compareTo(BigDecimal.ZERO) > 0 && netAmtFen.compareTo(BigDecimal.ZERO) > 0) {
            totalNetTransactionRoi = netAmtFen.divide(totalQianchuanCostFen, 4, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
        data.setTotalNetTransactionRoi(totalNetTransactionRoi);
    }

    /**
     * 获取在线复盘对比分析信息
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    public R<OnlineContrastAnalysisInfoVo> getOnlineContrastAnalysisInfo(String contrastId) throws Exception {
        OnlineContrastAnalysisInfoVo onlineContrastAnalysisInfoVo = new OnlineContrastAnalysisInfoVo();
        // 获取对比
        SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.infoByContrastId(contrastId);
        if(syncContrastInfoVo != null && !StringUtils.isEmpty(syncContrastInfoVo.getShareUrl())) {
            R<OnlineAnalysisInfoVo> onlineAnalysisInfo1 = this.getOnlineAnalysisInfo(syncContrastInfoVo.getFileOneId(), syncContrastInfoVo.getVideoOneId());
            onlineContrastAnalysisInfoVo.setSentenceMark1(onlineAnalysisInfo1.getData());

            R<OnlineAnalysisInfoVo> onlineAnalysisInfo2 = this.getOnlineAnalysisInfo(syncContrastInfoVo.getFileTwoId(), syncContrastInfoVo.getVideoTwoId());
            onlineContrastAnalysisInfoVo.setSentenceMark2(onlineAnalysisInfo2.getData());
        }

        return R.ok(onlineContrastAnalysisInfoVo);
    }

    /**
     * 获取在线复盘对比分析信息2_0
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    public R<ContrastAnalysisResultCloudVo> getOnlineContrastAnalysisInfo2_0(String contrastId) throws Exception {
        ContrastAnalysisResultCloudVo contrastAnalysisResultCloudVo = new ContrastAnalysisResultCloudVo();
        // 获取对比
        SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.infoByContrastId(contrastId);
        if(syncContrastInfoVo != null && !StringUtils.isEmpty(syncContrastInfoVo.getShareUrl())) {
            R<AnalysisResultClientCloudVo> onlineAnalysisInfo1 = this.getOnlineAnalysisInfo2_0(0, syncContrastInfoVo.getVideoOneId());
            contrastAnalysisResultCloudVo.setSentenceMark1(onlineAnalysisInfo1.getData());

            R<AnalysisResultClientCloudVo> onlineAnalysisInfo2 = this.getOnlineAnalysisInfo2_0(0, syncContrastInfoVo.getVideoTwoId());
            contrastAnalysisResultCloudVo.setSentenceMark2(onlineAnalysisInfo2.getData());
        }

        return R.ok(contrastAnalysisResultCloudVo);
    }

    /**
     * 获取之前最新的分析记录
     * @param videoId 视频id/文件id
     * @param type 类型 0：视频 1：文件
     * @return
     */
    public R<List<SentenceMarkVo>> getLastAnalysisList(String videoId, Integer type) {

        if(type == 0) {
            // 获取最后一条分析信息
            VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = videoAnalysisRecordProducer.getLastInfo(videoId);
            if(videoAnalysisRecordInfoVo != null) {
                // 获取分析内容
                String jsonDataStr = ReplayFileUtils.getFileContent(wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecordInfoVo.getStoreFileName());
                if(!StringUtils.isEmpty(jsonDataStr)) {
                    List<SentenceMarkVo> sentenceMarkVos = JSON.parseArray(jsonDataStr, SentenceMarkVo.class);
                    return R.ok(sentenceMarkVos);
                }
            }
        }else {
            // 获取最后一条分析信息
            UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = uploadFileAnalysisRecordProducer.getLastInfo(videoId);
            if(uploadFileAnalysisRecordInfoVo != null) {
                // 获取分析内容
                String jsonDataStr = ReplayFileUtils.getFileContent(wordsProperties.getFileAnalysisStorePath() + uploadFileAnalysisRecordInfoVo.getStoreFileName());
                if(!StringUtils.isEmpty(jsonDataStr)) {
                    List<SentenceMarkVo> sentenceMarkVos = JSON.parseArray(jsonDataStr, SentenceMarkVo.class);
                    return R.ok(sentenceMarkVos);
                }
            }
        }


        return R.error(5001, "数据不存在");
    }

    /**
     * 获取之前最新的分析记录2_0
     * @param videoId 视频id/文件id
     * @param type 类型 0：视频 1：文件
     * @return
     */
    public R<AnalysisResultVo> getLastAnalysisList2_0(String videoId, Integer type) {

        if(type == 0) {
            // 获取最后一条分析信息
            VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = videoAnalysisRecordProducer.getLastInfo(videoId);
            if(videoAnalysisRecordInfoVo != null) {
                // 获取分析内容
                String jsonDataStr = ReplayFileUtils.getFileContent(wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecordInfoVo.getStoreFileNameNew());
                if(!StringUtils.isEmpty(jsonDataStr)) {
                    AnalysisResultVo analysisResultVo = JSONObject.parseObject(jsonDataStr, AnalysisResultVo.class);
                    return R.ok(analysisResultVo);
                }
            }
        }else {
            // 获取最后一条分析信息
            UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = uploadFileAnalysisRecordProducer.getLastInfo(videoId);
            if(uploadFileAnalysisRecordInfoVo != null) {
                // 获取分析内容
                String jsonDataStr = ReplayFileUtils.getFileContent(wordsProperties.getFileAnalysisStorePath() + uploadFileAnalysisRecordInfoVo.getStoreFileNameNew());
                if(!StringUtils.isEmpty(jsonDataStr)) {
                    AnalysisResultVo analysisResultVo = JSONObject.parseObject(jsonDataStr, AnalysisResultVo.class);
                    return R.ok(analysisResultVo);
                }
            }
        }


        return R.error(5001, "数据不存在");
    }

    /**
     * 获取之前最新的分析记录OSS
     * @param videoId 视频id/文件id
     * @param type 类型 0：视频 1：文件
     * @return
     */
    public R<AnalysisResultVo> getLastAnalysisListByOss(String videoId, Integer type) {

        if(type == 0) {
            // 获取最后一条分析信息
            VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = videoAnalysisRecordProducer.getLastInfo(videoId);
            if(videoAnalysisRecordInfoVo != null) {
                // 从oss获取分析内容
                String downloadUrl = ossUtils.getSignDownloadUrl("replay-analysis-data", videoAnalysisRecordInfoVo.getStoreFileOssKey(), aliOssProperties.getIsOuterNet());
                String jsonDataStr = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);
//                String jsonDataStr = ReplayFileUtils.getFileContent(wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecordInfoVo.getStoreFileNameNew());
                if(!StringUtils.isEmpty(jsonDataStr)) {
                    AnalysisResultVo analysisResultVo = JSONObject.parseObject(jsonDataStr, AnalysisResultVo.class);
                    return R.ok(analysisResultVo);
                }
            }
        }else {
            // 获取最后一条分析信息
            UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = uploadFileAnalysisRecordProducer.getLastInfo(videoId);
            if(uploadFileAnalysisRecordInfoVo != null) {
                // 从oss获取分析内容
//                String jsonDataStr = ReplayFileUtils.getFileContent(wordsProperties.getFileAnalysisStorePath() + uploadFileAnalysisRecordInfoVo.getStoreFileNameNew());
                String downloadUrl = ossUtils.getSignDownloadUrl("replay-analysis-data", uploadFileAnalysisRecordInfoVo.getStoreFileOssKey(), false);
                String jsonDataStr = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);
                if(!StringUtils.isEmpty(jsonDataStr)) {
                    AnalysisResultVo analysisResultVo = JSONObject.parseObject(jsonDataStr, AnalysisResultVo.class);
                    return R.ok(analysisResultVo);
                }
            }
        }


        return R.error(5001, "数据不存在");
    }

    /**
     * 下载分析文件
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    public byte[] downloadAnalysisFile(Integer type, String uuid) {


        String filePath = "";

        if(type == 0) {
            // 视频
            AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoProducer.getByVideoId(uuid);
            if(anchorVideoInfoVo != null) {
                // 获取最后一条分析数据
                VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = this.videoAnalysisRecordProducer.infoLastByVideoIdAndTradeId(anchorVideoInfoVo.getVideoId(), anchorVideoInfoVo.getTradeId());
                if(videoAnalysisRecordInfoVo != null) {
                    filePath = wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecordInfoVo.getStoreFileName();
                }
            }
        }else {
            // 文件
            UploadFileInfoVo uploadFileInfoVo = this.uploadFileProducer.getByFileId(uuid);
            if(uploadFileInfoVo != null) {
                UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = this.uploadFileAnalysisRecordProducer.infoLastByFileIdAndTradeId(uploadFileInfoVo.getFileId(), uploadFileInfoVo.getTradeId());
                if(uploadFileAnalysisRecordInfoVo != null) {
                    filePath = wordsProperties.getFileAnalysisStorePath() + uploadFileAnalysisRecordInfoVo.getStoreFileName();
                }
            }
        }

        if(!StringUtils.isEmpty(filePath)) {
            // 读取文件
            try {

                Path path = Paths.get(filePath);
                return Files.readAllBytes(path);

            } catch (IOException e) {
                log.error("分析文件没找到:{}", filePath);
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * 下载分析文件2_0
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    public byte[] downloadAnalysisFile2_0(Integer type, String uuid) {

        return getAnalysisByteByOss(type, uuid);

//        String filePath = getAnalysisFilePath(type, uuid);
//
//        if(!StringUtils.isEmpty(filePath)) {
//            // 读取文件
//            try {
//
//                Path path = Paths.get(filePath);
//                return Files.readAllBytes(path);
//
//            } catch (IOException e) {
//                log.error("分析文件没找到:{}", filePath);
//                e.printStackTrace();
//            }
//        }

//        return null;
    }

    /**
     * 获取分析文件地址
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    private String getAnalysisFilePath(Integer type, String uuid) {
        String filePath = "";

        if(type == 0) {
            // 视频
            AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoProducer.getByVideoId(uuid);
            if(anchorVideoInfoVo != null) {
                // 获取最后一条分析数据
                VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = this.videoAnalysisRecordProducer.infoLastByVideoIdAndTradeId(anchorVideoInfoVo.getVideoId(), anchorVideoInfoVo.getTradeId());
                if(videoAnalysisRecordInfoVo != null) {
                    if(StringUtils.isEmpty(videoAnalysisRecordInfoVo.getStoreFileNameNew())) {
                        // 当前数据是2.0之前的数据，转成新的
                        String storeFileNameNew = convertNewAnalysis(
                                wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecordInfoVo.getStoreFileName(),
                                videoAnalysisRecordInfoVo.getVideoId(),
                                0,
                                videoAnalysisRecordInfoVo.getTradeId(),
                                videoAnalysisRecordInfoVo.getVersion(),
                                videoAnalysisRecordInfoVo.getId());

                        // 将新路径保存到数据库
                        this.videoAnalysisRecordProducer.saveNewFilePath(videoAnalysisRecordInfoVo.getId(), storeFileNameNew);

                        filePath = wordsProperties.getVideoAnalysisStorePath() + storeFileNameNew;
                    }else {
                        filePath = wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecordInfoVo.getStoreFileNameNew();
                    }
                }
            }
        }else {
            // 文件
            UploadFileInfoVo uploadFileInfoVo = this.uploadFileProducer.getByFileId(uuid);
            if(uploadFileInfoVo != null) {
                UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = this.uploadFileAnalysisRecordProducer.infoLastByFileIdAndTradeId(uploadFileInfoVo.getFileId(), uploadFileInfoVo.getTradeId());
                if(uploadFileAnalysisRecordInfoVo != null) {
                    if(StringUtils.isEmpty(uploadFileAnalysisRecordInfoVo.getStoreFileNameNew())) {
                        // 当前数据是2.0之前的数据，转成新的
                        String storeFileNameNew = convertNewAnalysis(
                                wordsProperties.getFileAnalysisStorePath() + uploadFileAnalysisRecordInfoVo.getStoreFileName(),
                                uploadFileAnalysisRecordInfoVo.getFileId(),
                                1,
                                uploadFileAnalysisRecordInfoVo.getTradeId(),
                                uploadFileAnalysisRecordInfoVo.getVersion(),
                                uploadFileAnalysisRecordInfoVo.getId());

                        // 将新路径保存到数据库
                        this.uploadFileAnalysisRecordProducer.saveNewFilePath(uploadFileAnalysisRecordInfoVo.getId(), storeFileNameNew);

                        filePath = wordsProperties.getFileAnalysisStorePath() + storeFileNameNew;
                    }else {
                        filePath = wordsProperties.getFileAnalysisStorePath() + uploadFileAnalysisRecordInfoVo.getStoreFileNameNew();
                    }

                }
            }
        }

        return filePath;
    }

    /**
     * 将旧数据读取出来，转成新的格式
     * @param oldStoreFileName 旧数据文件路径
     * @param uuid 视频/文件唯一标识
     * @param type 类型 0：视频 1：文件
     * @param tradeId 行业id
     * @param versionNum 版本号
     * @param recordId 记录id
     * @return
     */
    private String convertNewAnalysis(String oldStoreFileName, String uuid, int type, Long tradeId, int versionNum, Long recordId) {
        if(!StringUtils.isEmpty(oldStoreFileName)) {
            // 读取文件
            String oldContent = ReplayFileUtils.getFileContent(oldStoreFileName);
            if(!StringUtils.isEmpty(oldContent)) {
                List<SentenceMarkVo> sentenceMarkVos = JSON.parseArray(oldContent, SentenceMarkVo.class);

                // 将旧关键词的关键词分类信息填充上
                convertNewAnalysisCruxWord(sentenceMarkVos, tradeId);

                AnalysisResultVo analysisResultVo = new AnalysisResultVo();
                analysisResultVo.setSentenceMarkVos(sentenceMarkVos);

                // 封装关键词分类信息
                List<AnalysisResultCruxTypeVo> analysisResultCruxTypeVos = packageCruxType(sentenceMarkVos);
                analysisResultVo.setCruxTypeList(analysisResultCruxTypeVos);

                // 封装词语汇总列表
                List<WordsMarkVo> wordsCollect = packageWordCollect(sentenceMarkVos);
                analysisResultVo.setWordsCollect(wordsCollect);

                // 统计词语全文出现次数
                this.countWordTotalNum(wordsCollect, sentenceMarkVos);

                // 封装词语tab列表
                List<WordsTabVo> wordsTabList = this.packageWordTab(sentenceMarkVos, analysisResultCruxTypeVos);
                analysisResultVo.setWordsTabList(wordsTabList);

                String newJsonStr = JSONObject.toJSONString(analysisResultVo);
                return this.sensitiveWordsProducer.saveAnalysisDataToFile2_0("20", uuid, type, tradeId, versionNum, newJsonStr, recordId);
            }
        }
        return null;
    }

    /**
     * 将旧关键词的关键词分类信息填充上
     * @param sentenceMarkVos 段落词语信息
     * @param tradeId 行业id
     */
    public void convertNewAnalysisCruxWord(List<SentenceMarkVo> sentenceMarkVos, Long tradeId) {
        if(sentenceMarkVos != null && sentenceMarkVos.size() > 0) {

            List<SensitiveWordsVo> sensitiveWordsVos = this.sensitiveWordsProducer.listByUidAndPidAndTid(0L, 1, tradeId, 1);
            if(sensitiveWordsVos != null && sensitiveWordsVos.size() > 0) {

                List<CruxTypeInfoVo> cruxTypeInfoVos = this.cruxTypeProducer.listAll();
                if(cruxTypeInfoVos != null && cruxTypeInfoVos.size() > 0) {

                    for (SentenceMarkVo sentenceMarkVo : sentenceMarkVos) {
                        List<WordsMarkVo> wordsList = sentenceMarkVo.getWordsList();
                        if(wordsList != null && wordsList.size() > 0) {

                            for (WordsMarkVo wordsMarkVo : wordsList) {
                                if(wordsMarkVo.getWordsType() == 1 && wordsMarkVo.getResourceType() == 0) {

                                    for (SensitiveWordsVo sensitiveWordsVo : sensitiveWordsVos) {
                                        if(wordsMarkVo.getName().equals(sensitiveWordsVo.getName())) {
                                            wordsMarkVo.setCruxTypeId(sensitiveWordsVo.getCruxTypeId());

                                            for (CruxTypeInfoVo cruxTypeInfoVo : cruxTypeInfoVos) {
                                                if(cruxTypeInfoVo.getId().equals(wordsMarkVo.getCruxTypeId())) {
                                                    wordsMarkVo.setCruxTypeInfo(cruxTypeInfoVo);
                                                    break;
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


            }

        }
    }

    /**
     * 统计词语在全文出现的次数
     * @param wordsCollect 词语信息
     * @param resultMarkVos 段落信息
     */
    public void countWordTotalNum(List<WordsMarkVo> wordsCollect, List<SentenceMarkVo> resultMarkVos) {
        if(wordsCollect != null && wordsCollect.size() > 0) {
            for (WordsMarkVo wordsMarkVo : wordsCollect) {
                int totalNum = 0;
                String wordName = wordsMarkVo.getName();
                for (SentenceMarkVo resultMarkVo : resultMarkVos) {
                    String content = resultMarkVo.getContent();
                    while (content.contains(wordName)) {
                        content = content.substring(content.indexOf(wordName) + wordName.length());
                        totalNum ++;
                    }
                }
                wordsMarkVo.setTotalNum(totalNum);
            }
        }
    }

    /**
     * 封装词语tab列表
     * @param resultMarkVos 段落词语信息
     * @param resultCruxTypeVos 分类汇总信息
     * @return
     */
    public List<WordsTabVo> packageWordTab(List<SentenceMarkVo> resultMarkVos, List<AnalysisResultCruxTypeVo> resultCruxTypeVos) {
        if(resultMarkVos != null && resultMarkVos.size() > 0) {
            List<WordsTabVo> wordsTabVos = new LinkedList<>();

            // 全部、敏感词、关键词tab
            WordsTabVo allTab = new WordsTabVo(0L, 0, -99, 0.0, 0, "全部", 1);
            WordsTabVo sensitiveTab = new WordsTabVo(0L, 1, -98, 0.0, 0, "智能敏感词", 1);
            WordsTabVo cruxTab = new WordsTabVo(0L, 2, -97, 0.0, 0, "运营关键词", 1);
            wordsTabVos.add(allTab);
            wordsTabVos.add(sensitiveTab);
            wordsTabVos.add(cruxTab);

            // 2级分类tab
            List<CruxTypeInfoVo> twoLevelCruxType = this.cruxTypeProducer.listByLevel(2);
            if(twoLevelCruxType != null && twoLevelCruxType.size() > 0) {
                for (CruxTypeInfoVo cruxType : twoLevelCruxType) {
                    WordsTabVo cruxTypeTab = new WordsTabVo(cruxType.getId(), 3, cruxType.getTabSort(), 0.0, 0, cruxType.getName(), cruxType.getIsCount());
                    wordsTabVos.add(cruxTypeTab);
                }
            }

            // 统计数量
            for (SentenceMarkVo resultMarkVo : resultMarkVos) {
                List<WordsMarkVo> wordsList = resultMarkVo.getWordsList();
                if(wordsList != null && wordsList.size() > 0) {
                    for (WordsMarkVo wordsMarkVo : wordsList) {
                        // 全部
                        allTab.setNum(allTab.getNum() + wordsMarkVo.getCountNum());
                        // 敏感词
                        if(wordsMarkVo.getWordsType() == 0) {
                            sensitiveTab.setNum(sensitiveTab.getNum() + wordsMarkVo.getCountNum());
                        }
                        // 关键词
                        if(wordsMarkVo.getWordsType() == 1 && !StringUtils.isEmpty(wordsMarkVo.getCruxTypeId()) &&
                                !wordsMarkVo.getCruxTypeId().equals(0L) && wordsMarkVo.getCruxTypeInfo() != null) {

                            if(wordsMarkVo.getCruxTypeInfo().getIsCount() == 1) {
                                cruxTab.setNum(cruxTab.getNum() + wordsMarkVo.getCountNum());
                            }
                            for (WordsTabVo wordsTabVo : wordsTabVos) {
                                if(wordsMarkVo.getCruxTypeInfo().getIdArr().contains(wordsTabVo.getCruxTypeId())) {
                                    wordsTabVo.setNum(wordsTabVo.getNum() + wordsMarkVo.getCountNum());
                                }
                            }

                        }
                    }
                }
            }

            // 筛选出需要统计百分比的分类
            if(resultCruxTypeVos != null && resultCruxTypeVos.size() > 0) {
                List<AnalysisResultCruxTypeVo> cruxTypeVos = resultCruxTypeVos.stream().filter(item -> {
                    return item.getCruxTypeInfoVo().getLevel() == 2 && item.getCruxTypeInfoVo().getIsCount() == 1;
                }).toList();

                if(cruxTypeVos.size() > 0) {
                    for (AnalysisResultCruxTypeVo cruxTypeVo : cruxTypeVos) {
                        for (WordsTabVo wordsTabVo : wordsTabVos) {
                            if(cruxTypeVo.getCruxTypeInfoVo().getId().equals(wordsTabVo.getCruxTypeId())) {
                                wordsTabVo.setNum(cruxTypeVo.getNum());
                                wordsTabVo.setScale(cruxTypeVo.getScale());
                                break;
                            }
                        }
                    }
                }
            }

            // 排序
            wordsTabVos.sort(Comparator.comparingInt(WordsTabVo::getTabSort));

            return wordsTabVos;

        }
        return null;
    }

    /**
     * 封装词语汇总列表
     * @param resultMarkVos 分析结果段落内容列表
     * @return
     */
    public List<WordsMarkVo> packageWordCollect(List<SentenceMarkVo> resultMarkVos) {
        List<WordsMarkVo> wordsCollect = new LinkedList<>();
        if(resultMarkVos != null && resultMarkVos.size() > 0) {
            for (SentenceMarkVo resultMarkVo : resultMarkVos) {
                // 获取段落的关键词列表
                List<WordsMarkVo> wordsList = resultMarkVo.getWordsList();
                if(wordsList != null && wordsList.size() > 0) {
                    for (WordsMarkVo wordItem : wordsList) {
                        boolean exist = false;
                        // 判断是否已存在于汇总列表
                        for (WordsMarkVo collectItem : wordsCollect) {
                            if(wordItem.getName().equals(collectItem.getName()) && wordItem.getWordsType().equals(collectItem.getWordsType())) {
                                // 已存在，加数量
                                exist = true;
                                if(!StringUtils.isEmpty(wordItem.getCountNum())) {
                                    collectItem.setCountNum(collectItem.getCountNum() + wordItem.getCountNum());
                                }
                                if(!StringUtils.isEmpty(wordItem.getTotalNum())) {
                                    collectItem.setTotalNum(collectItem.getTotalNum() + wordItem.getTotalNum());
                                }
                                break;
                            }
                        }
                        if(!exist) {
                            WordsMarkVo collectItem = new WordsMarkVo();
                            BeanUtils.copyProperties(wordItem, collectItem);
                            if(StringUtils.isEmpty(collectItem.getCountNum())) {
                                collectItem.setCountNum(0);
                            }
                            if(StringUtils.isEmpty(collectItem.getTotalNum())) {
                                collectItem.setTotalNum(0);
                            }
                            wordsCollect.add(collectItem);
                        }
                    }

                }
            }

            // 排序
            if(wordsCollect.size() > 0) {
                for (WordsMarkVo wordsMarkVo : wordsCollect) {
                    if(wordsMarkVo.getCruxTypeInfo() == null) {
                        CruxTypeInfoVo cruxTypeInfoVo = new CruxTypeInfoVo();
                        cruxTypeInfoVo.setSort(9999);
                        wordsMarkVo.setCruxTypeInfo(cruxTypeInfoVo);
                    }
                    if(StringUtils.isEmpty(wordsMarkVo.getGroupStr())) {
                        wordsMarkVo.setGroupStr("");
                    }
                }

                wordsCollect.sort(
                        Comparator.comparingInt(WordsMarkVo::getWordsType) // 按词语类型升序排序
                                .thenComparingInt((WordsMarkVo w) -> w.getCruxTypeInfo().getSort()) // 再按词语分类升序排序
                                .thenComparing(WordsMarkVo::getGroupStr, Comparator.reverseOrder()) // 再按词组名称降序排序
                                .thenComparing(WordsMarkVo::getCountNum, Comparator.reverseOrder()) // 最后按词语的次数降序排序
                );
            }
        }

        return wordsCollect;
    }

    /**
     * 封装关键词分类占比等信息
     * @param resultMarkVos 段落词语内容列表
     * @return
     */
    public List<AnalysisResultCruxTypeVo> packageCruxType(List<SentenceMarkVo> resultMarkVos) {

        if(resultMarkVos != null && resultMarkVos.size() > 0) {

            // 获取所有关键词分类列表
            List<CruxTypeInfoVo> allCruxTypeList = this.cruxTypeProducer.listAll();
            if(allCruxTypeList != null && allCruxTypeList.size() > 0) {
                // 排序，先按层级，再按序号
                allCruxTypeList.sort(Comparator.comparingInt(CruxTypeVo::getLevel).thenComparing(CruxTypeVo::getSort));

                // 筛选出展示在内容罗盘的分类
                List<CruxTypeInfoVo> showCompassCruxTypeList = allCruxTypeList.stream().filter(item -> item.getIsShowCompass() == 1).toList();

                if(showCompassCruxTypeList.size() > 0) {
                    // 只需要在内容罗盘展示的分类以及其子分类
                    List<AnalysisResultCruxTypeVo> resultCruxTypeList = new LinkedList<>();
                    for (CruxTypeInfoVo item : allCruxTypeList) {
                        boolean showCompass = false;
                        for (CruxTypeInfoVo showCompassCruxType : showCompassCruxTypeList) {
                            if(item.getIdArr().contains(showCompassCruxType.getId()) || showCompassCruxType.getIdArr().contains(item.getId())) {
                                showCompass = true;
                                break;
                            }
                        }
                        if(showCompass) {
                            AnalysisResultCruxTypeVo analysisResultCruxTypeVo = new AnalysisResultCruxTypeVo();
                            analysisResultCruxTypeVo.setCruxTypeInfoVo(item);
                            analysisResultCruxTypeVo.setNum(0);
                            analysisResultCruxTypeVo.setScale(0.0);
                            resultCruxTypeList.add(analysisResultCruxTypeVo);
                        }

                    }

                    // 统计关键词分类信息
                    double totalNum = 0;
                    for (SentenceMarkVo resultMarkVo : resultMarkVos) {
                        List<WordsMarkVo> wordsList = resultMarkVo.getWordsList();
                        if(wordsList != null && wordsList.size() > 0) {
                            for (WordsMarkVo wordsMarkVo : wordsList) {
                                if(wordsMarkVo.getWordsType() == 1) {
                                    // 关键词的分类信息
                                    CruxTypeInfoVo cruxTypeInfo = wordsMarkVo.getCruxTypeInfo();
                                    // 只统计需要统计到总数的关键词分类
                                    if(cruxTypeInfo != null && cruxTypeInfo.getIsCount() == 1) {
                                        for (AnalysisResultCruxTypeVo analysisResultCruxTypeVo : resultCruxTypeList) {
                                            if(analysisResultCruxTypeVo.getCruxTypeInfoVo().getId().equals(cruxTypeInfo.getId())) {
                                                // 递归设置关键词分类信息的关键词数量
                                                packageSelfAndParentCruxType(resultCruxTypeList, analysisResultCruxTypeVo, wordsMarkVo.getCountNum());
                                            }
                                        }
                                        totalNum += wordsMarkVo.getCountNum();
                                    }
                                }
                            }
                        }
                    }

                    // 筛选，只保留关键词数量大于0的关键词分类
//                        List<AnalysisResultCruxTypeVo> analysisResultCruxTypeVos = resultCruxTypeList.stream().filter(item -> item.getNum() > 0).collect(Collectors.toList());

                    // 计算占比
                    // 指定保留三位小数
                    DecimalFormat df = new DecimalFormat("#.###");
                    // 设置舍入模式为四舍五入
                    df.setRoundingMode(java.math.RoundingMode.HALF_UP);
                    for (AnalysisResultCruxTypeVo analysisResultCruxTypeVo : resultCruxTypeList) {
                        if(analysisResultCruxTypeVo.getNum() > 0) {
                            double scale = analysisResultCruxTypeVo.getNum() / totalNum;
                            String formattedScale = df.format(scale);
                            analysisResultCruxTypeVo.setScale(Double.parseDouble(formattedScale));
                        }
                    }
                    return resultCruxTypeList;

                }
            }
        }

        return null;
    }

    /**
     * 递归封分类关键词数量
     * @param resultCruxTypeList 所有分析结果的关键词类型列表
     * @param currentCruxType 当前分析结果的关键词类型信息
     * @param countNum 词语数量
     */
    private void packageSelfAndParentCruxType(List<AnalysisResultCruxTypeVo> resultCruxTypeList, AnalysisResultCruxTypeVo currentCruxType, Integer countNum) {
        // 加分类关键词数量
        currentCruxType.setNum(currentCruxType.getNum() + countNum);
        // 判断是否存在父级，存在则同时增加父级的数量
        if(!currentCruxType.getCruxTypeInfoVo().getParentId().equals(0L)) {
            for (AnalysisResultCruxTypeVo resultCruxTypeVo : resultCruxTypeList) {
                if(resultCruxTypeVo.getCruxTypeInfoVo().getId().equals(currentCruxType.getCruxTypeInfoVo().getParentId())) {
                    packageSelfAndParentCruxType(resultCruxTypeList, resultCruxTypeVo, countNum);
                }
            }
        }
    }

    /**
     * 获取在线复盘分析信息，以zip形式返回
     * @param fileId 文件唯一标识 uuid，传其中一个
     * @param videoId 视频唯一标识 uuid，传其中一个
     * @return
     */
    public byte[] getOnlineAnalysisZip(String fileId, String videoId) throws Exception {

        // 获取分析信息
        R<OnlineAnalysisInfoVo> onlineAnalysisInfoR = this.getOnlineAnalysisInfo(fileId, videoId);
        if(onlineAnalysisInfoR.getCode() == 0 && onlineAnalysisInfoR.getData() != null) {
            OnlineAnalysisInfoVo analysisInfoVo = onlineAnalysisInfoR.getData();
            analysisInfoVo.getAnchorInfo().setId(0L);
            analysisInfoVo.getVideoInfo().setId(0L);
            // 转成zip byte[]
            return analysisInfoToZipByteArray(JSON.toJSONString(analysisInfoVo));
        }

        return null;
    }

    /**
     * 获取在线复盘分析信息，以zip形式返回2_0
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    public byte[] getOnlineAnalysisZip2_0(Integer type, String uuid) throws Exception {

        R<AnalysisResultClientCloudVo> r = this.getOnlineAnalysisInfo2_0(type, uuid);
        if(r.getCode() == 0 && r.getData() != null) {
            AnalysisResultClientCloudVo resultCloudVo = r.getData();
            resultCloudVo.getAnchorInfo().setId(0L);
            resultCloudVo.getVideoInfo().setId(0L);
            return analysisInfoToZipByteArray(JSON.toJSONString(resultCloudVo));
        }
        return null;
    }

    /**
     * 将分析内容转成zip byte数组
     * @param content 分析内容
     * @return
     */
    private byte[] analysisInfoToZipByteArray(String content) {


        try (ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(byteArrayOut)) {

            // 创建 ZIP 条目并写入内容
            ZipEntry entry = new ZipEntry("analysis.txt");
            zipOut.putNextEntry(entry);

            // 将字符串转换为字节并写入 ZIP 输出流
            byte[] inputBytes = content.getBytes("UTF-8");
            zipOut.write(inputBytes, 0, inputBytes.length);
            zipOut.closeEntry();

            // 确保所有数据都被写入输出流
            zipOut.finish(); // 这一步很重要，它会确保中央目录被写入
            zipOut.flush();  // 确保所有缓冲的数据都被写出

            // 返回包含 ZIP 文件的字节数组
            return byteArrayOut.toByteArray();
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取在线对比复盘分析信息，以zip形式返回
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    public byte[] getOnlineContrastAnalysisZip(String contrastId) throws Exception {

        R<OnlineContrastAnalysisInfoVo> onlineContrastAnalysisInfoR = this.getOnlineContrastAnalysisInfo(contrastId);
        if(onlineContrastAnalysisInfoR.getCode() == 0 && onlineContrastAnalysisInfoR.getData() != null) {
            OnlineContrastAnalysisInfoVo onlineContrastAnalysisInfo = onlineContrastAnalysisInfoR.getData();
            if(onlineContrastAnalysisInfo.getSentenceMark1() == null || onlineContrastAnalysisInfo.getSentenceMark1().getAnchorInfo() == null ||
                    onlineContrastAnalysisInfo.getSentenceMark1().getVideoInfo() == null) {
                return null;
            }
            if(onlineContrastAnalysisInfo.getSentenceMark2() == null || onlineContrastAnalysisInfo.getSentenceMark2().getAnchorInfo() == null ||
                    onlineContrastAnalysisInfo.getSentenceMark2().getVideoInfo() == null) {
                return null;
            }

            onlineContrastAnalysisInfo.getSentenceMark1().getAnchorInfo().setId(0L);
            onlineContrastAnalysisInfo.getSentenceMark1().getVideoInfo().setId(0L);
            onlineContrastAnalysisInfo.getSentenceMark2().getAnchorInfo().setId(0L);
            onlineContrastAnalysisInfo.getSentenceMark2().getVideoInfo().setId(0L);
            // 转成zip byte[]
            return analysisInfoToZipByteArray(JSON.toJSONString(onlineContrastAnalysisInfo));
        }

        return null;
    }

    /**
     * 获取在线对比复盘分析信息，以zip形式返回2_0
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    public byte[] getOnlineContrastAnalysisZip2_0(String contrastId) throws Exception {

        R<ContrastAnalysisResultCloudVo> r = this.getOnlineContrastAnalysisInfo2_0(contrastId);
        if(r.getCode() == 0 && r.getData() != null) {
            ContrastAnalysisResultCloudVo contrastAnalysisResultCloudVo = r.getData();
            if(contrastAnalysisResultCloudVo.getSentenceMark1() == null || contrastAnalysisResultCloudVo.getSentenceMark1().getAnchorInfo() == null ||
                    contrastAnalysisResultCloudVo.getSentenceMark1().getVideoInfo() == null) {
                return null;
            }
            if(contrastAnalysisResultCloudVo.getSentenceMark2() == null || contrastAnalysisResultCloudVo.getSentenceMark2().getAnchorInfo() == null ||
                    contrastAnalysisResultCloudVo.getSentenceMark2().getVideoInfo() == null) {
                return null;
            }

            contrastAnalysisResultCloudVo.getSentenceMark1().getAnchorInfo().setId(0L);
            contrastAnalysisResultCloudVo.getSentenceMark1().getVideoInfo().setId(0L);
            contrastAnalysisResultCloudVo.getSentenceMark2().getAnchorInfo().setId(0L);
            contrastAnalysisResultCloudVo.getSentenceMark2().getVideoInfo().setId(0L);
            // 转成zip byte[]
            return analysisInfoToZipByteArray(JSON.toJSONString(contrastAnalysisResultCloudVo));
        }

        return null;
    }

    /**
     * PC后端获取分析数据
     * @param fileId 文件ID
     * @param videoId 视频ID
     * @return
     */
    public R<OnlineAnalysisInfoVo> getAnalysisInfo(String fileId, String videoId) {
        OnlineAnalysisInfoVo onlineAnalysisInfoVo = new OnlineAnalysisInfoVo();
        if(!StringUtils.isEmpty(fileId)) {
            // 文件
            UploadFileInfoVo uploadFileInfoVo = this.uploadFileProducer.getByFileId(fileId);
            onlineAnalysisInfoVo.setUploadFile(uploadFileInfoVo);
            if(uploadFileInfoVo != null) {
                // 设置播放地址
                if(uploadFileInfoVo.getFileType() == 0 || uploadFileInfoVo.getFileType() == 1) {
                    onlineAnalysisInfoVo.setPlayUrl(uploadFileInfoVo.getPlayUrl());
                }

                // 设置词语段落分析数据
                String downloadUrl = this.getAnalysisDownloadUrl(1, uploadFileInfoVo.getFileId());
                String content = "";
                if(!StringUtils.isEmpty(downloadUrl)) {
                    content = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);
                }
                if(!StringUtils.isEmpty(content)) {
                    AnalysisResultVo analysisResultVo = JSONObject.parseObject(content, AnalysisResultVo.class);
                    List<SentenceMarkVo> sentenceMarkVos = analysisResultVo.getSentenceMarkVos();
                    if(sentenceMarkVos != null && sentenceMarkVos.size() > 0) {
                        List<OnlineAnalysisItemVo> onlineAnalysisItemVos = sentenceMarkVos.stream().map(item -> {
                            OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
                            onlineAnalysisItemVo.setFileUuid(uploadFileInfoVo.getFileId());
                            onlineAnalysisItemVo.setStatus(0);
                            onlineAnalysisItemVo.setDataJson(JSON.toJSONString(item));
                            onlineAnalysisItemVo.setTradeId(uploadFileInfoVo.getTradeId());
                            onlineAnalysisItemVo.setParagraph(item.getCurrentSort());
                            return onlineAnalysisItemVo;
                        }).sorted(Comparator.comparingInt(OnlineAnalysisItemVo::getParagraph)).collect(Collectors.toList());

                        onlineAnalysisInfoVo.setAnalysisList(onlineAnalysisItemVos);
                    }
                }

//                UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = this.uploadFileAnalysisRecordProducer.infoLastByFileIdAndTradeId(uploadFileInfoVo.getFileId(), uploadFileInfoVo.getTradeId());
//                if (uploadFileAnalysisRecordInfoVo != null){
//                    String dataJsonStr = ReplayFileUtils.getFileContent(wordsProperties.getFileAnalysisStorePath() + uploadFileAnalysisRecordInfoVo.getStoreFileName());
//                    if(!StringUtils.isEmpty(dataJsonStr)) {
//                        List<SentenceMarkVo> sentenceMarkVos = JSON.parseArray(dataJsonStr, SentenceMarkVo.class);
//
//                        List<OnlineAnalysisItemVo> onlineAnalysisItemVos = sentenceMarkVos.stream().map(item -> {
//                            OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
//                            onlineAnalysisItemVo.setFileUuid(uploadFileAnalysisRecordInfoVo.getFileId());
//                            onlineAnalysisItemVo.setStatus(0);
//                            onlineAnalysisItemVo.setDataJson(JSON.toJSONString(item));
//                            onlineAnalysisItemVo.setTradeId(uploadFileAnalysisRecordInfoVo.getTradeId());
//                            onlineAnalysisItemVo.setParagraph(item.getCurrentSort());
//                            return onlineAnalysisItemVo;
//                        }).sorted(Comparator.comparingInt(OnlineAnalysisItemVo::getParagraph)).collect(Collectors.toList());
//                        onlineAnalysisInfoVo.setAnalysisList(onlineAnalysisItemVos);
//                    }
//                }
            }
        }else {
            // 视频
            AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoProducer.getByVideoId(videoId);
            onlineAnalysisInfoVo.setVideoInfo(anchorVideoInfoVo);
            if(anchorVideoInfoVo != null) {
                // 获取主播
                AnchorUrlInfoVo anchorUrlInfoVo = this.anchorUrlProducer.infoBySecUid(anchorVideoInfoVo.getSecUid());
                onlineAnalysisInfoVo.setAnchorInfo(anchorUrlInfoVo);
                // 设置播放地址
                onlineAnalysisInfoVo.setPlayUrl(anchorVideoInfoVo.getPlayUrl());
                // 设置词语段落分析数据
                String downloadUrl = this.getAnalysisDownloadUrl(0, anchorVideoInfoVo.getVideoId());
                String content = "";
                if(!StringUtils.isEmpty(downloadUrl)) {
                    content = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);
                }
                if(!StringUtils.isEmpty(content)) {
                    AnalysisResultVo analysisResultVo = JSONObject.parseObject(content, AnalysisResultVo.class);
                    List<SentenceMarkVo> sentenceMarkVos = analysisResultVo.getSentenceMarkVos();
                    if(sentenceMarkVos != null && sentenceMarkVos.size() > 0) {
                        List<OnlineAnalysisItemVo> onlineAnalysisItemVos = sentenceMarkVos.stream().map(item -> {
                            OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
                            onlineAnalysisItemVo.setFileUuid(anchorVideoInfoVo.getVideoId());
                            onlineAnalysisItemVo.setStatus(0);
                            onlineAnalysisItemVo.setDataJson(JSON.toJSONString(item));
                            onlineAnalysisItemVo.setTradeId(anchorVideoInfoVo.getTradeId());
                            onlineAnalysisItemVo.setParagraph(item.getCurrentSort());
                            return onlineAnalysisItemVo;
                        }).sorted(Comparator.comparingInt(OnlineAnalysisItemVo::getParagraph)).collect(Collectors.toList());

                        onlineAnalysisInfoVo.setAnalysisList(onlineAnalysisItemVos);
                    }
                }

//                VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = this.videoAnalysisRecordProducer.infoLastByVideoIdAndTradeId(anchorVideoInfoVo.getVideoId(), anchorVideoInfoVo.getTradeId());
//                if(videoAnalysisRecordInfoVo != null) {
//                    String dataJsonStr = ReplayFileUtils.getFileContent(wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecordInfoVo.getStoreFileName());
//                    if(!StringUtils.isEmpty(dataJsonStr)) {
//                        List<SentenceMarkVo> sentenceMarkVos = JSON.parseArray(dataJsonStr, SentenceMarkVo.class);
//
//                        List<OnlineAnalysisItemVo> onlineAnalysisItemVos = sentenceMarkVos.stream().map(item -> {
//                            OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
//                            onlineAnalysisItemVo.setFileUuid(videoAnalysisRecordInfoVo.getVideoId());
//                            onlineAnalysisItemVo.setStatus(0);
//                            onlineAnalysisItemVo.setDataJson(JSON.toJSONString(item));
//                            onlineAnalysisItemVo.setTradeId(videoAnalysisRecordInfoVo.getTradeId());
//                            onlineAnalysisItemVo.setParagraph(item.getCurrentSort());
//                            return onlineAnalysisItemVo;
//                        }).sorted(Comparator.comparingInt(OnlineAnalysisItemVo::getParagraph)).collect(Collectors.toList());
//
//                        onlineAnalysisInfoVo.setAnalysisList(onlineAnalysisItemVos);
//                    }
//                }

//                List<OnlineAnalysisItemVo> analysisItemVoList = this.audioAnalysisProducer.listByVideoIdAndTradeId(anchorVideoInfoVo.getVideoId(), anchorVideoInfoVo.getTradeId());
//                onlineAnalysisInfoVo.setAnalysisList(analysisItemVoList);
                // 获取在线人数列表
//                List<OnlineNumInfoVo> onlineNumInfoVoList = onlineNumProducer.listByUserIdAndBatchNumber(anchorVideoInfoVo.getUserId(), anchorVideoInfoVo.getBatchNumber() + "");
                List<OnlineNumInfoVo> onlineNumInfoVoList = socketCollectMessageProducer.onlineNumByUserIdAndBatchNumber(anchorVideoInfoVo.getUserId(), anchorVideoInfoVo.getBatchNumber() + "", videoId);
                onlineAnalysisInfoVo.setOnlineNumList(onlineNumInfoVoList);
            }
        }

        return R.ok(onlineAnalysisInfoVo);
    }

    /**
     * 获取在线复盘分析信息2_0
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    public R<AnalysisResultCloudVo> getOnlineAnalysis2_0(Integer type, String uuid) {

        R<AnalysisResultClientCloudVo> analysisR = this.getOnlineAnalysisInfo2_0(type, uuid);
        if(analysisR != null && analysisR.getCode() == 0) {
            AnalysisResultClientCloudVo analysisRData = analysisR.getData();
            if(analysisRData != null) {

                AnalysisResultCloudVo data = packageOnlineAnalysis(analysisRData);

                if (type == 0 && ObjectUtil.isNotEmpty(data)) {
                    List<Map<String, Object>> hashMaps = tableStoreFeign.getBarrageDataList(uuid, data.getAudioaAlyses().stream().map(VideoParagraphAnalysisVoUpper::getDataJson).toList());
                    data.setBarrageDataList(hashMaps);

                    // 获取弹幕总数据量
                    data.setTotalBarrageNum(hashMaps.stream().mapToInt(map -> NumberUtil.parseInt(map.getOrDefault("barrageNum", "0").toString(), 0)).sum());

                    // 获取互动量
                    if (data.getInteractionPercent() == null && data.getTotalBarrageNum() != null && data.getTotalBarrageNum() > 0) {
                        data.setInteractionPercent(getInteractionPercent(data.getTotalBarrageNum(), uuid));
                    }

                    // 巨量分钟数据 enrich
                    enrichOceanEngineMinuteData(uuid, data);
                }

                return R.ok(data);
            }
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
    }

    /**
     * 获取互动量
     *
     * @param totalBarrageNum 弹幕数
     * @param videoId         视频id
     * @return 互动量
     */
    private Double getInteractionPercent(Integer totalBarrageNum, String videoId) {
        SocketCollectMessageInfoVo socketMessage = socketCollectMessageProducer.getByVideoId(videoId);
        if (socketMessage != null) {
            Integer observationNum = NumberUtil.parseInt(socketMessage.getObservationNum(), 0);
            if (observationNum > 0) {
                // 计算互动率
                double value = (double) totalBarrageNum / (double) observationNum * 100;
                // 精确4位小数，四舍五入
                BigDecimal bigDecimal = BigDecimal.valueOf(value)
                        .setScale(4, RoundingMode.HALF_UP);
                return bigDecimal.doubleValue();
            }
        }
        return null;
    }

    public R<AnalysisResultCloudVo> getAnalysis(Integer type, String uuid) {

        // 从oss获取分析数据
        String downloadUrl = this.getAnalysisDownloadUrl(type, uuid);
        if(!StringUtils.isEmpty(downloadUrl)) {
            String content = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);

            // 封装在线复盘信息
            if(!StringUtils.isEmpty(content)) {
                AnalysisResultClientCloudVo analysisResultCloudVo = packageAnalysisInfo(type, uuid, content);

                if(analysisResultCloudVo != null) {
                    return R.ok(packageOnlineAnalysis(analysisResultCloudVo));
                }
            }
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
    }

    private AnalysisResultClientCloudVo packageAnalysisInfo(Integer type, String uuid, String content) {
        if(type == 0) {
            // 视频
            AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoProducer.getByVideoId(uuid);

            AnalysisResultClientCloudVo analysisResultCloudVo = new AnalysisResultClientCloudVo();

            analysisResultCloudVo.setVideoInfo(anchorVideoInfoVo);
            // 获取主播
            AnchorUrlInfoVo anchorUrlInfoVo = this.anchorUrlProducer.infoBySecUid(anchorVideoInfoVo.getSecUid());
            analysisResultCloudVo.setAnchorInfo(anchorUrlInfoVo);
            // 设置行业信息
            if (anchorVideoInfoVo.getTradeId() != null) {
                TradeVo tradeInfoVo = this.tradeRse.getById(anchorVideoInfoVo.getTradeId());
                analysisResultCloudVo.setTradeInfo(tradeInfoVo);
            }
            // 设置播放地址
            analysisResultCloudVo.setPlayUrl(anchorVideoInfoVo.getPlayUrl());
            // 设置词语段落分析数据
            if(!StringUtils.isEmpty(content)) {
                AnalysisResultVo analysisResultVo = JSONObject.parseObject(content, AnalysisResultVo.class);
                analysisResultCloudVo.setWordsCollect(analysisResultVo.getWordsCollect());
                analysisResultCloudVo.setCruxTypeList(analysisResultVo.getCruxTypeList());
                analysisResultCloudVo.setWordsTabList(analysisResultVo.getWordsTabList());
                List<SentenceMarkVo> sentenceMarkVos = analysisResultVo.getSentenceMarkVos();
                if(sentenceMarkVos != null && sentenceMarkVos.size() > 0) {
                    List<OnlineAnalysisItemVo> onlineAnalysisItemVos = sentenceMarkVos.stream().map(item -> {
                        OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
                        onlineAnalysisItemVo.setFileUuid(uuid);
                        onlineAnalysisItemVo.setStatus(0);
                        onlineAnalysisItemVo.setDataJson(JSON.toJSONString(item));
                        onlineAnalysisItemVo.setTradeId(anchorVideoInfoVo.getTradeId());
                        onlineAnalysisItemVo.setParagraph(item.getCurrentSort());
                        return onlineAnalysisItemVo;
                    }).sorted(Comparator.comparingInt(OnlineAnalysisItemVo::getParagraph)).collect(Collectors.toList());

                    analysisResultCloudVo.setAnalysisList(onlineAnalysisItemVos);
                }
            }
            // 获取在线人数列表
            List<OnlineNumInfoVo> onlineNumInfoVoList = socketCollectMessageProducer.onlineNumByUserIdAndBatchNumber(anchorVideoInfoVo.getUserId(), anchorVideoInfoVo.getBatchNumber() + "", uuid);

            if (onlineNumInfoVoList == null) onlineNumInfoVoList = new ArrayList<>();
            if (onlineNumInfoVoList.isEmpty()){
                OnlineNumInfoVo e = new OnlineNumInfoVo();
                e.setRecordDate(DateUtil.format(anchorVideoInfoVo.getStartTime(), "yyyy-MM-dd HH:mm:ss"));
                e.setPeopleNum("0");
                onlineNumInfoVoList.add(e);
            }
            analysisResultCloudVo.setOnlineNumList(onlineNumInfoVoList);

            return analysisResultCloudVo;
        }else if (type == 1){
            // 文件
            // 视频
            UploadFileInfoVo fileInfoVo = this.uploadFileProducer.getByFileId(uuid);

            AnalysisResultClientCloudVo analysisResultCloudVo = new AnalysisResultClientCloudVo();

            analysisResultCloudVo.setUploadFile(fileInfoVo);
            // 获取主播
//            AnchorUrlInfoVo anchorUrlInfoVo = this.anchorUrlProducer.infoBySecUid(fileInfoVo.getSecUid());
//            analysisResultCloudVo.setAnchorInfo(anchorUrlInfoVo);
            // 设置播放地址
            analysisResultCloudVo.setPlayUrl(null);
            // 设置词语段落分析数据
            if(!StringUtils.isEmpty(content)) {
                AnalysisResultVo analysisResultVo = JSONObject.parseObject(content, AnalysisResultVo.class);
                analysisResultCloudVo.setWordsCollect(analysisResultVo.getWordsCollect());
                analysisResultCloudVo.setCruxTypeList(analysisResultVo.getCruxTypeList());
                analysisResultCloudVo.setWordsTabList(analysisResultVo.getWordsTabList());
                List<SentenceMarkVo> sentenceMarkVos = analysisResultVo.getSentenceMarkVos();
                if(sentenceMarkVos != null && sentenceMarkVos.size() > 0) {
                    List<OnlineAnalysisItemVo> onlineAnalysisItemVos = sentenceMarkVos.stream().map(item -> {
                        OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
                        onlineAnalysisItemVo.setFileUuid(uuid);
                        onlineAnalysisItemVo.setStatus(0);
                        onlineAnalysisItemVo.setDataJson(JSON.toJSONString(item));
                        onlineAnalysisItemVo.setTradeId(fileInfoVo.getTradeId());
                        onlineAnalysisItemVo.setParagraph(item.getCurrentSort());
                        return onlineAnalysisItemVo;
                    }).sorted(Comparator.comparingInt(OnlineAnalysisItemVo::getParagraph)).collect(Collectors.toList());

                    analysisResultCloudVo.setAnalysisList(onlineAnalysisItemVos);
                }
            }
            // 获取在线人数列表
//            List<OnlineNumInfoVo> onlineNumInfoVoList = socketCollectMessageProducer.onlineNumByUserIdAndBatchNumber(fileInfoVo.getUserId(), fileInfoVo.getBatchNumber() + "", uuid);

//            if (onlineNumInfoVoList == null) onlineNumInfoVoList = new ArrayList<>();
//            if (onlineNumInfoVoList.isEmpty()){
//                OnlineNumInfoVo e = new OnlineNumInfoVo();
//                e.setRecordDate(DateUtil.format(fileInfoVo.getStartTime(), "yyyy-MM-dd HH:mm:ss"));
//                e.setPeopleNum("0");
//                onlineNumInfoVoList.add(e);
//            }
//            analysisResultCloudVo.setOnlineNumList(onlineNumInfoVoList);

            return analysisResultCloudVo;
        }
        return null;
    }

    /**
     * 在线复盘分析信息
     * @param analysisRData 原分析数据
     * @return
     */
    private AnalysisResultCloudVo packageOnlineAnalysis(AnalysisResultClientCloudVo analysisRData) {
        AnalysisResultCloudVo analysisResultCloudVo = new AnalysisResultCloudVo();
        BeanUtils.copyProperties(analysisRData, analysisResultCloudVo);

        // 类型不一样，需要手动拷贝
        AnchorUrlInfoVo anchorInfo = analysisRData.getAnchorInfo();
        if(anchorInfo != null) {
            AnchorUrlInfoVoUpper anchorUrlInfoVoUpper = new AnchorUrlInfoVoUpper();
            BeanUtils.copyProperties(anchorInfo, anchorUrlInfoVoUpper);
            analysisResultCloudVo.setAnchorInfo(anchorUrlInfoVoUpper);
        }
        // 视频
        AnchorVideoInfoVo videoInfo = analysisRData.getVideoInfo();
        if(videoInfo != null) {
            AnchorVideoInfoVoUpper anchorVideoInfoVoUpper = new AnchorVideoInfoVoUpper();
            BeanUtils.copyProperties(videoInfo, anchorVideoInfoVoUpper);
            analysisResultCloudVo.setVideoInfo(anchorVideoInfoVoUpper);
        }
        // 文件
        UploadFileInfoVo uploadFile = analysisRData.getUploadFile();
        if (uploadFile != null){
            analysisResultCloudVo.setUploadFile(uploadFile);
        }

        // 在线人数
        List<OnlineNumInfoVo> onlineNumList = analysisRData.getOnlineNumList();
        if(onlineNumList != null && onlineNumList.size() > 0) {
            List<OnlineNumVoUpper> onlineNumVoUppers = onlineNumList.stream().map(item -> {
                OnlineNumVoUpper onlineNumVoUpper = new OnlineNumVoUpper();
                BeanUtils.copyProperties(item, onlineNumVoUpper);
                return onlineNumVoUpper;
            }).toList();
            analysisResultCloudVo.setOnlineNumList(onlineNumVoUppers);
        }

        // 封装段落信息
        if (analysisRData.getVideoInfo() != null){
            List<OnlineAnalysisItemVo> analysisList = analysisRData.getAnalysisList();
            if(analysisList != null && !analysisList.isEmpty()) {
                List<VideoParagraphAnalysisVoUpper> videoParagraphAnalysisVoUppers = analysisList.stream().map(item -> {
                    VideoParagraphAnalysisVoUpper videoParagraphAnalysisVoUpper = new VideoParagraphAnalysisVoUpper();
                    BeanUtils.copyProperties(item, videoParagraphAnalysisVoUpper);
                    videoParagraphAnalysisVoUpper.setVideoId(item.getFileUuid());
                    return videoParagraphAnalysisVoUpper;
                }).toList();
                analysisResultCloudVo.setAudioaAlyses(videoParagraphAnalysisVoUppers);
            }
        }
        if (analysisRData.getUploadFile() != null){
            List<OnlineAnalysisItemVo> analysisList = analysisRData.getAnalysisList();
            if(analysisList != null && !analysisList.isEmpty()) {
                List<FileParagraphAnalysisVo> videoParagraphAnalysisVoUppers = analysisList.stream().map(item -> {
                    FileParagraphAnalysisVo videoParagraphAnalysisVoUpper = new FileParagraphAnalysisVo();
                    BeanUtils.copyProperties(item, videoParagraphAnalysisVoUpper);
                    videoParagraphAnalysisVoUpper.setFileId(item.getFileUuid());
                    return videoParagraphAnalysisVoUpper;
                }).toList();
                analysisResultCloudVo.setFileAudioaAlyses(videoParagraphAnalysisVoUppers);
            }
        }

        return analysisResultCloudVo;
    }

    /**
     * 获取在线对比复盘分析信息2_0
     * @param contrastId 对比id
     * @return
     */
    public R<AnalysisContractResultCloudVo> getOnlineContrastAnalysis2_0(String contrastId) throws Exception {
        // 获取对比信息
        SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.infoByContrastId(contrastId);
        if(syncContrastInfoVo != null && !StringUtils.isEmpty(syncContrastInfoVo.getVideoOneId()) && !StringUtils.isEmpty(syncContrastInfoVo.getVideoTwoId())) {
            AnalysisContractResultCloudVo analysisContractResultCloudVo = new AnalysisContractResultCloudVo();
            SyncContrastInfoVoUpper syncContrastInfoVoUpper = new SyncContrastInfoVoUpper();
            BeanUtils.copyProperties(syncContrastInfoVo, syncContrastInfoVoUpper);
            analysisContractResultCloudVo.setVideoContrast(syncContrastInfoVoUpper);

            // 设置视频1信息
            R<AnalysisResultCloudVo> oneVideoR = this.getOnlineAnalysis2_0(0, syncContrastInfoVo.getVideoOneId());
            if(oneVideoR.getCode() == 0 && oneVideoR.getData() != null) {
                analysisContractResultCloudVo.setSentenceMark1(oneVideoR.getData());
            }
            // 设置视频2信息
            R<AnalysisResultCloudVo> twoVideoR = this.getOnlineAnalysis2_0(0, syncContrastInfoVo.getVideoTwoId());
            if(twoVideoR.getCode() == 0 && twoVideoR.getData() != null) {
                analysisContractResultCloudVo.setSentenceMark2(twoVideoR.getData());
            }

            return R.ok(analysisContractResultCloudVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
    }

    public R<AnalysisContractResultCloudVo> getContrastAnalysis(String contrastId) {
        // 获取对比信息
        SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.infoByContrastId(contrastId);
        if(syncContrastInfoVo != null) {
            AnalysisContractResultCloudVo analysisContractResultCloudVo = new AnalysisContractResultCloudVo();
            SyncContrastInfoVoUpper syncContrastInfoVoUpper = new SyncContrastInfoVoUpper();
            BeanUtils.copyProperties(syncContrastInfoVo, syncContrastInfoVoUpper);
            analysisContractResultCloudVo.setVideoContrast(syncContrastInfoVoUpper);

            // 设置视频1信息
            if (ObjectUtil.isNotEmpty(syncContrastInfoVo.getVideoOneId())){
                R<AnalysisResultCloudVo> oneVideoR = this.getAnalysis(0, syncContrastInfoVo.getVideoOneId());
                if(oneVideoR.getCode() == 0 && oneVideoR.getData() != null) {
                    analysisContractResultCloudVo.setSentenceMark1(oneVideoR.getData());
                }
            }
            // 设置视频2信息
            if (ObjectUtil.isNotEmpty(syncContrastInfoVo.getVideoTwoId())){
                R<AnalysisResultCloudVo> twoVideoR = this.getAnalysis(0, syncContrastInfoVo.getVideoTwoId());
                if(twoVideoR.getCode() == 0 && twoVideoR.getData() != null) {
                    analysisContractResultCloudVo.setSentenceMark2(twoVideoR.getData());
                }
            }

            // 获取文件1信息
            if (ObjectUtil.isNotEmpty(syncContrastInfoVo.getFileOneId())){
                R<AnalysisResultCloudVo> twoVideoR = this.getAnalysis(1, syncContrastInfoVo.getFileOneId());
                if(twoVideoR.getCode() == 0 && twoVideoR.getData() != null) {
                    analysisContractResultCloudVo.setSentenceMark1(twoVideoR.getData());
                }
            }

            // 获取文件2信息
            if (ObjectUtil.isNotEmpty(syncContrastInfoVo.getFileTwoId())){
                R<AnalysisResultCloudVo> twoVideoR = this.getAnalysis(1, syncContrastInfoVo.getFileTwoId());
                if(twoVideoR.getCode() == 0 && twoVideoR.getData() != null) {
                    analysisContractResultCloudVo.setSentenceMark2(twoVideoR.getData());
                }
            }


            return R.ok(analysisContractResultCloudVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
    }

    /**
     * 获取分析文件的下载链接地址
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    public R<String> getAnalysisDownloadUrlR(Integer type, String uuid) {

        // 获取下载链接地址
        String downloadUrl = getAnalysisDownloadUrl(type, uuid);

        if(!StringUtils.isEmpty(downloadUrl)) {
            return R.ok("获取成功", downloadUrl);
        }else {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "获取下载链接失败");
        }
    }

    /**
     * 获取分析文件的byte数组
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    public byte[] getAnalysisByteByOss(Integer type, String uuid) {
        if(type == 0) {
            // 视频
            AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoProducer.getByVideoId(uuid);
            if(anchorVideoInfoVo != null) {
                // 获取最后一条分析数据
                VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = this.videoAnalysisRecordProducer.infoLastByVideoIdAndTradeId(anchorVideoInfoVo.getVideoId(), anchorVideoInfoVo.getTradeId());
                if(videoAnalysisRecordInfoVo != null) {
                    // 获取文件的byte数组
                    return ossUtils.getObject("replay-analysis-data", videoAnalysisRecordInfoVo.getStoreFileOssKey());
                }
            }
        }else {
            // 文件
            UploadFileInfoVo uploadFileInfoVo = this.uploadFileProducer.getByFileId(uuid);
            if(uploadFileInfoVo != null) {
                UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = this.uploadFileAnalysisRecordProducer.infoLastByFileIdAndTradeId(uploadFileInfoVo.getFileId(), uploadFileInfoVo.getTradeId());
                if(uploadFileAnalysisRecordInfoVo != null) {
                    // 获取文件的byte数组
                    return ossUtils.getObject("replay-analysis-data", uploadFileAnalysisRecordInfoVo.getStoreFileOssKey());
                }
            }
        }

        return null;
    }

    /**
     * 获取分析文件的下载链接地址
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    public String getAnalysisDownloadUrl(Integer type, String uuid) {
        String downloadUrl = "";

        if(type == 0) {
            // 视频
            AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoProducer.getByVideoId(uuid);
            if(anchorVideoInfoVo != null) {
                // 获取最后一条分析数据
                VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = this.videoAnalysisRecordProducer.infoLastByVideoIdAndTradeId(anchorVideoInfoVo.getVideoId(), anchorVideoInfoVo.getTradeId());
                if(videoAnalysisRecordInfoVo != null && !StringUtils.isEmpty(videoAnalysisRecordInfoVo.getStoreFileOssKey())) {
                    // 获取下载链接地址
                    downloadUrl = ossUtils.getSignDownloadUrl("replay-analysis-data", videoAnalysisRecordInfoVo.getStoreFileOssKey(), true);
                }
            }
        }else {
            // 文件
            UploadFileInfoVo uploadFileInfoVo = this.uploadFileProducer.getByFileId(uuid);
            if(uploadFileInfoVo != null) {
                UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = this.uploadFileAnalysisRecordProducer.infoLastByFileIdAndTradeId(uploadFileInfoVo.getFileId(), uploadFileInfoVo.getTradeId());
                if(uploadFileAnalysisRecordInfoVo != null && !StringUtils.isEmpty(uploadFileAnalysisRecordInfoVo.getStoreFileOssKey())) {
                    // 获取下载链接地址
                    downloadUrl = ossUtils.getSignDownloadUrl("replay-analysis-data", uploadFileAnalysisRecordInfoVo.getStoreFileOssKey(), true);
                }
            }
        }

//        if(type == 0) {
//            // 视频
//            AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoProducer.getByVideoId(uuid);
//            if(anchorVideoInfoVo != null) {
//                // 获取最后一条分析数据
//                VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = this.videoAnalysisRecordProducer.infoLastByVideoIdAndTradeId(anchorVideoInfoVo.getVideoId(), anchorVideoInfoVo.getTradeId());
//                if(videoAnalysisRecordInfoVo != null) {
//                    if(StringUtils.isEmpty(videoAnalysisRecordInfoVo.getStoreFileNameNew())) {
//                        // 当前数据是2.0之前的数据，转成2.0的
//                        String storeFileNameNew = convertNewAnalysis(
//                                wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecordInfoVo.getStoreFileName(),
//                                videoAnalysisRecordInfoVo.getVideoId(),
//                                0,
//                                videoAnalysisRecordInfoVo.getTradeId(),
//                                videoAnalysisRecordInfoVo.getVersion(),
//                                videoAnalysisRecordInfoVo.getId());
//
//                        // 将新路径保存到数据库
//                        videoAnalysisRecordInfoVo.setStoreFileNameNew(storeFileNameNew);
//                        this.videoAnalysisRecordProducer.saveNewFilePath(videoAnalysisRecordInfoVo.getId(), storeFileNameNew);
//                    }
//
//                    if(StringUtils.isEmpty(videoAnalysisRecordInfoVo.getStoreFileOssKey())) {
//                        // 当前数据在cos上没有，上传到cos
//                        String cosSaveKey = uploadLocalAnalysisFileToCos(
//                                wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecordInfoVo.getStoreFileNameNew(),
//                                videoAnalysisRecordInfoVo.getVideoId(),
//                                0,
//                                videoAnalysisRecordInfoVo.getTradeId(),
//                                videoAnalysisRecordInfoVo.getVersion(),
//                                videoAnalysisRecordInfoVo.getId()
//                        );
//
//                        // 将cosKey保存到数据库
//                        if(!StringUtils.isEmpty(cosSaveKey)) {
//                            videoAnalysisRecordInfoVo.setStoreFileOssKey(cosSaveKey);
//                            this.videoAnalysisRecordProducer.saveCosKey(videoAnalysisRecordInfoVo.getId(), cosSaveKey);
//                        }
//                    }
//
//                    // 获取下载链接地址
//                    TencentCosTokenVo tencentCosTokenVo = TencentCosUtils.privateCosUploadTempToken();
//                    if(tencentCosTokenVo != null) {
//                        downloadUrl = TencentCosUtils.getDownloadUrl(tencentCosTokenVo, videoAnalysisRecordInfoVo.getStoreFileOssKey());
//                    }
//
//                }
//            }
//        }else {
//            // 文件
//            UploadFileInfoVo uploadFileInfoVo = this.uploadFileProducer.getByFileId(uuid);
//            if(uploadFileInfoVo != null) {
//                UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = this.uploadFileAnalysisRecordProducer.infoLastByFileIdAndTradeId(uploadFileInfoVo.getFileId(), uploadFileInfoVo.getTradeId());
//                if(uploadFileAnalysisRecordInfoVo != null) {
//                    if(StringUtils.isEmpty(uploadFileAnalysisRecordInfoVo.getStoreFileNameNew())) {
//                        // 当前数据是2.0之前的数据，转成新的
//                        String storeFileNameNew = convertNewAnalysis(
//                                wordsProperties.getFileAnalysisStorePath() + uploadFileAnalysisRecordInfoVo.getStoreFileName(),
//                                uploadFileAnalysisRecordInfoVo.getFileId(),
//                                1,
//                                uploadFileAnalysisRecordInfoVo.getTradeId(),
//                                uploadFileAnalysisRecordInfoVo.getVersion(),
//                                uploadFileAnalysisRecordInfoVo.getId());
//
//                        // 将新路径保存到数据库
//                        uploadFileAnalysisRecordInfoVo.setStoreFileNameNew(storeFileNameNew);
//                        this.uploadFileAnalysisRecordProducer.saveNewFilePath(uploadFileAnalysisRecordInfoVo.getId(), storeFileNameNew);
//                    }
//
//                    if(StringUtils.isEmpty(uploadFileAnalysisRecordInfoVo.getStoreFileOssKey())) {
//                        // 当前数据在cos上没有，上传到cos
//                        String cosSaveKey = uploadLocalAnalysisFileToCos(
//                                wordsProperties.getVideoAnalysisStorePath() + uploadFileAnalysisRecordInfoVo.getStoreFileNameNew(),
//                                uploadFileAnalysisRecordInfoVo.getFileId(),
//                                1,
//                                uploadFileAnalysisRecordInfoVo.getTradeId(),
//                                uploadFileAnalysisRecordInfoVo.getVersion(),
//                                uploadFileAnalysisRecordInfoVo.getId()
//                        );
//
//                        // 将cosKey保存到数据库
//                        if(!StringUtils.isEmpty(cosSaveKey)) {
//                            uploadFileAnalysisRecordInfoVo.setStoreFileOssKey(cosSaveKey);
//                            this.uploadFileAnalysisRecordProducer.saveCosKey(uploadFileAnalysisRecordInfoVo.getId(), cosSaveKey);
//                        }
//                    }
//
//                    // 获取下载链接地址
//                    TencentCosTokenVo tencentCosTokenVo = TencentCosUtils.privateCosUploadTempToken();
//                    if(tencentCosTokenVo != null) {
//                        downloadUrl = TencentCosUtils.getDownloadUrl(tencentCosTokenVo, uploadFileAnalysisRecordInfoVo.getStoreFileOssKey());
//                    }
//                }
//            }
//        }

        return downloadUrl;

    }

    private String uploadLocalAnalysisFileToCos(String localFilePath, String uuid, int type, Long tradeId, int versionNum, Long recordId) {

        String videoOrFile = type == 0 ? "video" : "file";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fileName = versionNum + "th-" + tradeId + "-" + recordId + ".zip";
        String cosSaveKey = "analysis/" + videoOrFile + "/" + sdf.format(new Date()) + "/" + uuid + "/20/" + fileName;
        Boolean isSuccess = TencentCosUtils.putLocalFileObject(TencentCosUtils.getPrivateCosBucketName(), localFilePath, cosSaveKey);
        if (isSuccess) {
            return cosSaveKey;
        }

        return "";
    }

    /**
     * 获取在线复盘分析信息，以zip形式返回2_1
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    public byte[] getOnlineAnalysisZip2_1(Integer type, String uuid) throws Exception {

        R<AnalysisResultClientCloudVo> r = this.getOnlineAnalysisInfo2_1(type, uuid);
        if(r.getCode() == 0 && r.getData() != null) {
            AnalysisResultClientCloudVo resultCloudVo = r.getData();
            resultCloudVo.getAnchorInfo().setId(0L);
            resultCloudVo.getVideoInfo().setId(0L);
            return analysisInfoToZipByteArray(JSON.toJSONString(resultCloudVo));
        }
        return null;
    }

    /**
     * 获取在线复盘分析信息2_1
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    private R<AnalysisResultClientCloudVo> getOnlineAnalysisInfo2_1(Integer type, String uuid) throws Exception {
        // 从oss获取分析数据
        String content = "";
        String downloadUrl = this.getAnalysisDownloadUrl(type, uuid);
        if(!StringUtils.isEmpty(downloadUrl)) {
            content = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);
        }

        // 封装在线复盘信息
        AnalysisResultClientCloudVo analysisResultCloudVo = packageOnlineAnalysisInfo2_0(type, uuid, content);

        if(analysisResultCloudVo != null) {
            return R.ok(analysisResultCloudVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据内容不存在");
    }

    /**
     * 获取在线对比复盘分析信息，以zip形式返回2_1
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    public byte[] getOnlineContrastAnalysisZip2_1(String contrastId) throws Exception {

        R<ContrastAnalysisResultCloudVo> r = this.getOnlineContrastAnalysisInfo2_1(contrastId);

        if(r.getCode() == 0 && r.getData() != null) {
            ContrastAnalysisResultCloudVo contrastAnalysisResultCloudVo = r.getData();
            if(contrastAnalysisResultCloudVo.getSentenceMark1() == null || contrastAnalysisResultCloudVo.getSentenceMark1().getAnchorInfo() == null ||
                    contrastAnalysisResultCloudVo.getSentenceMark1().getVideoInfo() == null) {
                return null;
            }
            if(contrastAnalysisResultCloudVo.getSentenceMark2() == null || contrastAnalysisResultCloudVo.getSentenceMark2().getAnchorInfo() == null ||
                    contrastAnalysisResultCloudVo.getSentenceMark2().getVideoInfo() == null) {
                return null;
            }

            contrastAnalysisResultCloudVo.getSentenceMark1().getAnchorInfo().setId(0L);
            contrastAnalysisResultCloudVo.getSentenceMark1().getVideoInfo().setId(0L);
            contrastAnalysisResultCloudVo.getSentenceMark2().getAnchorInfo().setId(0L);
            contrastAnalysisResultCloudVo.getSentenceMark2().getVideoInfo().setId(0L);
            // 转成zip byte[]
            return analysisInfoToZipByteArray(JSON.toJSONString(contrastAnalysisResultCloudVo));
        }

        return null;
    }

    /**
     * 获取在线复盘对比分析信息2_0
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    private R<ContrastAnalysisResultCloudVo> getOnlineContrastAnalysisInfo2_1(String contrastId) throws Exception {

        ContrastAnalysisResultCloudVo contrastAnalysisResultCloudVo = new ContrastAnalysisResultCloudVo();
        // 获取对比
        SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.infoByContrastId(contrastId);
        if(syncContrastInfoVo != null && !StringUtils.isEmpty(syncContrastInfoVo.getShareUrl())) {
            R<AnalysisResultClientCloudVo> onlineAnalysisInfo1 = this.getOnlineAnalysisInfo2_1(0, syncContrastInfoVo.getVideoOneId());
            contrastAnalysisResultCloudVo.setSentenceMark1(onlineAnalysisInfo1.getData());

            R<AnalysisResultClientCloudVo> onlineAnalysisInfo2 = this.getOnlineAnalysisInfo2_1(0, syncContrastInfoVo.getVideoTwoId());
            contrastAnalysisResultCloudVo.setSentenceMark2(onlineAnalysisInfo2.getData());
        }

        return R.ok(contrastAnalysisResultCloudVo);
    }

    /**
     * 获取在线复盘分析信息2_1
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    public R<AnalysisResultCloudVo> getOnlineAnalysis2_1(Integer type, String uuid) throws Exception {

        R<AnalysisResultClientCloudVo> analysisR = this.getOnlineAnalysisInfo2_1(type, uuid);
        if(analysisR != null && analysisR.getCode() == 0) {
            AnalysisResultClientCloudVo analysisRData = analysisR.getData();
            if(analysisRData != null) {

                return R.ok(packageOnlineAnalysis(analysisRData));
            }
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
    }

    /**
     * 获取在线对比复盘分析信息2_1
     * @param contrastId 对比id
     * @return
     */
    public R<AnalysisContractResultCloudVo> getOnlineContrastAnalysis2_1(String contrastId) throws Exception {
        // 获取对比信息
        SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.infoByContrastId(contrastId);
        if(syncContrastInfoVo != null && !StringUtils.isEmpty(syncContrastInfoVo.getVideoOneId()) && !StringUtils.isEmpty(syncContrastInfoVo.getVideoTwoId())) {
            AnalysisContractResultCloudVo analysisContractResultCloudVo = new AnalysisContractResultCloudVo();
            SyncContrastInfoVoUpper syncContrastInfoVoUpper = new SyncContrastInfoVoUpper();
            BeanUtils.copyProperties(syncContrastInfoVo, syncContrastInfoVoUpper);
            analysisContractResultCloudVo.setVideoContrast(syncContrastInfoVoUpper);

            // 设置视频1信息
            R<AnalysisResultCloudVo> oneVideoR = this.getOnlineAnalysis2_1(0, syncContrastInfoVo.getVideoOneId());
            if(oneVideoR.getCode() == 0 && oneVideoR.getData() != null) {
                analysisContractResultCloudVo.setSentenceMark1(oneVideoR.getData());
            }
            // 设置视频2信息
            R<AnalysisResultCloudVo> twoVideoR = this.getOnlineAnalysis2_1(0, syncContrastInfoVo.getVideoTwoId());
            if(twoVideoR.getCode() == 0 && twoVideoR.getData() != null) {
                analysisContractResultCloudVo.setSentenceMark2(twoVideoR.getData());
            }

            return R.ok(analysisContractResultCloudVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
    }

    /**
     * 根据AI分析出来的关键词内容，去查询关键词是否存在于词库中
     * @param aiContentList Ai分析出来的内容
     * @return
     */
    public List<String> aiAnswerAndSensitiveList(List<String> aiContentList) {
        return sensitiveWordsProducer.aiAnswerAndSensitiveList(aiContentList);
    }

    /**
     * PC后端获取对比分析数据
     * @param contrastId 对比分析唯一标识
     * @return
     */
    public R<OnlineContrastAnalysisInfoVo> getContrastAnalysisInfo(String contrastId) {
        OnlineContrastAnalysisInfoVo onlineContrastAnalysisInfoVo = new OnlineContrastAnalysisInfoVo();
        // 获取对比
        SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.infoByContrastId(contrastId);
        if(syncContrastInfoVo != null) {
            R<OnlineAnalysisInfoVo> onlineAnalysisInfo1 = this.getContrastAnalysisInfo(syncContrastInfoVo.getFileOneId(), syncContrastInfoVo.getVideoOneId());
            onlineContrastAnalysisInfoVo.setSentenceMark1(onlineAnalysisInfo1.getData());

            R<OnlineAnalysisInfoVo> onlineAnalysisInfo2 = this.getContrastAnalysisInfo(syncContrastInfoVo.getFileTwoId(), syncContrastInfoVo.getVideoTwoId());
            onlineContrastAnalysisInfoVo.setSentenceMark2(onlineAnalysisInfo2.getData());
        }
        return R.ok(onlineContrastAnalysisInfoVo);
    }

    /**
     * 获取在线复盘分析信息（对比分析）
     * @param fileId 文件唯一标识 uuid，传其中一个
     * @param videoId 视频唯一标识 uuid，传其中一个
     * @return
     */
    public R<OnlineAnalysisInfoVo> getContrastAnalysisInfo(String fileId, String videoId) {

        OnlineAnalysisInfoVo onlineAnalysisInfoVo = new OnlineAnalysisInfoVo();
        if(!StringUtils.isEmpty(fileId)) {
            // 文件
            UploadFileInfoVo uploadFileInfoVo = this.uploadFileProducer.getByFileId(fileId);
            onlineAnalysisInfoVo.setUploadFile(uploadFileInfoVo);
            if(uploadFileInfoVo != null) {
                // 设置播放地址
                if(uploadFileInfoVo.getFileType() == 0 || uploadFileInfoVo.getFileType() == 1) {
                    onlineAnalysisInfoVo.setPlayUrl(uploadFileInfoVo.getPlayUrl());
                }
                // 设置词语段落分析数据
                String downloadUrl = this.getAnalysisDownloadUrl(1, uploadFileInfoVo.getFileId());
                String content = "";
                if(!StringUtils.isEmpty(downloadUrl)) {
                    content = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);
                }
                if(!StringUtils.isEmpty(content)) {
                    AnalysisResultVo analysisResultVo = JSONObject.parseObject(content, AnalysisResultVo.class);
                    List<SentenceMarkVo> sentenceMarkVos = analysisResultVo.getSentenceMarkVos();
                    if(sentenceMarkVos != null && sentenceMarkVos.size() > 0) {
                        List<OnlineAnalysisItemVo> onlineAnalysisItemVos = sentenceMarkVos.stream().map(item -> {
                            OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
                            onlineAnalysisItemVo.setFileUuid(uploadFileInfoVo.getFileId());
                            onlineAnalysisItemVo.setStatus(0);
                            onlineAnalysisItemVo.setDataJson(JSON.toJSONString(item));
                            onlineAnalysisItemVo.setTradeId(uploadFileInfoVo.getTradeId());
                            onlineAnalysisItemVo.setParagraph(item.getCurrentSort());
                            return onlineAnalysisItemVo;
                        }).sorted(Comparator.comparingInt(OnlineAnalysisItemVo::getParagraph)).collect(Collectors.toList());

                        onlineAnalysisInfoVo.setAnalysisList(onlineAnalysisItemVos);
                    }
                }

//                List<OnlineAnalysisItemVo> analysisItemVoList = this.uploadFileAnalysisProducer.listByFileIdAndTradeId(uploadFileInfoVo.getFileId(), uploadFileInfoVo.getTradeId());
//                UploadFileAnalysisRecordInfoVo analysisItemInfoVo = this.uploadFileAnalysisRecordProducer.listByFileIdAndTradeId(uploadFileInfoVo.getFileId(), uploadFileInfoVo.getTradeId());
//                if(analysisItemInfoVo != null) {
//                    String dataJsonStr = ReplayFileUtils.getFileContent(wordsProperties.getFileAnalysisStorePath() + analysisItemInfoVo.getStoreFileName());
//                    if(!StringUtils.isEmpty(dataJsonStr)) {
//                        List<SentenceMarkVo> sentenceMarkVos = JSON.parseArray(dataJsonStr, SentenceMarkVo.class);
//                        List<OnlineAnalysisItemVo> onlineAnalysisItemVos = sentenceMarkVos.stream().map(item -> {
//                            OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
//                            onlineAnalysisItemVo.setFileUuid(analysisItemInfoVo.getFileId());
//                            onlineAnalysisItemVo.setStatus(0);
//                            onlineAnalysisItemVo.setDataJson(JSON.toJSONString(item));
//                            onlineAnalysisItemVo.setTradeId(analysisItemInfoVo.getTradeId());
//                            onlineAnalysisItemVo.setParagraph(item.getCurrentSort());
//                            return onlineAnalysisItemVo;
//                        }).sorted(Comparator.comparingInt(OnlineAnalysisItemVo::getParagraph)).collect(Collectors.toList());
//
//                        onlineAnalysisInfoVo.setAnalysisList(onlineAnalysisItemVos);
//                    }
//                }
            }
        }else {
            // 视频
            AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoProducer.getByVideoId(videoId);
            onlineAnalysisInfoVo.setVideoInfo(anchorVideoInfoVo);
            if(anchorVideoInfoVo != null) {
                // 获取主播
                AnchorUrlInfoVo anchorUrlInfoVo = this.anchorUrlProducer.infoBySecUid(anchorVideoInfoVo.getSecUid());
                onlineAnalysisInfoVo.setAnchorInfo(anchorUrlInfoVo);
                // 设置播放地址
                onlineAnalysisInfoVo.setPlayUrl(anchorVideoInfoVo.getPlayUrl());
                // 设置词语段落分析数据
                String downloadUrl = this.getAnalysisDownloadUrl(0, anchorVideoInfoVo.getVideoId());
                String content = "";
                if(!StringUtils.isEmpty(downloadUrl)) {
                    content = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);
                }
                if(!StringUtils.isEmpty(content)) {
                    AnalysisResultVo analysisResultVo = JSONObject.parseObject(content, AnalysisResultVo.class);
                    List<SentenceMarkVo> sentenceMarkVos = analysisResultVo.getSentenceMarkVos();
                    if(sentenceMarkVos != null && sentenceMarkVos.size() > 0) {
                        List<OnlineAnalysisItemVo> onlineAnalysisItemVos = sentenceMarkVos.stream().map(item -> {
                            OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
                            onlineAnalysisItemVo.setFileUuid(anchorVideoInfoVo.getVideoId());
                            onlineAnalysisItemVo.setStatus(0);
                            onlineAnalysisItemVo.setDataJson(JSON.toJSONString(item));
                            onlineAnalysisItemVo.setTradeId(anchorVideoInfoVo.getTradeId());
                            onlineAnalysisItemVo.setParagraph(item.getCurrentSort());
                            return onlineAnalysisItemVo;
                        }).sorted(Comparator.comparingInt(OnlineAnalysisItemVo::getParagraph)).collect(Collectors.toList());

                        onlineAnalysisInfoVo.setAnalysisList(onlineAnalysisItemVos);
                    }
                }

//                VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = this.videoAnalysisRecordProducer.infoLastByVideoIdAndTradeId(anchorVideoInfoVo.getVideoId(), anchorVideoInfoVo.getTradeId());
//                if(videoAnalysisRecordInfoVo != null) {
//                    String dataJsonStr = ReplayFileUtils.getFileContent(wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecordInfoVo.getStoreFileName());
//                    if(!StringUtils.isEmpty(dataJsonStr)) {
//                        List<SentenceMarkVo> sentenceMarkVos = JSON.parseArray(dataJsonStr, SentenceMarkVo.class);
//
//                        List<OnlineAnalysisItemVo> onlineAnalysisItemVos = sentenceMarkVos.stream().map(item -> {
//                            OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
//                            onlineAnalysisItemVo.setFileUuid(videoAnalysisRecordInfoVo.getVideoId());
//                            onlineAnalysisItemVo.setStatus(0);
//                            onlineAnalysisItemVo.setDataJson(JSON.toJSONString(item));
//                            onlineAnalysisItemVo.setTradeId(videoAnalysisRecordInfoVo.getTradeId());
//                            onlineAnalysisItemVo.setParagraph(item.getCurrentSort());
//                            return onlineAnalysisItemVo;
//                        }).sorted(Comparator.comparingInt(OnlineAnalysisItemVo::getParagraph)).collect(Collectors.toList());
//
//                        onlineAnalysisInfoVo.setAnalysisList(onlineAnalysisItemVos);
//                    }
//                }
                // 获取在线人数列表
                List<OnlineNumInfoVo> onlineNumInfoVoList = socketCollectMessageProducer.onlineNumByUserIdAndBatchNumber(anchorVideoInfoVo.getUserId(), anchorVideoInfoVo.getBatchNumber() + "", videoId);
                onlineAnalysisInfoVo.setOnlineNumList(onlineNumInfoVoList);
            }
        }

        return R.ok(onlineAnalysisInfoVo);
    }

    public AiAnalysisRecordVo aiAnswerAndSensitiveTotal(List<String> aiContentList, Long tradeId) {
        return sensitiveWordsProducer.aiAnswerAndSensitiveTotal(aiContentList, tradeId);
    }

    public R<AnalysisResultAllVo> getParagraphContent(Integer sourceType, String sourceId,Integer type) {
        // 从oss获取分析数据
        String downloadUrl = this.getAnalysisDownloadUrl(sourceType, sourceId);
        if(StringUtil.isNotBlank(downloadUrl)) {
            String content = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);

            // 封装在线复盘信息
            if(StringUtil.isNotBlank(content)) {
                AnalysisResultAllVo resultAllVo = encContent(sourceType, sourceId, content);
                if(resultAllVo != null) {
                    resultAllVo.setResourceType(sourceType);
                    resultAllVo.setType(type);
                    return R.ok(resultAllVo);
                }
            }
        }
        return R.ok();
    }

    private AnalysisResultAllVo encContent(Integer sourceType, String sourceId, String content) {
        AnalysisResultAllVo resultAllVo= new AnalysisResultAllVo();
        Long tradeId = null;
        if (sourceType==0){
            //获取视频信息
            AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoProducer.getByVideoId(sourceId);
            if(anchorVideoInfoVo != null) {
                resultAllVo.setPlayUrl(anchorVideoInfoVo.getPlayUrl());
                resultAllVo.setVideoInfo(anchorVideoInfoVo);
                tradeId = anchorVideoInfoVo.getTradeId();
            }
        }
        if (sourceType==1){
            UploadFileInfoVo uploadFileInfoVo = this.uploadFileProducer.getByFileId(sourceId);
            if(uploadFileInfoVo != null) {
                resultAllVo.setPlayUrl(uploadFileInfoVo.getPlayUrl());
                resultAllVo.setUploadFileInfoVo(uploadFileInfoVo);
                tradeId = uploadFileInfoVo.getTradeId();
            }
        }
        // 设置词语段落分析数据
        if(StringUtil.isNotBlank(content)) {
            AnalysisResultVo analysisResultVo = JSONObject.parseObject(content, AnalysisResultVo.class);
            List<SentenceMarkVo> sentenceMarkVos = analysisResultVo.getSentenceMarkVos();
            if(sentenceMarkVos != null && !sentenceMarkVos.isEmpty()) {
                Long finalTradeId = tradeId;
                List<OnlineAnalysisItemVo> onlineAnalysisItemVos = sentenceMarkVos.stream().map(item -> {
                    OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
                    onlineAnalysisItemVo.setFileUuid(sourceId);
                    onlineAnalysisItemVo.setStatus(0);
                    onlineAnalysisItemVo.setDataJson(JSON.toJSONString(item));
                    onlineAnalysisItemVo.setTradeId(finalTradeId);
                    onlineAnalysisItemVo.setParagraph(item.getCurrentSort());
                    return onlineAnalysisItemVo;
                }).sorted(Comparator.comparingInt(OnlineAnalysisItemVo::getParagraph)).collect(Collectors.toList());
                resultAllVo.setAnalysisList(onlineAnalysisItemVos);
                return resultAllVo;
            }
        }
        return null;
    }

    /**
     * 获取行业id
     *
     * @param sourceType 类型
     * @param sourceId   源id
     * @return 行业id
     */
    public Long getTradeId(Integer sourceType, String sourceId) {

        if (sourceType == null) {
            return null;
        }

        switch (sourceType) {
            case 0:
                // 视频
                AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoProducer.getByVideoId(sourceId);
                if (anchorVideoInfoVo != null) {
                    return anchorVideoInfoVo.getTradeId();
                }
                break;
            case 1:
                // 文件
                UploadFileInfoVo uploadFileInfoVo = this.uploadFileProducer.getByFileId(sourceId);
                if (uploadFileInfoVo != null) {
                    return uploadFileInfoVo.getTradeId();
                }
                break;

            case 2:
                // 同步对比
                SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.infoByContrastId(sourceId);
                if (syncContrastInfoVo != null) {
                    String videoId = ObjectUtil.defaultIfEmpty(syncContrastInfoVo.getVideoOneId(), syncContrastInfoVo.getVideoTwoId());
                    String fileId = ObjectUtil.defaultIfEmpty(syncContrastInfoVo.getFileOneId(), syncContrastInfoVo.getFileTwoId());
                    if (StringUtil.isNotBlank(videoId)) {
                        return this.getTradeId(0, videoId);
                    }
                    if (StringUtil.isNotBlank(fileId)) {
                        return this.getTradeId(1, fileId);
                    }
                }
        }
        return null;
    }

    /**
     * 按 fileId 获取上传文件精简信息（跨模块资源校验 SPI）。
     *
     * <p>委托 uploadFileProducer.getByFileId，将 words 内部 UploadFileInfoVo 映射为
     * generic UploadFileSimpleInfoVo，不暴露 words 内部嵌套类型。</p>
     *
     * @param fileId 文件唯一标识（uuid）
     * @return 精简文件信息，fileId 不存在时返回 null
     */
    public R<UploadFileSimpleInfoVo> getUploadFileInfo(String fileId) {
        if (StringUtils.isEmpty(fileId)) {
            return R.ok(null);
        }
        UploadFileInfoVo info = this.uploadFileProducer.getByFileId(fileId);
        if (info == null) {
            return R.ok(null);
        }
        UploadFileSimpleInfoVo vo = new UploadFileSimpleInfoVo();
        vo.setFileId(info.getFileId());
        vo.setUserId(info.getUserId());
        vo.setTenantId(info.getTenantId());
        vo.setFileName(info.getFileName());
        vo.setUploadTime(info.getUploadTime());
        vo.setFileType(info.getFileType());
        vo.setFileDuration(info.getFileDuration());
        vo.setFileWordNum(info.getFileWordNum());
        // 2026-06-10 修复 DEBT-019：暴露 tradeId 给 generic SPI，供质检按行业取定制提示词
        vo.setTradeId(info.getTradeId());
        return R.ok(vo);
    }
}

