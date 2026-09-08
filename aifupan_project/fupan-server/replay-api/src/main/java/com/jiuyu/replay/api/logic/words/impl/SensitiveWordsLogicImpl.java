package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.api.logic.words.SensitiveWordsLogic;
import com.jiuyu.replay.common.bll.RocketMqBll;
import com.jiuyu.replay.common.constant.BusinessCachePrefix;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.context.RequestContext;
import com.jiuyu.replay.common.properties.RocketMqProperties;
import com.jiuyu.replay.generic.constant.activity.InviteRewardRuleCodeEnum;
import com.jiuyu.replay.generic.dto.activity.UserInviteDto;
import com.jiuyu.replay.generic.dto.activity.UserInviteMqDto;
import com.jiuyu.replay.generic.dto.power.UserDeviceFingerprintDto;
import com.jiuyu.replay.generic.enums.words.VideoPlatformEnum;
import com.jiuyu.replay.generic.feign.activity.UserInviteFeign;
import com.jiuyu.replay.generic.feign.power.UserDeviceFingerprintFeign;
import com.jiuyu.replay.generic.feign.third.TableStoreFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.system.bll.DictDataBll;
import com.jiuyu.replay.third.bll.TableStoreBll;
import com.jiuyu.replay.words.bll.*;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.words.vo.video.BarrageDataVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * 敏感词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
@Service
public class SensitiveWordsLogicImpl implements SensitiveWordsLogic {

    @Resource
    private SensitiveWordsBll sensitiveWordsBll;
    @Resource
    private SensitiveWordsClientBll sensitiveWordsClientBll;
    @Resource
    private DictDataBll dictDataBll;
    @Resource
    private TradeBll tradeBll;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private WordsProperties wordsProperties;
    @Resource
    private UserBll userBll;
    @Resource
    private UploadFileBll uploadFileBll;
    @Resource
    private CruxTypeBll cruxTypeBll;
    @Resource
    private TableStoreBll tableStoreBll;
    @Resource
    private SocketCollectMessageBll socketCollectMessageBll;
    @Autowired
    private AnchorVideoBll anchorVideoBll;
    @Resource
    RocketMqBll rocketMqBll;
    @Resource
    RocketMqProperties rocketMqProperties;
    @Resource
    UserInviteFeign userInviteFeign;
    @Resource
    private TableStoreFeign tableStoreFeign;
    @Resource
    UserDeviceFingerprintFeign userDeviceFingerprintFeign;
    @Resource
    VideoSliceBll videoSliceBll;
    @Resource
    VideoDataViewingBll videoDataViewingBll;

    @Override
    public R<List<SentenceMarkVo>> wordsMarkReAnalysis(WordsMarkReAnalysisBo wordsMarkReAnalysisBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        wordsMarkReAnalysisBo.setUserId(user.getId());
        // 获取之前最新的分析记录
        R<List<SentenceMarkVo>> recordR = this.sensitiveWordsBll.getLastAnalysisList(wordsMarkReAnalysisBo.getVideoId(), wordsMarkReAnalysisBo.getType());
        if (recordR.getCode() == 0) {
            List<SentenceMarkVo> sentenceMarkVoList = recordR.getData();

            // 封装成分析的格式
            if (sentenceMarkVoList != null && sentenceMarkVoList.size() > 0) {
                List<WordsMarkBo> wordsMarkBos = sentenceMarkVoList.stream().map(sentenceMarkVo -> {
                    WordsMarkBo wordsMarkBo = new WordsMarkBo();
                    wordsMarkBo.setPlatformType(wordsMarkReAnalysisBo.getPlatformType());
                    wordsMarkBo.setTradeId(wordsMarkReAnalysisBo.getTradeId());
                    wordsMarkBo.setVideoId(wordsMarkReAnalysisBo.getVideoId());
                    wordsMarkBo.setCurrentSort(sentenceMarkVo.getCurrentSort());
                    wordsMarkBo.setContent(sentenceMarkVo.getContent());

                    // 设置段落词语
                    List<WordListItemVo> items = sentenceMarkVo.getItems();
                    List<WordsMarkItemBo> wordsMarkItemBos;
                    if (items != null && items.size() > 0) {
                        wordsMarkItemBos = items.stream().map(item -> {
                            WordsMarkItemBo wordsMarkItemBo = new WordsMarkItemBo();
                            BeanUtils.copyProperties(item, wordsMarkItemBo);
                            return wordsMarkItemBo;
                        }).toList();
                    } else {
                        wordsMarkItemBos = new LinkedList<>();
                        WordsMarkItemBo wordsMarkItemBo = new WordsMarkItemBo();
                        wordsMarkItemBo.setWord("-");
                        wordsMarkItemBo.setStartTime(0L);
                        wordsMarkItemBo.setEndTime(20L);
                        wordsMarkItemBos.add(wordsMarkItemBo);
                    }
                    wordsMarkBo.setItems(wordsMarkItemBos);

                    if (sentenceMarkVo.getCurrentSort() == sentenceMarkVoList.size()) {
                        wordsMarkBo.setIsLast(1);
                    } else {
                        wordsMarkBo.setIsLast(0);
                    }
                    wordsMarkBo.setIsReAnalysis(1);
                    wordsMarkBo.setType(wordsMarkReAnalysisBo.getType());
                    wordsMarkBo.setUserId(user.getId());
                    return wordsMarkBo;
                }).sorted(Comparator.comparingInt(WordsMarkBo::getCurrentSort)).toList();

                if (wordsMarkReAnalysisBo.getType() == 0) {
                    // 视频
                    return wordsMark(wordsMarkBos);
                } else {
                    // 文件
                    R<UploadFileInfoVo> uploadFileVoR = this.uploadFileBll.infoByFileId(wordsMarkReAnalysisBo.getVideoId());
                    UploadFileInfoVo uploadFileInfoVo = uploadFileVoR.getData();
                    if (uploadFileInfoVo != null) {
                        if (uploadFileInfoVo.getFileType() == 2) {
                            // 文本文件
                            R<SentenceMarkVo> sentenceMarkVoR = wordsMarkByText(wordsMarkBos.get(0));
                            List<SentenceMarkVo> sentenceMarkVos = new LinkedList<>();
                            sentenceMarkVos.add(sentenceMarkVoR.getData());
                            return R.ok(sentenceMarkVos);
                        } else {
                            // 视频/音频文件
                            return wordsMark(wordsMarkBos);
                        }
                    }

                }


            }
        }

        return R.error(5001, "数据不存在");
    }

    @Override
    public R<AnalysisResultVo> wordsMarkReAnalysis2_0(WordsMarkReAnalysisBo wordsMarkReAnalysisBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        wordsMarkReAnalysisBo.setUserId(user.getId());
        // 获取之前最新的分析记录
//        R<AnalysisResultVo> recordR = this.sensitiveWordsBll.getLastAnalysisList2_0(wordsMarkReAnalysisBo.getVideoId(), wordsMarkReAnalysisBo.getType());
        R<AnalysisResultVo> recordR = this.sensitiveWordsBll.getLastAnalysisListByOss(wordsMarkReAnalysisBo.getVideoId(), wordsMarkReAnalysisBo.getType());
        if (recordR.getCode() == 0) {
            AnalysisResultVo rData = recordR.getData();
            if (rData != null) {
                List<SentenceMarkVo> sentenceMarkVoList = rData.getSentenceMarkVos();

                // 封装成分析的格式
                if (sentenceMarkVoList != null && sentenceMarkVoList.size() > 0) {
                    List<WordsMarkBo> wordsMarkBos = sentenceMarkVoList.stream().map(sentenceMarkVo -> {
                        WordsMarkBo wordsMarkBo = new WordsMarkBo();
                        wordsMarkBo.setPlatformType(wordsMarkReAnalysisBo.getPlatformType());
                        wordsMarkBo.setTradeId(wordsMarkReAnalysisBo.getTradeId());
                        wordsMarkBo.setVideoId(wordsMarkReAnalysisBo.getVideoId());
                        wordsMarkBo.setCurrentSort(sentenceMarkVo.getCurrentSort());
                        wordsMarkBo.setContent(sentenceMarkVo.getContent());

                        // 设置段落词语
                        List<WordListItemVo> items = sentenceMarkVo.getItems();
                        List<WordsMarkItemBo> wordsMarkItemBos;
                        if (items != null && items.size() > 0) {
                            wordsMarkItemBos = items.stream().map(item -> {
                                WordsMarkItemBo wordsMarkItemBo = new WordsMarkItemBo();
                                BeanUtils.copyProperties(item, wordsMarkItemBo);
                                return wordsMarkItemBo;
                            }).toList();
                        } else {
                            wordsMarkItemBos = new LinkedList<>();
                            WordsMarkItemBo wordsMarkItemBo = new WordsMarkItemBo();
                            wordsMarkItemBo.setWord("-");
                            wordsMarkItemBo.setStartTime(0L);
                            wordsMarkItemBo.setEndTime(20L);
                            wordsMarkItemBos.add(wordsMarkItemBo);
                        }
                        wordsMarkBo.setItems(wordsMarkItemBos);

                        if (sentenceMarkVo.getCurrentSort() == sentenceMarkVoList.size()) {
                            wordsMarkBo.setIsLast(1);
                        } else {
                            wordsMarkBo.setIsLast(0);
                        }
                        wordsMarkBo.setIsReAnalysis(1);
                        wordsMarkBo.setType(wordsMarkReAnalysisBo.getType());
                        wordsMarkBo.setUserId(user.getId());
                        return wordsMarkBo;
                    }).sorted(Comparator.comparingInt(WordsMarkBo::getCurrentSort)).toList();

                    if (wordsMarkReAnalysisBo.getType() == 0) {
                        // 视频
                        return wordsMark2_0(wordsMarkBos);
                    } else {
                        // 文件
                        R<UploadFileInfoVo> uploadFileVoR = this.uploadFileBll.infoByFileId(wordsMarkReAnalysisBo.getVideoId());
                        UploadFileInfoVo uploadFileInfoVo = uploadFileVoR.getData();
                        if (uploadFileInfoVo != null) {
                            if (uploadFileInfoVo.getFileType() == 2) {
                                // 文本文件
                                return wordsMarkByText2_0(wordsMarkBos.get(0));
                            } else {
                                // 视频/音频文件
                                return wordsMark2_0(wordsMarkBos);
                            }
                        }

                    }

                }
            }


        }

        return R.error(5001, "数据不存在");
    }

    @Override
    public byte[] downloadAnalysisFile(Integer type, String uuid) {

        return sensitiveWordsBll.downloadAnalysisFile(type, uuid);
    }

    @Override
    public byte[] downloadAnalysisFile2_0(Integer type, String uuid) {

        return sensitiveWordsBll.downloadAnalysisFile2_0(type, uuid);
    }

    @Override
    public byte[] getOnlineAnalysisZip(String fileId, String videoId) throws Exception {

        return sensitiveWordsBll.getOnlineAnalysisZip(fileId, videoId);
    }

    @Override
    public byte[] getOnlineAnalysisZip2_0(Integer type, String uuid) throws Exception {

        return sensitiveWordsBll.getOnlineAnalysisZip2_0(type, uuid);
    }

    @Override
    public byte[] getOnlineContrastAnalysisZip(String contrastId) throws Exception {

        return sensitiveWordsBll.getOnlineContrastAnalysisZip(contrastId);
    }

    @Override
    public byte[] getOnlineContrastAnalysisZip2_0(String contrastId) throws Exception {

        return sensitiveWordsBll.getOnlineContrastAnalysisZip2_0(contrastId);
    }

    @Override
    public R<AnalysisResultVo> wordsMark2_0(List<WordsMarkBo> wordsMarkBoList) {

        if (wordsMarkBoList == null || wordsMarkBoList.size() < 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "分析异常");
        }

        UserCacheVo user = GlobalObject.getLocalUser();
        List<SentenceMarkVo> sentenceMarkVos = new LinkedList<>();
        for (WordsMarkBo wordsMarkBo : wordsMarkBoList) {
            sentenceMarkVos.addAll(sensitiveWordsBll.subSentence(wordsMarkBo, user.getId()));
        }

        if (sentenceMarkVos.size() > 0) {
            // 封装段落
            packageParagraph(sentenceMarkVos, wordsMarkBoList.get(0).getPlatformType(), wordsMarkBoList.get(0).getTradeId());
            AnalysisResultVo analysisResultVo = new AnalysisResultVo();
            analysisResultVo.setSentenceMarkVos(sentenceMarkVos);

            // 封装关键词分类信息
            List<AnalysisResultCruxTypeVo> analysisResultCruxTypeVos = this.sensitiveWordsBll.packageCruxType(sentenceMarkVos);
            analysisResultVo.setCruxTypeList(analysisResultCruxTypeVos);

            // 封装词语汇总列表
            List<WordsMarkVo> wordsCollect = this.sensitiveWordsBll.packageWordCollect(sentenceMarkVos);
            analysisResultVo.setWordsCollect(wordsCollect);

            // 统计词语全文出现次数
            this.sensitiveWordsBll.countWordTotalNum(wordsCollect, sentenceMarkVos);

            // 封装词语tab列表
            List<WordsTabVo> wordsTabList = this.sensitiveWordsBll.packageWordTab(sentenceMarkVos, analysisResultCruxTypeVos);
            analysisResultVo.setWordsTabList(wordsTabList);

            // 统计关键词、敏感词总数和文本总字数
            int cruxWordNum = 0;
            int sensitiveWordNum = 0;
            int contentNum = 0;
            if (wordsTabList != null && wordsTabList.size() > 0) {
                for (WordsTabVo wordsTabVo : wordsTabList) {
                    if (wordsTabVo.getTabType() == 1) {
                        sensitiveWordNum = wordsTabVo.getNum();
                    } else if (wordsTabVo.getTabType() == 2) {
                        cruxWordNum = wordsTabVo.getNum();
                    }
                }
            }
            for (WordsMarkBo wordsMarkBo : wordsMarkBoList) {
                contentNum += wordsMarkBo.getContent().length();
            }

            // 将分析结果存储到数据库
            WordsMarkBo wordsMarkBo = wordsMarkBoList.get(0);
            String oldJsonStr = JSONObject.toJSONString(sentenceMarkVos);
            String newJsonStr = JSONObject.toJSONString(analysisResultVo);
            this.sensitiveWordsBll.saveCacheToDb2_0(oldJsonStr, newJsonStr, wordsMarkBo.getType(), wordsMarkBo.getVideoId(), wordsMarkBo.getTradeId(), user.getId(), cruxWordNum, sensitiveWordNum, contentNum);

            // 删除弹幕标注的缓存
            redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.barrageDataCacheKey, wordsMarkBo.getVideoId()));

            // 发送用户分析直播数据异步消息
            String fingerprint = RequestContext.getFingerprint();
            if (StrUtil.isBlank(fingerprint)) {
                return R.ok(analysisResultVo);
            }
            UserDeviceFingerprintDto userDeviceFingerprintDto = userDeviceFingerprintFeign.getUserDeviceFingerprintByFingerprint(fingerprint);
            if (Objects.nonNull(userDeviceFingerprintDto) && !user.getId().equals(userDeviceFingerprintDto.getUserId())) {
                return R.ok(analysisResultVo);
            }
            UserInviteDto userInviteDto = userInviteFeign.judgeUserInviteEffective(user.getId());
            if (Objects.isNull(userInviteDto)) {
                return R.ok(analysisResultVo);
            }
            WordsMarkBo markBo = wordsMarkBoList.get(0);
            if (Objects.isNull(markBo) || Objects.isNull(markBo.getVideoId())) {
                return R.ok(analysisResultVo);
            }
            // 直播id
            String videoId = markBo.getVideoId();
            String key = BusinessCachePrefix.USER_ANALYZE_LIVE_BROADCAST_COUNT_CACHE.prefix + user.getId();
            // 记录每次直播分析时间
            redisTemplate.opsForZSet().add(key, videoId, System.currentTimeMillis());
            // 设置合理ttl,防止redis内存占用过大
            redisTemplate.expire(key, 30, TimeUnit.DAYS);
            // 获取不同直播数量
            Long distinctCount = redisTemplate.opsForZSet().zCard(key);
            if (distinctCount == 2) {
                // 发送用户录制分析2场直播消息队列
                Long userId = user.getId();
                UserInviteMqDto userInviteMqDto = new UserInviteMqDto(InviteRewardRuleCodeEnum.USE.getCode(), userId, fingerprint);
                rocketMqBll.syncSendNormalMessage(userId, rocketMqProperties.getTagUserInviteActivity(), IdUtil.simpleUUID(), JSON.toJSONString(userInviteMqDto));
            }
            return R.ok(analysisResultVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有数据");
    }

    @Override
    public R<AnalysisResultCloudVo> getOnlineAnalysis2_0(Integer type, String uuid) {
        return this.sensitiveWordsBll.getOnlineAnalysis2_0(type, uuid);
    }

    @Override
    public R<AnalysisResultCloudVo> getAnalysis(Integer type, String uuid) {
        R<AnalysisResultCloudVo> onlineAnalysis20 = this.sensitiveWordsBll.getAnalysis(type, uuid);
        if (type == 0 && ObjectUtil.isNotEmpty(onlineAnalysis20.getData())) {
            List<Map<String, Object>> hashMaps = tableStoreFeign.getBarrageDataList(uuid, onlineAnalysis20.getData().getAudioaAlyses().stream().map(VideoParagraphAnalysisVoUpper::getDataJson).toList());
            onlineAnalysis20.getData().setBarrageDataList(hashMaps);

            // 获取弹幕总数据量
            onlineAnalysis20.getData().setTotalBarrageNum(hashMaps.stream().mapToInt(map -> NumberUtil.parseInt(map.get("barrageNum").toString(), 0)).sum());
        }
        return onlineAnalysis20;
    }

    private long getStart(SentenceMarkVo sentenceMarkVo) {
        if (ObjectUtil.isNotEmpty(sentenceMarkVo) && ObjectUtil.isNotEmpty(sentenceMarkVo.getItems())) {
            List<WordListItemVo> items = sentenceMarkVo.getItems();
            return items.get(0).getStartTime();
        }
        return 0L;
    }

    @Override
    public R<AnalysisContractResultCloudVo> getOnlineContrastAnalysis2_0(String contrastId) throws Exception {

        return this.sensitiveWordsBll.getOnlineContrastAnalysis2_0(contrastId);
    }

    @Override
    public R<AnalysisContractResultCloudVo> getContrastAnalysis(String contrastId) {
        return this.sensitiveWordsBll.getContrastAnalysis(contrastId);
    }

    @Override
    public R<String> getAnalysisDownloadUrl(Integer type, String uuid) {

        return sensitiveWordsBll.getAnalysisDownloadUrlR(type, uuid);
    }

    @Override
    public byte[] getOnlineAnalysisZip2_1(Integer type, String uuid) throws Exception {

        return sensitiveWordsBll.getOnlineAnalysisZip2_1(type, uuid);
    }

    @Override
    public byte[] getOnlineContrastAnalysisZip2_1(String contrastId) throws Exception {

        return sensitiveWordsBll.getOnlineContrastAnalysisZip2_1(contrastId);
    }

    @Override
    public R<AnalysisResultCloudVo> getOnlineAnalysis2_1(Integer type, String uuid) throws Exception {

        return this.sensitiveWordsBll.getOnlineAnalysis2_1(type, uuid);
    }

    @Override
    public R<AnalysisContractResultCloudVo> getOnlineContrastAnalysis2_1(String contrastId) throws Exception {

        return this.sensitiveWordsBll.getOnlineContrastAnalysis2_1(contrastId);
    }

    @Override
    public R<List<SentenceMarkVo>> wordsMark(List<WordsMarkBo> wordsMarkBoList) {

        if (wordsMarkBoList == null || wordsMarkBoList.size() < 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "分析异常");
        }

        UserCacheVo user = GlobalObject.getLocalUser();
        List<SentenceMarkVo> sentenceMarkVos = new LinkedList<>();
        for (WordsMarkBo wordsMarkBo : wordsMarkBoList) {
            sentenceMarkVos.addAll(sensitiveWordsBll.subSentence(wordsMarkBo, user.getId()));
        }

        if (sentenceMarkVos.size() > 0) {
            // 封装段落
            packageParagraph(sentenceMarkVos, wordsMarkBoList.get(0).getPlatformType(), wordsMarkBoList.get(0).getTradeId());

            // 将分析结果存储到数据库
            WordsMarkBo wordsMarkBo = wordsMarkBoList.get(0);
            String jsonStr = JSONObject.toJSONString(sentenceMarkVos);
            this.sensitiveWordsBll.saveCacheToDb(jsonStr, wordsMarkBo.getType(), wordsMarkBo.getVideoId(), wordsMarkBo.getTradeId(), user.getId());
            return R.ok(sentenceMarkVos);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有数据");
    }

    /**
     * 封装段落内容以及词语信息
     *
     * @param sentenceMarkVoList 所有段落数据列表
     * @return
     */
    public void packageParagraph(List<SentenceMarkVo> sentenceMarkVoList, int platformType, long tradeId) {

        UserCacheVo user = GlobalObject.getLocalUser();
        Long userId = user.getId();
        R<Long> parentR = userBll.getCacheUserParentId(user.getId());
        if (parentR.getCode() == 0 && !StringUtils.isEmpty(parentR.getData())) {
            userId = parentR.getData();
        }

        // 获取系统的所有关键词
        List<SensitiveWordsVo> cruxWordsVoList = this.sensitiveWordsBll.listByUidAndPidAndTid(userId, platformType, tradeId, 1);
        // 获取用户自定义的关键词
        List<SensitiveWordsClientVo> cruxWordsClientVoList = this.sensitiveWordsClientBll.listByUidAndPidAndTid(userId, platformType, tradeId, 1);
        // 获取系统的所有敏感词
        List<SensitiveWordsVo> sensitiveWordsVos = this.sensitiveWordsBll.listByUidAndPidAndTid(userId, platformType, tradeId, 0);
        // 获取用户自定义的敏感词
        List<SensitiveWordsClientVo> sensitiveWordsClientVos = this.sensitiveWordsClientBll.listByUidAndPidAndTid(userId, platformType, tradeId, 0);
        // 获取客户自定义的白名单
        List<SensitiveWordsClientVo> clientWhite = this.sensitiveWordsClientBll.listByUidAndPidAndTid(userId, platformType, tradeId, 2);

        // 整合客户端本地关键词
        if (cruxWordsVoList == null || cruxWordsVoList.size() < 1) {
            cruxWordsVoList = new LinkedList<>();
        }
        if (cruxWordsClientVoList != null && cruxWordsClientVoList.size() > 0) {
            for (SensitiveWordsClientVo sensitiveWordsClientVo : cruxWordsClientVoList) {
                SensitiveWordsVo sensitiveWordsVo = new SensitiveWordsVo();
                BeanUtils.copyProperties(sensitiveWordsClientVo, sensitiveWordsVo);
                cruxWordsVoList.add(sensitiveWordsVo);
            }
        }
        // 去除重复的关键词
        List<SensitiveWordsVo> allCruxWordsVoList = new LinkedList<>(cruxWordsVoList);
        List<SensitiveWordsVo> resultCruxList = removeDuplicates(allCruxWordsVoList);

        // 整合客户端本地敏感词
        if (sensitiveWordsVos == null || sensitiveWordsVos.size() < 1) {
            sensitiveWordsVos = new LinkedList<>();
        }
        if (sensitiveWordsClientVos != null && sensitiveWordsClientVos.size() > 0) {
            for (SensitiveWordsClientVo sensitiveWordsClientVo : sensitiveWordsClientVos) {
                SensitiveWordsVo sensitiveWordsVo = new SensitiveWordsVo();
                BeanUtils.copyProperties(sensitiveWordsClientVo, sensitiveWordsVo);
                sensitiveWordsVos.add(sensitiveWordsVo);
            }
        }
        // 去除重复的敏感词
        List<SensitiveWordsVo> allSensitiveWordsVoList = new LinkedList<>(sensitiveWordsVos);
        List<SensitiveWordsVo> resultSensitiveList = removeDuplicates(allSensitiveWordsVoList);
        if (resultSensitiveList.size() > 0) {
            // 剔除在白名单里面的敏感词
            removeFromWhite(resultSensitiveList, clientWhite);
        }

        // 获取行业列表
        R<List<TradeVo>> tradeListR = this.tradeBll.listAll();
        List<TradeVo> tradeVos = tradeListR.getData();
        // 获取平台类型
        R<List<DictDataListVo>> r1 = this.dictDataBll.listByTypeLogo("words_platform_type");
        List<DictDataListVo> platformTypeList = r1.getData();
        // 获取敏感词类型
        R<List<DictDataListVo>> r2 = this.dictDataBll.listByTypeLogo("sensitive_words_type");
        List<DictDataListVo> sensitiveTypeList = r2.getData();
        // 获取关键词类型
        R<List<DictDataListVo>> r3 = this.dictDataBll.listByTypeLogo("crux_words_type");
        List<DictDataListVo> cruxTypeList = r3.getData();
        // 获取敏感词等级
        R<List<DictDataListVo>> r4 = this.dictDataBll.listByTypeLogo("sensitive_words_level_type");
        List<DictDataListVo> levelTypeList = r4.getData();
        // 获取关键词分类
        R<List<CruxTypeInfoVo>> cruxTypeInfoVoR = this.cruxTypeBll.listAll();
        List<CruxTypeInfoVo> cruxTypeInfoVoList = cruxTypeInfoVoR.getData();

        // 处理每一个段落内容
        for (SentenceMarkVo sentenceMarkVo : sentenceMarkVoList) {
            // 处理关键词
            if (cruxWordsVoList.size() > 0) {
                sentenceMarkVo.setWordsList(new LinkedList<>());
                for (SensitiveWordsVo cruxWordsVo : resultCruxList) {
                    // 判断是否包含关键词
                    if (sentenceMarkVo.getContent().contains(cruxWordsVo.getName())) {
                        // 添加进列表
                        addWordsMarkVo(1, sentenceMarkVo.getWordsList(), cruxWordsVo, sentenceMarkVo.getContent(), resultCruxList);
                    }
                }
            }

            // 处理敏感词
            if (sensitiveWordsVos.size() > 0) {
                if (sentenceMarkVo.getWordsList() == null) {
                    sentenceMarkVo.setWordsList(new LinkedList<>());
                }
                for (SensitiveWordsVo sensitiveWordsVo : resultSensitiveList) {
                    // 判断是否包含敏感词
                    if (sentenceMarkVo.getContent().contains(sensitiveWordsVo.getName())) {
                        // 添加进列表
                        addWordsMarkVo(0, sentenceMarkVo.getWordsList(), sensitiveWordsVo, sentenceMarkVo.getContent(), resultSensitiveList);
                    }
                }
            }

            // 填充关键词/敏感词的详细信息
            setWordsListStr(sentenceMarkVo, tradeVos, platformTypeList, sensitiveTypeList, cruxTypeList, levelTypeList, cruxTypeInfoVoList);

        }
    }

    /**
     * 剔除在白名单里面的敏感词
     *
     * @param sensitiveWordsVoList 敏感词列表
     * @param clientWhite          白名单列表
     */
    private void removeFromWhite(List<SensitiveWordsVo> sensitiveWordsVoList, List<SensitiveWordsClientVo> clientWhite) {
        if (clientWhite != null && clientWhite.size() > 0) {
            // 剔除在白名单里面的敏感词
            Iterator<SensitiveWordsVo> iterator = sensitiveWordsVoList.iterator();
            while (iterator.hasNext()) {
                SensitiveWordsVo sensitiveWordsVo = iterator.next();
                for (SensitiveWordsClientVo whiteWordsVo : clientWhite) {
                    if (whiteWordsVo.getName().equals(sensitiveWordsVo.getName())) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }

    /**
     * 去除重复的词语
     *
     * @param wordsVoList 词语列表
     * @return
     */
    private List<SensitiveWordsVo> removeDuplicates(List<SensitiveWordsVo> wordsVoList) {

//        wordsVoList.removeIf(item -> item.getWordsType() == 1 && item.getCruxTypeId().equals(0L));

        List<SensitiveWordsVo> resultList = new LinkedList<>();
        for (SensitiveWordsVo sensitiveWordsVo : wordsVoList) {
            boolean exist = false;
            for (int i = 0; i < resultList.size(); i++) {
                SensitiveWordsVo existWordsVo = resultList.get(i);
                if (sensitiveWordsVo.getName().equals(existWordsVo.getName())) {
                    exist = true;
                    if (sensitiveWordsVo.getResourceType() == 1 && existWordsVo.getResourceType() == 0) {
                        // 当前已存在的是系统的，新词是客户自定义的，优先使用客户自定义的词
                        resultList.set(i, sensitiveWordsVo);
                    } else if (sensitiveWordsVo.getTradeIdArr().length() > existWordsVo.getTradeIdArr().length()) {
                        // 优先使用子行业的词
                        resultList.set(i, sensitiveWordsVo);
                    } else if (!sensitiveWordsVo.getParentId().equals(0L) && existWordsVo.getParentId().equals(0L)) {
                        // 优先使用相似词
                        resultList.set(i, sensitiveWordsVo);
                    }
                }
            }
            if (!exist) {
                // 不存在，直接添加
                resultList.add(sensitiveWordsVo);
            }

        }
        return resultList;
    }

    @Override
    public R<SentenceMarkVo> wordsMarkByText(WordsMarkBo wordsMarkBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        wordsMarkBo.setUserId(user.getId());


        List<SentenceMarkVo> sentenceMarkVos = new LinkedList<>();
        SentenceMarkVo sentenceMarkVo = new SentenceMarkVo();
        sentenceMarkVo.setVideoId(wordsMarkBo.getVideoId());
        sentenceMarkVo.setCurrentSort(wordsMarkBo.getCurrentSort());
        sentenceMarkVo.setContent(wordsMarkBo.getContent());
        sentenceMarkVos.add(sentenceMarkVo);

        // 封装段落内容
        packageParagraph(sentenceMarkVos, wordsMarkBo.getPlatformType(), wordsMarkBo.getTradeId());

        // 将识别结果插入到数据库
        String jsonStr = JSONObject.toJSONString(sentenceMarkVos);
        this.sensitiveWordsBll.saveCacheToDb(jsonStr, wordsMarkBo.getType(), wordsMarkBo.getVideoId(), wordsMarkBo.getTradeId(), user.getId());

        return R.ok(sentenceMarkVo);
    }

    @Override
    public R<AnalysisResultVo> wordsMarkByText2_0(WordsMarkBo wordsMarkBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        wordsMarkBo.setUserId(user.getId());

        List<SentenceMarkVo> sentenceMarkVos = new LinkedList<>();
        SentenceMarkVo sentenceMarkVo = new SentenceMarkVo();
        sentenceMarkVo.setVideoId(wordsMarkBo.getVideoId());
        sentenceMarkVo.setCurrentSort(wordsMarkBo.getCurrentSort());
        sentenceMarkVo.setContent(wordsMarkBo.getContent());
        sentenceMarkVos.add(sentenceMarkVo);

        // 封装段落内容
        packageParagraph(sentenceMarkVos, wordsMarkBo.getPlatformType(), wordsMarkBo.getTradeId());

        AnalysisResultVo analysisResultVo = new AnalysisResultVo();
        analysisResultVo.setSentenceMarkVos(sentenceMarkVos);

        // 封装关键词分类信息
        List<AnalysisResultCruxTypeVo> analysisResultCruxTypeVos = this.sensitiveWordsBll.packageCruxType(sentenceMarkVos);
        analysisResultVo.setCruxTypeList(analysisResultCruxTypeVos);

        // 封装词语汇总列表
        List<WordsMarkVo> wordsCollect = this.sensitiveWordsBll.packageWordCollect(sentenceMarkVos);
        analysisResultVo.setWordsCollect(wordsCollect);

        // 统计词语全文出现次数
        this.sensitiveWordsBll.countWordTotalNum(wordsCollect, sentenceMarkVos);

        // 封装词语tab列表
        List<WordsTabVo> wordsTabList = this.sensitiveWordsBll.packageWordTab(sentenceMarkVos, analysisResultCruxTypeVos);
        analysisResultVo.setWordsTabList(wordsTabList);

        // 统计关键词、敏感词总数和文本总字数
        int cruxWordNum = 0;
        int sensitiveWordNum = 0;
        int contentNum = wordsMarkBo.getContent().length();
        if (wordsTabList != null && wordsTabList.size() > 0) {
            for (WordsTabVo wordsTabVo : wordsTabList) {
                if (wordsTabVo.getTabType() == 1) {
                    sensitiveWordNum = wordsTabVo.getNum();
                } else if (wordsTabVo.getTabType() == 2) {
                    cruxWordNum = wordsTabVo.getNum();
                }
            }
        }

        // 将分析结果存储到数据库
        String oldJsonStr = JSONObject.toJSONString(sentenceMarkVos);
        String newJsonStr = JSONObject.toJSONString(analysisResultVo);
        this.sensitiveWordsBll.saveCacheToDb2_0(oldJsonStr, newJsonStr, wordsMarkBo.getType(), wordsMarkBo.getVideoId(), wordsMarkBo.getTradeId(), user.getId(), cruxWordNum, sensitiveWordNum, contentNum);
        return R.ok(analysisResultVo);

    }

    @Override
    public R<List<String>> importExcel(MultipartFile excel) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return this.sensitiveWordsBll.importExcel(excel, user.getId());
    }

    @Override
    public R<OnlineAnalysisInfoVo> getOnlineAnalysisInfo(String fileId, String videoId) throws Exception {

        return this.sensitiveWordsBll.getOnlineAnalysisInfo(fileId, videoId);
    }

    @Override
    public R<OnlineContrastAnalysisInfoVo> getOnlineContrastAnalysisInfo(String contrastId) throws Exception {

        return this.sensitiveWordsBll.getOnlineContrastAnalysisInfo(contrastId);
    }


    /**
     * 设置关键词/敏感词列表的字符串
     *
     * @param sentenceMarkVo 段落
     * @param tradeList      行业信息列表
     */
    private void setWordsListStr(SentenceMarkVo sentenceMarkVo, List<TradeVo> tradeList, List<DictDataListVo> platformTypeList, List<DictDataListVo> sensitiveTypeList, List<DictDataListVo> cruxTypeList, List<DictDataListVo> levelTypeList, List<CruxTypeInfoVo> cruxTypeInfoVoList) {

        List<WordsMarkVo> wordsList = sentenceMarkVo.getWordsList();
        if (wordsList != null && wordsList.size() > 0) {

            for (WordsMarkVo wordsMarkVo : wordsList) {
                wordsMarkVo.setWordsTypeStr(List.of("智能敏感词", "运营关键词").get(wordsMarkVo.getWordsType()));
                wordsMarkVo.setResourceTypeStr(List.of("系统", "本地").get(wordsMarkVo.getResourceType()));

                if (wordsMarkVo.getWordsType() == 0) {
                    // 封装敏感词类型
                    for (DictDataListVo dictDataListVo : sensitiveTypeList) {
                        if (dictDataListVo.getValue().equals(wordsMarkVo.getType() + "")) {
                            wordsMarkVo.setTypeStr(dictDataListVo.getLabel());
                            break;
                        }
                    }
                    // 封装敏感词等级类型
                    for (DictDataListVo dictDataListVo : levelTypeList) {
                        if (dictDataListVo.getValue().equals(wordsMarkVo.getLevel() + "")) {
                            wordsMarkVo.setLevelStr(dictDataListVo.getLabel());
                            break;
                        }
                    }

                } else if (wordsMarkVo.getWordsType() == 1) {
                    // 封装关键词类型
                    for (DictDataListVo dictDataListVo : cruxTypeList) {
                        if (dictDataListVo.getValue().equals(wordsMarkVo.getType() + "")) {
                            wordsMarkVo.setTypeStr(dictDataListVo.getLabel());
                            break;
                        }
                    }
                    // 封装关键词分类
                    if (cruxTypeInfoVoList != null && cruxTypeInfoVoList.size() > 0) {
                        for (CruxTypeInfoVo cruxTypeInfoVo : cruxTypeInfoVoList) {
                            if (cruxTypeInfoVo.getId().equals(wordsMarkVo.getCruxTypeId())) {
                                wordsMarkVo.setCruxTypeInfo(cruxTypeInfoVo);
                                break;
                            }
                        }
                    }

                } else if (wordsMarkVo.getWordsType() == 2) {
                    wordsMarkVo.setTypeStr("其他");
                }
                // 封装平台类型
                for (DictDataListVo dictDataListVo : platformTypeList) {
                    if (dictDataListVo.getValue().equals(wordsMarkVo.getPlatformType() + "")) {
                        wordsMarkVo.setPlatformTypeStr(dictDataListVo.getLabel());
                        break;
                    }
                }
                // 封装行业
                if (tradeList != null) {
                    for (TradeVo tradeVo : tradeList) {
                        if (tradeVo.getId().equals(wordsMarkVo.getTradeId())) {
                            wordsMarkVo.setTradeStr(tradeVo.getName());
                            break;
                        }
                    }
                }

            }
        }
    }

    /**
     * 添加敏感词/关键词进列表
     *
     * @param wordsType      词语类型  0：敏感词 1：关键词 2：白名单(只有客户自定义有)
     * @param wordsList      当前已添加的词语列表
     * @param wordsVo        词语对象
     * @param content        段落内容
     * @param allWordsVoList 全部词语列表
     */
    private void addWordsMarkVo(Integer wordsType, List<WordsMarkVo> wordsList, SensitiveWordsVo wordsVo, String content, List<SensitiveWordsVo> allWordsVoList) {


        String word = wordsVo.getName();

        if (StringUtils.isEmpty(word)) {
            return;
        }

        int markNum = 0; // 统计词语需要标注的次数
        int countNum = 0; // 统计词语出现的次数
        List<RecordNeedsWordVo> recordNeedsWordList = new LinkedList<>();
        List<Integer> recordNeedsWordNumList = new LinkedList<>();
        String subContent = content; // 截取的内容
        int wordCurrentIndex = 0; // indexOf开始的索引
        // 判断词语有没有规则
        List<WordRuleInfoVo> ruleList = wordsVo.getRuleList();
        while (subContent.contains(word)) {

            if (ruleList != null && ruleList.size() > 0) {
                String restrictWord = ""; // 匹配中的限定词
                Integer restrictRange = 0; // 限定词范围
                // 分别取出限定词，白名单，黑名单
                List<WordRuleInfoVo> blackRuleList = ruleList.stream().filter(item -> item.getBlackOrWhite() == 0).toList();
                List<WordRuleInfoVo> whiteRuleList = ruleList.stream().filter(item -> item.getBlackOrWhite() == 1).toList();
                List<WordRuleInfoVo> restrictRuleList = ruleList.stream().filter(item -> item.getBlackOrWhite() == 2).toList();

                // 有规则，取出所有规则，判断是否符合规则，符合才统计字数
                // 取到词语出现的下标
                wordCurrentIndex = content.indexOf(word, wordCurrentIndex);

                boolean isMark = false;
                if (restrictRuleList.size() > 0) {
                    // 存在限定词，只有匹配上任意一个限定词，才算匹配上
                    isMark = false;

                    // 匹配限定词，只有匹配上任意一个限定词，才算匹配上
                    outerLoop:
                    for (WordRuleInfoVo rule : restrictRuleList) {
                        if (rule.getType() == 2) {
                            // 判断词语的两边是否任意一边包含规则关联词
                            List<String> containerWordList = rule.getContainerWordList();
                            // 判断词语的左边是否包含关联词
                            if (wordCurrentIndex != 0) {
                                int leftStartIndex = Math.max(wordCurrentIndex - rule.getRangeLength(), 0);
                                String leftContent = content.substring(leftStartIndex, wordCurrentIndex);
                                for (String containerWord : containerWordList) {
                                    if (leftContent.contains(containerWord)) {
                                        isMark = true;
                                        restrictWord = containerWord;
                                        restrictRange = rule.getRangeLength();
                                        break outerLoop;
                                    }
                                }
                            }
                            // 判断词语的右边是否包含关联词
                            int rightEndIndex = Math.min(wordCurrentIndex + word.length() + rule.getRangeLength(), content.length());
                            String rightContent = content.substring(wordCurrentIndex + word.length(), rightEndIndex);
                            for (String containerWord : containerWordList) {
                                if (rightContent.contains(containerWord)) {
                                    isMark = true;
                                    restrictWord = containerWord;
                                    restrictRange = rule.getRangeLength();
                                    break outerLoop;
                                }
                            }
                        }
                    }
                } else {
                    // 不存在限定词，默认已经匹配上
                    isMark = true;

                    // 匹配白名单，白名单只要匹配中一个，就表示不需要标注
                    outerLoop:
                    for (WordRuleInfoVo rule : whiteRuleList) {
                        if (rule.getType() == 2) {
                            // 判断词语的两边是否任意一边包含规则关联词
                            List<String> containerWordList = rule.getContainerWordList();
                            // 判断词语的左边是否包含关联词
                            if (wordCurrentIndex != 0) {
                                int leftStartIndex = Math.max(wordCurrentIndex - rule.getRangeLength(), 0);
                                String leftContent = content.substring(leftStartIndex, wordCurrentIndex);
                                for (String containerWord : containerWordList) {
                                    if (leftContent.contains(containerWord)) {
                                        isMark = false;
                                        break outerLoop;
                                    }
                                }
                            }
                            // 判断词语的右边是否包含关联词
                            int rightEndIndex = Math.min(wordCurrentIndex + word.length() + rule.getRangeLength(), content.length());
                            String rightContent = content.substring(wordCurrentIndex + word.length(), rightEndIndex);
                            for (String containerWord : containerWordList) {
                                if (rightContent.contains(containerWord)) {
                                    isMark = false;
                                    break outerLoop;
                                }
                            }
                        }
                    }

                    if (!isMark) {
                        // 已经匹配白名单成功，继续匹配黑名单，如果黑名单匹配成功，则优先黑名单，就是需要标注
                        // 判断黑名单规则，只要中一个，就表示要标注
                        outerLoop:
                        for (WordRuleInfoVo rule : blackRuleList) {
                            if (rule.getType() == 2) {
                                // 判断词语的两边是否任意一边包含规则关联词
                                List<String> containerWordList = rule.getContainerWordList();
                                // 判断词语的左边是否包含关联词
                                if (wordCurrentIndex != 0) {
                                    int leftStartIndex = Math.max(wordCurrentIndex - rule.getRangeLength(), 0);
                                    String leftContent = content.substring(leftStartIndex, wordCurrentIndex);
                                    for (String containerWord : containerWordList) {
                                        if (leftContent.contains(containerWord)) {
                                            isMark = true;
                                            break outerLoop;
                                        }
                                    }
                                }
                                // 判断词语的右边是否包含关联词
                                int rightEndIndex = Math.min(wordCurrentIndex + word.length() + rule.getRangeLength(), content.length());
                                String rightContent = content.substring(wordCurrentIndex + word.length(), rightEndIndex);
                                for (String containerWord : containerWordList) {
                                    if (rightContent.contains(containerWord)) {
                                        isMark = true;
                                        break outerLoop;
                                    }
                                }
                            }
                        }
                    }
                }

                // 截取当前词语索引后面的内容
                wordCurrentIndex = wordCurrentIndex + word.length();
                subContent = content.substring(wordCurrentIndex);

                if (isMark) {
                    // 需要标注
                    RecordNeedsWordVo recordNeedsWordVo = new RecordNeedsWordVo();
                    recordNeedsWordVo.setRecordNeedsNum(countNum);
                    recordNeedsWordVo.setRestrictWord(restrictWord);
                    recordNeedsWordVo.setRestrictRange(restrictRange);
                    recordNeedsWordList.add(recordNeedsWordVo);

                    // 兼容1.8.3版本
                    recordNeedsWordNumList.add(countNum);

                    markNum++;
                }


            } else {
                // 没有规则，直接统计出现次数
                RecordNeedsWordVo recordNeedsWordVo = new RecordNeedsWordVo();
                recordNeedsWordVo.setRecordNeedsNum(markNum);
                recordNeedsWordVo.setRestrictWord("");
                recordNeedsWordVo.setRestrictRange(0);
                recordNeedsWordList.add(recordNeedsWordVo);

                // 兼容1.8.3版本
                recordNeedsWordNumList.add(markNum);

                markNum++;
                subContent = subContent.substring(subContent.indexOf(word) + word.length());
            }

            countNum++;
        }

        // 需要标注的次数大于0才加入词语列表
        if (recordNeedsWordNumList.size() > 0) {
            WordsMarkVo wordsMarkVo = new WordsMarkVo();
            BeanUtils.copyProperties(wordsVo, wordsMarkVo);
            wordsMarkVo.setWordsType(wordsType);
            wordsMarkVo.setCountNum(markNum);
            wordsMarkVo.setRecordNeedsWordList(recordNeedsWordList);
            // 兼容1.8.3版本
            wordsMarkVo.setRecordNeedsWordNumList(recordNeedsWordNumList);

            wordsMarkVo.setName(word);
            wordsMarkVo.setTotalNum(countNum);
            // 添加描述
            if (wordsVo.getParentId().equals(0L) || !StringUtils.isEmpty(wordsVo.getRemarks())) {
                // 是词语，或者描述不为空，直接添加
                wordsMarkVo.setRemarks(wordsVo.getRemarks());
            } else {
                // 是相似词，并且描述为空，将父词语的描述作为添加
                for (SensitiveWordsVo sensitiveWordsVo : allWordsVoList) {
                    if (sensitiveWordsVo.getId().equals(wordsVo.getParentId())) {
                        wordsMarkVo.setRemarks(sensitiveWordsVo.getRemarks());
                    }
                }
            }
            wordsList.add(wordsMarkVo);
        }

    }


    @Override
    public R<PageUtils<SensitiveWordsListVo>> queryPage(SensitiveWordsListBo sensitiveWordsListBo) {

        return sensitiveWordsBll.queryPage(sensitiveWordsListBo);
    }

    @Override
    public R<SensitiveWordsInfoVo> info(Long id) {

        return sensitiveWordsBll.info(id);
    }

    @Override
    public R<List<String>> save(SensitiveWordsBo sensitiveWordsBo) {

        if (sensitiveWordsBo.getResourceType() == 0) {
            UserCacheVo user = GlobalObject.getLocalUser();
            sensitiveWordsBo.setUserId(user.getId());
        } else {
            UserCacheVo user = GlobalObject.getLocalUser();
            R<Long> parentR = userBll.getCacheUserParentId(user.getId());
            if (parentR.getCode() == 0 && !StringUtils.isEmpty(parentR.getData())) {
                sensitiveWordsBo.setUserId(parentR.getData());
            } else {
                sensitiveWordsBo.setUserId(user.getId());
            }
        }


        return sensitiveWordsBll.save(sensitiveWordsBo);
    }

    @Override
    public R<String> update(SensitiveWordsBo sensitiveWordsBo) {

        if (sensitiveWordsBo.getResourceType() == 0) {
            UserCacheVo user = GlobalObject.getLocalUser();
            sensitiveWordsBo.setUserId(user.getId());
        } else {
            UserCacheVo user = GlobalObject.getLocalUser();
            R<Long> parentR = userBll.getCacheUserParentId(user.getId());
            if (parentR.getCode() == 0 && !StringUtils.isEmpty(parentR.getData())) {
                sensitiveWordsBo.setUserId(parentR.getData());
            } else {
                sensitiveWordsBo.setUserId(user.getId());
            }
        }

        return sensitiveWordsBll.update(sensitiveWordsBo);
    }

    @Override
    public R<String> delete(Long id) {

        return sensitiveWordsBll.delete(id);
    }

    @Override
    public R<List<String>> saveBatch(SensitiveWordsBatchBo sensitiveWordsBatchBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        sensitiveWordsBatchBo.setUserId(user.getId());

        return sensitiveWordsBll.saveBatch(sensitiveWordsBatchBo);
    }

    @Override
    public R<BarrageDataVo> videoBarrageData(String videoId) {
        BarrageDataVo result = new BarrageDataVo();
        result.setBarrageDataList(new ArrayList<>());
        result.setTotalBarrageNum(0);
        R<AnchorVideoInfoVo> anchorVideoInfoVoR = anchorVideoBll.infoByVideoId(videoId);
        if (anchorVideoInfoVoR.getCode() != 0 && !ObjectUtil.isNotEmpty(anchorVideoInfoVoR.getData())) {
            return R.ok(result);
        }else if(anchorVideoInfoVoR.getData().getPlatformType().equals(VideoPlatformEnum.KUAISHOU.getCode())) {
            return R.ok(result);
        }
        List<Map<String, Object>> hashMaps = new ArrayList<>();
        Integer totalBarrageNum = null;
        R<AnalysisResultVo> analysisData = sensitiveWordsBll.getAnalysisData(0, videoId);
        if (analysisData.getCode() == 0 && ObjectUtil.isNotEmpty(analysisData.getData())) {
            List<SentenceMarkVo> sentenceMarkVos = analysisData.getData().getSentenceMarkVos();
            hashMaps = tableStoreFeign.getBarrageDataList(videoId, sentenceMarkVos.stream().map(JSONUtil::toJsonStr).toList());
            totalBarrageNum = hashMaps.stream().mapToInt(map -> NumberUtil.parseInt(map.get("barrageNum").toString(), 0)).sum();
        }
        result.setBarrageDataList(hashMaps);

        // 获取弹幕总数据量
        result.setTotalBarrageNum(totalBarrageNum);
        return R.ok(result);
    }

    @Override
    public R<VideoRoiVo> getVideoRoi(String videoId) {
        return R.ok(videoDataViewingBll.getVideoRoi(videoId));
    }
}

