package com.jiuyu.replay.api.logic.ai.placeholder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.jiuyu.replay.ai.entity.ConversationEntity;
import com.jiuyu.replay.ai.repository.service.ConversationService;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.utils.AiUtils;
import com.jiuyu.replay.common.utils.ApplicationContextUtil;
import com.jiuyu.replay.common.utils.ReplayFileUtils;
import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.third.bll.TableStoreBll;
import com.jiuyu.replay.third.vo.DanMuVo;
import com.jiuyu.replay.third.vo.QueryDanMuVo;
import com.jiuyu.replay.words.bll.*;
import com.jiuyu.replay.words.bo.AskRequestBo;
import com.jiuyu.replay.words.producer.AnchorKnowledgeProducer;
import com.jiuyu.replay.words.producer.IndustryKnowledgeProducer;
import com.jiuyu.replay.words.producer.TradeProducer;
import com.jiuyu.replay.words.rse.AiOptimizePurposeRse;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.generic.feign.ai.SceneSliceFeign;
import com.jiuyu.replay.generic.feign.third.GovernanceProductService;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.governance.GovernanceProductItemVo;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineProcessBo;
import com.jiuyu.replay.words.producer.impl.OceanEngineDataProducerImpl;
import com.jiuyu.replay.words.vo.chart.CurveData;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.words.vo.oceanEngineData.JuliangStatisticsVo;
import com.jiuyu.replay.words.vo.peer.DouyinPeerAvgVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 占位符数据协调者 — 持有 AskRequestBo，BLL 按需懒加载，VO 查询结果缓存重用。
 * <p>
 * 数据获取链路：
 * <ul>
 *   <li>sourceType=0（视频）：GetByVideoId → secUid → infoBySecUidOne → tradeId → TradeBll.info</li>
 *   <li>sourceType=1（文件）：infoByFileId → tradeId → TradeBll.info（文件无主播）</li>
 *   <li>sourceType=2（对比）：infoByContrastId → anchorOneId/anchorTwoId（即secUid）、tradeOneId/tradeTwoId 直接可用</li>
 * </ul>
 * 参考 VideoSentenceImpl.getAnchorVideoInfoVo() 的 secUid → AnchorUrlBll 查询模式。
 *
 * @author jy
 * @date 2026-06-18
 */
public class PlaceholderContext {

    private final AskRequestBo askRequestBo;

    // ===== BLL 缓存（懒加载，只 getBean 一次）=====
    private AnchorVideoBll anchorVideoBll;
    private AnchorUrlBll anchorUrlBll;
    private TradeBll tradeBll;
    private SyncContrastBll syncContrastBll;
    private UploadFileBll uploadFileBll;
    private SensitiveWordsBll sensitiveWordsBll;
    private SocketCollectMessageBll socketCollectMessageBll;
    private TableStoreBll tableStoreBll;
    private DataScreenshotBll dataScreenshotBll;
    private VideoDataViewingBll videoDataViewingBll;
    private BasicSettingsBll basicSettingsBll;
    private AiOptimizePurposeRse aiOptimizePurposeRse;
    private ConversationService conversationService;
    private IndustryKnowledgeProducer industryKnowledgeProducer;
    private TradeProducer tradeProducer;
    private AnchorKnowledgeProducer anchorKnowledgeProducer;
    private OceanEngineDataProducerImpl oceanEngineDataProducer;
    private SceneSliceFeign sceneSliceFeign;
    private GovernanceProductService governanceProductService;

    // ===== 单场 VO 缓存（sourceType=0/1）=====
    private AnchorVideoInfoVo singleVideo;
    private UploadFileInfoVo singleFile;

    // ===== 上一场视频缓存 =====
    private AnchorVideoInfoVo prevVideo;
    private boolean prevVideoLoaded;

    // ===== 对比 VO 缓存（sourceType=2）=====
    private SyncContrastInfoVo contrastInfo;

    // ===== 解析结果缓存 =====
    private String tradeName;
    private String platformName;
    private String anchorData;
    private String tradeNameOne;
    private String tradeNameTwo;

    // ===== 分钟段落缓存 =====
    private List<MinuteSegmentVo> minuteSegments;
    private boolean minuteSegmentsLoaded;
    private String minuteSegmentText;

    // ===== 共享弹幕缓存（minuteSegment 分段统计 + danmaku 格式化共用，只查一次）=====
    private List<DanMuVo> barrageMessages;
    private boolean barrageMessagesLoaded;

    // ===== danmaku 缓存 =====
    private String danmaku;
    private boolean danmakuLoaded;

    // ===== liveData 缓存 =====
    private String liveData;
    private boolean liveDataLoaded;
    // ===== optimizePlan 缓存 =====
    private String optimizePlan;
    private boolean optimizePlanLoaded;

    // ===== minuteSegmentDanmaku 缓存 =====
    private String minuteSegmentDanmaku;
    private boolean minuteSegmentDanmakuLoaded;

    // ===== liveKeywords 缓存 =====
    private String liveKeywords;
    private boolean liveKeywordsLoaded;

    // ===== 上一场 PlaceholderContext 缓存（所有 prev* 共享，复用已有解析链路）=====
    private PlaceholderContext prevCtx;
    private boolean prevCtxLoaded;

    // ===== AnalysisResultVo 缓存（分钟段落解析时填充，liveKeywords 复用）=====
    private AnalysisResultVo cachedAnalysis;

    // ===== aiDiagnosis 缓存 =====
    private String aiDiagnosis;
    private boolean aiDiagnosisLoaded;

    // ===== lastAnalysis 缓存 =====
    private String lastAnalysis;
    private boolean lastAnalysisLoaded;

    // ===== lastComprehensiveAnalysis 缓存 =====
    private String lastComprehensiveAnalysis;
    private boolean lastComprehensiveAnalysisLoaded;

    // ===== sceneSlice 缓存 =====
    private String sceneSliceResult;
    private boolean sceneSliceResultLoaded;

    // ===== 看板缓存（audienceProfile + trafficStructure 共用）=====
    private VideoDataViewingConfuseInfoVo cachedBoard;
    private boolean boardLoaded;

    // ===== audienceProfile 缓存 =====
    private String audienceProfile;
    private boolean audienceProfileLoaded;

    // ===== trafficStructure 缓存 =====
    private String trafficStructure;
    private boolean trafficStructureLoaded;

    // ===== tradeSensitiveWords 缓存 =====
    private String tradeSensitiveWords;
    private boolean tradeSensitiveWordsLoaded;

    // ===== tradeKeywords 缓存 =====
    private String tradeKeywords;
    private boolean tradeKeywordsLoaded;

    // ===== 知识库缓存 =====
    private String tradeOperationKnowledge;
    private boolean tradeOperationKnowledgeLoaded;
    private String tradeViolationKnowledge;
    private boolean tradeViolationKnowledgeLoaded;
    private String tradeSensitiveKnowledge;
    private boolean tradeSensitiveKnowledgeLoaded;
    private String anchorKnowledge;
    private boolean anchorKnowledgeLoaded;

    // ===== 主播知识库拆分缓存（共用 AnchorKnowledgeVo，各字段独立）=====
    private AnchorKnowledgeVo cachedAnchorKnowledgeVo;
    private AnchorKnowledgeVo cachedContrastAnchorKnowledgeVo1;
    private AnchorKnowledgeVo cachedContrastAnchorKnowledgeVo2;

    // ===== Top N 缓存 =====
    private String conversionIncreaseTop;
    private int conversionIncreaseTopN = -1;
    private String conversionDecreaseTop;
    private int conversionDecreaseTopN = -1;
    private String uvIncreaseTop;
    private int uvIncreaseTopN = -1;
    private String uvDecreaseTop;
    private int uvDecreaseTopN = -1;
    private String interactIncreaseTop;
    private int interactIncreaseTopN = -1;
    private String interactDecreaseTop;
    private int interactDecreaseTopN = -1;
    private String onlineIncreaseTop;
    private int onlineIncreaseTopN = -1;
    private String onlineDecreaseTop;
    private int onlineDecreaseTopN = -1;
    private String onlineRateIncreaseTop;
    private int onlineRateIncreaseTopN = -1;
    private String onlineRateDecreaseTop;
    private int onlineRateDecreaseTopN = -1;

    // ===== douyinPeerAvg 缓存 =====
    private String douyinPeerAvg;
    private int douyinPeerAvgDays = -1;

    // ===== 加载标记 =====
    private boolean singleVideoLoaded;
    private boolean singleFileLoaded;
    private boolean contrastLoaded;
    private boolean tradeNameLoaded;
    private boolean platformNameLoaded;
    private boolean anchorDataLoaded;

    private static final Logger log = LoggerFactory.getLogger(PlaceholderContext.class);

    /**
     * 构造数据协调者，绑定单次问答请求上下文。
     *
     * @param askRequestBo 问答请求参数，含 sourceType/sourceId/tenantId 等
     */
    public PlaceholderContext(AskRequestBo askRequestBo) {
        this.askRequestBo = askRequestBo;
    }

    // ===== BLL 懒加载 =====

    /**
     * 懒加载 AnchorVideoBll
     */
    private AnchorVideoBll getAnchorVideoBll() {
        if (anchorVideoBll == null) {
            anchorVideoBll = ApplicationContextUtil.getBean(AnchorVideoBll.class);
        }
        return anchorVideoBll;
    }

    /**
     * 懒加载 AnchorUrlBll
     */
    private AnchorUrlBll getAnchorUrlBll() {
        if (anchorUrlBll == null) {
            anchorUrlBll = ApplicationContextUtil.getBean(AnchorUrlBll.class);
        }
        return anchorUrlBll;
    }

    /**
     * 懒加载 TradeBll
     */
    private TradeBll getTradeBll() {
        if (tradeBll == null) {
            tradeBll = ApplicationContextUtil.getBean(TradeBll.class);
        }
        return tradeBll;
    }

    /**
     * 懒加载 SyncContrastBll
     */
    private SyncContrastBll getSyncContrastBll() {
        if (syncContrastBll == null) {
            syncContrastBll = ApplicationContextUtil.getBean(SyncContrastBll.class);
        }
        return syncContrastBll;
    }

    /**
     * 懒加载 UploadFileBll
     */
    private UploadFileBll getUploadFileBll() {
        if (uploadFileBll == null) {
            uploadFileBll = ApplicationContextUtil.getBean(UploadFileBll.class);
        }
        return uploadFileBll;
    }

    /**
     * 懒加载 SensitiveWordsBll
     */
    private SensitiveWordsBll getSensitiveWordsBll() {
        if (sensitiveWordsBll == null) {
            sensitiveWordsBll = ApplicationContextUtil.getBean(SensitiveWordsBll.class);
        }
        return sensitiveWordsBll;
    }

    /**
     * 懒加载 SocketCollectMessageBll
     */
    private SocketCollectMessageBll getSocketCollectMessageBll() {
        if (socketCollectMessageBll == null) {
            socketCollectMessageBll = ApplicationContextUtil.getBean(SocketCollectMessageBll.class);
        }
        return socketCollectMessageBll;
    }

    /**
     * 懒加载 TableStoreBll
     */
    private TableStoreBll getTableStoreBll() {
        if (tableStoreBll == null) {
            tableStoreBll = ApplicationContextUtil.getBean(TableStoreBll.class);
        }
        return tableStoreBll;
    }

    /**
     * 懒加载 DataScreenshotBll
     */
    private DataScreenshotBll getDataScreenshotBll() {
        if (dataScreenshotBll == null) {
            dataScreenshotBll = ApplicationContextUtil.getBean(DataScreenshotBll.class);
        }
        return dataScreenshotBll;
    }

    /**
     * 懒加载 VideoDataViewingBll
     */
    private VideoDataViewingBll getVideoDataViewingBll() {
        if (videoDataViewingBll == null) {
            videoDataViewingBll = ApplicationContextUtil.getBean(VideoDataViewingBll.class);
        }
        return videoDataViewingBll;
    }

    /**
     * 懒加载 BasicSettingsBll
     */
    private BasicSettingsBll getBasicSettingsBll() {
        if (basicSettingsBll == null) {
            basicSettingsBll = ApplicationContextUtil.getBean(BasicSettingsBll.class);
        }
        return basicSettingsBll;
    }

    /**
     * 懒加载 AiOptimizePurposeRse
     */
    private AiOptimizePurposeRse getAiOptimizePurposeRse() {
        if (aiOptimizePurposeRse == null) {
            aiOptimizePurposeRse = ApplicationContextUtil.getBean(AiOptimizePurposeRse.class);
        }
        return aiOptimizePurposeRse;
    }

    /**
     * 懒加载 ConversationService
     */
    private ConversationService getConversationService() {
        if (conversationService == null) {
            conversationService = ApplicationContextUtil.getBean(ConversationService.class);
        }
        return conversationService;
    }

    /**
     * 懒加载 IndustryKnowledgeProducer
     */
    private IndustryKnowledgeProducer getIndustryKnowledgeProducer() {
        if (industryKnowledgeProducer == null) {
            industryKnowledgeProducer = ApplicationContextUtil.getBean(IndustryKnowledgeProducer.class);
        }
        return industryKnowledgeProducer;
    }

    /**
     * 懒加载 TradeProducer
     */
    private TradeProducer getTradeProducer() {
        if (tradeProducer == null) {
            tradeProducer = ApplicationContextUtil.getBean(TradeProducer.class);
        }
        return tradeProducer;
    }

    private DictDataFeign getDictDataFeign() {
        return ApplicationContextUtil.getBean(DictDataFeign.class);
    }

    /**
     * 懒加载 DouyinPeerAvgBll
     */
    private DouyinPeerAvgBll getDouyinPeerAvgBll() {
        return ApplicationContextUtil.getBean(DouyinPeerAvgBll.class);
    }

    private AnchorKnowledgeProducer getAnchorKnowledgeProducer() {
        if (anchorKnowledgeProducer == null) {
            anchorKnowledgeProducer = ApplicationContextUtil.getBean(AnchorKnowledgeProducer.class);
        }
        return anchorKnowledgeProducer;
    }

    /**
     * 懒加载 SceneSliceFeign
     */
    private SceneSliceFeign getSceneSliceFeign() {
        if (sceneSliceFeign == null) {
            sceneSliceFeign = ApplicationContextUtil.getBean(SceneSliceFeign.class);
        }
        return sceneSliceFeign;
    }

    /**
     * 懒加载 GovernanceProductService
     */
    private GovernanceProductService getGovernanceProductService() {
        if (governanceProductService == null) {
            governanceProductService = ApplicationContextUtil.getBean(GovernanceProductService.class);
        }
        return governanceProductService;
    }

    private OceanEngineDataProducerImpl getOceanEngineDataProducer() {
        if (oceanEngineDataProducer == null) {
            oceanEngineDataProducer = ApplicationContextUtil.getBean(OceanEngineDataProducerImpl.class);
        }
        return oceanEngineDataProducer;
    }

    // ===== VO 缓存 =====

    /**
     * 获取单场视频信息（sourceType=0），缓存复用
     */
    private AnchorVideoInfoVo getSingleVideo() {
        if (!singleVideoLoaded) {
            singleVideoLoaded = true;
            if (askRequestBo.getSourceType() != null && askRequestBo.getSourceType() == 0
                    && askRequestBo.getSourceId() != null) {
                AnchorVideoInfoVo video = getAnchorVideoBll().GetByVideoId(askRequestBo.getSourceId()).getData();
                this.singleVideo = video;
            }
        }
        return singleVideo;
    }

    /**
     * 获取单场文件信息（sourceType=1），缓存复用
     */
    private UploadFileInfoVo getSingleFile() {
        if (!singleFileLoaded) {
            singleFileLoaded = true;
            if (askRequestBo.getSourceType() != null && askRequestBo.getSourceType() == 1
                    && askRequestBo.getSourceId() != null) {
                UploadFileInfoVo file = getUploadFileBll().infoByFileId(askRequestBo.getSourceId()).getData();
                this.singleFile = file;
            }
        }
        return singleFile;
    }

    /**
     * 查询上一场视频（同主播、同用户、同租户，startTime 在本场之前且分析完成的最近一场）。
     * 仅 sourceType=0（视频）支持，文件无主播信息。结果缓存复用。
     */
    private AnchorVideoInfoVo getPreviousVideo() {
        if (!prevVideoLoaded) {
            prevVideoLoaded = true;
            AnchorVideoInfoVo current = getSingleVideo();
            if (current != null
                    && StrUtil.isNotBlank(current.getSecUid())
                    && current.getStartTime() != null
                    && current.getUserId() != null) {
                prevVideo = getAnchorVideoBll().getPrevVideo(
                        current.getSecUid(),
                        current.getUserId(),
                        askRequestBo.getTenantId(),
                        current.getStartTime());
            }
        }
        return prevVideo;
    }

    /**
     * 获取对比记录（sourceType=2），缓存复用
     */
    private SyncContrastInfoVo getContrastInfo() {
        if (!contrastLoaded) {
            contrastLoaded = true;
            if (isContrast() && askRequestBo.getSourceId() != null) {
                SyncContrastInfoVo contrast = getSyncContrastBll().infoByContrastId(askRequestBo.getSourceId()).getData();
                this.contrastInfo = contrast;
            }
        }
        return contrastInfo;
    }

    // ===== 基础字段（优先级 0）=====

    /**
     * 行业名称。单场查 TradeBll；对比两场相同→"都是XX行业"，不同→分列
     */
    public String getTradeName() {
        if (!tradeNameLoaded) {
            tradeNameLoaded = true;
            if (isContrast()) {
                tradeName = resolveContrastTrade();
            } else {
                tradeName = resolveSingleTrade();
            }
        }
        return ObjectUtil.defaultIfEmpty(tradeName, "某行业");
    }

    /**
     * 平台名称。单场从视频/文件动态获取，对比从对比记录获取
     */
    public String getPlatformName() {
        if (!platformNameLoaded) {
            platformNameLoaded = true;
            if (isContrast()) {
                platformName = resolveContrastPlatform();
            } else {
                platformName = resolveSinglePlatform();
            }
        }
        return ObjectUtil.defaultIfEmpty(platformName, "抖音");
    }

    /**
     * 主播数据，调用 AnchorUrlBll.getAiAnchorPrompt 生成完整主播提示词（已内置 sourceType 0/1/2 处理）
     */
    public String getAnchorData() {
        if (!anchorDataLoaded) {
            anchorDataLoaded = true;
            anchorData = getAnchorUrlBll().getAiAnchorPrompt(askRequestBo);
            String roiSegment = computeAnchorRoi();
            if (StrUtil.isNotEmpty(roiSegment)) {
                anchorData = (StrUtil.isNotEmpty(anchorData) ? anchorData : "") + "\n" + roiSegment;
            }
        }
        return ObjectUtil.defaultIfEmpty(anchorData, "");
    }

    /**
     * 计算直播分段ROI，格式化输出"0-5分钟roi：x.xx，5-10分钟roi：x.xx..."
     * 入口分发：sourceType=0 单场 / sourceType=2 对比 / sourceType=1 跳过
     */
    private String computeAnchorRoi() {
        Integer sourceType = askRequestBo.getSourceType();
        if (sourceType == null) return null;

        if (sourceType == 0) {
            // 单场视频
            AnchorVideoInfoVo video = getSingleVideo();
            if (video == null) return null;
            String roiText = computeRoiForVideo(video.getVideoId(), video.getSecUid(),
                    video.getUserId(), video.getTenantId());
            if (StrUtil.isNotEmpty(roiText)) {
                return "本场直播ROI：" + roiText;
            }
        } else if (sourceType == 2) {
            // 对比模式：两个视频分别计算
            SyncContrastInfoVo contrast = getContrastInfo();
            if (contrast == null) return null;

            List<String> parts = new ArrayList<>();
            if (StrUtil.isNotEmpty(contrast.getVideoOneId())) {
                AnchorVideoInfoVo video1 = getAnchorVideoBll().GetByVideoId(contrast.getVideoOneId()).getData();
                if (video1 != null) {
                    String roi1 = computeRoiForVideo(contrast.getVideoOneId(), video1.getSecUid(),
                            video1.getUserId(), video1.getTenantId());
                    if (StrUtil.isNotEmpty(roi1)) {
                        parts.add("视频1的ROI：" + roi1);
                    }
                }
            }
            if (StrUtil.isNotEmpty(contrast.getVideoTwoId())) {
                AnchorVideoInfoVo video2 = getAnchorVideoBll().GetByVideoId(contrast.getVideoTwoId()).getData();
                if (video2 != null) {
                    String roi2 = computeRoiForVideo(contrast.getVideoTwoId(), video2.getSecUid(),
                            video2.getUserId(), video2.getTenantId());
                    if (StrUtil.isNotEmpty(roi2)) {
                        parts.add("视频2的ROI：" + roi2);
                    }
                }
            }
            if (!parts.isEmpty()) {
                return String.join("\n", parts);
            }
        }
        // sourceType=1 文件模式，无直播数据
        return null;
    }

    /**
     * 对单个视频按roiAccuracy粒度分段计算净成交ROI，数据为累计值，每段用终点值减起点值
     */
    private String computeRoiForVideo(String videoId, String secUid, Long userId, Long tenantId) {
        // 获取roiAccuracy粒度，默认15分钟
        int granularity = 15;
        try {
            BasicSettingsVo basicSettings = getBasicSettingsBll().getBySourceUser(secUid,
                    WordsEnum.basicSettingsType.ANCHOR.getCode(), userId, tenantId);
            if (basicSettings != null) {
                // 只有自有账号且流量结构不为1才计算ROI
                if (!Integer.valueOf(WordsEnum.accountType.OWN.getCode()).equals(basicSettings.getAccountType())
                        || Integer.valueOf(1).equals(basicSettings.getAccountFlow())) {
                    return null;
                }
                granularity = NumberUtil.parseInt(basicSettings.getRoiAccuracy(), 0);
                if (granularity < 1) {
                    granularity = 15;
                }
            }
        } catch (Exception e) {
            log.warn("获取roiAccuracy失败，videoId={}，使用默认15分钟", videoId, e);
        }

        // 获取巨量引擎实时数据
        VideoDataViewingConfuseInfoVo data = getVideoDataViewingBll().getOceanEngineDetailsByVideoId(videoId);
        if (data == null || CollUtil.isEmpty(data.getOceanEngineProcessList())) {
            return null;
        }

        List<OceanEngineProcessBo> processList = new ArrayList<>(data.getOceanEngineProcessList());
        processList.sort(Comparator.comparing(OceanEngineProcessBo::getGatherTimeStamp,
                Comparator.nullsLast(Comparator.naturalOrder())));

        // 获取视频时长和起始时间
        AnchorVideoInfoVo video = getAnchorVideoBll().GetByVideoId(videoId).getData();
        if (video == null || video.getDuration() == null || video.getStartTime() == null) {
            return null;
        }

        long videoStartMs = video.getStartTime().getTime();
        long durationMs = video.getDuration() * 1000L;
        long segmentMs = granularity * 60 * 1000L;

        StringBuilder sb = new StringBuilder();
        boolean hasAny = false;

        for (long segStart = 0; segStart < durationMs; segStart += segmentMs) {
            long segEnd = segStart + segmentMs;
            long segStartAbs = videoStartMs + segStart;
            long segEndAbs = videoStartMs + segEnd;

            // 找 ≤ 边界时间戳 最近的一条数据
            OceanEngineProcessBo startPoint = findClosestBefore(processList, segStartAbs);
            OceanEngineProcessBo endPoint = findClosestBefore(processList, segEndAbs);

            if (endPoint == null) {
                continue;
            }
            // 第一段无基线时，取第一条数据作为起点
            if (startPoint == null) {
                if (segStart == 0) {
                    startPoint = processList.get(0);
                } else {
                    continue;
                }
            }

            double segPayAmt = toDoubleValue(endPoint.getPayAmt()) - toDoubleValue(startPoint.getPayAmt());
            double segRefundAmt = toDoubleValue(endPoint.getRefundAmt()) - toDoubleValue(startPoint.getRefundAmt());
            double segCost = toDoubleValue(endPoint.getQianchuanCost()) - toDoubleValue(startPoint.getQianchuanCost());

            if (segCost == 0) {
                continue;
            }

            // 净成交ROI = (销售额 - 退款) / 投放消耗（三者单位均为分）
            double roi = (segPayAmt - segRefundAmt) / segCost;

            int segStartMin = (int) (segStart / 60000);
            int segEndMin = (int) (segEnd / 60000);

            if (hasAny) {
                sb.append("，");
            }
            sb.append(segStartMin).append("-").append(segEndMin).append("分钟roi：")
                    .append(String.format("%.2f", roi));
            hasAny = true;
        }

        return hasAny ? sb.toString() : null;
    }

    /**
     * 在已排序列表中找 ≤ targetMs 的最新一条数据
     */
    private OceanEngineProcessBo findClosestBefore(List<OceanEngineProcessBo> list, long targetMs) {
        OceanEngineProcessBo closest = null;
        for (OceanEngineProcessBo bo : list) {
            if (bo.getGatherTimeStamp() == null) continue;
            if (bo.getGatherTimeStamp() <= targetMs) {
                if (closest == null || bo.getGatherTimeStamp() > closest.getGatherTimeStamp()) {
                    closest = bo;
                }
            }
        }
        return closest;
    }

    /**
     * 安全转换为double值
     */
    private double toDoubleValue(Object val) {
        if (val instanceof BigDecimal) return ((BigDecimal) val).doubleValue();
        if (val instanceof Integer) return ((Integer) val).doubleValue();
        if (val == null) return 0;
        return 0;
    }

    // ===== 请求参数 =====

    /**
     * 获取请求中的违规原因（用户自行填写的违规描述）
     */
    public String getReasonViolation() {
        return askRequestBo != null ? askRequestBo.getReasonViolation() : null;
    }

    /**
     * 获取当前租户 ID
     */
    public Long getTenantId() {
        return askRequestBo != null ? askRequestBo.getTenantId() : null;
    }

    /**
     * 获取原始问答请求对象
     */
    public AskRequestBo getAskRequestBo() {
        return askRequestBo;
    }

    // ===== 共享弹幕缓存（优先级 10 入口，minuteSegment + danmaku 共用）=====

    /**
     * 全量弹幕消息列表 — minuteSegment 分段统计弹幕条数 + danmaku 格式化输出，
     * 共享同一份数据，只查一次 TableStoreBll.queryDanMuSearchData。
     */
    private List<DanMuVo> getBarrageMessages() {
        if (!barrageMessagesLoaded) {
            barrageMessagesLoaded = true;
            barrageMessages = resolveBarrageMessages();
        }
        return barrageMessages;
    }

    /**
     * 解析弹幕消息（入口分发）。当前仅 sourceType=0（视频）有弹幕数据，
     * 文件类型和对比主入口不走此方法。
     */
    private List<DanMuVo> resolveBarrageMessages() {
        if (askRequestBo.getSourceType() == null || askRequestBo.getSourceType() != 0
                || askRequestBo.getSourceId() == null) return null;
        return getBarrageMessagesForVideo(askRequestBo.getSourceId());
    }

    /**
     * 按 videoId 查弹幕消息（供对比模式各侧独立调用，不走 singleVideo 缓存）。
     */
    private List<DanMuVo> getBarrageMessagesForVideo(String videoId) {
        if (videoId == null) return null;
        AnchorVideoInfoVo video = getAnchorVideoBll().GetByVideoId(videoId).getData();
        // 不是抖音的不查询弹幕
        if (video == null || video.getStartTime() == null || !ObjectUtil.equal(video.getPlatformType(), String.valueOf(WordsEnum.platformType.DOU_YIN.getCode()))) {
            return null;
        }

        QueryDanMuBo bo = new QueryDanMuBo();
        bo.setTenantId(askRequestBo.getTenantId());
        bo.setUserId(video.getUserId());
        bo.setBatchNumber(video.getBatchNumber() != null ? video.getBatchNumber().toString() : null);
        bo.setVideoId(video.getVideoId());
        bo.setLimit(-1);
        bo.setPage(1);
        bo.setQueryType(1);
        long videoStart = video.getStartTime().getTime();
        bo.setStartTime(videoStart);
        bo.setEndTime(video.getEndTime() != null ? video.getEndTime().getTime() : videoStart + 3600000L);
        bo.setImportant(0);

        QueryDanMuVo result = getTableStoreBll().queryDanMuSearchData(bo).getData();
        if (result == null || CollUtil.isEmpty(result.getList())) return null;
        return result.getList();
    }

    // ===== minuteSegment（优先级 10）=====

    /**
     * 获取结构化分钟段落列表（缓存复用），供后续数值计算使用
     */
    public List<MinuteSegmentVo> getMinuteSegments() {
        if (!minuteSegmentsLoaded) {
            minuteSegmentsLoaded = true;
            resolveMinuteSegments();
        }
        return minuteSegments;
    }

    /**
     * 获取格式化后的分钟段落文本（缓存复用），用于拼入提示词
     */
    public String getMinuteSegment() {
        if (!minuteSegmentsLoaded) {
            minuteSegmentsLoaded = true;
            resolveMinuteSegments();
        }
        return minuteSegmentText;
    }

    /**
     * 弹幕的"文本内的参数说明"，帮助 AI 理解弹幕数据格式
     */
    public String getDanmakuParamDesc() {
        return "文本内的参数说明：\n"
                + "一条完整的弹幕为一行，数据格式为：弹幕发送时间(HH:mm:ss) 用户昵称(新,UL:用户等级,FL:粉丝团等级)：弹幕内容\n"
                + "弹幕格式中如果有\"新\"字则是新用户，没有就是老用户\n"
                + "UL是用户等级的简称，FL是用户粉丝团的简称\n"
                + "用户昵称是否隐藏判断：金***、建***、青***这种类型的昵称都是隐藏的，其他格式的都是不隐藏的\n\n";
    }

    /**
     * 分钟段落+弹幕的"文本内的参数说明"，综合段落字段 + 弹幕格式
     */
    public String getMinuteSegmentDanmakuParamDesc() {
        return "文本内的参数说明：\n"
                + "在线人数：指的是转译成的文字段落开始时对应的直播间在线人数。\n"
                + "语速：指的是1分钟内说的字数。\n"
                + "发弹幕条数：指的是转译成的文字段落开始时间到结束时间内直播间发送弹幕数量。\n"
                + "成交人数：指的是转译成的文字段落开始时间到结束时间内商品的成交人数。\n"
                + "互动率：指的是转译成的文字段落开始时间到结束时间内弹幕数量/在线人数得出的互动率。\n"
                + "成交率：指的是转译成的文字段落开始时间到结束时间内成交人数/在线人数得出的成交率。\n"
                + "销售额：指的是转译成的文字段落开始时间到结束时间内的销售额。\n"
                + "UV价值：指的是转译成的文字段落开始时间到结束时间内销售额/在线人数得出的UV价值。\n"
                + "一条完整的弹幕为一行，数据格式为：弹幕发送时间(HH:mm:ss) 用户昵称(新,UL:用户等级,FL:粉丝团等级)：弹幕内容\n"
                + "弹幕格式中如果有\"新\"字则是新用户，没有就是老用户\n"
                + "UL是用户等级的简称，FL是用户粉丝团的简称\n\n";
    }

    /**
     * 分钟段落的"文本内的参数说明"，帮助 AI 理解各字段含义
     */
    public String getMinuteSegmentParamDesc() {
        return "文本内的参数说明：\n"
                + "本段在线人数：指的是转译成的文字段落开始时对应的直播间在线人数。\n"
                + "本段语速：指的是1分钟内说的字数。\n"
                + "本段发弹幕条数：指的是转译成的文字段落开始时间到结束时间内直播间发送弹幕数量。\n"
                + "本段成交人数：指的是转译成的文字段落开始时间到结束时间内商品的成交人数。\n"
                + "本段互动率：指的是转译成的文字段落开始时间到结束时间内弹幕数量/在线人数得出的互动率。\n"
                + "本段成交率：指的是转译成的文字段落开始时间到结束时间内成交人数/在线人数得出的成交率。\n"
                + "本段销售额：指的是转译成的文字段落开始时间到结束时间内的销售额。\n"
                + "本段UV价值：指的是转译成的文字段落开始时间到结束时间内销售额/在线人数得出的UV价值。\n"
                + "本段投放消耗：指的是转译成的文字段落开始时间到结束时间内的千川广告投放消耗。\n"
;
    }

    /**
     * 分钟段落解析入口，按 sourceType 分发：
     * 0→视频（含在线人数/弹幕/巨量数据）、1→文件（仅转录文本）、2→对比（两侧分别解析后拼接）。
     * 结果缓存到 minuteSegments（结构化）和 minuteSegmentText（格式化文本）。
     */
    private void resolveMinuteSegments() {
        Integer sourceType = askRequestBo.getSourceType();
        String sourceId = askRequestBo.getSourceId();
        if (sourceType == null || sourceId == null) return;

        List<MinuteSegmentVo> list;
        if (sourceType == 0) {
            AnchorVideoInfoVo video = getSingleVideo();
            list = resolveMinuteSegmentsVideo(sourceId, video, getBarrageMessages());
        } else if (sourceType == 1) {
            list = resolveMinuteSegmentsFile(sourceId);
        } else if (sourceType == 2) {
            SyncContrastInfoVo contrast = getContrastInfo();
            if (contrast == null) return;
            List<MinuteSegmentVo> side1 = resolveMinuteSegmentsForSide(
                    contrast.getVideoOneId(), contrast.getFileOneId());
            List<MinuteSegmentVo> side2 = resolveMinuteSegmentsForSide(
                    contrast.getVideoTwoId(), contrast.getFileTwoId());
            list = new ArrayList<>();
            if (CollUtil.isNotEmpty(side1)) list.addAll(side1);
            if (CollUtil.isNotEmpty(side2)) list.addAll(side2);
            this.minuteSegments = list.isEmpty() ? null : list;
            this.minuteSegmentText = formatContrastMinuteSegments(side1, side2);
            return;
        } else {
            return;
        }

        this.minuteSegments = list;
        this.minuteSegmentText = formatMinuteSegments(list);
    }

    /**
     * sourceType=0：视频分钟段落，含在线人数/弹幕/巨量数据
     */
    private List<MinuteSegmentVo> resolveMinuteSegmentsVideo(String sourceId, AnchorVideoInfoVo video,
                                                             List<DanMuVo> barrageData) {
        if (video == null || video.getStartTime() == null) return null;

        SensitiveWordsBll swBll = getSensitiveWordsBll();
        String downloadUrl = swBll.getAnalysisDownloadUrl(0, sourceId);
        if (StrUtil.isBlank(downloadUrl)) return null;
        String jsonContent = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);
        if (StrUtil.isBlank(jsonContent)) return null;
        AnalysisResultVo dto = JSONObject.parseObject(jsonContent, AnalysisResultVo.class);
        if (dto == null || CollUtil.isEmpty(dto.getSentenceMarkVos())) return null;
        this.cachedAnalysis = dto;

        long allStartTime = video.getStartTime().getTime();

        List<SentenceMarkVo> vos = dto.getSentenceMarkVos().stream()
                .sorted(Comparator.comparingInt(SentenceMarkVo::getCurrentSort))
                .collect(Collectors.toList());

        List<OnlineNumInfoVo> onlineNumList = null;
        if (video.getBatchNumber() != null) {
            onlineNumList = getSocketCollectMessageBll()
                    .getOnlineNumList(video.getBatchNumber().toString(), video.getUserId(), video.getVideoId())
                    .getData();
        }

        Map<Long, OceanEngineProcessBo> juliangDataMap = getJuliangDataMap(sourceId, vos, allStartTime);

        return buildMinuteSegmentVoList(vos, allStartTime, onlineNumList, barrageData, juliangDataMap);
    }

    /**
     * sourceType=1：文件分钟段落，无在线人数/弹幕/巨量数据
     */
    private List<MinuteSegmentVo> resolveMinuteSegmentsFile(String sourceId) {
        SensitiveWordsBll swBll = getSensitiveWordsBll();
        String downloadUrl = swBll.getAnalysisDownloadUrl(1, sourceId);
        if (StrUtil.isBlank(downloadUrl)) return null;
        String jsonContent = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);
        if (StrUtil.isBlank(jsonContent)) return null;
        AnalysisResultVo dto = JSONObject.parseObject(jsonContent, AnalysisResultVo.class);
        if (dto == null || CollUtil.isEmpty(dto.getSentenceMarkVos())) return null;
        this.cachedAnalysis = dto;

        List<SentenceMarkVo> vos = dto.getSentenceMarkVos().stream()
                .sorted(Comparator.comparingInt(SentenceMarkVo::getCurrentSort))
                .collect(Collectors.toList());

        return buildMinuteSegmentVoList(vos, null, null, null, null);
    }

    /**
     * 对比单侧分钟段落解析
     */
    private List<MinuteSegmentVo> resolveMinuteSegmentsForSide(String videoId, String fileId) {
        if (StrUtil.isNotBlank(videoId)) {
            AnchorVideoInfoVo video = getAnchorVideoBll().GetByVideoId(videoId).getData();
            List<DanMuVo> barrage = getBarrageMessagesForVideo(videoId);
            return resolveMinuteSegmentsVideo(videoId, video, barrage);
        } else if (StrUtil.isNotBlank(fileId)) {
            return resolveMinuteSegmentsFile(fileId);
        }
        return null;
    }

    /**
     * 核心：从已准备的原始数据构建 MinuteSegmentVo 列表。
     * allStartTime 为 null 时（文件类型）不设置 startVideoTime/endVideoTime/startTime/endTime，
     * 不匹配在线人数/弹幕/巨量数据。
     */
    private List<MinuteSegmentVo> buildMinuteSegmentVoList(
            List<SentenceMarkVo> vos, Long allStartTime,
            List<OnlineNumInfoVo> onlineNumList, List<DanMuVo> barrageData,
            Map<Long, OceanEngineProcessBo> juliangDataMap) {
        List<MinuteSegmentVo> list = new ArrayList<>();
        int onlineIdx = 0;
        String onlineNum = "0";
        for (int i = 0; i < vos.size(); i++) {
            SentenceMarkVo item = vos.get(i);
            if (item == null) continue;
            if (CollUtil.isEmpty(item.getItems())) {
                if (StrUtil.isNotBlank(item.getContent())) {
                    MinuteSegmentVo seg = new MinuteSegmentVo();
                    seg.setIndex(i + 1);
                    seg.setContent(item.getContent());
                    list.add(seg);
                }
                continue;
            }
            Long startTemp = item.getItems().get(0).getStartTime();
            Long endTemp = item.getItems().get(item.getItems().size() - 1).getEndTime();

            if (i != vos.size() - 1) {
                SentenceMarkVo next = vos.get(i + 1);
                if (next != null && CollUtil.isNotEmpty(next.getItems())
                        && next.getItems().get(0).getStartTime() != null) {
                    endTemp = next.getItems().get(0).getStartTime();
                }
            }

            if (startTemp == null || endTemp == null) continue;

            MinuteSegmentVo seg = new MinuteSegmentVo();
            seg.setIndex(i + 1);
            seg.setStartTempTime(startTemp);
            seg.setEndTempTime(endTemp);
            if (allStartTime != null) {
                seg.setStartVideoTime(allStartTime + startTemp);
                seg.setEndVideoTime(allStartTime + endTemp);
                seg.setStartTime(new Date(allStartTime + startTemp));
                seg.setEndTime(new Date(allStartTime + endTemp));
            }
            seg.setTextStart(DateUtil.format(new DateTime(startTemp).offset(DateField.HOUR, -8), "HH:mm:ss"));
            seg.setTextEnd(DateUtil.format(new DateTime(endTemp).offset(DateField.HOUR, -8), "HH:mm:ss"));
            seg.setContent(ObjectUtil.defaultIfEmpty(item.getContent(), ""));

            if (allStartTime != null && CollUtil.isNotEmpty(onlineNumList)) {
                for (int j = onlineIdx; j < onlineNumList.size(); j++) {
                    OnlineNumInfoVo online = onlineNumList.get(j);
                    if (online.getRecordDate() == null) continue;
                    Date dt = DateUtil.parse(online.getRecordDate());
                    if (dt.compareTo(seg.getEndTime()) >= 0) {
                        break;
                    }else {
                        onlineNum = online.getPeopleNum();
                        onlineIdx = j;
                    }
                }
                seg.setOnlineNum(onlineNum);
            }

            if (i > 0 && CollUtil.isNotEmpty(list)) {
                int prevNum = NumberUtil.parseInt(ObjectUtil.defaultIfNull(list.get(i - 1).getOnlineNum(), "0"), 0);
                int currNum = NumberUtil.parseInt(ObjectUtil.defaultIfNull(seg.getOnlineNum(), "0"), 0);
                seg.setOnlineNumChange(Math.abs(currNum - prevNum));
                seg.setOnlineNumTrend(currNum >= prevNum ? "增加" : "减少");
            } else {
                int currNum = NumberUtil.parseInt(ObjectUtil.defaultIfNull(seg.getOnlineNum(), "0"), 0);
                seg.setOnlineNumChange(currNum);
                seg.setOnlineNumTrend("增加");
            }

            String contentNoPunct = removePunctuation(seg.getContent());
            double durationSec = (double) (endTemp - startTemp) / 1000;
            if (durationSec > 0) {
                seg.setSpeechSpeed(Math.round(contentNoPunct.length() / durationSec * 60));
            }

            if (allStartTime != null && CollUtil.isNotEmpty(barrageData)) {
                long segStartMs = seg.getStartVideoTime();
                long segEndMs = seg.getEndVideoTime();
                int barrageCount = 0;
                for (DanMuVo dm : barrageData) {
                    if (dm.getRecordDate() == null) continue;
                    if (dm.getRecordDate() > segEndMs) break;
                    if (dm.getRecordDate() >= segStartMs) barrageCount++;
                }
                seg.setBarrageNum(barrageCount);
            }

            if (allStartTime != null && juliangDataMap != null) {
                OceanEngineProcessBo bo = juliangDataMap.get(seg.getStartVideoTime());
                int renShuInt = NumberUtil.parseInt(ObjectUtil.defaultIfNull(seg.getOnlineNum(), "0"), 0);
                if (bo != null) {
                    seg.setDealCount(bo.getPayComboCnt());
                    seg.setSales(bo.getPayAmt() != null
                            ? NumberUtil.round((double) bo.getPayAmt() / 100, 2).doubleValue()
                            : null);
                    if (bo.getPayComboCnt() != null && renShuInt > 0) {
                        seg.setDealRate(NumberUtil.round((double) bo.getPayComboCnt() / renShuInt * 100, 2).doubleValue());
                    }
                    if (bo.getPayAmt() != null && renShuInt > 0) {
                        seg.setUvValue(NumberUtil.round((double) bo.getPayAmt() / 100 / renShuInt, 2).doubleValue());
                    }
                    if (bo.getQianchuanCost() != null) {
                        seg.setQianchuanCost(NumberUtil.round(bo.getQianchuanCost().doubleValue() / 100, 2).doubleValue());
                        BigDecimal netAmt = BigDecimal.valueOf(bo.getPayAmt() != null ? bo.getPayAmt() : 0)
                                .subtract(bo.getRefundAmt() != null ? bo.getRefundAmt() : BigDecimal.ZERO);
                        if (bo.getQianchuanCost().compareTo(BigDecimal.ZERO) > 0 && netAmt.compareTo(BigDecimal.ZERO) > 0) {
                            seg.setNetTransactionRoi(netAmt.divide(bo.getQianchuanCost(), 4, RoundingMode.HALF_UP)
                                    .setScale(2, RoundingMode.HALF_UP).doubleValue());
                        }
                    }
                }
                if (seg.getBarrageNum() > 0 && renShuInt > 0) {
                    seg.setInteractionRate(NumberUtil.round((double) seg.getBarrageNum() / renShuInt * 100, 2).doubleValue());
                }
            }

            list.add(seg);
        }
        return list.isEmpty() ? null : list;
    }

    /**
     * 对比模式分钟段落格式化：两侧分别格式化后加"场次1"/"场次2"标签拼接
     */
    private String formatContrastMinuteSegments(List<MinuteSegmentVo> side1, List<MinuteSegmentVo> side2) {
        StringBuilder sb = new StringBuilder();
        if (CollUtil.isNotEmpty(side1)) {
            sb.append("场次1：\n").append(formatMinuteSegments(side1));
        }
        if (CollUtil.isNotEmpty(side2)) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("场次2：\n").append(formatMinuteSegments(side2));
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * 单侧分钟段落格式化：将 MinuteSegmentVo 列表转为 AI 可读的多行文本，
     * 每段包含时间、在线人数、语速、弹幕条数、成交/互动率等数据 + 段落内容。
     */
    private String formatMinuteSegments(List<MinuteSegmentVo> list) {
        if (CollUtil.isEmpty(list)) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            MinuteSegmentVo seg = list.get(i);
            if (i > 0) sb.append("\n\n");

            if (seg.getTextStart() == null) {
                sb.append(StrUtil.format("段落{}内容：{}", seg.getIndex(),
                        ObjectUtil.defaultIfEmpty(seg.getContent(), "")));
            } else {
                StringBuilder timeSection = new StringBuilder();
                timeSection.append("开始时间：").append(ObjectUtil.defaultIfNull(seg.getTextStart(), ""))
                        .append("，结束时间：").append(ObjectUtil.defaultIfNull(seg.getTextEnd(), ""));
                if (seg.getStartTime() != null) {
                    timeSection.append("，自然时间：").append(DateUtil.format(seg.getStartTime(), "HH:mm:ss"));
                }
                sb.append(StrUtil.format("段落{}时间:（{}）\n", seg.getIndex(), timeSection));

                StringBuilder dataSection = new StringBuilder();
                String onlineNum = ObjectUtil.defaultIfNull(seg.getOnlineNum(), "0");
                dataSection.append("在线人数：").append(onlineNum);
                if (seg.getIndex() > 1 || seg.getOnlineNumChange() > 0) {
                    dataSection.append("，比上段").append(seg.getOnlineNumTrend())
                            .append(seg.getOnlineNumChange()).append("人");
                }
                dataSection.append("，语速：").append((int) seg.getSpeechSpeed()).append("字/分钟");
                dataSection.append("，弹幕条数：").append(seg.getBarrageNum()).append("条");
                if (seg.getDealCount() != null) {
                    dataSection.append("，成交人数：").append(seg.getDealCount());
                }
                if (seg.getInteractionRate() != null) {
                    dataSection.append("，互动率：").append(seg.getInteractionRate()).append("%");
                }
                if (seg.getDealRate() != null) {
                    dataSection.append("，成交率：").append(seg.getDealRate()).append("%");
                }
                if (seg.getSales() != null) {
                    dataSection.append("，销售额：").append(seg.getSales());
                }
                if (seg.getUvValue() != null) {
                    dataSection.append("，uv价值：").append(seg.getUvValue());
                }
                if (seg.getQianchuanCost() != null) {
                    dataSection.append("，投放消耗：").append(seg.getQianchuanCost());
                }
                sb.append(StrUtil.format("段落{}数据:（{}）\n", seg.getIndex(), dataSection));

                sb.append(StrUtil.format("段落{}内容：{}", seg.getIndex(), seg.getContent()));
            }
        }
        return sb.toString();
    }

    // ===== danmaku（优先级 10）=====

    /**
     * 获取格式化后的弹幕文本（缓存复用），sourceType=1（文件）返回 null
     */
    public String getDanmaku() {
        if (!danmakuLoaded) {
            danmakuLoaded = true;
            danmaku = resolveDanmaku();
        }
        return danmaku;
    }

    /**
     * 弹幕解析入口，按 sourceType 分发：
     * 0→视频弹幕（可按 timeRange 筛选）、1→无（文件无弹幕）、2→对比（两侧分别格式化后拼接）。
     */
    private String resolveDanmaku() {
        Integer sourceType = askRequestBo.getSourceType();
        String sourceId = askRequestBo.getSourceId();
        if (sourceType == null || sourceId == null) return null;

        if (sourceType == 0) {
            return resolveDanmakuVideo(getSingleVideo(), getBarrageMessages(),
                    askRequestBo.getVideoTimeOneList());
        } else if (sourceType == 1) {
            return null; // 文件无弹幕数据
        } else if (sourceType == 2) {
            return resolveDanmakuContrast();
        }
        return null;
    }

    /**
     * sourceType=0：视频弹幕格式化
     */
    private String resolveDanmakuVideo(AnchorVideoInfoVo video, List<DanMuVo> allMessages,
                                       List<Long> timeRange) {
        if (video == null || video.getStartTime() == null) return null;
        if (CollUtil.isEmpty(allMessages)) return null;

        long videoStart = video.getStartTime().getTime();

        long filterStart, filterEnd;
        if (CollUtil.isNotEmpty(timeRange) && timeRange.size() >= 2) {
            filterStart = videoStart + timeRange.get(0);
            filterEnd = videoStart + timeRange.get(1);
        } else {
            filterStart = videoStart;
            filterEnd = video.getEndTime() != null ? video.getEndTime().getTime() : videoStart + 3600000L;
        }

        return formatDanmakuText(allMessages, videoStart, filterStart, filterEnd);
    }

    /**
     * 弹幕格式化核心逻辑
     */
    private String formatDanmakuText(List<DanMuVo> allMessages, long videoStart,
                                     long filterStart, long filterEnd) {
        StringBuilder sb = new StringBuilder();
        for (DanMuVo dm : allMessages) {
            if (dm.getRecordDate() == null) continue;
            if (dm.getRecordDate() < filterStart) continue;
            if (dm.getRecordDate() > filterEnd) break;

            long time1 = dm.getRecordDate() - videoStart - TimeUnit.HOURS.toMillis(8);
            String timeStr = DateUtil.format(new Date(time1), "HH:mm:ss");

            String name = StrUtil.isNotBlank(dm.getNickName()) ? dm.getNickName() : "";

            List<String> attrs = new ArrayList<>();
            if (dm.getIsNew() != null && dm.getIsNew()) {
                attrs.add("新");
            }
            if (dm.getLevel() != null && dm.getLevel() > 0) {
                attrs.add("UL:" + dm.getLevel());
            }
            if (dm.getFansLevelCurrent() != null && dm.getFansLevelCurrent() > 0) {
                attrs.add("FL:" + dm.getFansLevelCurrent());
            }
            String attrStr = attrs.isEmpty() ? "" : "(" + String.join("，", attrs) + ")";

            sb.append(timeStr).append(" ");
            if (StrUtil.isNotBlank(name)) {
                sb.append(name).append(attrStr).append("：");
            } else if (!attrStr.isEmpty()) {
                sb.append(attrStr);
            }
            sb.append(dm.getContent()).append("\n");
        }
        return sb.toString();
    }

    /**
     * sourceType=2：对比弹幕，两侧分别格式化后加"场次1"/"场次2"标签拼接
     */
    private String resolveDanmakuContrast() {
        SyncContrastInfoVo contrast = getContrastInfo();
        if (contrast == null) return null;

        String side1 = resolveDanmakuForSide(contrast.getVideoOneId());
        String side2 = resolveDanmakuForSide(contrast.getVideoTwoId());

        if (StrUtil.isBlank(side1) && StrUtil.isBlank(side2)) return null;
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(side1)) {
            sb.append("场次1弹幕：\n").append(side1);
        }
        if (StrUtil.isNotBlank(side2)) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("场次2弹幕：\n").append(side2);
        }
        return sb.toString();
    }

    /**
     * 对比单侧弹幕：videoId 非空→查弹幕并格式化；文件返回 null
     */
    private String resolveDanmakuForSide(String videoId) {
        if (StrUtil.isBlank(videoId)) return null;
        AnchorVideoInfoVo video = getAnchorVideoBll().GetByVideoId(videoId).getData();
        if (video == null || video.getStartTime() == null) return null;
        List<DanMuVo> barrage = getBarrageMessagesForVideo(videoId);
        if (CollUtil.isEmpty(barrage)) return null;

        long videoStart = video.getStartTime().getTime();
        long filterEnd = video.getEndTime() != null ? video.getEndTime().getTime() : videoStart + 3600000L;
        return formatDanmakuText(barrage, videoStart, videoStart, filterEnd);
    }

    // ===== liveData（优先级 10）=====

    /**
     * 获取格式化后的直播数据文本（看板+截图，缓存复用）
     */
    public String getLiveData() {
        if (!liveDataLoaded) {
            liveDataLoaded = true;
            liveData = resolveLiveData();
        }
        return liveData;
    }

    /**
     * 直播数据解析入口，按 sourceType 分发：
     * 0→视频（截图+看板）、1→文件（仅截图）、2→对比（两侧分别取数据后拼接）。
     */
    private String resolveLiveData() {
        Integer sourceType = askRequestBo.getSourceType();
        String sourceId = askRequestBo.getSourceId();
        if (sourceType == null || sourceId == null) return null;

        if (sourceType == 0) {
            return resolveLiveDataSingle(0, sourceId);
        } else if (sourceType == 1) {
            return resolveLiveDataSingle(1, sourceId);
        } else if (sourceType == 2) {
            return resolveLiveDataContrast();
        }
        return null;
    }

    /**
     * sourceType=0/1：单场看板+截图数据。视频取看板+截图，文件仅取截图。
     */
    private String resolveLiveDataSingle(Integer sourceType, String sourceId) {
        StringBuilder sb = new StringBuilder();

        DataScreenshotBll screenshotBll = getDataScreenshotBll();
        List<DataScreenshotListVo> screenshots = screenshotBll
                .getExistDataScreenshotList(sourceType, sourceId).getData();
        if (CollUtil.isNotEmpty(screenshots)) {
            boolean hasContent = false;
            for (int i = 0; i < screenshots.size(); i++) {
                DataScreenshotListVo item = screenshots.get(i);
                if (StrUtil.isNotBlank(item.getAiContent())) {
                    if (!hasContent) {
                        sb.append("全场直播相关数据如下：\n");
                        hasContent = true;
                    }
                    sb.append("数据").append(i + 1).append("、\n")
                            .append(item.getAiContent()).append("\n");
                }
            }
        }

        // 看板数据仅视频有
        if (sourceType == 0) {
            VideoDataViewingBll viewingBll = getVideoDataViewingBll();
            VideoDataViewingConfuseInfoVo board = viewingBll
                    .infoByVideoIdPriorityParagraph(sourceId).getData();
            if (board != null) {
                String boardStr = board.formatStrForAi();
                if (StrUtil.isNotBlank(boardStr)) {
                    sb.append("看板数据：\n").append(boardStr);
                }
            }

            // 商品数据仅视频有，从企业后台查询
            AnchorVideoInfoVo video = getSingleVideo();
            if (video != null && StrUtil.isNotBlank(video.getBatchNumber())) {
                try {
                    String startTimeStr = video.getStartTime() != null
                            ? DateUtil.format(video.getStartTime(), "yyyy-MM-dd HH:mm:ss") : null;
                    String endTimeStr = video.getEndTime() != null
                            ? DateUtil.format(video.getEndTime(), "yyyy-MM-dd HH:mm:ss") : null;
                    R<List<GovernanceProductItemVo>> result = getGovernanceProductService()
                            .queryProductPage(video.getBatchNumber(), video.getTenantId(), startTimeStr, endTimeStr);
                    if (result != null && result.success() && CollUtil.isNotEmpty(result.getData())) {
                        String productTable = formatProductTable(result.getData());
                        if (StrUtil.isNotBlank(productTable)) {
                            sb.append("\n\n本场直播商品数据如下：\n").append(productTable);
                        }
                    }
                } catch (Exception e) {
                    log.warn("查询企业后台商品数据失败: tenantId={}, batchNumber={}, error={}",
                            video.getTenantId(), video.getBatchNumber(), e.getMessage());
                }
            }
        }
        return sb.toString();
    }

    /**
     * sourceType=2：对比看板+截图，两侧分别取数据后加"场次1"/"场次2"标签拼接
     */
    private String resolveLiveDataContrast() {
        SyncContrastInfoVo contrast = getContrastInfo();
        if (contrast == null) return null;

        String side1 = resolveLiveDataForSide(contrast.getVideoOneId(), contrast.getFileOneId());
        String side2 = resolveLiveDataForSide(contrast.getVideoTwoId(), contrast.getFileTwoId());

        if (StrUtil.isBlank(side1) && StrUtil.isBlank(side2)) return null;
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(side1)) {
            sb.append("场次1数据：\n").append(side1);
        }
        if (StrUtil.isNotBlank(side2)) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("场次2数据：\n").append(side2);
        }
        return sb.toString();
    }

    /**
     * 对比单侧数据：videoId 非空→视频、fileId 非空→文件
     */
    private String resolveLiveDataForSide(String videoId, String fileId) {
        if (StrUtil.isNotBlank(videoId)) {
            return resolveLiveDataSingle(0, videoId);
        } else if (StrUtil.isNotBlank(fileId)) {
            return resolveLiveDataSingle(1, fileId);
        }
        return null;
    }

    /**
     * 将商品列表格式化为 markdown 表格，列：商品名称、商品曝光率、讲解次数、商品点击率、点击成交转化率、订单量、成交金额
     *
     * @param products 商品列表
     * @return markdown 表格字符串
     */
    private String formatProductTable(List<GovernanceProductItemVo> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("| 商品名称 | 商品曝光率 | 讲解次数 | 商品点击率 | 点击成交转化率 | 订单量 | 成交金额 |\n");
        sb.append("|---------|-----------|--------|-----------|---------------|--------|---------|\n");
        for (GovernanceProductItemVo item : products) {
            sb.append("| ").append(StrUtil.isNotBlank(item.getTitle()) ? item.getTitle() : "-")
                    .append(" | ").append(formatPercent(item.getProductViewShowRatio()))
                    .append(" | ").append(item.getExplainCnt() != null ? item.getExplainCnt().toString() : "-")
                    .append(" | ").append(formatPercent(item.getProductShowClickUcntRatio()))
                    .append(" | ").append(formatPercent(item.getProductClickPayUcntRatio()))
                    .append(" | ").append(item.getPayCnt() != null ? item.getPayCnt().toString() : "-")
                    .append(" | ").append(formatAmount(item.getPayAmt()))
                    .append(" |\n");
        }
        return sb.toString();
    }

    /**
     * 格式化百分比：保留两位小数 + "%"，null 返回 "-"
     */
    private String formatPercent(BigDecimal value) {
        if (value == null) return "-";
        return value.setScale(2, RoundingMode.HALF_UP) + "%";
    }

    /**
     * 格式化金额：¥ 前缀 + 千分位，null 返回 "-"
     */
    private String formatAmount(BigDecimal value) {
        if (value == null) return "-";
        return "¥" + NumberUtil.decimalFormat(",###.##", value.doubleValue());
    }

    // ===== optimizePlan（优先级 10）=====

    /**
     * 获取优化计划文本（缓存复用），查询 AiOptimizePurposeRse 并格式化
     */
    public String getOptimizePlan() {
        if (!optimizePlanLoaded) {
            optimizePlanLoaded = true;
            optimizePlan = resolveOptimizePlan();
        }
        return optimizePlan;
    }

    /**
     * 优化计划解析入口，按 sourceType 分发：
     * 0→视频（按 sourceId 查）、1→文件（按 sourceId 查）、2→对比（两侧分别查后拼接）。
     */
    private String resolveOptimizePlan() {
        Integer sourceType = askRequestBo.getSourceType();
        String sourceId = askRequestBo.getSourceId();
        if (sourceType == null || sourceId == null) return null;

        if (sourceType == 0 || sourceType == 1) {
            return resolveOptimizePlanSingle(sourceId);
        } else if (sourceType == 2) {
            SyncContrastInfoVo contrast = getContrastInfo();
            if (contrast == null) return null;
            String side1 = resolveOptimizePlanSingle(contrast.getVideoOneId() != null ? contrast.getVideoOneId() : contrast.getFileOneId());
            String side2 = resolveOptimizePlanSingle(contrast.getVideoTwoId() != null ? contrast.getVideoTwoId() : contrast.getFileTwoId());
            if (StrUtil.isBlank(side1) && StrUtil.isBlank(side2)) return null;
            StringBuilder sb = new StringBuilder();
            if (StrUtil.isNotBlank(side1)) sb.append("场次1优化计划：\n").append(side1);
            if (StrUtil.isNotBlank(side2)) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("场次2优化计划：\n").append(side2);
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * 单场优化计划：查 AiOptimizePurposeRse，格式化优化动作 + 优化目的
     */
    private String resolveOptimizePlanSingle(String sourceId) {
        if (StrUtil.isBlank(sourceId)) return null;
        AiOptimizePurposeVo vo = getAiOptimizePurposeRse().getBySourceId(sourceId);
        if (vo == null) return null;
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(vo.getOptimizeAction())) {
            sb.append("优化动作：").append(vo.getOptimizeAction());
        }
        if (StrUtil.isNotBlank(vo.getOptimizePurpose())) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("优化目的：").append(vo.getOptimizePurpose());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    // ===== minuteSegmentDanmaku（优先级 10）=====

    /**
     * 获取分钟段落+弹幕交叉格式化文本（缓存复用），sourceType=1 返回 null
     */
    public String getMinuteSegmentDanmaku() {
        if (!minuteSegmentDanmakuLoaded) {
            minuteSegmentDanmakuLoaded = true;
            minuteSegmentDanmaku = resolveMinuteSegmentDanmaku();
        }
        return minuteSegmentDanmaku;
    }

    /**
     * 分钟段落+弹幕解析入口，按 sourceType 分发：
     * 0→视频（段落+弹幕）、1→文件（仅段落，无弹幕）、2→对比（两侧分别解析后拼接）。
     */
    private String resolveMinuteSegmentDanmaku() {
        Integer sourceType = askRequestBo.getSourceType();
        String sourceId = askRequestBo.getSourceId();
        if (sourceType == null || sourceId == null) return null;

        if (sourceType == 0) {
            AnchorVideoInfoVo video = getSingleVideo();
            if (video == null || video.getStartTime() == null) return null;
            return formatMinuteSegmentDanmaku(getMinuteSegments(), getBarrageMessages(),
                    video.getStartTime().getTime());
        } else if (sourceType == 1) {
            return formatMinuteSegmentDanmaku(getMinuteSegments(), null, 0);
        } else if (sourceType == 2) {
            SyncContrastInfoVo contrast = getContrastInfo();
            if (contrast == null) return null;
            String side1 = resolveMinuteSegmentDanmakuForSide(contrast.getVideoOneId(), contrast.getFileOneId());
            String side2 = resolveMinuteSegmentDanmakuForSide(contrast.getVideoTwoId(), contrast.getFileTwoId());
            if (StrUtil.isBlank(side1) && StrUtil.isBlank(side2)) return null;
            StringBuilder sb = new StringBuilder();
            if (StrUtil.isNotBlank(side1)) sb.append("场次1：\n").append(side1);
            if (StrUtil.isNotBlank(side2)) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("场次2：\n").append(side2);
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * 对比单侧分钟段落+弹幕：videoId 非空→解析段落+弹幕并格式化；fileId 非空→返回 null（文件无弹幕）
     */
    private String resolveMinuteSegmentDanmakuForSide(String videoId, String fileId) {
        if (StrUtil.isNotBlank(videoId)) {
            AnchorVideoInfoVo video = getAnchorVideoBll().GetByVideoId(videoId).getData();
            if (video == null || video.getStartTime() == null) return null;
            List<DanMuVo> barrage = getBarrageMessagesForVideo(videoId);
            List<MinuteSegmentVo> segs = resolveMinuteSegmentsVideo(videoId, video, barrage);
            return formatMinuteSegmentDanmaku(segs, barrage, video.getStartTime().getTime());
        } else if (StrUtil.isNotBlank(fileId)) {
            List<MinuteSegmentVo> segs = resolveMinuteSegmentsFile(fileId);
            return formatMinuteSegmentDanmaku(segs, null, 0);
        }
        return null;
    }

    /**
     * 分钟段落+弹幕交叉格式化：每段输出时间行→数据行→内容行。
     * barrageData 非空时追加"段落i弹幕："+该时段弹幕行；为空时跳过弹幕部分。
     * barrageData 按 recordDate 升序，用滑动窗口索引避免 O(n²)。
     *
     * @param segs        分钟段落列表
     * @param barrageData 全量弹幕（按 recordDate 升序）
     * @param videoStart  视频起始时间戳（ms），用于弹幕显示时间换算
     */
    private String formatMinuteSegmentDanmaku(List<MinuteSegmentVo> segs, List<DanMuVo> barrageData, long videoStart) {
        if (CollUtil.isEmpty(segs)) return null;
        StringBuilder sb = new StringBuilder();
        int barrageIdx = 0;

        for (int i = 0; i < segs.size(); i++) {
            MinuteSegmentVo seg = segs.get(i);
            if (i > 0) sb.append("\n\n");

            // 段落时间行
            StringBuilder timeSection = new StringBuilder();
            timeSection.append("开始时间：").append(ObjectUtil.defaultIfNull(seg.getTextStart(), ""))
                    .append("，结束时间：").append(ObjectUtil.defaultIfNull(seg.getTextEnd(), ""));
            if (seg.getStartTime() != null) {
                timeSection.append("，自然时间：").append(DateUtil.format(seg.getStartTime(), "HH:mm:ss"));
            }
            sb.append(StrUtil.format("段落{}时间:（{}）\n", seg.getIndex(), timeSection));

            // 段落数据行
            StringBuilder dataSection = new StringBuilder();
            String onlineNum = ObjectUtil.defaultIfNull(seg.getOnlineNum(), "0");
            dataSection.append("在线人数：").append(onlineNum);
            if (seg.getIndex() > 1 || seg.getOnlineNumChange() > 0) {
                dataSection.append("，比上段").append(seg.getOnlineNumTrend())
                        .append(seg.getOnlineNumChange()).append("人");
            }
            dataSection.append("，语速：").append((int) seg.getSpeechSpeed()).append("字/分钟");
            dataSection.append("，弹幕条数：").append(seg.getBarrageNum()).append("条");
            if (seg.getDealCount() != null) {
                dataSection.append("，成交人数：").append(seg.getDealCount());
            }
            if (seg.getInteractionRate() != null) {
                dataSection.append("，互动率：").append(seg.getInteractionRate()).append("%");
            }
            if (seg.getDealRate() != null) {
                dataSection.append("，成交率：").append(seg.getDealRate()).append("%");
            }
            if (seg.getSales() != null) {
                dataSection.append("，销售额：").append(seg.getSales());
            }
            if (seg.getUvValue() != null) {
                dataSection.append("，uv价值：").append(seg.getUvValue());
            }
            if (seg.getQianchuanCost() != null) {
                dataSection.append("，投放消耗：").append(seg.getQianchuanCost());
            }
            sb.append(StrUtil.format("段落{}数据:（{}）\n", seg.getIndex(), dataSection));

            // 段落内容行
            sb.append(StrUtil.format("段落{}内容：{}\n", seg.getIndex(),
                    ObjectUtil.defaultIfEmpty(seg.getContent(), "")));

            // 段落弹幕（仅在有弹幕数据时输出）
            if (CollUtil.isNotEmpty(barrageData) && seg.getStartVideoTime() != null
                    && seg.getEndVideoTime() != null) {
                long segStartMs = seg.getStartVideoTime();
                long segEndMs = seg.getEndVideoTime();

                // 快进到当前段起始时间
                while (barrageIdx < barrageData.size()) {
                    DanMuVo dm = barrageData.get(barrageIdx);
                    if (dm.getRecordDate() == null) {
                        barrageIdx++;
                        continue;
                    }
                    if (dm.getRecordDate() >= segStartMs) break;
                    barrageIdx++;
                }

                // 收集段内弹幕
                List<String> danmakuLines = new ArrayList<>();
                int tempIdx = barrageIdx;
                while (tempIdx < barrageData.size()) {
                    DanMuVo dm = barrageData.get(tempIdx);
                    if (dm.getRecordDate() == null) {
                        tempIdx++;
                        continue;
                    }
                    if (dm.getRecordDate() >= segEndMs) break;

                    long time1 = dm.getRecordDate() - videoStart - TimeUnit.HOURS.toMillis(8);
                    String timeStr = DateUtil.format(new Date(time1), "HH:mm:ss");

                    String name = StrUtil.isNotBlank(dm.getNickName()) ? dm.getNickName() : "";

                    List<String> attrs = new ArrayList<>();
                    if (dm.getIsNew() != null && dm.getIsNew()) attrs.add("新");
                    if (dm.getLevel() != null && dm.getLevel() > 0) attrs.add("UL:" + dm.getLevel());
                    if (dm.getFansLevelCurrent() != null && dm.getFansLevelCurrent() > 0)
                        attrs.add("FL:" + dm.getFansLevelCurrent());
                    String attrStr = attrs.isEmpty() ? "" : "(" + String.join("，", attrs) + ")";

                    StringBuilder line = new StringBuilder();
                    line.append(timeStr).append(" ");
                    if (StrUtil.isNotBlank(name)) {
                        line.append(name).append(attrStr).append("：");
                    } else if (!attrStr.isEmpty()) {
                        line.append(attrStr);
                    }
                    line.append(dm.getContent());
                    danmakuLines.add(line.toString());

                    tempIdx++;
                }
                barrageIdx = tempIdx;

                // 有弹幕才输出标题+弹幕行
                if (!danmakuLines.isEmpty()) {
                    sb.append(StrUtil.format("段落{}弹幕：\n", seg.getIndex()));
                    for (String line : danmakuLines) {
                        sb.append(line).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    // ===== liveKeywords（优先级 10）=====

    /**
     * 获取本场直播关键词文本（缓存复用）
     */
    public String getLiveKeywords() {
        if (!liveKeywordsLoaded) {
            liveKeywordsLoaded = true;
            liveKeywords = resolveLiveKeywords();
        }
        return liveKeywords;
    }

    /**
     * 本场关键词解析入口，按 sourceType 分发。
     * 单场复用分钟段落解析时缓存的 AnalysisResultVo；对比两侧独立下载。
     */
    private String resolveLiveKeywords() {
        Integer sourceType = askRequestBo.getSourceType();
        String sourceId = askRequestBo.getSourceId();
        if (sourceType == null || sourceId == null) return null;

        if (isContrast()) {
            SyncContrastInfoVo contrast = getContrastInfo();
            if (contrast == null) return null;
            String side1 = formatLiveKeywordsForSide(contrast.getVideoOneId(), contrast.getFileOneId());
            String side2 = formatLiveKeywordsForSide(contrast.getVideoTwoId(), contrast.getFileTwoId());
            if (StrUtil.isBlank(side1) && StrUtil.isBlank(side2)) return null;
            StringBuilder sb = new StringBuilder();
            if (StrUtil.isNotBlank(side1)) sb.append("场次1：\n").append(side1);
            if (StrUtil.isNotBlank(side2)) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("场次2：\n").append(side2);
            }
            return sb.toString();
        } else {
            if (cachedAnalysis != null) {
                return formatLiveKeywords(cachedAnalysis);
            }
            AnalysisResultVo analysis = downloadAnalysis(sourceType, sourceId);
            if (analysis != null) {
                this.cachedAnalysis = analysis;
            }
            return formatLiveKeywords(analysis);
        }
    }

    /**
     * 对比单侧关键词：下载 AnalysisResultVo → 提取 wordsType=1 并格式化
     */
    private String formatLiveKeywordsForSide(String videoId, String fileId) {
        AnalysisResultVo analysis = null;
        if (StrUtil.isNotBlank(videoId)) {
            analysis = downloadAnalysis(0, videoId);
        } else if (StrUtil.isNotBlank(fileId)) {
            analysis = downloadAnalysis(1, fileId);
        }
        return formatLiveKeywords(analysis);
    }

    /**
     * 下载 AnalysisResultVo
     */
    private AnalysisResultVo downloadAnalysis(Integer sourceType, String sourceId) {
        SensitiveWordsBll swBll = getSensitiveWordsBll();
        String downloadUrl = swBll.getAnalysisDownloadUrl(sourceType, sourceId);
        if (StrUtil.isBlank(downloadUrl)) return null;
        String jsonContent = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);
        if (StrUtil.isBlank(jsonContent)) return null;
        return JSONObject.parseObject(jsonContent, AnalysisResultVo.class);
    }

    /**
     * 从 AnalysisResultVo.wordsCollect 中提取 keywords（wordsType=1），扁平拼接
     */
    private String formatLiveKeywords(AnalysisResultVo analysis) {
        return formatKeywords(analysis);
    }

    /**
     * 提取 keywords（wordsType=1），扁平拼接为 "关键词A(3次)、关键词B(2次)" 格式
     */
    private String formatKeywords(AnalysisResultVo analysis) {
        if (analysis == null || CollUtil.isEmpty(analysis.getWordsCollect())) return null;

        List<WordsMarkVo> keywords = analysis.getWordsCollect().stream()
                .filter(w -> w.getWordsType() != null && w.getWordsType() == 1)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(keywords)) return null;

        return keywords.stream()
                .map(w -> {
                    String name = w.getName();
                    if (w.getCountNum() != null && w.getCountNum() > 0) {
                        name += "(" + w.getCountNum() + "次)";
                    }
                    return name;
                })
                .collect(Collectors.joining("、"));
    }

    // ===== prev* 系列（优先级 10）=====

    /**
     * 获取上一场视频的 PlaceholderContext（缓存复用）。
     * new 出一个最小 AskRequestBo（sourceType=0, sourceId=prevVideoId, tenantId），
     * 复用本场全部解析链路（getMinuteSegment/getDanmaku/getLiveData/getLiveKeywords 等）。
     */
    private PlaceholderContext getPrevContext() {
        if (!prevCtxLoaded) {
            prevCtxLoaded = true;
            AnchorVideoInfoVo prev = getPreviousVideo();
            if (prev != null && StrUtil.isNotBlank(prev.getVideoId())) {
                AskRequestBo prevBo = new AskRequestBo();
                prevBo.setSourceType(0);
                prevBo.setSourceId(prev.getVideoId());
                prevBo.setTenantId(askRequestBo.getTenantId());
                prevCtx = new PlaceholderContext(prevBo);
            }
        }
        return prevCtx;
    }

    /**
     * 获取上一场分钟段落文本（缓存复用）
     */
    public String getPrevMinuteSegment() {
        PlaceholderContext pCtx = getPrevContext();
        return pCtx != null ? pCtx.getMinuteSegment() : null;
    }

    /**
     * 获取上一场基础数据（看板+截图）（缓存复用）
     */
    public String getPrevBasicData() {
        PlaceholderContext pCtx = getPrevContext();
        return pCtx != null ? pCtx.getLiveData() : null;
    }

    /**
     * 获取上一场弹幕文本（缓存复用）
     */
    public String getPrevDanmaku() {
        PlaceholderContext pCtx = getPrevContext();
        return pCtx != null ? pCtx.getDanmaku() : null;
    }

    /**
     * 获取上一场分钟段落+弹幕交叉文本（缓存复用）
     */
    public String getPrevMinuteSegmentDanmaku() {
        PlaceholderContext pCtx = getPrevContext();
        return pCtx != null ? pCtx.getMinuteSegmentDanmaku() : null;
    }

    /**
     * 获取上一场AI数据诊断结果（缓存复用）
     */
    public String getPrevAiDiagnosis() {
        PlaceholderContext pCtx = getPrevContext();
        if (pCtx == null) return null;
        String result = pCtx.getAiDiagnosis();
        if (StrUtil.isNotBlank(result)) {
            result = result.replace("本场AI数据诊断结果", "上一场AI数据诊断结果");
        }
        return result;
    }

    /**
     * 获取上一场最后一次分析结果（缓存复用）
     */
    public String getPrevLastAnalysis() {
        PlaceholderContext pCtx = getPrevContext();
        if (pCtx == null) return null;
        String result = pCtx.getLastAnalysis();
        if (StrUtil.isNotBlank(result)) {
            result = result.replace("本场最后一次分析结果", "上一场最后一次分析结果");
        }
        return result;
    }

    /**
     * 获取本场最后一次综合分析结果（缓存复用）
     */
    public String getLastComprehensiveAnalysis() {
        if (!lastComprehensiveAnalysisLoaded) {
            lastComprehensiveAnalysisLoaded = true;
            lastComprehensiveAnalysis = resolveLastComprehensiveAnalysis();
        }
        return lastComprehensiveAnalysis;
    }

    /**
     * 本场最后一次综合分析解析入口，按 sourceType 分发。
     * askType 从 askRequestBo.type 获取，analysisType=1（综合分析）。
     */
    private String resolveLastComprehensiveAnalysis() {
        Integer askType = askRequestBo.getType();
        return resolveLastAnalysisByAskType(askType, "本场最后一次综合分析结果", 1);
    }

    /**
     * 获取上一场最后一次综合分析结果（缓存复用）
     */
    public String getPrevLastComprehensiveAnalysis() {
        PlaceholderContext pCtx = getPrevContext();
        if (pCtx == null) return null;
        String result = pCtx.getLastComprehensiveAnalysis();
        if (StrUtil.isNotBlank(result)) {
            result = result.replace("本场最后一次综合分析结果", "上一场最后一次综合分析结果");
        }
        return result;
    }

    /**
     * 获取上一场关键词文本（缓存复用）。先 warm 分钟段落以填充 prevCtx 的 cachedAnalysis。
     */
    public String getPrevKeywords() {
        PlaceholderContext pCtx = getPrevContext();
        if (pCtx == null) return null;
        // warm: 分钟段落解析会填充 cachedAnalysis，liveKeywords 依赖它
        pCtx.getMinuteSegment();
        return pCtx.getLiveKeywords();
    }

    // ===== sceneSlice（优先级 10）=====

    /**
     * 获取本场场景切片AI分析结果（缓存复用）
     */
    public String getSceneSliceResult() {
        if (!sceneSliceResultLoaded) {
            sceneSliceResultLoaded = true;
            String videoId = askRequestBo.getSourceId();
            sceneSliceResult = getSceneSliceFeign().getSceneSliceResultByVideoId(videoId);
        }
        return sceneSliceResult;
    }

    /**
     * 获取上一场场景切片AI分析结果（缓存复用）
     */
    public String getPrevSceneSliceResult() {
        PlaceholderContext pCtx = getPrevContext();
        if (pCtx == null) return null;
        return pCtx.getSceneSliceResult();
    }

    // ===== aiDiagnosis（优先级 10）=====

    /**
     * 获取本场AI数据诊断结果文本（缓存复用）
     */
    public String getAiDiagnosis() {
        if (!aiDiagnosisLoaded) {
            aiDiagnosisLoaded = true;
            aiDiagnosis = resolveAiDiagnosis();
        }
        return aiDiagnosis;
    }

    /**
     * AI数据诊断解析入口，按 sourceType 分发：
     * 0→视频（查诊断结果）、1→文件（无诊断）、2→对比（两侧分别查后拼接）。
     */
    private String resolveAiDiagnosis() {
        return resolveLastAnalysisByAskType(AiEnums.askType.DATA_DIAGNOSIS.getCode(),
                "本场AI数据诊断结果");
    }

    // ===== lastAnalysis（优先级 10）=====

    /**
     * 获取本场最后一次分析结果文本（缓存复用）
     */
    public String getLastAnalysis() {
        if (!lastAnalysisLoaded) {
            lastAnalysisLoaded = true;
            lastAnalysis = resolveLastAnalysis();
        }
        return lastAnalysis;
    }

    /**
     * 本场最后一次分析解析入口，按 sourceType 分发。
     * askType 从 askRequestBo.type 获取（对应 AiEnums.askType 0~4）。
     */
    private String resolveLastAnalysis() {
        Integer askType = askRequestBo.getType();
        return resolveLastAnalysisByAskType(askType, "本场最后一次分析结果");
    }

    /**
     * 通用：按指定 askType 查最近一条 Q 回答，按 sourceType 分派。
     *
     * @param askType 助手类型（AiEnums.askType.code），null 时不限制
     * @param label   本场标签（如"本场AI数据诊断结果"），prev 调用后会替换"本场"→"上一场"
     */
    private String resolveLastAnalysisByAskType(Integer askType, String label) {
        return resolveLastAnalysisByAskType(askType, label, null);
    }

    /**
     * 通用：按指定 askType + analysisType 查最近一条 Q 回答，按 sourceType 分派。
     *
     * @param askType      助手类型（AiEnums.askType.code），null 时不限制
     * @param label        本场标签
     * @param analysisType 分析类型（0=普通分析, 1=综合分析），null 时不限制
     */
    private String resolveLastAnalysisByAskType(Integer askType, String label, Integer analysisType) {
        Integer sourceType = askRequestBo.getSourceType();
        String sourceId = askRequestBo.getSourceId();
        if (sourceType == null || sourceId == null) return null;

        if (sourceType == 0) {
            String content = resolveLastConversationSingle(sourceId, askType, analysisType);
            return content != null ? label + "：\n" + content : null;
        } else if (sourceType == 1) {
            return null;
        } else if (sourceType == 2) {
            SyncContrastInfoVo contrast = getContrastInfo();
            if (contrast == null) return null;
            String side1 = resolveLastConversationSingle(contrast.getVideoOneId(), askType, analysisType);
            String side2 = resolveLastConversationSingle(contrast.getVideoTwoId(), askType, analysisType);
            if (StrUtil.isBlank(side1) && StrUtil.isBlank(side2)) return null;
            StringBuilder sb = new StringBuilder();
            if (StrUtil.isNotBlank(side1)) sb.append("场次1：\n").append(side1);
            if (StrUtil.isNotBlank(side2)) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("场次2：\n").append(side2);
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * 查 MongoDB 最近一条回答（type=Q），可按 askType 过滤（null 时不限类型）
     */
    private String resolveLastConversationSingle(String sourceId, Integer askType) {
        return resolveLastConversationSingle(sourceId, askType, null);
    }

    /**
     * 查 MongoDB 最近一条回答（type=Q），可按 askType + analysisType 过滤（null 时不限）
     */
    private String resolveLastConversationSingle(String sourceId, Integer askType, Integer analysisType) {
        if (StrUtil.isBlank(sourceId)) return null;
        AnchorVideoInfoVo video = getAnchorVideoBll().GetByVideoId(sourceId).getData();
        if (video == null) return null;

        ConversationEntity probe = new ConversationEntity();
        probe.setSourceId(sourceId);
        probe.setSourceType(0);
        probe.setUserId(video.getUserId());
        probe.setTenantId(askRequestBo.getTenantId());
        probe.setType("Q");
        if (askType != null) {
            probe.setAskType(askType);
        }
        if (analysisType != null) {
            probe.setAnalysisType(analysisType);
        }

        Page<ConversationEntity> result = getConversationService().page(probe, 1, 1);
        if (result == null || CollUtil.isEmpty(result.getContent())) return null;

        return AiUtils.deleteDeepThinking(result.getContent().get(0).getContent());
    }

    // ===== audienceProfile / trafficStructure（优先级 10）=====

    /**
     * 获取看板数据（缓存复用），audienceProfile + trafficStructure 共用
     */
    private VideoDataViewingConfuseInfoVo getBoard() {
        if (!boardLoaded) {
            boardLoaded = true;
            Integer sourceType = askRequestBo.getSourceType();
            String sourceId = askRequestBo.getSourceId();
            if (sourceType != null && sourceType == 0 && sourceId != null) {
                cachedBoard = getVideoDataViewingBll()
                        .infoByVideoIdPriorityParagraph(sourceId).getData();
            }
        }
        return cachedBoard;
    }

    /**
     * 获取本场人群画像文本（缓存复用），含看播+成交
     */
    public String getAudienceProfile() {
        if (!audienceProfileLoaded) {
            audienceProfileLoaded = true;
            audienceProfile = resolveAudienceProfile();
        }
        return audienceProfile;
    }

    /**
     * 获取本场流量结构文本（缓存复用），含看播+成交
     */
    public String getTrafficStructure() {
        if (!trafficStructureLoaded) {
            trafficStructureLoaded = true;
            trafficStructure = resolveTrafficStructure();
        }
        return trafficStructure;
    }

    private String resolveAudienceProfile() {
        return resolveBoardSection(true);
    }

    private String resolveTrafficStructure() {
        return resolveBoardSection(false);
    }

    /**
     * 按 sourceType 分发看板数据，提取画像或流量部分。
     *
     * @param portrait true=人群画像, false=流量结构
     */
    private String resolveBoardSection(boolean portrait) {
        Integer sourceType = askRequestBo.getSourceType();
        String sourceId = askRequestBo.getSourceId();
        if (sourceType == null || sourceId == null) return null;

        if (sourceType == 0) {
            VideoDataViewingConfuseInfoVo board = getBoard();
            if (board == null || board.empty()) return null;
            VideoDataViewingConfuseVo.StructuredData data = board.buildStructuredData();
            return portrait ? formatAudienceProfile(data) : formatTrafficStructure(data);
        } else if (sourceType == 1) {
            return null;
        } else if (sourceType == 2) {
            SyncContrastInfoVo contrast = getContrastInfo();
            if (contrast == null) return null;
            String side1 = resolveBoardSectionForSide(contrast.getVideoOneId(), portrait);
            String side2 = resolveBoardSectionForSide(contrast.getVideoTwoId(), portrait);
            if (StrUtil.isBlank(side1) && StrUtil.isBlank(side2)) return null;
            StringBuilder sb = new StringBuilder();
            if (StrUtil.isNotBlank(side1)) sb.append("场次1：\n").append(side1);
            if (StrUtil.isNotBlank(side2)) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("场次2：\n").append(side2);
            }
            return sb.toString();
        }
        return null;
    }

    private String resolveBoardSectionForSide(String videoId, boolean portrait) {
        if (StrUtil.isBlank(videoId)) return null;
        VideoDataViewingConfuseInfoVo board = getVideoDataViewingBll()
                .infoByVideoIdPriorityParagraph(videoId).getData();
        if (board == null || board.empty()) return null;
        VideoDataViewingConfuseVo.StructuredData data = board.buildStructuredData();
        return portrait ? formatAudienceProfile(data) : formatTrafficStructure(data);
    }

    /**
     * 格式化人群画像：看播 + 成交
     */
    private String formatAudienceProfile(VideoDataViewingConfuseVo.StructuredData data) {
        if (data == null) return null;
        StringBuilder sb = new StringBuilder();
        appendPortraitSection(sb, "看播人群画像", data.getWatchUserPortraitData());
        appendPortraitSection(sb, "成交人群画像", data.getPayUserPortraitData());
        return sb.length() > 0 ? sb.toString() : null;
    }

    private void appendPortraitSection(StringBuilder sb, String title,
                                       VideoDataViewingConfuseVo.BaseUserPortraitStructure<?> portraitData) {
        if (portraitData == null) return;
        if (sb.length() > 0) sb.append("\n");
        sb.append(title).append("：");
        appendPortraitCategory(sb, "年龄", portraitData.getAgePortrait());
        appendPortraitCategory(sb, "性别", portraitData.getGenderPortrait());
    }

    private void appendPortraitCategory(StringBuilder sb, String category,
                                        List<VideoDataViewingConfuseVo.PortraitItem> items) {
        if (items == null || items.isEmpty()) return;
        sb.append("\n  ").append(category).append(": ");
        for (int i = 0; i < items.size(); i++) {
            VideoDataViewingConfuseVo.PortraitItem item = items.get(i);
            sb.append(item.getLabel()).append("(")
                    .append(BigDecimal.valueOf(item.getValue()).multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP)).append("%)");
            if (i < items.size() - 1) sb.append(", ");
        }
    }

    /**
     * 格式化流量结构：看播 + 成交
     */
    private String formatTrafficStructure(VideoDataViewingConfuseVo.StructuredData data) {
        if (data == null) return null;
        StringBuilder sb = new StringBuilder();
        appendFlowSection(sb, "看播流量结构", data.getWatchFlowStructures());
        appendFlowSection(sb, "成交流量结构", data.getPayFlowStructures());
        return sb.length() > 0 ? sb.toString() : null;
    }

    private void appendFlowSection(StringBuilder sb, String title,
                                   List<? extends VideoDataViewingConfuseVo.BaseFlowStructure<?>> flows) {
        if (flows == null || flows.isEmpty()) return;
        if (sb.length() > 0) sb.append("\n");
        sb.append(title).append("：");
        for (VideoDataViewingConfuseVo.BaseFlowStructure<?> f : flows) {
            sb.append("\n  ").append(f.getChannelName()).append(": ")
                    .append(BigDecimal.valueOf(f.getRatio()).multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP)).append("%");
        }
    }

    // ===== prev audienceProfile / trafficStructure =====

    /**
     * 获取上一场人群画像数据（缓存复用）
     */
    public String getPrevAudienceProfile() {
        PlaceholderContext pCtx = getPrevContext();
        return pCtx != null ? pCtx.getAudienceProfile() : null;
    }

    /**
     * 获取上一场流量结构数据（缓存复用）
     */
    public String getPrevTrafficStructure() {
        PlaceholderContext pCtx = getPrevContext();
        return pCtx != null ? pCtx.getTrafficStructure() : null;
    }

    // ===== tradeSensitiveWords / tradeKeywords（优先级 10）=====

    /**
     * 获取行业敏感词文本（缓存复用）
     */
    public String getTradeSensitiveWords() {
        if (!tradeSensitiveWordsLoaded) {
            tradeSensitiveWordsLoaded = true;
            tradeSensitiveWords = resolveTradeWords(0);
        }
        return tradeSensitiveWords;
    }

    /**
     * 获取行业关键词文本（缓存复用）
     */
    public String getTradeKeywords() {
        if (!tradeKeywordsLoaded) {
            tradeKeywordsLoaded = true;
            tradeKeywords = resolveTradeWords(1);
        }
        return tradeKeywords;
    }

    // ===== 行业知识库（优先级 10）=====

    /**
     * 获取系统行业运营知识库（缓存复用）
     */
    public String getTradeOperationKnowledge() {
        if (!tradeOperationKnowledgeLoaded) {
            tradeOperationKnowledgeLoaded = true;
            tradeOperationKnowledge = resolveKnowledgeByType(1);
        }
        return tradeOperationKnowledge;
    }

    /**
     * 获取系统行业违规知识库（缓存复用）
     */
    public String getTradeViolationKnowledge() {
        if (!tradeViolationKnowledgeLoaded) {
            tradeViolationKnowledgeLoaded = true;
            tradeViolationKnowledge = resolveKnowledgeByType(2);
        }
        return tradeViolationKnowledge;
    }

    /**
     * 获取系统行业敏感词知识库（缓存复用）
     */
    public String getTradeSensitiveKnowledge() {
        if (!tradeSensitiveKnowledgeLoaded) {
            tradeSensitiveKnowledgeLoaded = true;
            tradeSensitiveKnowledge = resolveKnowledgeByType(3);
        }
        return tradeSensitiveKnowledge;
    }

    /**
     * 主播级知识库（运营+敏感词+健康值），仅视频支持。
     * 对比分析时区分同主播/不同主播。
     */
    public String getAnchorKnowledge() {
        if (!anchorKnowledgeLoaded) {
            anchorKnowledgeLoaded = true;
            Integer sourceType = askRequestBo.getSourceType();
            if (sourceType == null) return null;
            if (sourceType == 0) {
                anchorKnowledge = resolveSingleAnchorKnowledge();
            } else if (sourceType == 2) {
                anchorKnowledge = resolveContrastAnchorKnowledge();
            }
        }
        return anchorKnowledge;
    }

    private String resolveSingleAnchorKnowledge() {
        AnchorVideoInfoVo video = getSingleVideo();
        if (video == null || video.getUserId() == null || StrUtil.isBlank(video.getSecUid())) return null;
        AnchorKnowledgeVo vo = getAnchorKnowledgeProducer().getByUserAndSecUid(video.getUserId(), video.getSecUid());
        this.cachedAnchorKnowledgeVo = vo;
        return formatAnchorKnowledge(vo);
    }

    private String resolveContrastAnchorKnowledge() {
        SyncContrastInfoVo contrast = getContrastInfo();
        if (contrast == null) return null;
        String anchor1 = contrast.getAnchorOneId();
        String anchor2 = contrast.getAnchorTwoId();
        if (StrUtil.isBlank(anchor1) && StrUtil.isBlank(anchor2)) return null;

        // 从对比的一个视频获取 userId
        Long userId = null;
        if (StrUtil.isNotBlank(contrast.getVideoOneId())) {
            AnchorVideoInfoVo video = getAnchorVideoBll().GetByVideoId(contrast.getVideoOneId()).getData();
            if (video != null) userId = video.getUserId();
        }

        if (ObjectUtil.equals(anchor1, anchor2)) {
            AnchorKnowledgeVo vo = getAnchorKnowledgeProducer().getByUserAndSecUid(userId, anchor1);
            this.cachedContrastAnchorKnowledgeVo1 = vo;
            return formatAnchorKnowledge(vo);
        } else {
            AnchorKnowledgeVo vo1 = getAnchorKnowledgeProducer().getByUserAndSecUid(userId, anchor1);
            AnchorKnowledgeVo vo2 = getAnchorKnowledgeProducer().getByUserAndSecUid(userId, anchor2);
            this.cachedContrastAnchorKnowledgeVo1 = vo1;
            this.cachedContrastAnchorKnowledgeVo2 = vo2;
            return formatContrastAnchorKnowledge(vo1, vo2);
        }
    }

    private String formatAnchorKnowledge(AnchorKnowledgeVo vo) {
        if (vo == null) return null;
        StringBuilder sb = new StringBuilder();
        boolean hasContent = false;
        if (StrUtil.isNotBlank(vo.getOperationContent())) {
            sb.append("【主播运营知识库】\n").append(vo.getOperationContent());
            hasContent = true;
        }
        if (StrUtil.isNotBlank(vo.getSensitiveContent())) {
            if (hasContent) sb.append("\n");
            sb.append("【主播敏感词知识库】\n").append(vo.getSensitiveContent());
            hasContent = true;
        }
        if (StrUtil.isNotBlank(vo.getHealthScore())) {
            if (hasContent) sb.append("\n");
            sb.append("【直播间健康值】\n").append(vo.getHealthScore());
            hasContent = true;
        }
        return hasContent ? sb.toString() : null;
    }

    private String formatContrastAnchorKnowledge(AnchorKnowledgeVo vo1, AnchorKnowledgeVo vo2) {
        String s1 = formatSideAnchorKnowledge("场次1", vo1);
        String s2 = formatSideAnchorKnowledge("场次2", vo2);
        if (s1 == null && s2 == null) return null;
        StringBuilder sb = new StringBuilder();
        if (s1 != null) sb.append(s1);
        if (s2 != null) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(s2);
        }
        return sb.toString();
    }

    // ===== 拆分后的主播知识库（按字段独立）=====

    public String getAnchorOperationKnowledge() {
        return getAnchorKnowledgePart(AnchorKnowledgeVo::getOperationContent, "主播运营知识库");
    }

    public String getAnchorSensitiveKnowledge() {
        return getAnchorKnowledgePart(AnchorKnowledgeVo::getSensitiveContent, "主播敏感词知识库");
    }

    public String getAnchorHealthScore() {
        return getAnchorKnowledgePart(AnchorKnowledgeVo::getHealthScore, "直播间健康值");
    }

    /**
     * 通用：从已缓存的 AnchorKnowledgeVo 中提取单个字段并格式化。
     * 单场模式用 cachedAnchorKnowledgeVo；对比模式用 cachedContrastAnchorKnowledgeVo1/2 并加场次标签。
     */
    private String getAnchorKnowledgePart(Function<AnchorKnowledgeVo, String> extractor, String label) {
        if (!anchorKnowledgeLoaded) getAnchorKnowledge();
        // 单场
        if (cachedAnchorKnowledgeVo != null) {
            return formatPart(label, extractor.apply(cachedAnchorKnowledgeVo));
        }
        // 对比
        String s1 = cachedContrastAnchorKnowledgeVo1 != null
                ? formatPart(label, extractor.apply(cachedContrastAnchorKnowledgeVo1)) : null;
        String s2 = cachedContrastAnchorKnowledgeVo2 != null
                ? formatPart(label, extractor.apply(cachedContrastAnchorKnowledgeVo2)) : null;
        if (s1 == null && s2 == null) return null;
        if (s2 == null) return s1;
        if (s1 == null) {
            return "场次2-" + s2;
        }
        // 两场不同 → 场次标签区分
        if (s1.startsWith("场次1")) return s1 + "\n\n" + s2;
        return "场次1：" + s1 + "\n\n场次2：" + s2;
    }

    private String formatPart(String label, String content) {
        if (StrUtil.isBlank(content)) return null;
        return "【" + label + "】\n" + content;
    }

    private String formatSideAnchorKnowledge(String prefix, AnchorKnowledgeVo vo) {
        if (vo == null) return null;
        StringBuilder sb = new StringBuilder();
        boolean hasContent = false;
        if (StrUtil.isNotBlank(vo.getOperationContent())) {
            sb.append("【").append(prefix).append("-主播运营知识库】\n").append(vo.getOperationContent());
            hasContent = true;
        }
        if (StrUtil.isNotBlank(vo.getSensitiveContent())) {
            if (hasContent) sb.append("\n");
            sb.append("【").append(prefix).append("-主播敏感词知识库】\n").append(vo.getSensitiveContent());
            hasContent = true;
        }
        if (StrUtil.isNotBlank(vo.getHealthScore())) {
            if (hasContent) sb.append("\n");
            sb.append("【").append(prefix).append("-直播间健康值】\n").append(vo.getHealthScore());
            hasContent = true;
        }
        return hasContent ? sb.toString() : null;
    }

    /**
     * 按行业层级获取知识库，规则：当前行业 → 上级行业 → ... → 通用行业（tradeId=1）。
     *
     * @param knowledgeType 知识库类型：1=运营，2=违规，3=敏感词
     */
    private String resolveKnowledgeByType(int knowledgeType) {
        Long tradeId = resolveTradeId();
        if (tradeId == null) {
            // 无行业信息时直接查通用行业
            return getKnowledgeContent(1L, knowledgeType);
        }

        // 获取行业父链（从子到父）
        List<Long> tradeIds = new ArrayList<>();
        List<TradeInfoVo> tradeVos = getTradeProducer().listParentsByTradeId(tradeId, 0);
        if (CollUtil.isNotEmpty(tradeVos)) {
            tradeVos.stream()
                    .map(TradeInfoVo::getId)
                    .sorted(Comparator.reverseOrder())
                    .forEach(tradeIds::add);
        }
        // 兜底通用行业
        if (!tradeIds.contains(1L)) {
            tradeIds.add(1L);
        }

        // 按子→父→通用顺序找第一个有内容的
        for (Long tid : tradeIds) {
            String content = getKnowledgeContent(tid, knowledgeType);
            if (StrUtil.isNotBlank(content)) {
                return content;
            }
        }
        return null;
    }

    /**
     * 查单个行业的知识库内容
     */
    private String getKnowledgeContent(Long tradeId, int knowledgeType) {
        IndustryKnowledgeVo vo = getIndustryKnowledgeProducer().getByTradeIdAndType(tradeId, knowledgeType);
        return vo != null ? vo.getContent() : null;
    }

    /**
     * 行业词库解析入口，按 sourceType 分发。
     *
     * @param wordsType 0=敏感词, 1=关键词
     */
    private String resolveTradeWords(int wordsType) {
        Integer sourceType = askRequestBo.getSourceType();
        String sourceId = askRequestBo.getSourceId();
        if (sourceType == null || sourceId == null) return null;

        if (isContrast()) {
            SyncContrastInfoVo contrast = getContrastInfo();
            if (contrast == null) return null;
            String side1 = formatTradeWords(contrast.getTradeOneId(), wordsType);
            String side2 = formatTradeWords(contrast.getTradeTwoId(), wordsType);
            if (StrUtil.isBlank(side1) && StrUtil.isBlank(side2)) return null;
            StringBuilder sb = new StringBuilder();
            if (StrUtil.isNotBlank(side1)) sb.append("场次1：\n").append(side1);
            if (StrUtil.isNotBlank(side2)) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("场次2：\n").append(side2);
            }
            return sb.toString();
        } else {
            Long tradeId = resolveTradeId();
            if (tradeId == null) return null;
            return formatTradeWords(tradeId, wordsType);
        }
    }

    /**
     * 获取当前请求的 tradeId（sourceType=0/1）
     */
    private Long resolveTradeId() {
        Integer sourceType = askRequestBo.getSourceType();
        if (sourceType == null) return null;
        if (sourceType == 0) {
            AnchorVideoInfoVo video = getSingleVideo();
            return video != null ? video.getTradeId() : null;
        } else if (sourceType == 1) {
            UploadFileInfoVo file = getSingleFile();
            return file != null ? file.getTradeId() : null;
        }
        return null;
    }

    /**
     * 格式化行业词库列表，按树形层级展示（parentId=0 为一级分类，其余为二级词）
     */
    private String formatTradeWords(Long tradeId, int wordsType) {
        List<SensitiveWordsVo> words = getSensitiveWordsBll()
                .listByUidAndPidAndTid(0L, 1, tradeId, wordsType);
        if (CollUtil.isEmpty(words)) return null;

        List<SensitiveWordsVo> level1 = words.stream()
                .filter(w -> w.getParentId() != null && w.getParentId() == 0L
                        && StrUtil.isNotBlank(w.getName()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(level1)) return null;

        Map<Long, List<String>> childrenMap = words.stream()
                .filter(w -> w.getParentId() != null && w.getParentId() != 0L
                        && StrUtil.isNotBlank(w.getName()))
                .collect(Collectors.groupingBy(
                        SensitiveWordsVo::getParentId,
                        LinkedHashMap::new,
                        Collectors.mapping(SensitiveWordsVo::getName, Collectors.toList())));

        StringBuilder sb = new StringBuilder();
        for (SensitiveWordsVo parent : level1) {
            List<String> children = childrenMap.get(parent.getId());
            if (CollUtil.isEmpty(children)) continue;
            sb.append(parent.getName()).append("：\n");
            sb.append(String.join("、", children)).append("\n");
        }
        return sb.length() > 0 ? sb.toString().trim() : null;
    }

    // ===== Top N（优先级 20）=====

    // ---- 成交率 ----

    public String getConversionIncreaseTop(int topN) {
        if (conversionIncreaseTopN != topN) {
            conversionIncreaseTopN = topN;
            conversionIncreaseTop = resolveDeltaTop(
                    MinuteSegmentVo::getDealRate, true, false, topN, false);
        }
        return conversionIncreaseTop;
    }

    public String getConversionDecreaseTop(int topN) {
        if (conversionDecreaseTopN != topN) {
            conversionDecreaseTopN = topN;
            conversionDecreaseTop = resolveDeltaTop(
                    MinuteSegmentVo::getDealRate, false, false, topN, false);
        }
        return conversionDecreaseTop;
    }

    // ---- UV 价值 ----

    public String getUvIncreaseTop(int topN) {
        if (uvIncreaseTopN != topN) {
            uvIncreaseTopN = topN;
            uvIncreaseTop = resolveDeltaTop(
                    MinuteSegmentVo::getUvValue, true, false, topN, false);
        }
        return uvIncreaseTop;
    }

    public String getUvDecreaseTop(int topN) {
        if (uvDecreaseTopN != topN) {
            uvDecreaseTopN = topN;
            uvDecreaseTop = resolveDeltaTop(
                    MinuteSegmentVo::getUvValue, false, false, topN, false);
        }
        return uvDecreaseTop;
    }

    // ---- 互动率 ----

    public String getInteractIncreaseTop(int topN) {
        if (interactIncreaseTopN != topN) {
            interactIncreaseTopN = topN;
            interactIncreaseTop = resolveDeltaTop(
                    MinuteSegmentVo::getInteractionRate, true, false, topN, false);
        }
        return interactIncreaseTop;
    }

    public String getInteractDecreaseTop(int topN) {
        if (interactDecreaseTopN != topN) {
            interactDecreaseTopN = topN;
            interactDecreaseTop = resolveDeltaTop(
                    MinuteSegmentVo::getInteractionRate, false, false, topN, false);
        }
        return interactDecreaseTop;
    }

    // ---- 在线人数 ----

    public String getOnlineIncreaseTop(int topN) {
        if (onlineIncreaseTopN != topN) {
            onlineIncreaseTopN = topN;
            onlineIncreaseTop = resolveDeltaTop(
                    s -> s.getOnlineNum() != null ? Double.parseDouble(s.getOnlineNum()) : null,
                    true, true, topN, false, true);
        }
        return onlineIncreaseTop;
    }

    public String getOnlineDecreaseTop(int topN) {
        if (onlineDecreaseTopN != topN) {
            onlineDecreaseTopN = topN;
            onlineDecreaseTop = resolveDeltaTop(
                    s -> s.getOnlineNum() != null ? Double.parseDouble(s.getOnlineNum()) : null,
                    false, true, topN, false, true);
        }
        return onlineDecreaseTop;
    }

    // ---- 在线率 ----

    public String getOnlineRateIncreaseTop(int topN) {
        if (onlineRateIncreaseTopN != topN) {
            onlineRateIncreaseTopN = topN;
            onlineRateIncreaseTop = resolveDeltaTop(
                    s -> s.getOnlineNum() != null ? Double.parseDouble(s.getOnlineNum()) : null,
                    true, true, topN, true, true);
        }
        return onlineRateIncreaseTop;
    }

    public String getOnlineRateDecreaseTop(int topN) {
        if (onlineRateDecreaseTopN != topN) {
            onlineRateDecreaseTopN = topN;
            onlineRateDecreaseTop = resolveDeltaTop(
                    s -> s.getOnlineNum() != null ? Double.parseDouble(s.getOnlineNum()) : null,
                    false, true, topN, true, true);
        }
        return onlineRateDecreaseTop;
    }

    // ===== douyinPeerAvg（优先级 20）=====

    /**
     * 获取抖音同行直播基础数据平均值
     */
    public String getDouyinPeerAvg(int days) {
        if (douyinPeerAvgDays != days) {
            douyinPeerAvgDays = days;
            douyinPeerAvg = resolveDouyinPeerAvg(days);
        }
        return douyinPeerAvg;
    }

    /**
     * 按行业层级查询同行平均值，不到通用行业（tradeId=1）。
     * 仅支持单场模式（sourceType=0/1），对比模式返回 null。
     */
    private String resolveDouyinPeerAvg(int days) {
        Integer sourceType = askRequestBo.getSourceType();
        if (sourceType == null || sourceType == 2) return null;

        Long tradeId = resolveTradeId();
        if (tradeId == null) return null;

        String tradeName = getTradeName();

        // 构建行业链：当前 → 父级 → ...（排除通用行业 tradeId=1）
        List<Long> tradeIds = new ArrayList<>();
        tradeIds.add(tradeId);
        List<TradeInfoVo> parents = getTradeProducer().listParentsByTradeId(tradeId, 0);
        if (CollUtil.isNotEmpty(parents)) {
            for (TradeInfoVo p : parents) {
                if (!Objects.equals(p.getId(), 1L)) {
                    tradeIds.add(p.getId());
                }
            }
        }

        DouyinPeerAvgBll bll = getDouyinPeerAvgBll();
        for (Long tid : tradeIds) {
            List<DouyinPeerAvgVo> data = bll.getPeerAvgByTradeAndDays(tid, days);
            if (CollUtil.isNotEmpty(data)) {
                return bll.formatPeerAvgForAi(data, tradeName, days);
            }
        }
        return null;
    }

    // ===== Top N 通用方法 =====

    /**
     * 通用差值路由，按 sourceType 分发。
     *
     * @param extractor 从 MinuteSegmentVo 提取指标值
     * @param increase  true=提升, false=降低
     * @param asInt     在线人数为 int 字符串，需特殊处理 0 值
     * @param topN      取前 N 段
     * @param skipEnds  是否跳过首段和末两段（在线人数/率专用）
     */
    private String resolveDeltaTop(Function<MinuteSegmentVo, Double> extractor,
                                   boolean increase, boolean asInt, int topN, boolean skipEnds) {
        return resolveDeltaTop(extractor, increase, asInt, topN, false, skipEnds);
    }

    /**
     * @param asRate   是否为率（在线提升率/降低率需除以上一段）
     * @param skipEnds 是否跳过首段和末两段
     */
    private String resolveDeltaTop(Function<MinuteSegmentVo, Double> extractor,
                                   boolean increase, boolean asInt, int topN, boolean asRate, boolean skipEnds) {
        Integer sourceType = askRequestBo.getSourceType();
        String sourceId = askRequestBo.getSourceId();
        if (sourceType == null || sourceId == null) return null;

        if (sourceType == 0 || sourceType == 1) {
            return computeDeltaSegments(getMinuteSegments(), extractor, increase, asInt, topN, asRate, skipEnds);
        } else if (sourceType == 2) {
            SyncContrastInfoVo contrast = getContrastInfo();
            if (contrast == null) return null;
            List<MinuteSegmentVo> side1 = resolveMinuteSegmentsForSide(
                    contrast.getVideoOneId(), contrast.getFileOneId());
            List<MinuteSegmentVo> side2 = resolveMinuteSegmentsForSide(
                    contrast.getVideoTwoId(), contrast.getFileTwoId());
            String text1 = computeDeltaSegments(side1, extractor, increase, asInt, topN, asRate, skipEnds);
            String text2 = computeDeltaSegments(side2, extractor, increase, asInt, topN, asRate, skipEnds);
            if (StrUtil.isBlank(text1) && StrUtil.isBlank(text2)) return null;
            StringBuilder sb = new StringBuilder();
            if (StrUtil.isNotBlank(text1)) sb.append("场次1：\n").append(text1);
            if (StrUtil.isNotBlank(text2)) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("场次2：\n").append(text2);
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * 计算段间差值，按提升/降低排序后返回段落编号。skipEnds 为 true 时跳过首段和末两段。
     */
    private String computeDeltaSegments(List<MinuteSegmentVo> segs,
                                        Function<MinuteSegmentVo, Double> extractor,
                                        boolean increase, boolean asInt, int topN, boolean asRate, boolean skipEnds) {
        int startIdx = skipEnds ? 2 : 1;
        int endIdx = skipEnds ? segs.size() - 2 : segs.size();
        int minSize = skipEnds ? 5 : 2;
        if (CollUtil.isEmpty(segs) || segs.size() < minSize) return null;

        // 收集: (段落index, 差值)
        List<double[]> deltas = new ArrayList<>();
        for (int i = startIdx; i < endIdx; i++) {
            MinuteSegmentVo prev = segs.get(i - 1);
            MinuteSegmentVo curr = segs.get(i);
            Double prevVal = extractor.apply(prev);
            Double currVal = extractor.apply(curr);
            if (prevVal == null || currVal == null) continue;
            if (asInt && (prevVal == 0 || currVal == 0)) continue;

            double diff = increase ? currVal - prevVal : prevVal - currVal;
            if (diff <= 0) continue;

            if (asRate) {
                diff = NumberUtil.round(diff / prevVal * 100, 2).doubleValue();
            }

            deltas.add(new double[]{(double) curr.getIndex(), diff});
        }

        if (deltas.isEmpty()) return null;
        deltas.sort((a, b) -> Double.compare(b[1], a[1]));
        return deltas.stream()
                .limit(topN)
                .map(d -> "段落" + (int) d[0])
                .collect(Collectors.joining("、"));
    }

    // ===== 巨量引擎辅助 =====

    /**
     * 构建段落起始时间 → 巨量增量数据的映射。
     * 复用 OceanEngineDataProducerImpl.convertToIncrementalData 做 fixNonMonotonic + 基线 + per-field max-min。
     */
    private Map<Long, OceanEngineProcessBo> getJuliangDataMap(String videoId,
                                                               List<SentenceMarkVo> vos, long allStartTime) {
        VideoDataViewingConfuseInfoVo data = getVideoDataViewingBll().getOceanEngineDetailsByVideoId(videoId);
        if (data == null || CollUtil.isEmpty(data.getOceanEngineProcessList())) {
            return Collections.emptyMap();
        }

        List<CurveData> curveDataList = new ArrayList<>();
        List<SentenceMarkVo> validVos = new ArrayList<>();
        for (SentenceMarkVo vo : vos) {
            if (vo == null || CollUtil.isEmpty(vo.getItems())) continue;
            Long startTemp = vo.getItems().get(0).getStartTime();
            Long endTemp = vo.getItems().get(vo.getItems().size() - 1).getEndTime();
            if (startTemp == null || endTemp == null) continue;
            CurveData cd = new CurveData();
            cd.setDateTime(allStartTime + startTemp);
            curveDataList.add(cd);
            validVos.add(vo);
        }
        if (curveDataList.isEmpty()) return Collections.emptyMap();

        Date startTime = new Date(allStartTime + validVos.get(0).getItems().get(0).getStartTime());
        Date endTime = new Date(allStartTime + validVos.get(validVos.size() - 1).getItems()
                .get(validVos.get(validVos.size() - 1).getItems().size() - 1).getEndTime());

        List<OceanEngineProcessBo> incrementalList = getOceanEngineDataProducer()
                .convertToIncrementalData(data.getOceanEngineProcessList(), curveDataList, startTime, endTime);

        return incrementalList.stream()
                .filter(bo -> bo.getGatherTimeStamp() != null)
                .collect(Collectors.toMap(OceanEngineProcessBo::getGatherTimeStamp, Function.identity(), (a, b) -> b));
    }

    /**
     * 去掉文本中的所有标点符号和特殊符号，用于语速计算
     */
    private String removePunctuation(String text) {
        if (StrUtil.isBlank(text)) return "";
        return text.replaceAll("[\\p{P}\\p{S}]", "");
    }

    // ===== 内部辅助 =====

    /**
     * 当前请求是否为对比模式（sourceType=2）
     */
    private boolean isContrast() {
        return askRequestBo != null && Integer.valueOf(2).equals(askRequestBo.getSourceType());
    }

    // ---- 单场 resolve ----

    /**
     * 单场平台解析。
     * sourceType=0 → getSingleVideo().platformType
     * sourceType=1 → getSingleFile().platformType
     */
    private String resolveSinglePlatform() {
        Integer sourceType = askRequestBo.getSourceType();
        if (sourceType == null) return null;
        if (sourceType == 0) {
            AnchorVideoInfoVo video = getSingleVideo();
            return video != null ? getPlatformLabel(video.getPlatformType()) : null;
        } else if (sourceType == 1) {
            UploadFileInfoVo file = getSingleFile();
            return file != null ? getPlatformLabel(file.getPlatformType()) : null;
        }
        return null;
    }

    /**
     * 通过字典 replay_platform_type 获取平台中文名
     *
     * @param platformCode 平台code（WordsEnum.platformType: "1"=抖音/"2"=快手/"3"=视频号）
     */
    private String getPlatformLabel(String platformCode) {
        if (StrUtil.isEmpty(platformCode)) return null;
        DictDataListVo dict = getDictDataFeign().dictDataByValue("replay_platform_type", platformCode);
        return dict != null ? dict.getLabel() : null;
    }

    /**
     * 单场行业解析。
     * sourceType=0 → getSingleVideo().tradeId → TradeBll
     * sourceType=1 → getSingleFile().tradeId → TradeBll
     */
    private String resolveSingleTrade() {
        Long tradeId = null;
        Integer sourceType = askRequestBo.getSourceType();
        if (sourceType == null) return null;
        if (sourceType == 0) {
            AnchorVideoInfoVo video = getSingleVideo();
            tradeId = video != null ? video.getTradeId() : null;
        } else if (sourceType == 1) {
            UploadFileInfoVo file = getSingleFile();
            tradeId = file != null ? file.getTradeId() : null;
        }
        if (tradeId == null) return null;
        TradeInfoVo info = getTradeBll().info(tradeId).getData();
        return info != null ? info.getName() : null;
    }

    // ---- 对比 resolve ----

    /**
     * 对比行业：直接用 contrast.tradeOneId / tradeTwoId
     */
    private String resolveContrastTrade() {
        SyncContrastInfoVo contrast = getContrastInfo();
        if (contrast == null) return null;

        TradeBll bll = getTradeBll();
        String name1 = getTradeNameById(contrast.getTradeOneId());
        String name2 = getTradeNameById(contrast.getTradeTwoId());
        name1 = ObjectUtil.defaultIfEmpty(name1, "未知行业");
        name2 = ObjectUtil.defaultIfEmpty(name2, "未知行业");

        this.tradeNameOne = name1;
        this.tradeNameTwo = name2;

        if (name1.equals(name2)) {
            return "都是" + name1 + "行业";
        }
        return "场次1的行业是" + name1 + "行业，场次2的行业是" + name2 + "行业";
    }

    /**
     * 对比平台：从对比记录中动态获取视频或文件的平台信息
     */
    private String resolveContrastPlatform() {
        SyncContrastInfoVo contrast = getContrastInfo();
        if (contrast == null) return null;

        String p1 = null, p2 = null;

        if (ObjectUtil.isNotEmpty(contrast.getVideoOneId()) && contrast.getVideoOneInfo() != null) {
            p1 = getPlatformLabel(contrast.getVideoOneInfo().getPlatformType());
        } else if (ObjectUtil.isNotEmpty(contrast.getFileOneId()) && contrast.getFileOneInfo() != null) {
            p1 = getPlatformLabel(contrast.getFileOneInfo().getPlatformType());
        }

        if (ObjectUtil.isNotEmpty(contrast.getVideoTwoId()) && contrast.getVideoTwoInfo() != null) {
            p2 = getPlatformLabel(contrast.getVideoTwoInfo().getPlatformType());
        } else if (ObjectUtil.isNotEmpty(contrast.getFileTwoId()) && contrast.getFileTwoInfo() != null) {
            p2 = getPlatformLabel(contrast.getFileTwoInfo().getPlatformType());
        }

        if (StrUtil.isEmpty(p1) && StrUtil.isEmpty(p2)) return null;

        p1 = ObjectUtil.defaultIfEmpty(p1, "未知平台");
        p2 = ObjectUtil.defaultIfEmpty(p2, "未知平台");

        if (p1.equals(p2)) {
            return "都是" + p1;
        }
        return "场次1的平台是" + p1 + "，场次2的平台是" + p2;
    }

    // ---- 通用工具 ----

    /**
     * 按 tradeId 查行业名称
     */
    private String getTradeNameById(Long tradeId) {
        if (tradeId == null) return null;
        TradeInfoVo info = getTradeBll().info(tradeId).getData();
        return info != null ? info.getName() : null;
    }

    /**
     * 获取对比时场次1的行业原始名称
     */
    public String getTradeNameOne() {
        return tradeNameOne;
    }

    /**
     * 获取对比时场次2的行业原始名称
     */
    public String getTradeNameTwo() {
        return tradeNameTwo;
    }
}
