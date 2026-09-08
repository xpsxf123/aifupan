package com.jiuyu.replay.api.service.aiagent.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.api.service.aiagent.AiAgentInfluencerVideoService;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import com.jiuyu.replay.generic.bo.aiagent.AiAgentBaseBo;
import com.jiuyu.replay.generic.bo.aiagent.HotSearchAnalyticsOverviewBo;
import com.jiuyu.replay.generic.bo.aiagent.HotSearchKeywordQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.HotSearchVideoQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerAnalyticsOverviewBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerDetailBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerListQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerSearchBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerVideoStatsBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoAudioTextBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoDetailBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoGlobalSearchBo;
import com.jiuyu.replay.generic.vo.aiagent.CursorPageVo;
import com.jiuyu.replay.generic.vo.aiagent.HotSearchAnalyticsVo;
import com.jiuyu.replay.generic.vo.aiagent.HotSearchKeywordVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerAnalyticsVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerDetailBatchVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerItemVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerVideoItemVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerVideoStatsVo;
import com.jiuyu.replay.generic.vo.aiagent.StreamPageVo;
import com.jiuyu.replay.generic.vo.aiagent.VideoAudioTextVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.video.project.dao.VideoHotSearchDao;
import com.jiuyu.replay.video.project.dao.VideoInfoDao.ClipMetricRow;
import com.jiuyu.replay.video.project.dao.VideoInfoDao.HotSearchClipRow;
import com.jiuyu.replay.video.project.dao.VideoInfoDao.HotSearchVideoRow;
import com.jiuyu.replay.video.project.dao.VideoInfluencerInfoDao;
import com.jiuyu.replay.video.project.dao.VideoInfoDao;
import com.jiuyu.replay.video.project.dao.VideoInfoDao.AuthorVideoCountResult;
import com.jiuyu.replay.video.project.dao.VideoInfoDao.AuthorVideoStatsResult;
import com.jiuyu.replay.video.project.dao.VideoInfoDao.VideoCollectTimeRow;
import com.jiuyu.replay.video.project.dao.VideoInfoDao.VideoHashRow;
import com.jiuyu.replay.video.project.dao.VideoUserHotSubscriptionDao;
import com.jiuyu.replay.video.project.document.VideoContentExtract;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEntity;
import com.jiuyu.replay.video.project.entity.VideoInfluencerInfoEntity;
import com.jiuyu.replay.video.project.entity.VideoInfoEntity;
import com.jiuyu.replay.video.project.entity.VideoUserHotSubscriptionEntity;
import com.jiuyu.replay.video.project.repository.VideoExtractContentRepository;
import com.jiuyu.replay.words.producer.TradeProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI Agent 达人短视频开放接口服务实现。
 *
 * <p>统一遵循「关联查询先取 id → 单表批量捞取 → 内存装配」，避免宽表 JOIN + GROUP BY。
 * 身份显式从入参取，租户隔离叠加软删除；子账号（userType=2）再叠加 userId。</p>
 *
 * <p><b>租户可见范围与文案授权口径</b>（两者是相互独立的两层，勿混用）：</p>
 * <ul>
 *   <li><b>可见范围</b> = 本租户已订阅达人（{@code tb_video_user_influencer_subscription}）名下的<b>全部</b>
 *       {@code tb_video_info}。订阅即可见，与该租户是否提取过文案无关。</li>
 *   <li><b>文案授权</b> = 该视频在本租户下已提取<b>成功</b>（{@code tb_video_user_video.extract_status=3}）。
 *       {@code tb_video_user_video} 只在该租户发起过提取时才有记录（成功、失败都会落行），
 *       故它是判定「本租户能否读该视频文案」的唯一依据。未授权时文案字段一律返回 null。</li>
 *   <li>租户链路对外的 {@code extractStatus} 亦为<b>租户口径</b>（1=本租户已提取成功 / 0=其余），
 *       保证调用方看到 1 就一定能取到文案。</li>
 *   <li><b>全库爬取库检索不做任何租户限制</b>：{@code collectedOnly=false} 的短视频检索、爆款视频列表
 *       读的是跨租户公共爬取数据，{@code extractStatus} 取 {@code tb_video_info} 原值、文案按
 *       {@code withAudioText} 原样返回，不叠加订阅关系与提取授权。</li>
 * </ul>
 *
 * @author fupan-server
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentInfluencerVideoServiceImpl implements AiAgentInfluencerVideoService {

    private static final String LOG_PREFIX = "[AI-AGENT][INBOUND]";
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * 达人短视频列表专用页大小上限：用于「拆解达人全部短视频」，需一次性拉更多，故放宽到 5000。
     */
    private static final int MAX_VIDEO_PAGE_SIZE = 5000;

    /**
     * 主键 IN 单批上限：pageIds 批量回捞时按此分批，避免超长 IN（项目红线 IN>500）。
     */
    private static final int ID_IN_BATCH_SIZE = 1000;

    /**
     * 对外文案提取状态：1=本租户已提取成功（可读文案）；0=其余（未提取 / 处理中 / 失败）。
     */
    private static final byte EXTRACT_STATUS_DONE = 1;
    private static final byte EXTRACT_STATUS_NONE = 0;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * author_id IN 过滤的最大长度；超过则截断并告警，避免超长 IN（项目红线 IN>500）。
     */
    private static final int MAX_AUTHOR_IN = 500;

    /**
     * 全库/爆款搜索「仅首页(cursor==null)」短期缓存前缀（结果与调用方身份无关，跨租户共享）。
     * 只缓存第一页——热点重复请求的主要来源；深翻页游标各异不缓存，避免键爆炸与串号。
     */
    private static final String CACHE_PREFIX_VIDEO_SEARCH = "replay:business:aiagent:videoSearch:p1:";
    private static final String CACHE_PREFIX_INFLUENCER_SEARCH = "replay:business:aiagent:influencerSearch:p1:";
    private static final String CACHE_PREFIX_HOT_KEYWORD = "replay:business:aiagent:hotKeyword:p1:";
    private static final String CACHE_PREFIX_HOT_VIDEO = "replay:business:aiagent:hotVideo:p1:";

    /**
     * 首页缓存 TTL（秒）：仅挡高频重复首页请求，短 TTL 保证实时性。
     */
    private static final int FIRST_PAGE_CACHE_TTL_SECONDS = 60;

    /**
     * 游标内部分隔符（排序值 与 id 之间），编码后 Base64，对外不透明。
     */
    private static final char CURSOR_SEP = '\u0001';

    /**
     * 统计概览：单达人时间窗内作品兜底上限（防海量作品打爆内存；命中即截断并告警）。
     */
    private static final int ANALYTICS_MAX_CLIPS = 20000;

    /**
     * 统计窗口跨度硬上限（天，≤1 年）；fupan 第三层兜底夹取，不做无界扫描（见 doc17 §6.20 时间范围约束）。
     */
    private static final int MAX_WINDOW_DAYS = 366;

    /**
     * 爆款阈值倍数：阈值 = 该倍数 × 中位互动量（中位为 0 时不识别爆款）。
     */
    private static final int BREAKOUT_MEDIAN_MULTIPLE = 3;

    /**
     * 互动趋势最多返回的周点数（降采样上限，保留最近 N 周）。
     */
    private static final int MAX_TREND_POINTS = 16;

    /**
     * 时长分布固定桶标签（秒），含 count=0 桶以稳定前端坐标轴。
     */
    private static final String[] DURATION_BUCKET_LABELS = {"0-15s", "15-30s", "30-60s", "60-180s", "180s+"};

    /**
     * 趋势周起始日格式。
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 爆款选题拆解（§6.21）首页短期缓存前缀。结果含租户口径字段 tenantExtractedCount，键必带 tenantId。
     */
    private static final String CACHE_PREFIX_HOT_ANALYTICS = "replay:business:aiagent:hotAnalytics:";

    /**
     * 爆款选题拆解默认统计窗口（天）：不传时间窗时取近 7 天，对齐前端默认档位。
     */
    private static final int HOT_ANALYTICS_DEFAULT_WINDOW_DAYS = 7;

    /**
     * 趋势粒度切换阈值（天）：窗口跨度 ≤ 该值按天聚合，否则按自然周。
     */
    private static final int TREND_DAY_MAX_SPAN_DAYS = 15;

    /**
     * 按天聚合时最多返回的点数（保留最近 N 天）。
     */
    private static final int MAX_TREND_DAY_POINTS = 31;

    /**
     * 趋势粒度对外取值。
     */
    private static final String TREND_GRANULARITY_DAY = "day";
    private static final String TREND_GRANULARITY_WEEK = "week";

    /**
     * 选题下头部达人 / 高频标签的返回条数上限。
     */
    private static final int TOP_AUTHOR_LIMIT = 10;
    private static final int TOP_TAG_LIMIT = 20;

    /**
     * 集中度口径：取互动量最高的前 N 条算占比。
     */
    private static final int TOP_SHARE_HEAD_COUNT = 10;

    /**
     * 标题内 #标签 抽取正则：从 # 抽到空白 / 中英标点 / 下一个 # 为止，长度上限 30。
     * 确定性抽取，<b>不做中文分词 / n-gram</b> —— 宁可少给，也不拼出无意义碎片。
     */
    private static final Pattern TITLE_TAG_PATTERN =
            Pattern.compile("#([^#\\s，,。、!！?？:：;；]{1,30})");

    private final VideoInfluencerInfoDao videoInfluencerInfoDao;
    private final VideoInfoDao videoInfoDao;
    private final VideoHotSearchDao videoHotSearchDao;
    private final VideoUserHotSubscriptionDao videoUserHotSubscriptionDao;
    private final VideoExtractContentRepository videoExtractContentRepository;
    private final ResilientRedisTemplate<String, Object> resilientRedisTemplate;
    private final TradeProducer tradeProducer;

    @Override
    public R<CursorPageVo<InfluencerItemVo>> influencerList(InfluencerListQueryBo bo) {
        R<CursorPageVo<InfluencerItemVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        int pageSize = clampPageSize(bo.getPageSize());

        // 1) 取达人 id + 行业 id 分页：来源 = 租户达人订阅（id-first，只按 tenantId、不过滤用户）
        List<VideoInfluencerInfoDao.TenantInfluencerRow> rows = videoInfluencerInfoDao.pageTenantInfluencerIds(
                bo.getTenantId(), bo.getCursor(),
                bo.getPlatformTypeList(), bo.getKeyword(), bo.getInfluencerIdList(),
                bo.getCollectStartTime(), bo.getCollectEndTime(), bo.getIndustryId(), bo.getCollectAccountId(), pageSize + 1);
        if (CollUtil.isEmpty(rows)) {
            return R.ok(CursorPageVo.empty());
        }
        boolean hasMore = rows.size() > pageSize;
        List<VideoInfluencerInfoDao.TenantInfluencerRow> pageRows = hasMore ? new ArrayList<>(rows.subList(0, pageSize)) : rows;
        List<Long> pageIds = pageRows.stream().map(VideoInfluencerInfoDao.TenantInfluencerRow::getInfluencerId)
                .collect(Collectors.toList());
        Long nextCursor = hasMore ? pageIds.get(pageIds.size() - 1) : null;

        // 2) 单表批量捞达人明细（含 last_sync_time = 最近采集时间）
        Map<Long, VideoInfluencerInfoEntity> infMap = videoInfluencerInfoDao.selectBatchIds(pageIds).stream()
                .collect(Collectors.toMap(VideoInfluencerInfoEntity::getId, Function.identity(), (a, b) -> a));

        // 3) 批量回填行业名（去重非空 industryId → 行业库，无 N+1）
        Map<Long, String> industryNameMap = loadIndustryNames(pageRows);

        // 4) 内存装配，保持分页顺序
        List<InfluencerItemVo> list = new ArrayList<>(pageRows.size());
        for (VideoInfluencerInfoDao.TenantInfluencerRow row : pageRows) {
            Long id = row.getInfluencerId();
            VideoInfluencerInfoEntity e = infMap.get(id);
            if (e == null) {
                continue;
            }
            InfluencerItemVo vo = buildInfluencerItemVo(e);
            vo.setIndustryId(row.getIndustryId());
            vo.setIndustryName(row.getIndustryId() == null ? null : industryNameMap.get(row.getIndustryId()));
            vo.setLastCollectTime(formatDateTime(e.getLastSyncTime()));
            vo.setAccountType(row.getAccountType());
            list.add(vo);
        }
        return R.ok(CursorPageVo.of(list, nextCursor, hasMore));
    }

    @Override
    public R<List<VideoAudioTextVo>> batchVideoAudioText(VideoAudioTextBatchBo bo) {
        R<List<VideoAudioTextVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        if (CollUtil.isEmpty(bo.getVideoIdList())) {
            return R.error("videoIdList 不能为空");
        }
        List<VideoHashRow> rows = videoInfoDao.listVideoHashByIds(bo.getVideoIdList());
        if (CollUtil.isEmpty(rows)) {
            return R.ok(Collections.emptyList());
        }
        // 文案授权：仅本租户提取成功（tb_video_user_video.extract_status=3）的视频才允许取文案，其余返回空内容
        Set<Long> authorized = loadTenantExtractedVideoIds(bo.getTenantId(),
                rows.stream().map(VideoHashRow::getId).collect(Collectors.toList()));
        Map<String, VideoContentExtract> extractMap = loadExtractMap(rows.stream()
                .filter(r -> authorized.contains(r.getId()))
                .map(VideoHashRow::getVideoHash).filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList()));

        List<VideoAudioTextVo> list = rows.stream().map(r -> {
            VideoAudioTextVo vo = new VideoAudioTextVo();
            vo.setVideoId(r.getId());
            vo.setVideoHash(r.getVideoHash());
            VideoContentExtract ce = authorized.contains(r.getId()) && StrUtil.isNotBlank(r.getVideoHash())
                    ? extractMap.get(r.getVideoHash()) : null;
            if (ce != null) {
                vo.setOriginalAudioContent(ce.getOriginalAudioContent());
                vo.setAudioContent(ce.getAudioContent());
            }
            return vo;
        }).collect(Collectors.toList());
        return R.ok(list);
    }

    @Override
    public R<List<InfluencerVideoStatsVo>> batchInfluencerVideoStats(InfluencerVideoStatsBatchBo bo) {
        R<List<InfluencerVideoStatsVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        if (CollUtil.isEmpty(bo.getInfluencerIdList())) {
            return R.error("influencerIdList 不能为空");
        }
        // collectedOnly=true：租户口径 —— 先把达人收窄到「本租户已订阅」子集，未订阅达人统计恒为 0
        List<Long> influencerIds = Boolean.TRUE.equals(bo.getCollectedOnly())
                ? videoInfluencerInfoDao.selectSubscribedInfluencerIds(bo.getTenantId(), null, bo.getInfluencerIdList())
                : bo.getInfluencerIdList();
        if (CollUtil.isEmpty(influencerIds)) {
            return R.ok(emptyVideoStats(bo.getInfluencerIdList()));
        }
        List<String> authorIds = influencerIds.stream().map(String::valueOf).collect(Collectors.toList());
        Map<String, AuthorVideoStatsResult> statMap = videoInfoDao.batchStatsByAuthorIds(
                        authorIds, bo.getPublishStartTime(), bo.getPublishEndTime()).stream()
                .collect(Collectors.toMap(AuthorVideoStatsResult::getAuthorId, Function.identity(), (a, b) -> a));

        List<InfluencerVideoStatsVo> list = bo.getInfluencerIdList().stream()
                .map(influencerId -> buildVideoStatsVo(influencerId, statMap.get(String.valueOf(influencerId))))
                .collect(Collectors.toList());
        return R.ok(list);
    }

    /**
     * 全零统计（达人未被本租户订阅 / 无匹配作品时的占位），保持出参与入参达人一一对应。
     */
    private List<InfluencerVideoStatsVo> emptyVideoStats(List<Long> influencerIds) {
        return influencerIds.stream().map(id -> buildVideoStatsVo(id, null)).collect(Collectors.toList());
    }

    /**
     * 装配单个达人的短视频统计项；统计缺失时各项补 0。
     */
    private InfluencerVideoStatsVo buildVideoStatsVo(Long influencerId, AuthorVideoStatsResult s) {
        InfluencerVideoStatsVo vo = new InfluencerVideoStatsVo();
        vo.setInfluencerId(influencerId);
        vo.setVideoCount(s == null || s.getVideoCount() == null ? 0 : s.getVideoCount());
        vo.setTotalLikeCount(s == null || s.getTotalLikeCount() == null ? 0L : s.getTotalLikeCount());
        vo.setTotalCommentCount(s == null || s.getTotalCommentCount() == null ? 0L : s.getTotalCommentCount());
        vo.setTotalShareCount(s == null || s.getTotalShareCount() == null ? 0L : s.getTotalShareCount());
        vo.setTotalCollectCount(s == null || s.getTotalCollectCount() == null ? 0L : s.getTotalCollectCount());
        return vo;
    }

    @Override
    public R<StreamPageVo<InfluencerVideoItemVo>> globalVideoSearch(VideoGlobalSearchBo bo) {
        R<StreamPageVo<InfluencerVideoItemVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        int pageSize = clampPageSize(bo.getPageSize(), MAX_VIDEO_PAGE_SIZE);
        // 租户范围（collectedOnly=true）：限定在本租户已订阅达人名下，走 XML 检索（同款 keyword/排序/keyset），不入缓存。
        if (Boolean.TRUE.equals(bo.getCollectedOnly())) {
            return R.ok(doSubscribedVideoSearch(bo, pageSize));
        }
        // 仅首页(cursor==null)短期缓存挡热点重复请求；深翻页游标各异不缓存
        if (StrUtil.isNotBlank(bo.getCursor()) || EmptyUtil.isNotEmpty(bo.getPageSize())) {
            return R.ok(doGlobalVideoSearch(bo, pageSize));
        }
        String cacheKey = buildCacheKey(CACHE_PREFIX_VIDEO_SEARCH, pageSize,
                sortedCopy(bo.getInfluencerIdList()), sortedCopy(bo.getPlatformUserIdList()),
                sortedCopy(bo.getPlatformAccountList()), sortedCopy(bo.getPlatformTypeList()), bo.getKeyword(),
                sortedCopy(bo.getTagList()), bo.getLikeCountMin(), bo.getCommentCountMin(), bo.getShareCountMin(),
                bo.getCollectCountMin(), bo.getDurationMin(), bo.getDurationMax(), bo.getPublishStartTime(),
                bo.getPublishEndTime(), sortedCopy(bo.getExtractStatusList()), bo.getSortField(), bo.getSortDesc(),
                bo.getWithAudioText());
        return R.ok(fromCacheOrLoad(cacheKey, FIRST_PAGE_CACHE_TTL_SECONDS,
                new TypeReference<StreamPageVo<InfluencerVideoItemVo>>() {
                }, () -> doGlobalVideoSearch(bo, pageSize)));
    }

    /**
     * 本租户可见范围内的短视频检索：可见范围 = 本租户已订阅达人名下的全部作品。订阅达人集合在 Java 侧解析成
     * author_id 绑定参数（不在 SQL 里 CAST 比较，避免 collation 冲突）。keyword / 动态排序 / 复合 keyset 分页
     * 全部在 mapper XML 内完成，游标语义与全库检索一致（opaque token）。文案按「本租户提取成功」授权返回。
     */
    private StreamPageVo<InfluencerVideoItemVo> doSubscribedVideoSearch(VideoGlobalSearchBo bo, int pageSize) {
        int field = bo.getSortField() == null ? 0 : bo.getSortField();
        boolean desc = bo.getSortDesc() == null || bo.getSortDesc();
        List<String> authorIds = resolveSubscribedAuthorIds(bo);
        if (CollUtil.isEmpty(authorIds)) {
            return StreamPageVo.empty();   // 未订阅任何达人，或达人维度过滤后与订阅无交集
        }
        Object[] cursor = decodeCursor(bo.getCursor(), field, field >= 1 && field <= 4);
        Object cursorSortVal = cursor == null ? null : cursor[0];
        Long cursorId = cursor == null ? null : (Long) cursor[1];
        boolean idOnly = field == 0 || cursorSortVal == null;

        List<VideoInfoEntity> rows = videoInfoDao.pageSubscribedInfluencerVideos(
                bo.getTenantId(), authorIds,
                bo.getPlatformTypeList(), resolveTenantExtractedFilter(bo.getExtractStatusList()),
                StrUtil.isNotBlank(bo.getKeyword()) ? bo.getKeyword() : null,
                bo.getPublishStartTime(), bo.getPublishEndTime(),
                field, desc, idOnly, cursorSortVal, cursorId, pageSize + 1);
        if (CollUtil.isEmpty(rows)) {
            return StreamPageVo.empty();
        }
        boolean hasMore = rows.size() > pageSize;
        List<VideoInfoEntity> pageRows = hasMore ? rows.subList(0, pageSize) : rows;
        List<InfluencerVideoItemVo> list = assembleVideoItems(bo.getTenantId(), pageRows,
                Boolean.TRUE.equals(bo.getWithAudioText()));
        VideoInfoEntity last = pageRows.get(pageRows.size() - 1);
        String nextCursor = hasMore ? encodeCursor(field, videoSortVal(field, last), last.getId()) : null;
        return StreamPageVo.of(list, nextCursor, hasMore);
    }

    private StreamPageVo<InfluencerVideoItemVo> doGlobalVideoSearch(VideoGlobalSearchBo bo, int pageSize) {
        int field = bo.getSortField() == null ? 0 : bo.getSortField();
        boolean desc = bo.getSortDesc() == null || bo.getSortDesc();
        boolean isIdSort = field == 0;
        SFunction<VideoInfoEntity, ?> sortCol = videoSortColumn(field);

        LambdaQueryWrapper<VideoInfoEntity> wrapper = new LambdaQueryWrapper<>();
        // 达人维度（达人id / secUid / 抖音号）统一解析为 author_id 交集
        List<String> authorIds = resolveAuthorIdFilter(bo.getInfluencerIdList(),
                bo.getPlatformUserIdList(), bo.getPlatformAccountList());
        if (authorIds != null && authorIds.isEmpty()) {
            return StreamPageVo.empty();   // 达人维度过滤后无匹配达人
        }
        if (authorIds != null) {
            wrapper.in(VideoInfoEntity::getAuthorId, authorIds);
        }
        if (CollUtil.isNotEmpty(bo.getPlatformTypeList())) {
            wrapper.in(VideoInfoEntity::getPlatformType, bo.getPlatformTypeList());
        }
        if (StrUtil.isNotBlank(bo.getKeyword())) {
            String kw = bo.getKeyword();
            wrapper.and(w -> w.like(VideoInfoEntity::getTitle, kw).or().like(VideoInfoEntity::getDescription, kw));
        }
        applyTitleTagFilter(wrapper, bo.getTagList());
        wrapper.ge(bo.getLikeCountMin() != null, VideoInfoEntity::getLikeCount, bo.getLikeCountMin());
        wrapper.ge(bo.getCommentCountMin() != null, VideoInfoEntity::getCommentCount, bo.getCommentCountMin());
        wrapper.ge(bo.getShareCountMin() != null, VideoInfoEntity::getShareCount, bo.getShareCountMin());
        wrapper.ge(bo.getCollectCountMin() != null, VideoInfoEntity::getCollectCount, bo.getCollectCountMin());
        wrapper.ge(bo.getDurationMin() != null, VideoInfoEntity::getDuration, bo.getDurationMin());
        wrapper.le(bo.getDurationMax() != null, VideoInfoEntity::getDuration, bo.getDurationMax());
        wrapper.ge(StrUtil.isNotBlank(bo.getPublishStartTime()), VideoInfoEntity::getPublishTime, bo.getPublishStartTime());
        wrapper.le(StrUtil.isNotBlank(bo.getPublishEndTime()), VideoInfoEntity::getPublishTime, bo.getPublishEndTime());
        if (CollUtil.isNotEmpty(bo.getExtractStatusList())) {
            wrapper.in(VideoInfoEntity::getExtractStatus, bo.getExtractStatusList());
        }
        Object[] cursor = decodeCursor(bo.getCursor(), field, field >= 1 && field <= 4);
        if (cursor != null) {
            boolean idOnly = isIdSort || cursor[0] == null;
            applyKeyset(wrapper, sortCol, VideoInfoEntity::getId, desc, idOnly, cursor[0], (Long) cursor[1]);
        }
        applyOrder(wrapper, sortCol, VideoInfoEntity::getId, desc, isIdSort);
        wrapper.last("LIMIT " + (pageSize + 1));

        List<VideoInfoEntity> rows = videoInfoDao.selectList(wrapper);
        if (CollUtil.isEmpty(rows)) {
            return StreamPageVo.empty();
        }
        boolean hasMore = rows.size() > pageSize;
        List<VideoInfoEntity> pageRows = hasMore ? rows.subList(0, pageSize) : rows;
        // 全库爬取库：跨租户公共数据，不叠加订阅关系与提取授权，extractStatus / 文案原样返回
        List<InfluencerVideoItemVo> list = assembleGlobalVideoItems(pageRows,
                Boolean.TRUE.equals(bo.getWithAudioText()));
        VideoInfoEntity last = pageRows.get(pageRows.size() - 1);
        String nextCursor = hasMore ? encodeCursor(field, videoSortVal(field, last), last.getId()) : null;
        return StreamPageVo.of(list, nextCursor, hasMore);
    }

    @Override
    public R<List<InfluencerVideoItemVo>> videoDetailBatch(VideoDetailBatchBo bo) {
        R<List<InfluencerVideoItemVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        if (CollUtil.isEmpty(bo.getVideoIdList())) {
            return R.error("videoIdList 不能为空");
        }
        List<VideoInfoEntity> videos = videoInfoDao.selectBatchIds(bo.getVideoIdList());
        if (CollUtil.isEmpty(videos)) {
            return R.ok(Collections.emptyList());
        }
        Map<Long, VideoInfoEntity> videoMap = videos.stream()
                .collect(Collectors.toMap(VideoInfoEntity::getId, Function.identity(), (a, b) -> a));
        List<VideoInfoEntity> ordered = bo.getVideoIdList().stream().distinct()
                .map(videoMap::get).filter(Objects::nonNull).collect(Collectors.toList());
        return R.ok(assembleVideoItems(bo.getTenantId(), ordered, Boolean.TRUE.equals(bo.getWithAudioText())));
    }

    @Override
    public R<StreamPageVo<InfluencerItemVo>> influencerSearch(InfluencerSearchBo bo) {
        R<StreamPageVo<InfluencerItemVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        int pageSize = clampPageSize(bo.getPageSize());
        // 仅首页(cursor==null)短期缓存挡热点重复请求；深翻页游标各异不缓存
        if (StrUtil.isNotBlank(bo.getCursor())) {
            return R.ok(doInfluencerSearch(bo, pageSize));
        }
        String cacheKey = buildCacheKey(CACHE_PREFIX_INFLUENCER_SEARCH, pageSize,
                sortedCopy(bo.getPlatformTypeList()), bo.getKeyword(), bo.getFollowersCountMin(),
                bo.getSortField(), bo.getSortDesc());
        return R.ok(fromCacheOrLoad(cacheKey, FIRST_PAGE_CACHE_TTL_SECONDS,
                new TypeReference<StreamPageVo<InfluencerItemVo>>() {
                }, () -> doInfluencerSearch(bo, pageSize)));
    }

    private StreamPageVo<InfluencerItemVo> doInfluencerSearch(InfluencerSearchBo bo, int pageSize) {
        int field = bo.getSortField() != null && bo.getSortField() == 1 ? 1 : 0;
        boolean desc = bo.getSortDesc() == null || bo.getSortDesc();
        SFunction<VideoInfluencerInfoEntity, ?> sortCol =
                field == 1 ? VideoInfluencerInfoEntity::getLikeCount : VideoInfluencerInfoEntity::getFollowersCount;

        LambdaQueryWrapper<VideoInfluencerInfoEntity> wrapper = new LambdaQueryWrapper<>();
        if (CollUtil.isNotEmpty(bo.getPlatformTypeList())) {
            wrapper.in(VideoInfluencerInfoEntity::getPlatformType, bo.getPlatformTypeList());
        }
        if (StrUtil.isNotBlank(bo.getKeyword())) {
            String kw = bo.getKeyword();
            wrapper.and(w -> w.like(VideoInfluencerInfoEntity::getNickname, kw)
                    .or().like(VideoInfluencerInfoEntity::getPlatformAccount, kw));
        }
        wrapper.ge(bo.getFollowersCountMin() != null, VideoInfluencerInfoEntity::getFollowersCount, bo.getFollowersCountMin());
        Object[] cursor = decodeCursor(bo.getCursor(), field, true);
        if (cursor != null) {
            boolean idOnly = cursor[0] == null;
            applyKeyset(wrapper, sortCol, VideoInfluencerInfoEntity::getId, desc, idOnly, cursor[0], (Long) cursor[1]);
        }
        applyOrder(wrapper, sortCol, VideoInfluencerInfoEntity::getId, desc, false);
        wrapper.last("LIMIT " + (pageSize + 1));

        List<VideoInfluencerInfoEntity> rows = videoInfluencerInfoDao.selectList(wrapper);
        if (CollUtil.isEmpty(rows)) {
            return StreamPageVo.empty();
        }
        boolean hasMore = rows.size() > pageSize;
        List<VideoInfluencerInfoEntity> pageRows = hasMore ? rows.subList(0, pageSize) : rows;
        List<InfluencerItemVo> list = pageRows.stream().map(this::buildInfluencerItemVo).collect(Collectors.toList());
        VideoInfluencerInfoEntity last = pageRows.get(pageRows.size() - 1);
        String sortValStr = field == 1 ? String.valueOf(last.getLikeCount()) : String.valueOf(last.getFollowersCount());
        String nextCursor = hasMore ? encodeCursor(field, sortValStr, last.getId()) : null;
        return StreamPageVo.of(list, nextCursor, hasMore);
    }

    @Override
    public R<StreamPageVo<HotSearchKeywordVo>> hotSearchKeywordList(HotSearchKeywordQueryBo bo) {
        R<StreamPageVo<HotSearchKeywordVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        int pageSize = clampPageSize(bo.getPageSize());
        boolean loadMe = Boolean.TRUE.equals(bo.getLoadMe());
        if (bo.getIndustryId() != null && !loadMe) {
            // 行业维度只存在于租户订阅表；全库词表无此字段。静默忽略会让调用方误以为已过滤，故直接报错
            return R.error("industryId 仅在 loadMe=true 时可用");
        }
        // 仅首页(cursor==null)短期缓存挡热点重复请求；深翻页游标各异不缓存
        // loadMe=true 结果按租户订阅词过滤、因人而异，首页不缓存（现有缓存键不含身份，复用会串租户）
        if (loadMe || StrUtil.isNotBlank(bo.getCursor())) {
            return R.ok(doHotSearchKeywordList(bo, pageSize));
        }
        // industryId 入键只是保险：它当前只在 loadMe=true（不走缓存）路径下可用，走到这里恒为 null；
        // 一旦将来放开，键已经带上了，不会退化成串行业
        String cacheKey = buildCacheKey(CACHE_PREFIX_HOT_KEYWORD, pageSize,
                sortedCopy(bo.getPlatformTypeList()), bo.getKeyword(), bo.getSyncStartTime(), bo.getSyncEndTime(),
                bo.getSortField(), bo.getSortDesc(), bo.getIndustryId());
        return R.ok(fromCacheOrLoad(cacheKey, FIRST_PAGE_CACHE_TTL_SECONDS,
                new TypeReference<StreamPageVo<HotSearchKeywordVo>>() {
                }, () -> doHotSearchKeywordList(bo, pageSize)));
    }

    private StreamPageVo<HotSearchKeywordVo> doHotSearchKeywordList(HotSearchKeywordQueryBo bo, int pageSize) {
        int field = bo.getSortField() != null && bo.getSortField() == 1 ? 1 : 0;
        boolean desc = bo.getSortDesc() == null || bo.getSortDesc();
        SFunction<VideoHotSearchEntity, ?> sortCol =
                field == 1 ? VideoHotSearchEntity::getLastSyncTime : VideoHotSearchEntity::getVideoCount;

        LambdaQueryWrapper<VideoHotSearchEntity> wrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(bo.getLoadMe())) {
            // loadMe=true：仅查当前租户订阅监控的爆款关键词（keyword 来自订阅表），订阅词为空时短路返回空页，勿退化全量
            List<String> subscribedKeywords = listSubscribedKeywords(bo.getTenantId(), bo.getIndustryId());
            if (CollUtil.isEmpty(subscribedKeywords)) {
                return StreamPageVo.empty();
            }
            wrapper.in(VideoHotSearchEntity::getSearchKeyword, subscribedKeywords);
        }
        if (CollUtil.isNotEmpty(bo.getPlatformTypeList())) {
            wrapper.in(VideoHotSearchEntity::getPlatformType, bo.getPlatformTypeList());
        }
        wrapper.like(StrUtil.isNotBlank(bo.getKeyword()), VideoHotSearchEntity::getSearchKeyword, bo.getKeyword());
        wrapper.ge(StrUtil.isNotBlank(bo.getSyncStartTime()), VideoHotSearchEntity::getLastSyncTime, bo.getSyncStartTime());
        wrapper.le(StrUtil.isNotBlank(bo.getSyncEndTime()), VideoHotSearchEntity::getLastSyncTime, bo.getSyncEndTime());
        // 按同步时间排序时排除空值，避免 keyset 比较歧义
        if (field == 1) {
            wrapper.isNotNull(VideoHotSearchEntity::getLastSyncTime);
        }
        Object[] cursor = decodeCursor(bo.getCursor(), field, field == 0);
        if (cursor != null) {
            boolean idOnly = cursor[0] == null;
            applyKeyset(wrapper, sortCol, VideoHotSearchEntity::getId, desc, idOnly, cursor[0], (Long) cursor[1]);
        }
        applyOrder(wrapper, sortCol, VideoHotSearchEntity::getId, desc, false);
        wrapper.last("LIMIT " + (pageSize + 1));

        List<VideoHotSearchEntity> rows = videoHotSearchDao.selectList(wrapper);
        if (CollUtil.isEmpty(rows)) {
            return StreamPageVo.empty();
        }
        boolean hasMore = rows.size() > pageSize;
        List<VideoHotSearchEntity> pageRows = hasMore ? rows.subList(0, pageSize) : rows;
        List<HotSearchKeywordVo> list = pageRows.stream().map(e -> {
            HotSearchKeywordVo vo = new HotSearchKeywordVo();
            vo.setSearchId(e.getId());
            vo.setPlatformType(e.getPlatformType());
            vo.setSearchKeyword(e.getSearchKeyword());
            vo.setVideoCount(e.getVideoCount());
            vo.setLastSyncTime(formatDateTime(e.getLastSyncTime()));
            return vo;
        }).collect(Collectors.toList());
        VideoHotSearchEntity last = pageRows.get(pageRows.size() - 1);
        String sortValStr = field == 1 ? formatDateTime(last.getLastSyncTime()) : String.valueOf(last.getVideoCount());
        String nextCursor = hasMore ? encodeCursor(field, sortValStr, last.getId()) : null;
        return StreamPageVo.of(list, nextCursor, hasMore);
    }

    /**
     * 取当前租户订阅监控的爆款关键词（去重）。订阅表按 tenantId + 可选 industryId 过滤，is_deleted=0 由 MP @TableLogic 自动叠加；
     * 不过滤 is_enabled（订阅过即视为租户关注词，监控启停不影响关键词清单）。
     * 显式 LIMIT 500 截断（同 IN>500 项目红线），超出部分丢弃——订阅词清单天然有限，截断远高于实际数量。
     */
    private List<String> listSubscribedKeywords(Long tenantId, Long industryId) {
        LambdaQueryWrapper<VideoUserHotSubscriptionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(VideoUserHotSubscriptionEntity::getKeyword)
                .eq(VideoUserHotSubscriptionEntity::getTenantId, tenantId)
                .eq(industryId != null, VideoUserHotSubscriptionEntity::getIndustryId, industryId)
                .last("LIMIT 500");
        List<VideoUserHotSubscriptionEntity> subs = videoUserHotSubscriptionDao.selectList(wrapper);
        if (CollUtil.isEmpty(subs)) {
            return Collections.emptyList();
        }
        return subs.stream().map(VideoUserHotSubscriptionEntity::getKeyword)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
    }

    @Override
    public R<StreamPageVo<InfluencerVideoItemVo>> hotSearchVideoList(HotSearchVideoQueryBo bo) {
        R<StreamPageVo<InfluencerVideoItemVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        if (bo.getSearchId() == null) {
            return R.error("searchId 不能为空");
        }
        int pageSize = clampPageSize(bo.getPageSize());
        // 仅首页(cursor==null)短期缓存挡热点重复请求；深翻页游标各异不缓存
        if (StrUtil.isNotBlank(bo.getCursor())) {
            return R.ok(doHotSearchVideoList(bo, pageSize));
        }
        // tenantScopeExtract=true 时 extractStatus / 文案按租户裁剪，结果因人而异 → 键并入 tenantId 分片，
        // 避免复用串租户；false 路径身份位写 null，键仍身份无关、跨租户共享，命中率不退化
        boolean tenantScope = Boolean.TRUE.equals(bo.getTenantScopeExtract());
        String cacheKey = buildCacheKey(CACHE_PREFIX_HOT_VIDEO, tenantScope, tenantScope ? bo.getTenantId() : null,
                bo.getSearchId(), pageSize, bo.getKeyword(),
                sortedCopy(bo.getTagList()), sortedCopy(bo.getPlatformUserIdList()), sortedCopy(bo.getPlatformAccountList()),
                sortedCopy(bo.getAuthorIdList()),
                bo.getLikeCountMin(), bo.getCommentCountMin(), bo.getShareCountMin(), bo.getCollectCountMin(),
                bo.getDurationMin(), bo.getDurationMax(), bo.getPublishStartTime(), bo.getPublishEndTime(),
                bo.getSortField(), bo.getSortDesc(), bo.getWithAudioText());
        return R.ok(fromCacheOrLoad(cacheKey, FIRST_PAGE_CACHE_TTL_SECONDS,
                new TypeReference<StreamPageVo<InfluencerVideoItemVo>>() {
                }, () -> doHotSearchVideoList(bo, pageSize)));
    }

    private StreamPageVo<InfluencerVideoItemVo> doHotSearchVideoList(HotSearchVideoQueryBo bo, int pageSize) {
        int field = bo.getSortField() == null ? 0 : bo.getSortField();
        // sort_order(0) 恒升序取更大；指标列按 sortDesc
        boolean keysetDesc = field != 0 && (bo.getSortDesc() == null || bo.getSortDesc());
        Object[] cursor = decodeCursor(bo.getCursor(), field, false);
        String lastSortVal = cursor == null ? null : (String) cursor[0];
        Long lastVideoId = cursor == null ? null : (Long) cursor[1];

        // 达人维度（secUid/抖音号）→ author_id 交集；有过滤但无匹配达人则直接空
        List<String> authorIds = resolveAuthorIdFilter(null, bo.getPlatformUserIdList(), bo.getPlatformAccountList());
        // authorIdList 直筛：直接比 author_id，不反查达人表 —— 爆款上榜账号多数不在 tb_video_influencer_info 里，
        // 走 platformUserIdList 那条路会被达人表的低覆盖率吃掉、返回空页
        if (CollUtil.isNotEmpty(bo.getAuthorIdList())) {
            List<String> direct = bo.getAuthorIdList().stream().filter(StrUtil::isNotBlank).distinct()
                    .collect(Collectors.toList());
            if (authorIds == null) {
                authorIds = direct;
            } else {
                authorIds.retainAll(new HashSet<>(direct));   // AND 收窄：与达人维度过滤取交集
            }
        }
        if (authorIds != null && authorIds.isEmpty()) {
            return StreamPageVo.empty();
        }

        List<HotSearchVideoRow> rows = videoInfoDao.pageHotSearchVideoRows(bo.getSearchId(), bo.getKeyword(),
                bo.getTagList(), bo.getLikeCountMin(), bo.getCommentCountMin(), bo.getShareCountMin(),
                bo.getCollectCountMin(), bo.getDurationMin(), bo.getDurationMax(),
                bo.getPublishStartTime(), bo.getPublishEndTime(), authorIds, field, keysetDesc,
                lastSortVal, lastVideoId, pageSize + 1);
        if (CollUtil.isEmpty(rows)) {
            return StreamPageVo.empty();
        }
        boolean hasMore = rows.size() > pageSize;
        List<HotSearchVideoRow> pageRows = hasMore ? rows.subList(0, pageSize) : rows;
        List<Long> ids = pageRows.stream().map(HotSearchVideoRow::getVideoId).collect(Collectors.toList());
        Map<Long, VideoInfoEntity> videoMap = videoInfoDao.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(VideoInfoEntity::getId, Function.identity(), (a, b) -> a));
        List<VideoInfoEntity> ordered = ids.stream().map(videoMap::get).filter(Objects::nonNull)
                .collect(Collectors.toList());
        // 默认（模型工具口径）：爆款视频同属全库爬取数据，不做租户限制；
        // tenantScopeExtract=true（前端口径）：extractStatus 只认本租户提取成功、文案过授权闸，避免「假入口」
        boolean withText = Boolean.TRUE.equals(bo.getWithAudioText());
        List<InfluencerVideoItemVo> list = Boolean.TRUE.equals(bo.getTenantScopeExtract())
                ? assembleVideoItems(bo.getTenantId(), ordered, withText)
                : assembleGlobalVideoItems(ordered, withText);
        HotSearchVideoRow last = pageRows.get(pageRows.size() - 1);
        String nextCursor = hasMore ? encodeCursor(field, last.getSortVal(), last.getVideoId()) : null;
        return StreamPageVo.of(list, nextCursor, hasMore);
    }

    @Override
    public R<InfluencerAnalyticsVo> influencerAnalyticsOverview(InfluencerAnalyticsOverviewBo bo) {
        R<InfluencerAnalyticsVo> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        if (bo.getInfluencerId() == null) {
            return R.error("influencerId 不能为空");
        }
        // scope=2 全库爬取；否则租户口径 —— 达人须被本租户订阅，订阅即可见该达人全部作品
        int scope = bo.getScope() != null && bo.getScope() == 2 ? 2 : 1;

        // 时间窗兜底：默认近 3 个月 + 跨度硬夹取 ≤ 1 年（fupan 第三层设防，不信任调用方）
        LocalDateTime[] window = resolveAnalyticsWindow(bo.getPublishStartTime(), bo.getPublishEndTime());
        String startStr = window[0].format(DATE_TIME_FORMATTER);
        String endStr = window[1].format(DATE_TIME_FORMATTER);

        if (scope == 1 && CollUtil.isEmpty(videoInfluencerInfoDao.selectSubscribedInfluencerIds(
                bo.getTenantId(), null, List.of(bo.getInfluencerId())))) {
            // 未订阅该达人 → 租户口径下无可见作品，返回全零统计（不泄露全库数据）
            return R.ok(buildAnalytics(Collections.emptyList(), window[0], window[1], null));
        }

        List<ClipMetricRow> clips = videoInfoDao.listAuthorClipMetrics(
                String.valueOf(bo.getInfluencerId()), bo.getPlatformType(), startStr, endStr, ANALYTICS_MAX_CLIPS);
        if (clips.size() >= ANALYTICS_MAX_CLIPS) {
            log.warn("{} analytics clips 命中兜底上限 {}, influencerId={}, 统计基于截断集", LOG_PREFIX,
                    ANALYTICS_MAX_CLIPS, bo.getInfluencerId());
        }
        // 粉丝数用于雷达 reach 轴（公共达人库，不做租户隔离）
        VideoInfluencerInfoEntity inf = videoInfluencerInfoDao.selectById(bo.getInfluencerId());
        Long followers = inf == null ? null : inf.getFollowersCount();

        return R.ok(buildAnalytics(clips, window[0], window[1], followers));
    }

    /**
     * 统计时间窗兜底：不传止=当前、不传起=止-3个月；非法区间纠正；跨度夹取 ≤ MAX_WINDOW_DAYS。
     */
    private LocalDateTime[] resolveAnalyticsWindow(String startStr, String endStr) {
        LocalDateTime end = parseDateTime(endStr);
        if (end == null) {
            end = LocalDateTime.now();
        }
        LocalDateTime start = parseDateTime(startStr);
        if (start == null || start.isAfter(end)) {
            start = end.minusMonths(3);
        }
        LocalDateTime floor = end.minusDays(MAX_WINDOW_DAYS);
        if (start.isBefore(floor)) {
            start = floor;
        }
        return new LocalDateTime[]{start, end};
    }

    private LocalDateTime parseDateTime(String s) {
        if (StrUtil.isBlank(s)) {
            return null;
        }
        try {
            return LocalDateTime.parse(s.trim(), DATE_TIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 由单达人作品互动明细在内存里算出全部统计指标（KPI/结构/时长/热力/趋势/爆款/雷达）。
     */
    private InfluencerAnalyticsVo buildAnalytics(List<ClipMetricRow> clips, LocalDateTime windowStart,
                                                LocalDateTime windowEnd, Long followers) {
        InfluencerAnalyticsVo vo = new InfluencerAnalyticsVo();
        int n = clips.size();
        vo.setSampleCount(n);
        vo.setDurationBuckets(durationBuckets(clips));
        if (n == 0) {
            vo.setAvgEngagement(0L);
            vo.setMedianEngagement(0L);
            vo.setMaxEngagement(0L);
            vo.setPostingPerWeek(0d);
            vo.setBreakoutCount(0);
            vo.setBreakoutRate(0d);
            vo.setBreakoutThreshold(0L);
            vo.setEngagementCV(0d);
            vo.setSumLike(0L);
            vo.setSumComment(0L);
            vo.setSumShare(0L);
            vo.setSumCollect(0L);
            vo.setPublishHeatmap(Collections.emptyList());
            vo.setTrendSeries(Collections.emptyList());
            vo.setRadar(zeroRadar());
            return vo;
        }

        long sumLike = 0;
        long sumComment = 0;
        long sumShare = 0;
        long sumCollect = 0;
        long sumEng = 0;
        long maxEng = -1;
        Long maxVideoId = null;
        String maxTitle = null;
        long[] engArr = new long[n];
        for (int i = 0; i < n; i++) {
            ClipMetricRow c = clips.get(i);
            long like = nz(c.getLikeCount());
            long comment = nz(c.getCommentCount());
            long share = nz(c.getShareCount());
            long collect = nz(c.getCollectCount());
            sumLike += like;
            sumComment += comment;
            sumShare += share;
            sumCollect += collect;
            long eng = like + comment + share + collect;
            engArr[i] = eng;
            sumEng += eng;
            if (eng > maxEng) {
                maxEng = eng;
                maxVideoId = c.getVideoId();
                maxTitle = c.getTitle();
            }
        }
        vo.setSumLike(sumLike);
        vo.setSumComment(sumComment);
        vo.setSumShare(sumShare);
        vo.setSumCollect(sumCollect);

        double avg = (double) sumEng / n;
        long median = median(engArr);
        vo.setAvgEngagement(Math.round(avg));
        vo.setMedianEngagement(median);
        vo.setMaxEngagement(Math.max(maxEng, 0));
        vo.setMaxEngagementVideoId(maxVideoId);
        vo.setMaxEngagementTitle(maxTitle);

        // 均值口径 CV（标准差/均值）：保留作 median=0 时的兜底。
        double cv = 0d;
        if (avg > 0) {
            double variance = 0d;
            for (long e : engArr) {
                double d = e - avg;
                variance += d * d;
            }
            cv = Math.sqrt(variance / n) / avg;
        }
        // 稳健离散度 robustCV = MAD(中位绝对偏差) / 中位数：抗长尾爆款离群，不被单条爆款绑架；
        // median=0（过半作品零互动）时退回均值口径，避免"零除→0 反被判为极稳"。
        double robustCv;
        if (median > 0) {
            long[] absDev = new long[n];
            for (int i = 0; i < n; i++) {
                absDev[i] = Math.abs(engArr[i] - median);
            }
            robustCv = (double) median(absDev) / median;
        } else {
            robustCv = cv;
        }
        vo.setEngagementCV(round2(robustCv));

        long threshold = median * BREAKOUT_MEDIAN_MULTIPLE;
        vo.setBreakoutThreshold(threshold);
        int breakout = 0;
        if (threshold > 0) {
            for (long e : engArr) {
                if (e >= threshold) {
                    breakout++;
                }
            }
        }
        vo.setBreakoutCount(breakout);
        vo.setBreakoutRate(round2((double) breakout / n));

        // 更新频率：优先按作品实际发布跨度折算，退化到查询窗口
        LocalDateTime minPub = null;
        LocalDateTime maxPub = null;
        for (ClipMetricRow c : clips) {
            LocalDateTime p = c.getPublishTime();
            if (p == null) {
                continue;
            }
            if (minPub == null || p.isBefore(minPub)) {
                minPub = p;
            }
            if (maxPub == null || p.isAfter(maxPub)) {
                maxPub = p;
            }
        }
        double spanDays = (minPub != null && maxPub != null)
                ? Math.max(1d, Duration.between(minPub, maxPub).toHours() / 24d)
                : Math.max(1d, Duration.between(windowStart, windowEnd).toHours() / 24d);
        vo.setPostingPerWeek(round2(n / (spanDays / 7d)));

        vo.setPublishHeatmap(publishHeatmap(clips));
        vo.setTrendSeries(trendSeries(clips));

        long totalInteraction = sumLike + sumComment + sumShare + sumCollect;
        double shareRatio = totalInteraction > 0 ? (double) sumShare / totalInteraction : 0d;
        vo.setRadar(buildRadar(nz(followers), vo.getPostingPerWeek(), vo.getBreakoutRate(), avg, shareRatio, robustCv));
        // radarBaseline 暂留 null：缺赛道字段 + 达人库基准作业（见 doc17 需求4）
        return vo;
    }

    private List<InfluencerAnalyticsVo.DurationBucket> durationBuckets(List<ClipMetricRow> clips) {
        int[] counts = new int[DURATION_BUCKET_LABELS.length];
        for (ClipMetricRow c : clips) {
            int d = c.getDuration() == null ? 0 : c.getDuration();
            int idx;
            if (d < 15) {
                idx = 0;
            } else if (d < 30) {
                idx = 1;
            } else if (d < 60) {
                idx = 2;
            } else if (d < 180) {
                idx = 3;
            } else {
                idx = 4;
            }
            counts[idx]++;
        }
        List<InfluencerAnalyticsVo.DurationBucket> list = new ArrayList<>(DURATION_BUCKET_LABELS.length);
        for (int i = 0; i < DURATION_BUCKET_LABELS.length; i++) {
            list.add(new InfluencerAnalyticsVo.DurationBucket(DURATION_BUCKET_LABELS[i], counts[i]));
        }
        return list;
    }

    private List<InfluencerAnalyticsVo.HeatCell> publishHeatmap(List<ClipMetricRow> clips) {
        Map<Integer, Integer> cellCount = new HashMap<>();
        for (ClipMetricRow c : clips) {
            LocalDateTime p = c.getPublishTime();
            if (p == null) {
                continue;
            }
            int weekday = p.getDayOfWeek().getValue();   // 1..7
            int slot = p.getHour() / 4;                  // 0..5
            cellCount.merge(weekday * 10 + slot, 1, Integer::sum);
        }
        List<InfluencerAnalyticsVo.HeatCell> list = new ArrayList<>(cellCount.size());
        for (Map.Entry<Integer, Integer> e : cellCount.entrySet()) {
            int k = e.getKey();
            list.add(new InfluencerAnalyticsVo.HeatCell(k / 10, k % 10, e.getValue()));
        }
        list.sort(Comparator.comparingInt(InfluencerAnalyticsVo.HeatCell::getWeekday)
                .thenComparingInt(InfluencerAnalyticsVo.HeatCell::getSlot));
        return list;
    }

    private List<InfluencerAnalyticsVo.TrendPoint> trendSeries(List<ClipMetricRow> clips) {
        Map<LocalDate, long[]> weekMap = new TreeMap<>();
        for (ClipMetricRow c : clips) {
            LocalDateTime p = c.getPublishTime();
            if (p == null) {
                continue;
            }
            LocalDate weekStart = p.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            long eng = nz(c.getLikeCount()) + nz(c.getCommentCount()) + nz(c.getShareCount()) + nz(c.getCollectCount());
            long[] agg = weekMap.computeIfAbsent(weekStart, k -> new long[2]);
            agg[0] += eng;
            agg[1] += 1;
        }
        List<InfluencerAnalyticsVo.TrendPoint> all = new ArrayList<>(weekMap.size());
        for (Map.Entry<LocalDate, long[]> e : weekMap.entrySet()) {
            all.add(new InfluencerAnalyticsVo.TrendPoint(e.getKey().format(DATE_FORMATTER),
                    e.getValue()[0], (int) e.getValue()[1]));
        }
        if (all.size() > MAX_TREND_POINTS) {
            return new ArrayList<>(all.subList(all.size() - MAX_TREND_POINTS, all.size()));
        }
        return all;
    }

    /**
     * 6 轴归一化到 0–100（固定锚点口径，非赛道分位；锚点见各轴注释）。
     */
    private InfluencerAnalyticsVo.Radar buildRadar(long followers, double postingPerWeek, double breakoutRate,
                                                   double avgEngagement, double shareRatio, double robustCv) {
        InfluencerAnalyticsVo.Radar r = new InfluencerAnalyticsVo.Radar();
        r.setReach(round2(clamp100(log10p(followers) / 7d * 100d)));                 // 1e7 粉丝 ≈ 100
        r.setOutput(round2(clamp100(postingPerWeek / 7d * 100d)));                   // 1 条/天 ≈ 100
        r.setBreakout(round2(clamp100(breakoutRate * 100d)));                        // 率 0–1 → 0–100
        r.setEngagement(round2(clamp100(log10p(Math.round(avgEngagement)) / 6d * 100d))); // 1e6 均互动 ≈ 100
        r.setVirality(round2(clamp100(shareRatio * 5d * 100d)));                     // 分享占比 ×5 封顶
        r.setConsistency(round2(clamp100(100d / (1d + robustCv))));                  // 平滑衰减，CV 越大越低但不硬归零
        return r;
    }

    private InfluencerAnalyticsVo.Radar zeroRadar() {
        InfluencerAnalyticsVo.Radar r = new InfluencerAnalyticsVo.Radar();
        r.setReach(0d);
        r.setOutput(0d);
        r.setBreakout(0d);
        r.setEngagement(0d);
        r.setVirality(0d);
        r.setConsistency(0d);
        return r;
    }

    private long median(long[] arr) {
        long[] a = arr.clone();
        Arrays.sort(a);
        int m = a.length / 2;
        if (a.length % 2 == 1) {
            return a[m];
        }
        return Math.round((a[m - 1] + a[m]) / 2.0);
    }

    private long nz(Long v) {
        return v == null ? 0L : v;
    }

    private double log10p(long x) {
        return Math.log10(Math.max(x, 0L) + 1d);
    }

    private double clamp100(double v) {
        return v < 0d ? 0d : Math.min(v, 100d);
    }

    private double round2(double v) {
        return Math.round(v * 100d) / 100d;
    }

    @Override
    public R<HotSearchAnalyticsVo> hotSearchAnalyticsOverview(HotSearchAnalyticsOverviewBo bo) {
        R<HotSearchAnalyticsVo> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        if (bo.getSearchId() == null) {
            return R.error("searchId 不能为空");
        }
        // 缓存键用「原始入参」而非兜底后的窗口：不传时间窗时兜底值含 now()，用兜底值做键会导致每次请求键都不同、缓存恒失效。
        // 代价是窗口回显最多滞后一个 TTL（60s），对 7~60 天档位无实际影响。
        // tenantExtractedCount 是租户口径 → 键必带 tenantId（tenantScopeExtract 不改变任何返回值，故不入键，避免无谓分片）。
        String cacheKey = buildCacheKey(CACHE_PREFIX_HOT_ANALYTICS, bo.getTenantId(), bo.getSearchId(),
                bo.getPlatformType(), bo.getPublishStartTime(), bo.getPublishEndTime());
        return R.ok(fromCacheOrLoad(cacheKey, FIRST_PAGE_CACHE_TTL_SECONDS,
                new TypeReference<HotSearchAnalyticsVo>() {
                }, () -> doHotSearchAnalyticsOverview(bo)));
    }

    private HotSearchAnalyticsVo doHotSearchAnalyticsOverview(HotSearchAnalyticsOverviewBo bo) {
        // 时间窗兜底：默认近 7 天（对齐前端默认档）+ 跨度硬夹取 ≤ 1 年，不信任调用方、不做无界扫描
        LocalDateTime[] window = resolveHotAnalyticsWindow(bo.getPublishStartTime(), bo.getPublishEndTime());
        VideoHotSearchEntity search = videoHotSearchDao.selectById(bo.getSearchId());
        if (search == null) {
            // 爆款词不存在 / 已删除 → 返回空统计而非报错，前端走空态（与 §6.20 未订阅达人的处理一致）
            return buildHotSearchAnalytics(bo, null, Collections.emptyList(), window[0], window[1], false);
        }
        List<HotSearchClipRow> clips = videoInfoDao.listHotSearchClipMetrics(bo.getSearchId(), bo.getPlatformType(),
                window[0].format(DATE_TIME_FORMATTER), window[1].format(DATE_TIME_FORMATTER), ANALYTICS_MAX_CLIPS);
        boolean truncated = clips.size() >= ANALYTICS_MAX_CLIPS;
        if (truncated) {
            log.warn("{} hotsearch analytics 样本命中兜底上限 {}, searchId={}, 统计基于互动量 Top N 截断集",
                    LOG_PREFIX, ANALYTICS_MAX_CLIPS, bo.getSearchId());
        }
        return buildHotSearchAnalytics(bo, search, clips, window[0], window[1], truncated);
    }

    /**
     * 选题拆解时间窗兜底：不传止=当前、不传起=止-7天；非法区间纠正；跨度夹取 ≤ MAX_WINDOW_DAYS。
     */
    private LocalDateTime[] resolveHotAnalyticsWindow(String startStr, String endStr) {
        LocalDateTime end = parseDateTime(endStr);
        if (end == null) {
            end = LocalDateTime.now();
        }
        LocalDateTime start = parseDateTime(startStr);
        if (start == null || start.isAfter(end)) {
            start = end.minusDays(HOT_ANALYTICS_DEFAULT_WINDOW_DAYS);
        }
        LocalDateTime floor = end.minusDays(MAX_WINDOW_DAYS);
        if (start.isBefore(floor)) {
            start = floor;
        }
        return new LocalDateTime[]{start, end};
    }

    /**
     * 由爆款样本在内存里算出全部选题指标（门槛/集中度/互动结构/趋势/时长/热力/头部达人/高频标签/文案可用性）。
     *
     * @param search    爆款词记录（不存在时传 null，仅影响回显字段）
     * @param clips     样本（已按互动量降序；空样本走全零分支）
     * @param truncated 样本是否命中上限被截断
     */
    private HotSearchAnalyticsVo buildHotSearchAnalytics(HotSearchAnalyticsOverviewBo bo, VideoHotSearchEntity search,
                                                         List<HotSearchClipRow> clips, LocalDateTime windowStart,
                                                         LocalDateTime windowEnd, boolean truncated) {
        HotSearchAnalyticsVo vo = new HotSearchAnalyticsVo();
        vo.setSearchId(bo.getSearchId());
        vo.setSearchKeyword(search == null ? null : search.getSearchKeyword());
        vo.setPlatformType(search == null ? bo.getPlatformType() : search.getPlatformType());
        vo.setLastSyncTime(search == null ? null : formatDateTime(search.getLastSyncTime()));
        vo.setWindowStart(formatDateTime(windowStart));
        vo.setWindowEnd(formatDateTime(windowEnd));
        vo.setTruncated(truncated);

        int n = clips.size();
        vo.setSampleCount(n);
        long spanDays = Math.max(0L, Duration.between(windowStart, windowEnd).toDays());
        boolean dayGranularity = spanDays <= TREND_DAY_MAX_SPAN_DAYS;
        vo.setTrendGranularity(dayGranularity ? TREND_GRANULARITY_DAY : TREND_GRANULARITY_WEEK);
        // 时长桶恒返回全 5 桶（含 0 桶）以稳定前端坐标轴，空样本也不例外
        vo.setDurationBuckets(hotDurationBuckets(clips));
        if (n == 0) {
            vo.setMedianEngagement(0L);
            vo.setMaxEngagement(0L);
            vo.setTopDecileThreshold(0L);
            vo.setTop10Share(0d);
            vo.setEngagementCV(0d);
            vo.setSumLike(0L);
            vo.setSumComment(0L);
            vo.setSumShare(0L);
            vo.setSumCollect(0L);
            vo.setTrendSeries(Collections.emptyList());
            vo.setPublishHeatmap(Collections.emptyList());
            vo.setTopAuthors(Collections.emptyList());
            vo.setTopTags(Collections.emptyList());
            vo.setTenantExtractedCount(0);
            return vo;
        }

        long sumLike = 0;
        long sumComment = 0;
        long sumShare = 0;
        long sumCollect = 0;
        long sumEng = 0;
        long maxEng = -1;
        Long maxVideoId = null;
        String maxTitle = null;
        long[] engArr = new long[n];
        for (int i = 0; i < n; i++) {
            HotSearchClipRow c = clips.get(i);
            long like = nz(c.getLikeCount());
            long comment = nz(c.getCommentCount());
            long share = nz(c.getShareCount());
            long collect = nz(c.getCollectCount());
            sumLike += like;
            sumComment += comment;
            sumShare += share;
            sumCollect += collect;
            long eng = like + comment + share + collect;
            engArr[i] = eng;
            sumEng += eng;
            if (eng > maxEng) {
                maxEng = eng;
                maxVideoId = c.getVideoId();
                maxTitle = c.getTitle();
            }
        }
        vo.setSumLike(sumLike);
        vo.setSumComment(sumComment);
        vo.setSumShare(sumShare);
        vo.setSumCollect(sumCollect);
        vo.setMaxEngagement(Math.max(maxEng, 0));
        vo.setMaxEngagementVideoId(maxVideoId);
        vo.setMaxEngagementTitle(maxTitle);

        long median = median(engArr);
        vo.setMedianEngagement(median);

        // 降序副本：门槛线（Top10% 下限）与集中度（Top10 条占比）都从它取，不依赖 SQL 排序是否稳定
        long[] desc = engArr.clone();
        Arrays.sort(desc);
        reverse(desc);
        int decileIdx = Math.min(Math.max((int) Math.ceil(n * 0.1d) - 1, 0), n - 1);
        vo.setTopDecileThreshold(desc[decileIdx]);
        long headSum = 0;
        int head = Math.min(TOP_SHARE_HEAD_COUNT, n);
        for (int i = 0; i < head; i++) {
            headSum += desc[i];
        }
        vo.setTop10Share(sumEng > 0 ? round2((double) headSum / sumEng) : 0d);

        // 离散度口径与 §6.20 完全一致：robustCV = MAD/中位数，中位为 0 时退回 标准差/均值
        double avg = (double) sumEng / n;
        double cv = 0d;
        if (avg > 0) {
            double variance = 0d;
            for (long e : engArr) {
                double d = e - avg;
                variance += d * d;
            }
            cv = Math.sqrt(variance / n) / avg;
        }
        double robustCv = cv;
        if (median > 0) {
            long[] absDev = new long[n];
            for (int i = 0; i < n; i++) {
                absDev[i] = Math.abs(engArr[i] - median);
            }
            robustCv = (double) median(absDev) / median;
        }
        vo.setEngagementCV(round2(robustCv));

        vo.setTrendSeries(hotTrendSeries(clips, dayGranularity));
        vo.setPublishHeatmap(hotPublishHeatmap(clips));
        vo.setTopAuthors(hotTopAuthors(clips));
        vo.setTopTags(hotTopTags(clips));
        vo.setTenantExtractedCount(countTenantExtracted(bo.getTenantId(),
                clips.stream().map(HotSearchClipRow::getVideoId).filter(Objects::nonNull).collect(Collectors.toList())));
        return vo;
    }

    /**
     * 时长分布：固定 5 桶 + 每桶均互动量（业务问的是「哪个片长更容易爆」，不是「哪个片长条数多」）。
     */
    private List<HotSearchAnalyticsVo.DurationBucket> hotDurationBuckets(List<HotSearchClipRow> clips) {
        int[] counts = new int[DURATION_BUCKET_LABELS.length];
        long[] sums = new long[DURATION_BUCKET_LABELS.length];
        for (HotSearchClipRow c : clips) {
            int d = c.getDuration() == null ? 0 : c.getDuration();
            int idx;
            if (d < 15) {
                idx = 0;
            } else if (d < 30) {
                idx = 1;
            } else if (d < 60) {
                idx = 2;
            } else if (d < 180) {
                idx = 3;
            } else {
                idx = 4;
            }
            counts[idx]++;
            sums[idx] += hotEngagement(c);
        }
        List<HotSearchAnalyticsVo.DurationBucket> list = new ArrayList<>(DURATION_BUCKET_LABELS.length);
        for (int i = 0; i < DURATION_BUCKET_LABELS.length; i++) {
            long avg = counts[i] == 0 ? 0L : Math.round((double) sums[i] / counts[i]);
            list.add(new HotSearchAnalyticsVo.DurationBucket(DURATION_BUCKET_LABELS[i], counts[i], avg));
        }
        return list;
    }

    /**
     * 互动趋势：按天（窗口 ≤15 天）或自然周聚合，升序，超出点数上限时保留最近的点。
     */
    private List<HotSearchAnalyticsVo.TrendPoint> hotTrendSeries(List<HotSearchClipRow> clips, boolean dayGranularity) {
        Map<LocalDate, long[]> bucketMap = new TreeMap<>();
        for (HotSearchClipRow c : clips) {
            LocalDateTime p = c.getPublishTime();
            if (p == null) {
                continue;
            }
            LocalDate key = dayGranularity ? p.toLocalDate()
                    : p.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            long[] agg = bucketMap.computeIfAbsent(key, k -> new long[2]);
            agg[0] += hotEngagement(c);
            agg[1] += 1;
        }
        List<HotSearchAnalyticsVo.TrendPoint> all = new ArrayList<>(bucketMap.size());
        for (Map.Entry<LocalDate, long[]> e : bucketMap.entrySet()) {
            all.add(new HotSearchAnalyticsVo.TrendPoint(e.getKey().format(DATE_FORMATTER),
                    e.getValue()[0], (int) e.getValue()[1]));
        }
        int max = dayGranularity ? MAX_TREND_DAY_POINTS : MAX_TREND_POINTS;
        if (all.size() > max) {
            return new ArrayList<>(all.subList(all.size() - max, all.size()));
        }
        return all;
    }

    private List<HotSearchAnalyticsVo.HeatCell> hotPublishHeatmap(List<HotSearchClipRow> clips) {
        Map<Integer, Integer> cellCount = new HashMap<>();
        for (HotSearchClipRow c : clips) {
            LocalDateTime p = c.getPublishTime();
            if (p == null) {
                continue;
            }
            int weekday = p.getDayOfWeek().getValue();   // 1..7
            int slot = p.getHour() / 4;                  // 0..5
            cellCount.merge(weekday * 10 + slot, 1, Integer::sum);
        }
        List<HotSearchAnalyticsVo.HeatCell> list = new ArrayList<>(cellCount.size());
        for (Map.Entry<Integer, Integer> e : cellCount.entrySet()) {
            int k = e.getKey();
            list.add(new HotSearchAnalyticsVo.HeatCell(k / 10, k % 10, e.getValue()));
        }
        list.sort(Comparator.comparingInt(HotSearchAnalyticsVo.HeatCell::getWeekday)
                .thenComparingInt(HotSearchAnalyticsVo.HeatCell::getSlot));
        return list;
    }

    /**
     * 头部达人 Top10：按作品数主序、互动量次序（authorId 兜底第三键保证排序稳定）。
     *
     * <p><b>身份字段用 tb_video_info.author_id</b>，与 §6.19 列表项的 authorId 同口径，前端可直接交叉跳转。</p>
     *
     * <p><b>档案字段（昵称/头像/粉丝数/secUid）取自 tb_video_hot_search_video 的冗余列</b> —— 它们跟着爆款视频
     * 一起同步，对上榜账号必然有值；而 tb_video_influencer_info 只由「达人订阅」链路填充，爆款上榜的全网账号
     * 多数查不到，走那条路会大面积拿到空档案。昵称优先用 tb_video_info.author_name，为空再退冗余列。</p>
     *
     * <p>样本已按互动量降序，档案字段一律<b>取第一个非空值</b>（= 该达人互动量最高那条作品所在行），
     * 保证同一达人的各档案字段来自尽量同一次爬取快照，而不是东拼西凑。</p>
     */
    private List<HotSearchAnalyticsVo.AuthorStat> hotTopAuthors(List<HotSearchClipRow> clips) {
        Map<String, HotSearchAnalyticsVo.AuthorStat> statMap = new LinkedHashMap<>();
        for (HotSearchClipRow c : clips) {
            if (StrUtil.isBlank(c.getAuthorId())) {
                continue;
            }
            HotSearchAnalyticsVo.AuthorStat stat = statMap.computeIfAbsent(c.getAuthorId(), id -> {
                HotSearchAnalyticsVo.AuthorStat a = new HotSearchAnalyticsVo.AuthorStat();
                a.setAuthorId(id);
                a.setVideoCount(0);
                a.setSumEngagement(0L);
                a.setMaxEngagement(0L);
                return a;
            });
            long eng = hotEngagement(c);
            stat.setVideoCount(stat.getVideoCount() + 1);
            stat.setSumEngagement(stat.getSumEngagement() + eng);
            stat.setMaxEngagement(Math.max(stat.getMaxEngagement(), eng));
            fillAuthorProfile(stat, c);
        }
        return statMap.values().stream()
                .sorted(Comparator.comparingInt(HotSearchAnalyticsVo.AuthorStat::getVideoCount).reversed()
                        .thenComparing(Comparator.comparingLong(HotSearchAnalyticsVo.AuthorStat::getSumEngagement).reversed())
                        .thenComparing(HotSearchAnalyticsVo.AuthorStat::getAuthorId))
                .limit(TOP_AUTHOR_LIMIT)
                .collect(Collectors.toList());
    }

    /**
     * 填充达人档案字段：每个字段各取第一个非空值（样本按互动量降序，故优先取该达人最热那条作品所在行）。
     *
     * @param stat 累加中的达人统计
     * @param c    当前作品行
     */
    private void fillAuthorProfile(HotSearchAnalyticsVo.AuthorStat stat, HotSearchClipRow c) {
        if (StrUtil.isBlank(stat.getAuthorName())) {
            // 昵称优先 tb_video_info.author_name，为空退爆款关联表的冗余昵称
            stat.setAuthorName(StrUtil.isNotBlank(c.getAuthorName()) ? c.getAuthorName() : c.getInfluencerNickname());
        }
        if (stat.getPlatformType() == null) {
            stat.setPlatformType(c.getPlatformType() != null ? c.getPlatformType() : c.getInfluencerPlatformType());
        }
        if (StrUtil.isBlank(stat.getAvatar()) && StrUtil.isNotBlank(c.getInfluencerAvatar())) {
            stat.setAvatar(c.getInfluencerAvatar());
        }
        if (stat.getFollowersCount() == null && c.getInfluencerFollowersCount() != null) {
            stat.setFollowersCount(c.getInfluencerFollowersCount());
        }
        if (StrUtil.isBlank(stat.getPlatformUserId()) && StrUtil.isNotBlank(c.getInfluencerPlatformUserId())) {
            stat.setPlatformUserId(c.getInfluencerPlatformUserId());
        }
    }

    /**
     * 高频 #标签 Top20：从 title 正则确定性抽取，同一条视频内同名标签只计 1 次。
     */
    private List<HotSearchAnalyticsVo.TagStat> hotTopTags(List<HotSearchClipRow> clips) {
        Map<String, long[]> tagAgg = new LinkedHashMap<>();
        for (HotSearchClipRow c : clips) {
            if (StrUtil.isBlank(c.getTitle())) {
                continue;
            }
            Set<String> tags = new LinkedHashSet<>();
            Matcher m = TITLE_TAG_PATTERN.matcher(c.getTitle());
            while (m.find()) {
                String tag = m.group(1).trim().toLowerCase();
                if (StrUtil.isNotBlank(tag)) {
                    tags.add(tag);
                }
            }
            if (tags.isEmpty()) {
                continue;
            }
            long eng = hotEngagement(c);
            for (String tag : tags) {
                long[] agg = tagAgg.computeIfAbsent(tag, k -> new long[2]);
                agg[0] += 1;
                agg[1] += eng;
            }
        }
        return tagAgg.entrySet().stream()
                .sorted(Comparator
                        .comparingLong((Map.Entry<String, long[]> e) -> e.getValue()[0]).reversed()
                        .thenComparing(Comparator.comparingLong((Map.Entry<String, long[]> e) -> e.getValue()[1]).reversed())
                        .thenComparing(Map.Entry::getKey))
                .limit(TOP_TAG_LIMIT)
                .map(e -> {
                    HotSearchAnalyticsVo.TagStat t = new HotSearchAnalyticsVo.TagStat();
                    t.setTag(e.getKey());
                    t.setCount((int) e.getValue()[0]);
                    t.setSumEngagement(e.getValue()[1]);
                    return t;
                })
                .collect(Collectors.toList());
    }

    /**
     * 样本内本租户已提取成功的条数：分批只回 COUNT，不把 id 列表拉回内存（样本上限 2 万条）。
     */
    private int countTenantExtracted(Long tenantId, List<Long> videoIds) {
        if (CollUtil.isEmpty(videoIds)) {
            return 0;
        }
        long total = 0;
        for (List<Long> batch : CollUtil.split(videoIds, ID_IN_BATCH_SIZE)) {
            Long hit = videoInfoDao.countTenantExtractedVideoIds(tenantId, batch);
            total += nz(hit);
        }
        return (int) total;
    }

    private long hotEngagement(HotSearchClipRow c) {
        return nz(c.getLikeCount()) + nz(c.getCommentCount()) + nz(c.getShareCount()) + nz(c.getCollectCount());
    }

    private void reverse(long[] arr) {
        for (int i = 0, j = arr.length - 1; i < j; i++, j--) {
            long tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }

    @Override
    public R<InfluencerDetailBatchVo> influencerDetailBatch(InfluencerDetailBatchBo bo) {
        R<InfluencerDetailBatchVo> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        if (CollUtil.isEmpty(bo.getAuthorIdList())) {
            return R.error("authorIdList 不能为空");
        }
        // author_id 是字符串列，语义上是 tb_video_influencer_info.id 的字符串形式；
        // 解析不了的直接进 notFound，不吞掉 —— 解析失败本身就是「上游 id 口径变了」的信号
        List<String> ordered = bo.getAuthorIdList().stream().filter(StrUtil::isNotBlank).distinct()
                .collect(Collectors.toList());
        List<String> notFound = new ArrayList<>();
        List<Long> ids = new ArrayList<>(ordered.size());
        for (String raw : ordered) {
            Long id = parseInfluencerId(raw);
            if (id == null) {
                notFound.add(raw);
            } else {
                ids.add(id);
            }
        }

        InfluencerDetailBatchVo vo = new InfluencerDetailBatchVo();
        if (ids.isEmpty()) {
            vo.setList(Collections.emptyList());
            vo.setNotFoundAuthorIds(notFound);
            return R.ok(vo);
        }
        Map<Long, VideoInfluencerInfoEntity> hit = videoInfluencerInfoDao.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(VideoInfluencerInfoEntity::getId, Function.identity(), (a, b) -> a));
        List<InfluencerItemVo> list = new ArrayList<>(hit.size());
        for (String raw : ordered) {
            Long id = parseInfluencerId(raw);
            if (id == null) {
                continue;   // 已在上面记进 notFound
            }
            VideoInfluencerInfoEntity e = hit.get(id);
            if (e == null) {
                notFound.add(raw);
            } else {
                list.add(buildInfluencerItemVo(e));
            }
        }
        if (!notFound.isEmpty()) {
            log.info("{} influencer/detail/batch 未命中 {}/{} 个 author_id（爆款上榜账号未必在爬取达人库里有档案）",
                    LOG_PREFIX, notFound.size(), ordered.size());
        }
        vo.setList(list);
        vo.setNotFoundAuthorIds(notFound);
        return R.ok(vo);
    }

    /**
     * author_id 字符串 → 达人主键；非数字 / 越界返回 null（交给调用方按 notFound 处理）。
     */
    private Long parseInfluencerId(String raw) {
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 批量加载行业名：取分页行内去重的非空 industryId，一次回源行业库（无 N+1）。
     *
     * @param rows 当前页达人行（含 industryId）
     * @return industryId → 行业名（不存在的行业不入图）；无非空 industryId 时返回空图
     */
    private Map<Long, String> loadIndustryNames(List<VideoInfluencerInfoDao.TenantInfluencerRow> rows) {
        List<Long> industryIds = rows.stream()
                .map(VideoInfluencerInfoDao.TenantInfluencerRow::getIndustryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (industryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> map = tradeProducer.getTradeNames(industryIds);
        return map == null ? Collections.emptyMap() : map;
    }

    /**
     * 装配达人列表项基础字段（不含租户维度的 industryName / lastCollectTime）。
     */
    private InfluencerItemVo buildInfluencerItemVo(VideoInfluencerInfoEntity e) {
        InfluencerItemVo vo = new InfluencerItemVo();
        vo.setId(e.getId());
        vo.setPlatformType(e.getPlatformType());
        vo.setPlatformAccount(e.getPlatformAccount());
        vo.setPlatformUserId(e.getPlatformUserId());
        vo.setNickname(e.getNickname());
        vo.setAvatar(e.getAvatar());
        vo.setFollowersCount(e.getFollowersCount());
        vo.setFollowingCount(e.getFollowingCount());
        vo.setVideoCount(e.getVideoCount());
        vo.setLikeCount(e.getLikeCount());
        vo.setVerificationStatus(e.getVerificationStatus());
        vo.setVerificationInfo(e.getVerificationInfo());
        vo.setInfluencerDescription(e.getDescription());   // 实体列 influencer_description，getter 名为 getDescription()
        return vo;
    }

    /**
     * 租户口径装配：先按 videoId 批量取本租户「提取成功」集合，据此给出租户口径的 extractStatus，
     * 并只对已授权的视频加载文案（withText=true 时）。用于租户可见范围（订阅达人）与按 id 直查明细。
     *
     * @param tenantId 租户 id
     * @param videos   已按目标顺序排好的视频实体
     * @param withText 是否需要文案（最终仍受「本租户提取成功」授权约束）
     * @return 列表项（保持入参顺序）
     */
    private List<InfluencerVideoItemVo> assembleVideoItems(Long tenantId, List<VideoInfoEntity> videos,
                                                           boolean withText) {
        if (CollUtil.isEmpty(videos)) {
            return Collections.emptyList();
        }
        Set<Long> authorized = loadTenantExtractedVideoIds(tenantId,
                videos.stream().map(VideoInfoEntity::getId).collect(Collectors.toList()));
        Map<String, VideoContentExtract> extractMap = withText
                ? loadExtractMapForVideos(videos.stream().filter(v -> authorized.contains(v.getId()))
                        .collect(Collectors.toList()))
                : Collections.emptyMap();
        return videos.stream().map(e -> buildVideoItemVo(e, withText, extractMap, authorized))
                .collect(Collectors.toList());
    }

    /**
     * 全库爬取库装配：跨租户公共数据，<b>不做任何租户限制</b> —— extractStatus 取 tb_video_info 原值，
     * 文案按 withText 原样返回，不查提取授权。用于全库短视频检索与爆款视频列表。
     *
     * @param videos   已按目标顺序排好的视频实体
     * @param withText 是否需要文案
     * @return 列表项（保持入参顺序）
     */
    private List<InfluencerVideoItemVo> assembleGlobalVideoItems(List<VideoInfoEntity> videos, boolean withText) {
        if (CollUtil.isEmpty(videos)) {
            return Collections.emptyList();
        }
        Map<String, VideoContentExtract> extractMap = withText
                ? loadExtractMapForVideos(videos) : Collections.emptyMap();
        return videos.stream().map(e -> buildVideoItemVo(e, withText, extractMap, null))
                .collect(Collectors.toList());
    }

    /**
     * 取本租户在给定视频集合中「文案提取成功」的视频 id（分批查询，避免超长 IN）。
     *
     * @param tenantId 租户 id
     * @param videoIds 视频 id 集合
     * @return 已授权（提取成功）的视频 id 集合；入参为空时返回空集合
     */
    private Set<Long> loadTenantExtractedVideoIds(Long tenantId, List<Long> videoIds) {
        if (CollUtil.isEmpty(videoIds)) {
            return Collections.emptySet();
        }
        Set<Long> result = new HashSet<>();
        for (List<Long> batch : CollUtil.split(videoIds, ID_IN_BATCH_SIZE)) {
            List<Long> hit = videoInfoDao.listTenantExtractedVideoIds(tenantId, batch);
            if (CollUtil.isNotEmpty(hit)) {
                result.addAll(hit);
            }
        }
        return result;
    }

    /**
     * 解析租户可见范围的 author_id 集合 = 本租户已订阅达人 ∩ 达人维度过滤（达人 id / secUid / 抖音号）。
     *
     * <p>订阅达人 id 在 Java 侧转成字符串后作为绑定参数下推，不在 SQL 里 CAST 比较 —— CAST 结果携带连接默认
     * 字符序，与 {@code tb_video_info.author_id} 列字符序不一致会抛 error 1267（Illegal mix of collations）。</p>
     *
     * @param bo 查询入参（取 tenantId / collectAccountId / 三种达人维度过滤）
     * @return author_id 字符串集合；空集合表示无可见达人，调用方应直接返回空页
     */
    private List<String> resolveSubscribedAuthorIds(VideoGlobalSearchBo bo) {
        List<Long> subscribed = videoInfluencerInfoDao.selectSubscribedInfluencerIds(
                bo.getTenantId(), bo.getCollectAccountId(), null);
        if (CollUtil.isEmpty(subscribed)) {
            return Collections.emptyList();
        }
        List<String> subscribedAuthorIds = subscribed.stream().map(String::valueOf).distinct()
                .collect(Collectors.toList());
        if (subscribedAuthorIds.size() > MAX_AUTHOR_IN) {
            log.warn("{} 租户 {} 订阅达人数 {} 超过 IN 建议上限 {}，本次检索 IN 列表偏长", LOG_PREFIX,
                    bo.getTenantId(), subscribedAuthorIds.size(), MAX_AUTHOR_IN);
        }
        List<String> filter = resolveAuthorIdFilter(bo.getInfluencerIdList(),
                bo.getPlatformUserIdList(), bo.getPlatformAccountList());
        if (filter == null) {
            return subscribedAuthorIds;   // 未施加达人维度过滤 → 全部订阅达人
        }
        filter.retainAll(new HashSet<>(subscribedAuthorIds));   // AND 收窄：过滤条件 ∩ 订阅范围
        return filter;
    }

    /**
     * 把对外的 extractStatus 过滤（0-未提取 1-已提取，租户口径）翻译成「本租户是否提取成功」的三态过滤。
     *
     * @param extractStatusList 调用方传入的状态集合
     * @return null=不过滤（未传 / 0 和 1 都要 / 值非法）；true=仅提取成功；false=仅未提取成功
     */
    private Boolean resolveTenantExtractedFilter(List<Byte> extractStatusList) {
        if (CollUtil.isEmpty(extractStatusList)) {
            return null;
        }
        boolean wantDone = extractStatusList.contains(EXTRACT_STATUS_DONE);
        boolean wantNone = extractStatusList.contains(EXTRACT_STATUS_NONE);
        return wantDone == wantNone ? null : wantDone;
    }

    /**
     * 装配短视频列表项（不含租户维度的 collectTime）。
     *
     * @param authorizedVideoIds 本租户已提取成功的视频 id 集合 —— 租户口径下据此给 extractStatus 并放行文案；
     *                           传 {@code null} 表示全库爬取库口径：extractStatus 取 tb_video_info 原值、文案不设闸
     */
    private InfluencerVideoItemVo buildVideoItemVo(VideoInfoEntity e, boolean withText,
                                                  Map<String, VideoContentExtract> extractMap,
                                                  Set<Long> authorizedVideoIds) {
        InfluencerVideoItemVo vo = new InfluencerVideoItemVo();
        vo.setId(e.getId());
        vo.setPlatformType(e.getPlatformType());
        vo.setPlatformVideoId(e.getPlatformVideoId());
        vo.setVideoHash(e.getVideoHash());
        vo.setTitle(e.getTitle());
        vo.setCoverUrl(e.getCoverUrl());
        vo.setVideoUrl(e.getVideoUrl());
        vo.setAuthorId(e.getAuthorId());
        vo.setAuthorName(e.getAuthorName());
        vo.setLikeCount(e.getLikeCount());
        vo.setCommentCount(e.getCommentCount());
        vo.setShareCount(e.getShareCount());
        vo.setCollectCount(e.getCollectCount());
        vo.setDuration(e.getDuration());
        vo.setPublishTime(formatDateTime(e.getPublishTime()));
        // 租户口径：只有本租户提取成功才算「已提取」，调用方看到 1 就一定能取到文案；全库口径取原值
        boolean authorized = authorizedVideoIds == null || authorizedVideoIds.contains(e.getId());
        vo.setExtractStatus(authorizedVideoIds == null ? e.getExtractStatus()
                : (authorized ? EXTRACT_STATUS_DONE : EXTRACT_STATUS_NONE));
        vo.setAnalysisStatus(e.getAnalysisStatus());
        if (withText && authorized && StrUtil.isNotBlank(e.getVideoHash())) {
            VideoContentExtract ce = extractMap.get(e.getVideoHash());
            if (ce != null) {
                vo.setOriginalAudioContent(ce.getOriginalAudioContent());
                vo.setAudioContent(ce.getAudioContent());
            }
        }
        return vo;
    }

    /**
     * 标题内 #标签 过滤：任一标签命中即可（title LIKE '%#tag%'）。
     */
    private void applyTitleTagFilter(LambdaQueryWrapper<VideoInfoEntity> wrapper, List<String> tags) {
        if (CollUtil.isEmpty(tags)) {
            return;
        }
        wrapper.and(w -> {
            boolean first = true;
            for (String tag : tags) {
                if (StrUtil.isBlank(tag)) {
                    continue;
                }
                if (first) {
                    w.like(VideoInfoEntity::getTitle, "#" + tag);
                    first = false;
                } else {
                    w.or().like(VideoInfoEntity::getTitle, "#" + tag);
                }
            }
        });
    }

    private SFunction<VideoInfoEntity, ?> videoSortColumn(int field) {
        return switch (field) {
            case 1 -> VideoInfoEntity::getLikeCount;
            case 2 -> VideoInfoEntity::getCommentCount;
            case 3 -> VideoInfoEntity::getShareCount;
            case 4 -> VideoInfoEntity::getCollectCount;
            case 5 -> VideoInfoEntity::getPublishTime;
            default -> VideoInfoEntity::getId;
        };
    }

    /**
     * 取实体在排序字段上的值（用于编码 nextCursor）；id 排序返回 null（游标仅编码 id）。
     */
    private String videoSortVal(int field, VideoInfoEntity e) {
        return switch (field) {
            case 1 -> String.valueOf(e.getLikeCount());
            case 2 -> String.valueOf(e.getCommentCount());
            case 3 -> String.valueOf(e.getShareCount());
            case 4 -> String.valueOf(e.getCollectCount());
            case 5 -> formatDateTime(e.getPublishTime());
            default -> null;
        };
    }

    /**
     * keyset 排序：非 id 排序时先排序列再排 id 兜底；方向由 desc 决定。
     */
    private <E> void applyOrder(LambdaQueryWrapper<E> wrapper, SFunction<E, ?> sortCol, SFunction<E, ?> idCol,
                                boolean desc, boolean isIdSort) {
        if (!isIdSort) {
            wrapper.orderBy(true, !desc, sortCol);
        }
        wrapper.orderBy(true, !desc, idCol);
    }

    /**
     * keyset 游标条件：desc 取更小、asc 取更大；(sortCol, id) 复合定位，避免深翻页。
     */
    private <E> void applyKeyset(LambdaQueryWrapper<E> wrapper, SFunction<E, ?> sortCol, SFunction<E, ?> idCol,
                                 boolean desc, boolean isIdSort, Object lastVal, Long lastId) {
        if (lastId == null) {
            return;
        }
        if (isIdSort) {
            if (desc) {
                wrapper.lt(idCol, lastId);
            } else {
                wrapper.gt(idCol, lastId);
            }
            return;
        }
        if (desc) {
            wrapper.and(x -> x.lt(sortCol, lastVal).or(y -> y.eq(sortCol, lastVal).lt(idCol, lastId)));
        } else {
            wrapper.and(x -> x.gt(sortCol, lastVal).or(y -> y.eq(sortCol, lastVal).gt(idCol, lastId)));
        }
    }

    /**
     * 编码不透明游标：Base64("排序值id")；排序值为空表示 id-only 游标。
     */
    private String encodeCursor(int sortField, String sortVal, Long id) {
        String raw = "" + sortField + CURSOR_SEP + (sortVal == null ? "" : sortVal) + CURSOR_SEP + id;
        return Base64.encodeUrlSafe(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解码游标为 [排序值(可空), id]；非法/空返回 null。
     */
    private Object[] decodeCursor(String cursor, int currentSortField, boolean sortValNumeric) {
        if (StrUtil.isBlank(cursor)) {
            return null;
        }
        try {
            String raw = new String(Base64.decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split(String.valueOf(CURSOR_SEP), -1);
            if (parts.length != 3 || Integer.parseInt(parts[0]) != currentSortField) {
                return null;
            }
            Object sortVal = StrUtil.isEmpty(parts[1])
                    ? null : (sortValNumeric ? (Object) Long.valueOf(parts[1]) : parts[1]);
            return new Object[]{sortVal, Long.valueOf(parts[2])};
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 把「达人维度」的三种过滤（达人 id / 平台用户id secUid / 平台账号 抖音号）解析成统一的 author_id 字符串集合
     * （= tb_video_info.author_id）。platform_user_id / platform_account 只存在于达人表，先反查达人 id 再字符串化。
     *
     * <p>三者之间 <b>AND 收窄</b>（取交集），符合「新增过滤维度」直觉。返回值语义：
     * <ul>
     *   <li>{@code null}：未施加任何达人维度过滤（调用方不应据此收窄）；</li>
     *   <li>空集合：有过滤但无任何匹配达人（调用方应直接返回空结果，切勿当作"不过滤"去全量查）。</li>
     * </ul></p>
     *
     * @param influencerIdList   达人 id 集合（可空）
     * @param platformUserIdList 平台用户 id(secUid) 集合（可空）
     * @param platformAccountList 平台账号(抖音号) 集合（可空）
     * @return author_id 字符串集合；见上文语义
     */
    private List<String> resolveAuthorIdFilter(List<Long> influencerIdList,
                                               List<String> platformUserIdList,
                                               List<String> platformAccountList) {
        List<String> filter = null;
        if (CollUtil.isNotEmpty(influencerIdList)) {
            filter = influencerIdList.stream().map(String::valueOf).distinct().collect(Collectors.toList());
        }
        if (CollUtil.isNotEmpty(platformUserIdList) || CollUtil.isNotEmpty(platformAccountList)) {
            List<String> byPlatform = videoInfluencerInfoDao.selectIdsByPlatform(platformUserIdList, platformAccountList)
                    .stream().map(String::valueOf).distinct().collect(Collectors.toList());
            if (filter == null) {
                filter = byPlatform;
            } else {
                filter.retainAll(new HashSet<>(byPlatform));   // AND 收窄：取交集
            }
        }
        if (filter != null && filter.size() > MAX_AUTHOR_IN) {
            log.warn("{} authorIds 解析结果 {} 超过上限 {}，已截断，避免超长 IN", LOG_PREFIX, filter.size(), MAX_AUTHOR_IN);
            filter = new ArrayList<>(filter.subList(0, MAX_AUTHOR_IN));
        }
        return filter;
    }

    /**
     * 首页缓存读取，未命中则回源并写入（结果与身份无关，跨租户共享）。Redis 异常时静默降级为直接回源。
     */
    private <T> T fromCacheOrLoad(String key, int ttlSeconds, TypeReference<T> type, Supplier<T> loader) {
        try {
            Object cached = resilientRedisTemplate.opsForValue().get(key);
            if (cached instanceof String json && StrUtil.isNotBlank(json)) {
                T hit = JSONUtil.toBean(json, type, true);
                if (hit != null) {
                    return hit;
                }
            }
        } catch (Exception e) {
            log.warn("{} cache read fail, key={}, err={}", LOG_PREFIX, key, e.getMessage());
        }
        T value = loader.get();
        if (value != null) {
            try {
                resilientRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), ttlSeconds, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("{} cache write fail, key={}, err={}", LOG_PREFIX, key, e.getMessage());
            }
        }
        return value;
    }

    /**
     * 用查询相关字段（不含 cursor / userId / tenantId / userType）构造稳定缓存键，集合参数先排序保证顺序无关。
     */
    private String buildCacheKey(String prefix, Object... parts) {
        String raw = JSONUtil.toJsonStr(parts);
        return prefix + DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 返回排序后的副本（用于缓存键，顺序无关）；null 原样返回。
     */
    private <E extends Comparable<? super E>> List<E> sortedCopy(List<E> list) {
        if (list == null) {
            return null;
        }
        return list.stream().sorted().collect(Collectors.toList());
    }

    /**
     * 从视频实体集合按 videoHash 批量加载 Mongo 内容。
     */
    private Map<String, VideoContentExtract> loadExtractMapForVideos(List<VideoInfoEntity> videos) {
        return loadExtractMap(videos.stream().map(VideoInfoEntity::getVideoHash)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList()));
    }

    /**
     * 批量按 videoHash 加载 Mongo 内容，返回 hash → 文档映射。
     */
    private Map<String, VideoContentExtract> loadExtractMap(List<String> videoHashes) {
        if (CollUtil.isEmpty(videoHashes)) {
            return Collections.emptyMap();
        }
        return videoExtractContentRepository.findByVideoHashIn(videoHashes).stream()
                .filter(c -> StrUtil.isNotBlank(c.getVideoHash()))
                .collect(Collectors.toMap(VideoContentExtract::getVideoHash, Function.identity(), (a, b) -> a));
    }

    /**
     * 身份三件套校验，非法返回统一错误。
     */
    private <T> R<T> validateIdentity(AiAgentBaseBo bo) {
        if (bo == null || bo.getUserId() == null || bo.getTenantId() == null || bo.getUserType() == null) {
            return R.error("身份参数缺失：userId/tenantId/userType 必填");
        }
        Integer t = bo.getUserType();
        if (t != 0 && t != 1 && t != 2) {
            return R.error("userType 非法，仅支持 0/1/2");
        }
        return null;
    }

    private int clampPageSize(Integer pageSize) {
        return clampPageSize(pageSize, MAX_PAGE_SIZE);
    }

    private int clampPageSize(Integer pageSize, int maxPageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, maxPageSize);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME_FORMATTER);
    }
}
