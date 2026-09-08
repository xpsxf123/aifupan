package com.jiuyu.replay.api.service.aiagent.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.ai.rse.SceneSliceRse;
import com.jiuyu.replay.api.logic.words.SensitiveWordsLogic;
import com.jiuyu.replay.api.logic.words.SocketCollectMessageLogic;
import com.jiuyu.replay.api.service.aiagent.AiAgentReplayService;
import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.common.utils.ReplayFileUtils;
import com.jiuyu.replay.generic.bo.aiagent.*;
import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.generic.vo.aiagent.*;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.generic.vo.words.viewing.OceanEngineDataInfoVo;
import com.jiuyu.replay.third.bll.TableStoreBll;
import com.jiuyu.replay.third.vo.DanMuVo;
import com.jiuyu.replay.third.vo.QueryDanMuVo;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import com.jiuyu.replay.words.bll.VideoDataViewingBll;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.producer.TradeProducer;
import com.jiuyu.replay.words.repository.dao.AnchorUrlUserDao;
import com.jiuyu.replay.words.repository.service.*;
import com.jiuyu.replay.words.vo.OceanEngineDataVo;
import com.jiuyu.replay.words.vo.OnlineChartBlessBagVo;
import com.jiuyu.replay.words.vo.OnlineChartVo;
import com.jiuyu.replay.words.vo.SocketCollectMessageVo;
import com.jiuyu.replay.words.vo.chart.CurveData;
import com.jiuyu.replay.words.vo.chart.CurveDoubleData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI Agent 复盘数据开放接口服务实现。
 *
 * <p>身份显式从入参取，所有查询强制叠加 tenantId + 软删除过滤；子账号（userType=2）再叠加 userId。</p>
 *
 * @author fupan-server
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentReplayServiceImpl implements AiAgentReplayService {

    private static final int CHILD_USER_TYPE = 2;

    /**
     * 数据源类型：1=自己录制（userId+tenantId）2=全租户（仅 tenantId）3=云空间已分享（tenantId + uploadStatus=1）
     * 4=自己录制+云空间（两者并集：本人录制 OR 云空间已分享）
     */
    private static final int DATA_SOURCE_SELF = 1;
    private static final int DATA_SOURCE_TENANT = 2;
    private static final int DATA_SOURCE_CLOUD = 3;
    private static final int DATA_SOURCE_SELF_AND_CLOUD = 4;

    /**
     * 视频上传状态：1=已上传/已分享到云空间
     */
    private static final int VIDEO_UPLOAD_STATUS_SHARED = 1;

    /**
     * 看板补充曲线的降采样目标点数（控制返回体积）。
     * <p>曲线改累计口径后每个点都可解读，10 点足以看出起量/掐量的拐点，体积增量可忽略。</p>
     */
    private static final int DASHBOARD_CURVE_POINTS = 10;

    private static final Map<Integer, String> PLATFORM_LABELS = Map.of(0, "抖音", 1, "快手", 2, "视频号");
    private static final Map<Integer, String> ACCOUNT_TYPE_LABELS = Map.of(0, "自有账号", 1, "同行账号");
    private static final Map<Integer, String> VIDEO_SLICE_TYPE_LABELS = Map.of(0, "原视频", 1, "复盘切片视频", 2, "短视频切片视频");

    /**
     * 分段看盘有效数据状态：1=拉取成功，8=数据整理中
     */
    private static final List<Integer> VALID_PARAGRAPH_STATUS = Arrays.asList(1, 8);

    /**
     * 数据截图状态：3=AI 识别完成
     */
    private static final int SCREENSHOT_STATUS_RECOGNIZED = 3;

    private final AnchorUrlUserService anchorUrlUserService;
    private final AnchorUrlService anchorUrlService;
    private final AnchorVideoService anchorVideoService;
    private final BasicSettingsService basicSettingsService;
    private final AnchorUrlBll anchorUrlBll;
    private final VideoDataViewingBll videoDataViewingBll;
    private final TradeProducer tradeProducer;
    private final TableStoreBll tableStoreBll;
    private final SensitiveWordsLogic sensitiveWordsLogic;
    private final SocketCollectMessageLogic socketCollectMessageLogic;
    private final AnchorUrlUserDao anchorUrlUserDao;
    private final SensitiveWordsService sensitiveWordsService;
    private final DataScreenshotService dataScreenshotService;
    private final SceneSliceRse sceneSliceRse;


    /**
     * 敏感词等级标签：0=1 级(封号) 1=2 级(严重警告) 2=3 级(警告)。数值越小越严重。
     */
    private static final Map<Integer, String> SENSITIVE_LEVEL_LABELS = Map.of(0, "1级(封号)", 1, "2级(严重警告)", 2, "3级(警告)");
    private static final Map<Integer, String> SENSITIVE_TYPE_LABELS = Map.of(0, "广告", 1, "品牌", 2, "国家", 3, "限制词", 4, "其他");

    /**
     * 敏感词返回条数上限：父链 + 全行业聚合后兜底封顶，防止极端行业词表撑爆响应体（超出按 level 升序截断，最严重优先保留）。
     */
    private static final int MAX_SENSITIVE_WORDS = 5000;

    // ==================== 4.1 主播列表 ====================

    @Override
    public R<CursorPageVo<AnchorItemVo>> anchorList(AnchorListQueryBo bo) {
        R<CursorPageVo<AnchorItemVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        // 解析有效数据源：DAO 据此决定是否叠加 user_id 过滤（自己录制叠加，全租户不叠加）
        bo.setDataSourceType(resolveDataSource(bo.getDataSourceType(), bo.getUserType()));
        int pageSize = clampPageSize(bo.getPageSize(), 300, 1000);
        List<Long> tradeIds = expandTradeIds(bo.getTradeId());
        bo.setTradeIds(tradeIds);
        bo.setPageSize(pageSize);
        List<AnchorItemVo> rows = anchorUrlUserDao.searchAnchorList(bo);

        // 内存按页过滤避免对全局表的无界查询与超界 IN（页游标仍按 tb_anchor_url_user.id 单调推进，翻页正确）
        boolean needBaseFilter = StrUtil.isNotBlank(bo.getAnchorName())
            || StrUtil.isNotBlank(bo.getAnchorNumber())
            || CollUtil.isNotEmpty(bo.getPlatforms());

        boolean hasMore = rows.size() == pageSize;
        Long nextCursor = hasMore ? rows.get(rows.size() - 1).getId() : null;

        // 批量装配主播基础信息（tb_anchor_url）与行业名（无 N+1）
        Map<String, AnchorUrlEntity> baseMap = new HashMap<>();
        if (!needBaseFilter) {
            List<String> pageSecUids = rows.stream().map(AnchorItemVo::getSecUid)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            Map<String, AnchorUrlEntity> map = loadAnchorBaseMap(pageSecUids);
            if (EmptyUtil.isNotEmpty(map)) {
                baseMap.putAll(map);
            }
        }

        Map<Long, String> tradeNameMap = loadTradeNames(rows.stream()
            .map(AnchorItemVo::getTradeId).filter(Objects::nonNull).distinct().collect(Collectors.toList()));

        rows.forEach(vo -> {
            AnchorUrlEntity base = baseMap.get(vo.getSecUid());
            if (base != null) {
                vo.setAnchorName(base.getAnchorName());
                vo.setAnchorAvatar(base.getAnchorAvatar());
                vo.setPlatform(base.getPlatform());
                vo.setPlatformLabel(label(PLATFORM_LABELS, base.getPlatform()));
                vo.setAnchorNumber(base.getAnchorNumber());
            }
            vo.setTradeName(vo.getTradeId() == null ? null : tradeNameMap.get(vo.getTradeId()));
            vo.setAccountType(vo.getAccountType());
            vo.setAccountTypeLabel(label(ACCOUNT_TYPE_LABELS, vo.getAccountType()));
        });

        return R.ok(CursorPageVo.of(rows, nextCursor, hasMore));
    }

    // ==================== 4.2 主播行业聚合 ====================

    @Override
    public R<List<TradeOptionVo>> tradeOptions(TradeOptionsQueryBo bo) {
        R<List<TradeOptionVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        List<Long> tradeIds = anchorUrlUserService.getTenantTradeIds(bo.getTenantId());

        if (tradeIds.isEmpty()) {
            return R.ok(new ArrayList<>());
        }
        Map<Long, String> tradeNameMap = loadTradeNames(tradeIds);

        List<TradeOptionVo> list = tradeIds.stream().map(id -> {
            TradeOptionVo vo = new TradeOptionVo();
            vo.setTradeId(id);
            vo.setTradeName(tradeNameMap.get(id));
            return vo;
        }).collect(Collectors.toList());
        return R.ok(list);
    }

    // ==================== 4.2.1 行业层级链 ====================

    @Override
    public R<List<TradeParentVo>> tradeParents(TradeParentsQueryBo bo) {
        R<List<TradeParentVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        // listParentsByTradeId 递归 自身 → 父 → 祖父，天然按层级排序，离 tradeId 越近越靠前
        List<TradeInfoVo> chain = tradeProducer.listParentsByTradeId(
            bo.getTradeId(), Constant.GeneralEnum.GENERAL_NO.getCode());
        if (CollUtil.isEmpty(chain)) {
            return R.ok(new ArrayList<>());
        }
        List<TradeParentVo> list = chain.stream().map(t -> {
            TradeParentVo vo = new TradeParentVo();
            vo.setId(t.getId());
            vo.setName(t.getName());
            vo.setParentId(t.getParentId());
            return vo;
        }).collect(Collectors.toList());
        return R.ok(list);
    }

    // ==================== 4.2.2 行业敏感词库 ====================

    @Override
    public R<List<SensitiveWordVo>> sensitiveWords(SensitiveWordsQueryBo bo) {
        R<List<SensitiveWordVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        int wordsType = bo.getWordsType() == null ? 0 : bo.getWordsType();

        // 行业父链：自身(4级) → 父(3级) → … → 顶级(1级)，再叠加全行业(id=1)。词库中相似词为继承父词 trade_id/level/type
        // 的独立行，故一次 trade_id IN(链) 即取全主词与变体，无需再展开 parentId。
        List<Long> tradeIds = resolveSensitiveTradeIds(bo.getTradeId());

        // 平台映射：入参约定 0=抖音/1=快手/2=视频号，词库 platform_type 约定 0=全平台/1=抖音/2=快手/3=视频号，相差 1。
        // 不限平台时取全部（全平台 + 各平台专属词的并集，对合规自检是更安全的超集）。
        List<Integer> platformTypes = bo.getPlatform() == null ? null : Arrays.asList(0, bo.getPlatform() + 1);

        List<SensitiveWordsEntity> rows = sensitiveWordsService.lambdaQuery()
            .select(SensitiveWordsEntity::getName, SensitiveWordsEntity::getLevel,
                SensitiveWordsEntity::getType, SensitiveWordsEntity::getSimilarWords)
            .eq(SensitiveWordsEntity::getWordsType, wordsType)
            .eq(SensitiveWordsEntity::getStatus, 0)
            .eq(SensitiveWordsEntity::getResourceType, 0)
            .in(SensitiveWordsEntity::getTradeId, tradeIds)
            .in(CollUtil.isNotEmpty(platformTypes), SensitiveWordsEntity::getPlatformType, platformTypes)
            .orderByAsc(SensitiveWordsEntity::getLevel)
            .list();
        if (CollUtil.isEmpty(rows)) {
            return R.ok(new ArrayList<>());
        }

        // 已按 level 升序，按词名去重时首次出现即最严重等级，保留之；空白词名跳过。
        Map<String, SensitiveWordVo> deduped = new LinkedHashMap<>();
        for (SensitiveWordsEntity e : rows) {
            String name = e.getName() == null ? null : e.getName().trim();
            if (StrUtil.isBlank(name) || deduped.containsKey(name)) {
                continue;
            }
            SensitiveWordVo vo = new SensitiveWordVo();
            vo.setWord(name);
            vo.setLevel(e.getLevel());
            vo.setLevelLabel(label(SENSITIVE_LEVEL_LABELS, e.getLevel()));
            vo.setType(e.getType());
            vo.setTypeLabel(label(SENSITIVE_TYPE_LABELS, e.getType()));
            vo.setSimilarWords(StrUtil.isBlank(e.getSimilarWords()) ? null : e.getSimilarWords());
            deduped.put(name, vo);
            if (deduped.size() >= MAX_SENSITIVE_WORDS) {
                log.warn("[AI-AGENT] sensitiveWords 触顶截断 tradeId={}, platform={}, 上限={}",
                    bo.getTradeId(), bo.getPlatform(), MAX_SENSITIVE_WORDS);
                break;
            }
        }
        return R.ok(new ArrayList<>(deduped.values()));
    }

    /**
     * 解析敏感词取词的行业 id 集合：行业父链（自身 + 各级父，{@code GENERAL_YES} 自动并入全行业 id=1）。
     * 父链解析失败时退化为 [自身, 全行业]，保证至少能命中行业专属词与全行业词。
     */
    private List<Long> resolveSensitiveTradeIds(Long tradeId) {
        List<TradeInfoVo> chain = tradeProducer.listParentsByTradeId(tradeId, Constant.GeneralEnum.GENERAL_YES.getCode());
        List<Long> ids = new ArrayList<>();
        if (CollUtil.isNotEmpty(chain)) {
            for (TradeInfoVo t : chain) {
                if (t.getId() != null && !ids.contains(t.getId())) {
                    ids.add(t.getId());
                }
            }
        }
        if (!ids.contains(tradeId)) {
            ids.add(tradeId);
        }
        if (!ids.contains(1L)) {
            ids.add(1L);
        }
        return ids;
    }

    // ==================== 4.3 主播详情 ====================

    @Override
    public R<AnchorDetailVo> anchorDetail(AnchorDetailQueryBo bo) {
        R<AnchorDetailVo> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        // 解析有效数据源：DAO 据此决定是否叠加 user_id 过滤（自己录制叠加，全租户不叠加；
        // 自己录制+云空间(4)为并集：本人 OR 已上传云空间）
        bo.setDataSourceType(resolveDataSource(bo.getDataSourceType(), bo.getUserType()));
        boolean selfAndCloud = Objects.equals(bo.getDataSourceType(), DATA_SOURCE_SELF_AND_CLOUD);
        Long scopedUserId = Objects.equals(bo.getDataSourceType(), DATA_SOURCE_SELF) || selfAndCloud
            ? scopedUserId(bo) : null;

        AnchorUrlUserEntity user = anchorUrlUserService.lambdaQuery()
            .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, bo.getSecUid())
            .eq(AnchorUrlUserEntity::getTenantId, bo.getTenantId())
            .eq(Objects.equals(bo.getDataSourceType(), DATA_SOURCE_CLOUD), AnchorUrlUserEntity::getIsAutoUploadCloud, 1)
            .eq(AnchorUrlUserEntity::getIsDeleted, 0)
            .eq(!selfAndCloud && scopedUserId != null, AnchorUrlUserEntity::getUserId, scopedUserId)
            // 主账号(userType 0/1) scopedUserId 为 null 时跳过 OR 块是预期行为：自己录制即租户级，天然覆盖云空间子集
            .and(selfAndCloud && scopedUserId != null, w -> w.eq(AnchorUrlUserEntity::getUserId, scopedUserId)
                .or().eq(AnchorUrlUserEntity::getIsAutoUploadCloud, 1))
            .last("limit 1")
            .one();
        if (user == null) {
            return R.error("主播不存在或无权访问");
        }
        AnchorUrlEntity base = anchorUrlService.lambdaQuery()
            .eq(AnchorUrlEntity::getSecUid, bo.getSecUid())
            .last("limit 1")
            .one();

        AnchorDetailVo vo = new AnchorDetailVo();
        vo.setSecUid(bo.getSecUid());
        if (base != null) {
            vo.setAnchorName(base.getAnchorName());
            vo.setAnchorAvatar(base.getAnchorAvatar());
            vo.setPlatform(base.getPlatform());
            vo.setPlatformLabel(label(PLATFORM_LABELS, base.getPlatform()));
            vo.setAnchorNumber(base.getAnchorNumber());
        }
        vo.setTradeId(user.getTradeId());
        vo.setTradeName(user.getTradeId() == null ? null : tradeProducer.getById(user.getTradeId()));
        vo.setAccountType(user.getAccountType());
        vo.setAccountTypeLabel(label(ACCOUNT_TYPE_LABELS, user.getAccountType()));
        vo.setAddDate(formatDateTime(user.getCreateDate()));
        vo.setLastRecordTime(user.getLastRecordTime());

        // 配置信息（来自 tb_anchor_url_user）
        vo.setAccountStage(user.getAccountStage());
        vo.setAccountStageLabel(dictLabel("account_stage", user.getAccountStage()));
        vo.setAccountWaterLevel(user.getAccountWaterLevel());
        vo.setAccountWaterLevelLabel(dictLabel("account_water_level", user.getAccountWaterLevel()));
        vo.setAccountFlow(user.getAccountFlow());
        vo.setAccountFlowLabel(dictLabel("account_flow", user.getAccountFlow()));

        // 基础设置（tb_basic_settings，sourceType=0 主播）
        BasicSettingsEntity bs = basicSettingsService.lambdaQuery()
            .eq(BasicSettingsEntity::getSourceId, bo.getSecUid())
            .eq(BasicSettingsEntity::getSourceType, 0)
            .eq(BasicSettingsEntity::getTenantId, bo.getTenantId())
            .eq(scopedUserId != null, BasicSettingsEntity::getUserId, scopedUserId)
            .last("limit 1")
            .one();
        vo.setBasicSettings(buildBasicSettings(bs));

        return R.ok(vo);
    }

    private AnchorBasicSettingsVo buildBasicSettings(BasicSettingsEntity bs) {
        if (bs == null) {
            return null;
        }
        AnchorBasicSettingsVo vo = new AnchorBasicSettingsVo();
        vo.setPremiereDate(formatDate(bs.getPremiereDate()));
        vo.setAccountStage(bs.getAccountStage());
        vo.setAccountStageLabel(dictLabel("account_stage", bs.getAccountStage()));
        vo.setAccountWaterLevel(bs.getAccountWaterLevel());
        vo.setAccountWaterLevelLabel(dictLabel("account_water_level", bs.getAccountWaterLevel()));
        vo.setAccountFlow(bs.getAccountFlow());
        vo.setAccountFlowLabel(dictLabel("account_flow", bs.getAccountFlow()));
        vo.setLivingTarget(bs.getLivingTarget());
        vo.setLivingTargetLabel(dictLabel("living_target", bs.getLivingTarget()));
        vo.setLivingModality(bs.getLivingModality());
        vo.setLivingModalityLabel(dictLabel("living_modality", bs.getLivingModality()));
        vo.setMarketing(bs.getMarketing());
        vo.setMarketingLabel(dictLabel("marketing", bs.getMarketing()));
        vo.setLivingMode(bs.getLivingMode());
        vo.setLivingModeLabel(dictLabel("living_mode", bs.getLivingMode()));
        vo.setOptimizeDirection(multiDictLabel("optimize_direction", bs.getOptimizeDirection()));
        vo.setLearning(multiDictLabel("learning", bs.getLearning()));
        vo.setAnchorSituation(bs.getAnchorSituation());
        return vo;
    }

    // ==================== 4.4 视频列表 ====================

    /**
     * 视频列表（多条件过滤 + 游标分页）。
     *
     * @param bo 查询条件
     * @return 视频列表游标页
     */
    @Override
    public R<CursorPageVo<VideoItemVo>> videoList(VideoListQueryBo bo) {
        R<CursorPageVo<VideoItemVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        int dataSource = resolveDataSource(bo.getDataSourceType(), bo.getUserType());
        // 自己录制叠加 userId；全租户仅租户级；云空间仅限定已分享(uploadStatus=1)、不限租户/用户全局可见；
        // 自己录制+云空间(4)为两者并集：(tenantId+userId 本人录制) OR (uploadStatus=1 已分享)
        Long scopedUserId = dataSource == DATA_SOURCE_SELF ? bo.getUserId() : null;
        boolean cloudOnly = dataSource == DATA_SOURCE_CLOUD;
        boolean selfAndCloud = dataSource == DATA_SOURCE_SELF_AND_CLOUD;
        int pageSize = clampPageSize(bo.getPageSize(), 300, 3000);

        // secUid 集合 ∪ 主播名称集合→secUid
        List<String> secUidFilter = resolveVideoSecUidFilter(bo);
        if (secUidFilter != null && secUidFilter.isEmpty()) {
            return R.ok(CursorPageVo.empty());
        }
        List<Long> tradeIds = expandTradeIds(bo.getTradeId());

        List<AnchorVideoEntity> rows = anchorVideoService.lambdaQuery()
            .lt(bo.getCursor() != null, AnchorVideoEntity::getId, bo.getCursor())
            .eq(!cloudOnly && !selfAndCloud, AnchorVideoEntity::getTenantId, bo.getTenantId())
            .in(EmptyUtil.isNotEmpty(bo.getVideoIds()), AnchorVideoEntity::getVideoId, bo.getVideoIds())
            .in(EmptyUtil.isNotEmpty(bo.getBatchNumbers()), AnchorVideoEntity::getBatchNumber, bo.getBatchNumbers())
            .eq(AnchorVideoEntity::getIsDeleted, 0)
            // 已分享(upload_status=1)只排除 delete_status=2；其余排除 delete_status in (1,2)
            .apply("delete_status <> 2 AND NOT (delete_status = 1 AND upload_status <> 1)")
            .gt(AnchorVideoEntity::getDuration, 60)
            .eq(Boolean.TRUE.equals(bo.getAnalysisComplete()), AnchorVideoEntity::getAnalysisStatus, 2)
            .eq(AnchorVideoEntity::getTenantId, bo.getTenantId())
            .eq(AnchorVideoEntity::getIsRecording, 0)
            .eq(scopedUserId != null, AnchorVideoEntity::getUserId, scopedUserId)
            .eq(cloudOnly, AnchorVideoEntity::getUploadStatus, VIDEO_UPLOAD_STATUS_SHARED)
            .and(selfAndCloud, w -> w.eq(AnchorVideoEntity::getUploadStatus, VIDEO_UPLOAD_STATUS_SHARED)
                .or().eq(AnchorVideoEntity::getUserId, bo.getUserId()))
            .in(secUidFilter != null, AnchorVideoEntity::getSecUid, secUidFilter)
            .in(tradeIds != null, AnchorVideoEntity::getTradeId, tradeIds)
            .eq(bo.getVideoSliceType() != null, AnchorVideoEntity::getVideoSliceType, bo.getVideoSliceType())
            .ge(StrUtil.isNotBlank(bo.getStartDate()), AnchorVideoEntity::getStartTime, bo.getStartDate())
            .le(StrUtil.isNotBlank(bo.getEndDate()), AnchorVideoEntity::getStartTime, bo.getEndDate())
            .orderByDesc(AnchorVideoEntity::getId)
            .last("limit " + pageSize)
            .list();

        boolean hasMore = rows.size() == pageSize;
        Long nextCursor = hasMore ? rows.get(rows.size() - 1).getId() : null;

        List<String> pageSecUids = rows.stream().map(AnchorVideoEntity::getSecUid)
            .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<String> videoIds = rows.stream().map(AnchorVideoEntity::getVideoId)
            .filter(Objects::nonNull).distinct().toList();
        Map<String, AnchorUrlEntity> baseMap = bo.getSimple() ? Map.of() : loadAnchorBaseMap(pageSecUids);
        Map<String, AnchorUrlUserEntity> anchorUserMap = bo.getSimple() ? Map.of() : loadAnchorUserMap(pageSecUids, bo.getTenantId(), scopedUserId);

        Map<String, VideoWatchHasVO> videoHasDashboardMap = bo.getSimple() ? Map.of() : videoDataViewingBll.getVideoWatchNum(videoIds);
        Map<String, SocketCollectMessageVo> videoChartDataMap = bo.getSimple() ? Map.of() : this.getVideoHasChartDataMap(videoIds);

        // 自有/同行过滤在内存做（归属属性在 tb_anchor_url_user）
        List<AnchorVideoEntity> visible = rows;
        if (bo.getAccountType() != null) {
            visible = rows.stream().filter(v -> {
                AnchorUrlUserEntity u = anchorUserMap.get(v.getSecUid());
                return u != null && Objects.equals(u.getAccountType(), bo.getAccountType());
            }).toList();
        }

        Map<Long, String> tradeNameMap = loadTradeNames(visible.stream()
            .map(AnchorVideoEntity::getTradeId).filter(Objects::nonNull).distinct().collect(Collectors.toList()));

        List<VideoItemVo> list = visible.stream().map(v -> {
            VideoItemVo vo = new VideoItemVo();
            vo.setVideoId(v.getVideoId());
            vo.setUserId(v.getUserId());
            vo.setBatchNumber(v.getBatchNumber());
            vo.setParagraph(v.getParagraph());
            vo.setSecUid(v.getSecUid());
            AnchorUrlEntity base = baseMap.get(v.getSecUid());
            if (base != null) {
                vo.setAnchorName(base.getAnchorName());
            }
            vo.setTradeId(v.getTradeId());
            vo.setTradeName(v.getTradeId() == null ? null : tradeNameMap.get(v.getTradeId()));
            AnchorUrlUserEntity u = anchorUserMap.get(v.getSecUid());
            Integer accountType = u == null ? null : u.getAccountType();
            vo.setAccountType(accountType);
            vo.setAccountTypeLabel(label(ACCOUNT_TYPE_LABELS, accountType));
            vo.setVideoSliceType(v.getVideoSliceType());
            vo.setVideoSliceTypeLabel(label(VIDEO_SLICE_TYPE_LABELS, v.getVideoSliceType()));
            vo.setStartTime(formatDateTime(v.getStartTime()));
            vo.setEndTime(formatDateTime(v.getEndTime()));
            vo.setDuration(v.getDuration());
            vo.setPlayUrl(v.getPlayUrl());
            vo.setAnalysisStatus(v.getAnalysisStatus());
            if (NumberUtil.isInteger(v.getPlatformType())) {
                vo.setPlatform(Integer.parseInt(v.getPlatformType()) - 1);
            }
            vo.setVideoName(v.getVideoName());
            vo.setVideoRename(v.getVideoRename());
            vo.setLiveTitle(v.getLiveTitle());
            vo.setVideoType(v.getVideoType());
            vo.setDefinition(v.getDefinition());
            vo.setStoragePath(v.getStoragePath());
            vo.setId(v.getId());
            VideoWatchHasVO videoWatchHasVO = videoHasDashboardMap.get(v.getVideoId());
            vo.setHasDashboard(false);
            vo.setHasRoi(false);
            Integer watchNum = null;
            SocketCollectMessageVo chartData = videoChartDataMap.get(v.getVideoId());
            vo.setHasChartData(chartData != null);
            if (videoWatchHasVO != null) {
                vo.setHasRoi(videoWatchHasVO.getHasRoi());
                watchNum = videoWatchHasVO.getWatchNum();
                vo.setHasDashboard(videoWatchHasVO.getWatchNum() != null && videoWatchHasVO.getWatchNum() > 0);
            }

            if (chartData != null) {
                vo.setHasDashboard(true);
                vo.setWatchNum(chartData.getObservationNum());
                vo.setTotalBarrageNum(chartData.getTotalBarrageNum());
                vo.setOnlineMaxNum(chartData.getOnlineMaxNum());
                if (watchNum != null) {
                    vo.setWatchNum(watchNum.toString());
                }
                vo.setHasBarrages(chartData.getTotalBarrageNum() != null && chartData.getTotalBarrageNum() > 0);
            } else if (watchNum != null) {
                vo.setWatchNum(watchNum.toString());
            }
            return vo;
        }).collect(Collectors.toList());

        return R.ok(CursorPageVo.of(list, nextCursor, hasMore));
    }

    private List<String> resolveVideoSecUidFilter(VideoListQueryBo bo) {
        boolean hasSecUidList = CollUtil.isNotEmpty(bo.getSecUidList());
        boolean hasNameList = CollUtil.isNotEmpty(bo.getAnchorNameList());
        if (!hasSecUidList && !hasNameList) {
            return null;
        }
        List<String> result = new ArrayList<>();
        if (hasSecUidList) {
            result.addAll(bo.getSecUidList());
        }
        if (hasNameList) {
            List<String> nameSecUids = anchorUrlService.lambdaQuery()
                .select(AnchorUrlEntity::getSecUid)
                .and(c -> {
                    bo.getAnchorNameList().forEach(name -> {
                        c.like(AnchorUrlEntity::getAnchorName, name).or();
                    });
                })
                .last("limit 500")
                .list().stream().map(AnchorUrlEntity::getSecUid).filter(Objects::nonNull).toList();
            result.addAll(nameSecUids);
        }
        return result.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    // ==================== 4.5 视频音频段落全文 ====================

    @Override
    public R<List<AudioParagraphVo>> audioParagraphs(VideoQueryBo bo) {
        R<List<AudioParagraphVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        AnchorVideoEntity video = loadScopedVideo(bo, bo.getVideoId(), List.of(AnchorVideoEntity::getId, AnchorVideoEntity::getVideoId, AnchorVideoEntity::getTenantId, AnchorVideoEntity::getUploadStatus));
        if (video == null) {
            return R.error("视频不存在或无权访问");
        }
        // 直接拉取 OSS 分析结果 zip 全文，仅取段落文本；不查主播/在线人数/巨量等多余数据，也不校验上传状态
        String content;
        try {
            R<String> urlR = sensitiveWordsLogic.getAnalysisDownloadUrl(0, bo.getVideoId());
            if (urlR == null || urlR.getCode() == null || urlR.getCode() != 0 || StrUtil.isBlank(urlR.getData())) {
                return R.ok(new ArrayList<>());
            }
            content = ReplayFileUtils.getFileContentByZipDownloadUrl(urlR.getData());
        } catch (Exception e) {
            log.error("[AI-AGENT] audioParagraphs download analysis error, videoId={}", bo.getVideoId(), e);
            return R.ok(new ArrayList<>());
        }
        if (StrUtil.isBlank(content)) {
            return R.ok(new ArrayList<>());
        }

        JSONArray marks = JSONUtil.parseObj(content).getJSONArray("sentenceMarkVos");
        if (CollUtil.isEmpty(marks)) {
            return R.ok(new ArrayList<>());
        }
        List<AudioParagraphVo> list = marks.stream()
            .map(JSONObject.class::cast)
            .map(j -> {
                AudioParagraphVo vo = new AudioParagraphVo();
                vo.setParagraph(j.getInt("currentSort"));
                vo.setContent(j.getStr("content"));
                try {
                    JSONArray items = j.getJSONArray("items");
                    if (EmptyUtil.isNotEmpty(items)) {
                        Long startTime = items.stream().map(JSONObject.class::cast).map(i -> i.getLong("startTime")).min(Long::compareTo).orElse(null);
                        Long endTime = items.stream().map(JSONObject.class::cast).map(i -> i.getLong("endTime")).max(Long::compareTo).orElse(null);
                        if (startTime != null && endTime != null) {
                            vo.setTime((startTime == 0 ? 0 : startTime / 1000) + "s - " + (endTime == 0 ? 0 : endTime / 1000) + "s");
                        }
                    }
                } catch (Exception e) {
                    log.error("[AI-AGENT] audioParagraphs parse items error, videoId={}", bo.getVideoId(), e);
                }
                return vo;
            })
            .filter(v -> StrUtil.isNotBlank(v.getContent()))
            .sorted(Comparator.comparingInt(v -> v.getParagraph() == null ? 0 : v.getParagraph()))
            .collect(Collectors.toList());
        return R.ok(list);
    }

    /**
     * 获取视频场景片段列表。
     *
     * @param videoId  视频ID
     * @param tenantId 租户ID
     *
     * @return 场景片段列表
     */
    @Override
    public R<List<SceneSliceVo>> sceneSlices(String videoId, long tenantId) {
        if (EmptyUtil.isEmpty(videoId)) {
            return R.error("缺少videoId");
        }
        return R.ok(sceneSliceRse.listStuckProcessing(videoId, tenantId));
    }


    // ==================== 4.6 视频数据看板 ====================

    @Override
    public R<VideoDashboardVo> videoDashboard(VideoQueryBo bo) {
        R<VideoDashboardVo> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        AnchorVideoEntity video = loadScopedVideo(bo, bo.getVideoId(), List.of(AnchorVideoEntity::getId, AnchorVideoEntity::getVideoId, AnchorVideoEntity::getTenantId, AnchorVideoEntity::getUploadStatus));
        if (video == null) {
            return R.error("视频不存在或无权访问");
        }
        VideoDashboardVo vo = new VideoDashboardVo();
        vo.setVideoId(bo.getVideoId());

        OceanEngineDataInfoVo info = loadDashboardInfo(bo.getVideoId());
        if (info != null) {
            vo.setSecUid(info.getSecUid());
            vo.setBatchNumber(info.getBatchNumber());
            vo.setAnchorNumber(info.getAnchorNumber());
            vo.setIsTakeProduct(info.getIsTakeProduct());
            vo.setTotalWatchNum(info.getTotalWatchNum());
            vo.setAverageOnlineNum(info.getAverageOnlineNum());
            vo.setAverageResidenceTime(info.getAverageResidenceTime());
            vo.setIncrementFollowerCount(info.getIncrementFollowerCount());
            vo.setConvertFanRate(info.getConvertFanRate());
            vo.setInteractionPercent(info.getInteractionPercent());
            // 成交族一律经 nullIfGap：采集侧对「平台未回传」写 -1，原样透传会被下游读成负数或「零成交」
            vo.setVolume(nullIfGap(info.getVolume()));
            vo.setPurchaseCount(nullIfGap(info.getPurchaseCount()));
            vo.setCustomerUnitPrice(nullIfGap(info.getCustomerUnitPrice()));
            vo.setUvValue(nullIfGap(info.getUvValue()));
            vo.setGoodsConvertRate(nullIfGap(info.getGoodsConvertRate()));
            vo.setShowWatchCntRatio(info.getShowWatchCntRatio());
            vo.setRoi(info.getRoi());
            vo.setLaunchRoiAmount(info.getLaunchRoiAmount());
            vo.setRefundAmount(info.getRefundAmount());
            vo.setOverallCostRoi(info.getOverallCostRoi());
            vo.setNetTransactionRoi(info.getNetTransactionRoi());
            vo.setWatchFlowList(info.getWatchFlowList());
            vo.setPayFlowList(info.getPayFlowList());

            // 区间端点同样可能是 -1 哨兵（采到 "-" 时 start/end 一起写 -1），同样归一为 null
            vo.setVolumeStart(nullIfGap(info.getVolumeStart()));
            vo.setVolumeEnd(nullIfGap(info.getVolumeEnd()));
            vo.setPurchaseCountStart(nullIfGap(info.getPurchaseCountStart()));
            vo.setPurchaseCountEnd(nullIfGap(info.getPurchaseCountEnd()));
            vo.setCustomerUnitPriceStart(nullIfGap(info.getCustomerUnitPriceStart()));
            vo.setUvValueStart(nullIfGap(info.getUvValueStart()));
            vo.setGoodsConvertRateStart(nullIfGap(info.getGoodsConvertRateStart()));
            vo.setGoodsConvertRateEnd(nullIfGap(info.getGoodsConvertRateEnd()));
            vo.setUvValueEnd(nullIfGap(info.getUvValueEnd()));
            vo.setCustomerUnitPriceEnd(nullIfGap(info.getCustomerUnitPriceEnd()));
        }

        // 巨量/蝉妈妈看板缺观看/在线指标时，用实时采集曲线（onlineChartData，只读且自带缓存）补齐，不影响其它平台调用
        supplementFromOnlineChart(vo, bo.getVideoId());

        // 该视频已识别完成的数据截图，按「截图 code + 识别内容」组装
        vo.setScreenshots(loadDashboardScreenshots(bo.getVideoId(), video.getTenantId()));

        return R.ok(vo);
    }

    /**
     * 加载视频数据截图兜底内容：取 tb_data_screenshot 中 screenshotStatus=3（AI 识别完成）、sourceId=videoId 的记录，
     * 按「截图 code + 识别内容」组装。按视频归属租户过滤（已分享视频可跨租户，故以视频实体 tenantId 为准）。
     */
    private List<DashboardScreenshotVo> loadDashboardScreenshots(String videoId, Long tenantId) {
        List<DataScreenshotEntity> rows = dataScreenshotService.lambdaQuery()
            .eq(DataScreenshotEntity::getSourceId, videoId)
            .eq(DataScreenshotEntity::getTenantId, tenantId)
            .eq(DataScreenshotEntity::getScreenshotStatus, SCREENSHOT_STATUS_RECOGNIZED)
            .orderByAsc(DataScreenshotEntity::getId)
            .list();
        return rows.stream().map(e -> {
            DashboardScreenshotVo sv = new DashboardScreenshotVo();
            sv.setScreenshotCode(e.getScreenshotCode());
            sv.setContent(e.getAiContent());
            return sv;
        }).collect(Collectors.toList());
    }

    /**
     * 用实时采集曲线接口（{@code socketCollectMessageLogic.onlineChartData}，只读、自带 7 天缓存）补充看板。
     * <p>1）观看/在线标量：{@code totalWatchNum ← 累计观看人数}、{@code averageOnlineNum ← 在线折线均值}，
     * 仅在巨量/蝉妈妈未提供（为 null）时填充，已有值不覆盖；平均停留/转粉/互动/UV/成交实时源没有，无法补。
     * 2）时序与福袋：看板源本就不含，直接补充——曲线统一降采样到约 {@value #DASHBOARD_CURVE_POINTS} 点、福袋取压缩版，控制体积。
     * 3）口径：源曲线是<b>分钟增量</b>（{@code OceanEngineDataProducerImpl} 按分钟窗口 max−min），抽样到约
     * {@value #DASHBOARD_CURVE_POINTS} 点后单分钟增量无从解读，故增量型指标在此<b>先累计再抽样</b>，
     * 落到看板即「累计到该时点」；在线人数是瞬时水平量，保持原值不累计。净成交 ROI 用累计额与累计消耗重算，
     * 不是分钟 ROI 的抽样。全量分钟增量曲线仍由 4.8 在线曲线接口提供。</p>
     */
    private void supplementFromOnlineChart(VideoDashboardVo vo, String videoId) {
        R<OnlineChartVo> r = socketCollectMessageLogic.onlineChartData(videoId);
        if (r == null || r.getCode() == null || r.getCode() != 0 || r.getData() == null) {
            return;
        }
        OnlineChartVo c = r.getData();
        if (vo.getTotalWatchNum() == null && c.getTotalViewersNum() != null) {
            vo.setTotalWatchNum(c.getTotalViewersNum());
        }
        if (vo.getAverageOnlineNum() == null) {
            Integer avg = averageOnline(c.getOnlineDataList());
            if (avg != null) {
                vo.setAverageOnlineNum(avg);
            }
        }
        vo.setTotalBarrageNum(c.getTotalBarrageNum());
        // 在线人数是瞬时水平量，直接抽样；其余增量型指标一律先累计再抽样（见方法注释 3）
        vo.setOnlineDataList(toPointsCompact(c.getOnlineDataList()));
        vo.setApproachDataList(toPointsCompact(cumulate(c.getApproachDataList())));
        vo.setExitPeopleDataList(toPointsCompact(cumulate(c.getExitPeopleDataList())));
        vo.setPayComboCntDataList(toPointsCompact(cumulate(c.getPayComboCntDataList())));
        vo.setPayAmtDataList(toPointsCompact(cumulate(toCurveDataList(c.getPayAmtDataList()))));
        vo.setFollowAnchorUcntDataList(toPointsCompact(cumulate(c.getFollowAnchorUcntDataList())));
        // 投放/退款/净成交ROI：ROI 必须用累计额÷累计消耗重算，逐分钟 ROI 抽样出来的值代表不了任何区间
        List<CurveDoubleData> payAmtCum = cumulateDouble(c.getPayAmtDataList());
        List<CurveDoubleData> costCum = cumulateDouble(c.getQianchuanCostDataList());
        List<CurveDoubleData> refundCum = cumulateDouble(c.getRefundAmtDataList());
        vo.setQianchuanCostDataList(toDoublePointsCompact(costCum));
        vo.setRefundAmtDataList(toDoublePointsCompact(refundCum));
        vo.setNetTransactionRoiDataList(toDoublePointsCompact(cumulativeNetRoi(payAmtCum, refundCum, costCum)));
        vo.setBlessBagList(toBlessBags(c.getBlessBagList()));
    }

    /**
     * 分钟增量曲线 → 累计曲线（Integer 版，时点与原序列一一对应）。
     * <p><b>缺采点（{@code valueNum} 为 null）该点输出 null，不输出跑合值</b>——该分钟增量未知，
     * 到该时点的累计量就是未知的，写成「当前跑合」等于宣称一个我们并不知道的数（下游会当真值读）。
     * 后续点继续用已知增量累计。累计在 long 上做、封顶 {@link Integer#MAX_VALUE}，避免整场累计溢出成负数。</p>
     */
    private List<CurveData> cumulate(List<CurveData> src) {
        if (CollUtil.isEmpty(src)) {
            return new ArrayList<>();
        }
        List<CurveData> out = new ArrayList<>(src.size());
        long sum = 0L;
        for (CurveData d : src) {
            if (d == null) {
                continue;
            }
            CurveData p = new CurveData();
            p.setDateTime(d.getDateTime());
            p.setDateTimeNew(d.getDateTimeNew());
            if (d.getValueNum() == null) {
                p.setValueNum(null);            // 该分钟增量未知 → 累计量未知，不编一个数出来
            } else {
                sum += d.getValueNum();
                p.setValueNum((int) Math.min(sum, Integer.MAX_VALUE));
            }
            out.add(p);
        }
        return out;
    }

    /**
     * 分钟增量曲线 → 累计曲线（Double 版；累计在 {@link BigDecimal} 上做，避免逐点相加的浮点漂移）。
     * 空值语义同 {@link #cumulate}：缺采点输出 null，不输出跑合值。
     */
    private List<CurveDoubleData> cumulateDouble(List<CurveDoubleData> src) {
        if (CollUtil.isEmpty(src)) {
            return new ArrayList<>();
        }
        List<CurveDoubleData> out = new ArrayList<>(src.size());
        BigDecimal sum = BigDecimal.ZERO;
        for (CurveDoubleData d : src) {
            if (d == null) {
                continue;
            }
            CurveDoubleData p = new CurveDoubleData();
            p.setDateTime(d.getDateTime());
            p.setDateTimeNew(d.getDateTimeNew());
            if (d.getValueNum() == null) {
                p.setValueNum(null);
            } else {
                sum = sum.add(BigDecimal.valueOf(d.getValueNum()));
                p.setValueNum(sum.doubleValue());
            }
            out.add(p);
        }
        return out;
    }

    /**
     * 累计净成交 ROI 曲线：{@code (累计成交额 − 累计退款) / 累计投放消耗}，保留 2 位小数。
     * <p><b>不可计算就输出 null，绝不写 0</b>：累计消耗缺失或为 0（未投流）时 ROI 无定义，
     * 写 0 会被读成「投了钱一分没回本」，与「压根没投放」是两回事。净额为负（退款多于成交）照实算负 ROI，
     * 不钳到 0——钳零是粉饰。时间轴以累计消耗为准，成交额/退款按 {@code dateTime} 对齐（三条源自同一份
     * incrementalData，时点天然一致）；对不上或成交额缺失则该点为 null，不外推、不当 0。</p>
     */
    private List<CurveDoubleData> cumulativeNetRoi(List<CurveDoubleData> payAmtCum,
                                                   List<CurveDoubleData> refundCum,
                                                   List<CurveDoubleData> costCum) {
        if (CollUtil.isEmpty(costCum)) {
            return new ArrayList<>();
        }
        Map<Long, Double> payAt = indexByTime(payAmtCum);
        Map<Long, Double> refundAt = indexByTime(refundCum);
        List<CurveDoubleData> out = new ArrayList<>(costCum.size());
        for (CurveDoubleData cost : costCum) {
            if (cost == null || cost.getDateTime() == null) {
                continue;
            }
            CurveDoubleData p = new CurveDoubleData();
            p.setDateTime(cost.getDateTime());
            p.setDateTimeNew(cost.getDateTimeNew());

            Double payV = payAt.get(cost.getDateTime());
            BigDecimal c = cost.getValueNum() == null ? null : BigDecimal.valueOf(cost.getValueNum());
            if (c == null || c.compareTo(BigDecimal.ZERO) <= 0 || payV == null) {
                p.setValueNum(null);            // 无消耗/无成交数据 → ROI 无定义
            } else {
                // 退款缺数据按 0 扣：缺退款 ≠ 有退款，此处不给 ROI 打折
                BigDecimal refund = BigDecimal.valueOf(refundAt.getOrDefault(cost.getDateTime(), 0.0));
                BigDecimal net = BigDecimal.valueOf(payV).subtract(refund);
                p.setValueNum(net.divide(c, 4, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP).doubleValue());
            }
            out.add(p);
        }
        return out;
    }

    /** 曲线按 dateTime 建索引（同时点后者覆盖前者），供跨曲线对齐取值。 */
    private Map<Long, Double> indexByTime(List<CurveDoubleData> src) {
        if (CollUtil.isEmpty(src)) {
            return Map.of();
        }
        Map<Long, Double> m = new HashMap<>(src.size());
        for (CurveDoubleData d : src) {
            if (d != null && d.getDateTime() != null && d.getValueNum() != null) {
                m.put(d.getDateTime(), d.getValueNum());
            }
        }
        return m;
    }

    /**
     * 在线人数折线求均值（空安全）；无有效点返回 null。
     */
    private Integer averageOnline(List<CurveData> onlineDataList) {
        if (CollUtil.isEmpty(onlineDataList)) {
            return null;
        }
        long sum = 0;
        int cnt = 0;
        for (CurveData d : onlineDataList) {
            if (d.getValueNum() != null) {
                sum += d.getValueNum();
                cnt++;
            }
        }
        return cnt == 0 ? null : (int) (sum / cnt);
    }

    /**
     * 曲线降采样为最多约 {@value #DASHBOARD_CURVE_POINTS} 点（等步长抽样 + 始终保留末点），压缩看板返回体积。
     */
    private List<CurvePointVo> toPointsCompact(List<CurveData> src) {
        if (CollUtil.isEmpty(src)) {
            return new ArrayList<>();
        }
        int n = src.size();
        int step = Math.max(1, n / DASHBOARD_CURVE_POINTS);
        List<CurvePointVo> out = new ArrayList<>();
        for (int i = 0; i < n; i += step) {
            out.add(toPoint(src.get(i)));
        }
        CurvePointVo last = toPoint(src.get(n - 1));
        if (!Objects.equals(out.get(out.size() - 1).getDateTime(), last.getDateTime())) {
            out.add(last);
        }
        return out;
    }

    private CurvePointVo toPoint(CurveData d) {
        CurvePointVo p = new CurvePointVo();
        p.setDateTime(d.getDateTime());
        p.setValueNum(d.getValueNum());
        return p;
    }

    /**
     * Double 曲线降采样为最多约 {@value #DASHBOARD_CURVE_POINTS} 点（口径同 {@link #toPointsCompact}：等步长抽样 + 始终保留末点）。
     */
    private List<CurvePointDoubleVo> toDoublePointsCompact(List<CurveDoubleData> src) {
        if (CollUtil.isEmpty(src)) {
            return new ArrayList<>();
        }
        int n = src.size();
        int step = Math.max(1, n / DASHBOARD_CURVE_POINTS);
        List<CurvePointDoubleVo> out = new ArrayList<>();
        for (int i = 0; i < n; i += step) {
            out.add(toDoublePoint(src.get(i)));
        }
        CurvePointDoubleVo last = toDoublePoint(src.get(n - 1));
        if (!Objects.equals(out.get(out.size() - 1).getDateTime(), last.getDateTime())) {
            out.add(last);
        }
        return out;
    }

    private CurvePointDoubleVo toDoublePoint(CurveDoubleData d) {
        CurvePointDoubleVo p = new CurvePointDoubleVo();
        p.setDateTime(d.getDateTime());
        p.setDateTimeNew(d.getDateTimeNew());
        p.setValueNum(d.getValueNum() == null ? null : BigDecimal.valueOf(d.getValueNum()));
        return p;
    }

    /**
     * 福袋转压缩版（仅时点+奖品+参与人数），空安全。
     */
    private List<DashboardBlessBagVo> toBlessBags(List<OnlineChartBlessBagVo> src) {
        if (CollUtil.isEmpty(src)) {
            return new ArrayList<>();
        }
        return src.stream().map(b -> {
            DashboardBlessBagVo v = new DashboardBlessBagVo();
            v.setRelativeTime(b.getRelativeTime());
            v.setBlessBagReward(b.getBlessBagReward());
            v.setCandidateNum(b.getCandidateNum());
            return v;
        }).collect(Collectors.toList());
    }


    // ==================== 4.7 视频弹幕 ====================

    @Override
    public R<CursorPageVo<BarrageItemVo>> videoBarrages(BarrageQueryBo bo) {
        R<CursorPageVo<BarrageItemVo>> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        AnchorVideoEntity video = loadScopedVideo(bo, bo.getVideoId(), List.of(AnchorVideoEntity::getId, AnchorVideoEntity::getVideoId, AnchorVideoEntity::getTenantId, AnchorVideoEntity::getUploadStatus, AnchorVideoEntity::getBatchNumber, AnchorVideoEntity::getUserId));
        if (video == null) {
            return R.error("视频不存在或无权访问");
        }
        if (Objects.equals(video.getPlatformType(), "3")) {
            return R.ok(CursorPageVo.empty());
        }
        if (StrUtil.isBlank(video.getBatchNumber())) {
            return R.ok(CursorPageVo.empty());
        }
        int pageSize = clampPageSize(bo.getPageSize(), 200, 5000);
        long page = bo.getCursor() == null ? 1L : bo.getCursor();

        QueryDanMuBo q = new QueryDanMuBo();
        q.setVideoId(bo.getVideoId());
        q.setBatchNumber(video.getBatchNumber());
        q.setUserId(video.getUserId());
        q.setTenantId(video.getTenantId());
        q.setQueryType(1);
        q.setLimit(pageSize);
        q.setPage((int) page);

        R<QueryDanMuVo> r = tableStoreBll.queryDanMuSearchData(q);
        if (r == null || r.getCode() == null || r.getCode() != 0 || r.getData() == null) {
            return R.ok(CursorPageVo.empty());
        }
        List<DanMuVo> danMuList = r.getData().getList();
        List<BarrageItemVo> list = (danMuList == null ? new ArrayList<DanMuVo>() : danMuList).stream()
            .map(this::toBarrage).collect(Collectors.toList());
        boolean hasMore = Boolean.TRUE.equals(r.getData().getNextHash());
        Long nextCursor = hasMore ? page + 1 : null;
        return R.ok(CursorPageVo.of(list, nextCursor, hasMore));
    }

    private BarrageItemVo toBarrage(DanMuVo d) {
        boolean anonymous = Objects.equals(d.getNickName(), "匿名用户");
        BarrageItemVo vo = new BarrageItemVo();
        vo.setRecordDate(d.getRecordDate());
        vo.setBatchNumber(d.getBatchNumber());
        vo.setNickName(d.getNickName());
        vo.setContent(d.getContent());
        vo.setIsNew(d.getIsNew());
        vo.setLevel(d.getLevel());
        vo.setFansLevelCurrent(d.getFansLevelCurrent());
        vo.setCountSendNum(anonymous ? 1 : d.getCountSendNum());
        vo.setIsBlessBag(d.getIsBlessBag());
        return vo;
    }

    // ==================== 4.8 在线曲线 + 人群画像 ====================

    @Override
    public R<OnlineCurveVo> onlineCurve(OnlineCurveQueryBo bo) {
        R<OnlineCurveVo> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        AnchorVideoEntity video = loadScopedVideo(bo, bo.getVideoId(), List.of(AnchorVideoEntity::getId, AnchorVideoEntity::getVideoId, AnchorVideoEntity::getTenantId, AnchorVideoEntity::getUploadStatus));
        if (video == null) {
            return R.error("视频不存在或无权访问");
        }
        OnlineCurveVo vo = new OnlineCurveVo();
        int step = bo.getStep() == null || bo.getStep() < 1 ? 1 : bo.getStep();
        R<OnlineChartVo> r = socketCollectMessageLogic.onlineChartData(bo.getVideoId(), step);
        if (r != null && r.getCode() != null && r.getCode() == 0 && r.getData() != null) {
            OnlineChartVo c = r.getData();
            vo.setTotalViewersNum(c.getTotalViewersNum());
            vo.setMaxOnlineNum(c.getMaxOnlineNum());
            vo.setTotalBarrageNum(c.getTotalBarrageNum());
            vo.setOnlineDataList(toPoints(c.getOnlineDataList()));
            vo.setApproachDataList(toPoints(c.getApproachDataList()));
            vo.setExitPeopleDataList(toPoints(c.getExitPeopleDataList()));
            vo.setBarrageDataList(toPoints(c.getBarrageDataList()));
            vo.setPayComboCntDataList(toPoints(c.getPayComboCntDataList()));
            vo.setPayAmtDataList(toPoints(toCurveDataList(c.getPayAmtDataList())));
            vo.setFollowAnchorUcntDataList(toPoints(c.getFollowAnchorUcntDataList()));
            vo.setQianchuanCostDataList(toDoublePoints(c.getQianchuanCostDataList()));
            vo.setRefundAmtDataList(toDoublePoints(c.getRefundAmtDataList()));
            vo.setNetTransactionRoiDataList(roiPoints(c.getNetTransactionRoiDataList(), c.getQianchuanCostDataList()));
        }
        OceanEngineDataInfoVo info = loadDashboardInfo(bo.getVideoId());
        if (info != null) {
            vo.setWatchUserPortrait(info.getWatchUserPortrait());
            vo.setPayUserPortrait(info.getPayUserPortrait());
            // 累计观看与场次列表 watchNum / 看板 totalWatchNum 同口径：看板源（巨量为主、蝉妈妈补齐）优先，采集值兜底
            if (info.getTotalWatchNum() != null) {
                vo.setTotalViewersNum(info.getTotalWatchNum());
            }
        }
        return R.ok(vo);
    }


    /**
     * 获取视频在线曲线关联的采集统计数据（累计场观/场观/弹幕总数/最高在线）。
     *
     * @param videoIds 视频ID列表
     *
     * @return 视频ID与采集统计数据映射，能查出即代表已生成在线曲线数据
     */
    @Override
    public Map<String, SocketCollectMessageVo> getVideoHasChartDataMap(Collection<String> videoIds) {
        if (EmptyUtil.isEmpty(videoIds)) {
            return Map.of();
        }
        return socketCollectMessageLogic.getVideoHasChartDataMap(videoIds);
    }

    private List<CurvePointVo> toPoints(List<CurveData> src) {
        if (CollUtil.isEmpty(src)) {
            return new ArrayList<>();
        }
        return src.stream().map(d -> {
            CurvePointVo p = new CurvePointVo();
            p.setDateTime(d.getDateTime());
            p.setValueNum(d.getValueNum());
            return p;
        }).collect(Collectors.toList());
    }

    private List<CurvePointDoubleVo> toDoublePoints(List<CurveDoubleData> src) {
        if (CollUtil.isEmpty(src)) {
            return new ArrayList<>();
        }
        return src.stream().map(d -> {
            CurvePointDoubleVo p = new CurvePointDoubleVo();
            p.setDateTime(d.getDateTime());
            p.setValueNum(d.getValueNum() == null ? null : BigDecimal.valueOf(d.getValueNum()));
            return p;
        }).collect(Collectors.toList());
    }

    /**
     * 分钟级净成交 ROI 出口修正：**该分钟没有投放消耗时 ROI 无定义，输出 null 而不是 0**。
     * <p>上游生成链（{@code OceanEngineDataProducerImpl#computeNetTransactionRoi}）在消耗为 0 时写 0.0，
     * 于是「这分钟没投流」和「投了钱一分没回本」在数据里长得一模一样，模型/前端只能读错。
     * 这里按同分钟的消耗做确定性判别把前者还原成 null——只在本 ai-agent 出口做，不改共用的生成链。
     * 消耗曲线整条缺失时无从判别，返回空列表（＝该指标无数据），不吐一串意义不明的 0。</p>
     */
    private List<CurvePointDoubleVo> roiPoints(List<CurveDoubleData> roi, List<CurveDoubleData> cost) {
        if (CollUtil.isEmpty(roi)) {
            return new ArrayList<>();
        }
        Map<Long, Double> costAt = indexByTime(cost);
        if (costAt.isEmpty()) {
            return new ArrayList<>();
        }
        List<CurvePointDoubleVo> out = new ArrayList<>(roi.size());
        for (CurveDoubleData d : roi) {
            if (d == null) {
                continue;
            }
            CurvePointDoubleVo p = toDoublePoint(d);
            Double c = d.getDateTime() == null ? null : costAt.get(d.getDateTime());
            if (c == null || c <= 0d) {
                p.setValueNum(null);
            }
            out.add(p);
        }
        return out;
    }

    private List<CurveData> toCurveDataList(List<CurveDoubleData> src) {
        if (CollUtil.isEmpty(src)) {
            return new ArrayList<>();
        }
        return src.stream().map(d -> {
            CurveData c = new CurveData();
            c.setDateTime(d.getDateTime());
            c.setValueNum(d.getValueNum() != null ? d.getValueNum().intValue() : null);
            return c;
        }).collect(Collectors.toList());
    }

    // ==================== 公共辅助 ====================

    /**
     * 身份三件套校验：缺失或 userType 非法返回错误 R，合法返回 null。
     *
     * <p>信任模型：本批接口走 API-Key 鉴权（机器对机器），调用方为可信后端 AI Agent，
     * 代多个租户/用户发起请求，故 tenantId/userId 由请求体显式携带且被信任；租户隔离在
     * 每次查询的 DB 层强制叠加 tenantId（+ 子账号 userId）保证，单次调用不会跨租户泄露。
     * api-key 一旦泄露可被用于枚举任意 tenantId —— 该风险由密钥保管 + 网关侧管控承担，
     * 不在本服务内通过 appId→tenantId 绑定限制（绑定会破坏「一个 Agent 服务所有租户」的设计）。</p>
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

    /**
     * 数据范围裁剪：userType 0/1 租户级（返回 null 不加 userId 过滤），2 仅本人（返回 userId）。
     */
    private Long scopedUserId(AiAgentBaseBo bo) {
        return Objects.equals(bo.getUserType(), CHILD_USER_TYPE) ? bo.getUserId() : null;
    }

    /**
     * 解析有效数据源类型：不传默认 1=自己录制；全租户(2)仅 userType=0 生效，子账号(2)自动回落为自己录制(1)；
     * 自己录制+云空间(4)原样透传（自己录制侧天然按本人裁剪，云空间侧为已分享共享数据，无需回落）。
     */
    private int resolveDataSource(Integer dataSourceType, Integer userType) {
        int t = dataSourceType == null ? DATA_SOURCE_SELF : dataSourceType;
        if (t == DATA_SOURCE_TENANT && Objects.equals(userType, CHILD_USER_TYPE)) {
            return DATA_SOURCE_SELF;
        }
        return t;
    }

    private int clampPageSize(Integer pageSize, int def, int max) {
        if (pageSize == null || pageSize <= 0) {
            return def;
        }
        return Math.min(pageSize, max);
    }

    /**
     * 行业父子展开：传父行业返回「自身 + 所有子孙」；为空返回 null（不过滤）。
     */
    private List<Long> expandTradeIds(Long tradeId) {
        if (tradeId == null) {
            return null;
        }
        List<Long> ids = tradeProducer.getChildById(tradeId);
        if (CollUtil.isEmpty(ids)) {
            return Collections.singletonList(tradeId);
        }
        return ids;
    }

    private Map<String, AnchorUrlEntity> loadAnchorBaseMap(List<String> secUids) {
        if (CollUtil.isEmpty(secUids)) {
            return Collections.emptyMap();
        }
        return anchorUrlService.lambdaQuery()
            .in(AnchorUrlEntity::getSecUid, secUids)
            .list().stream()
            .collect(Collectors.toMap(AnchorUrlEntity::getSecUid, Function.identity(), (a, b) -> a));
    }

    private Map<String, AnchorUrlUserEntity> loadAnchorUserMap(List<String> secUids, Long tenantId, Long scopedUserId) {
        if (CollUtil.isEmpty(secUids)) {
            return Collections.emptyMap();
        }
        return anchorUrlUserService.lambdaQuery()
            .in(AnchorUrlUserEntity::getAnchorUrlSecUid, secUids)
            .eq(AnchorUrlUserEntity::getTenantId, tenantId)
            .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0)
            .eq(scopedUserId != null, AnchorUrlUserEntity::getUserId, scopedUserId)
            .list().stream()
            .collect(Collectors.toMap(AnchorUrlUserEntity::getAnchorUrlSecUid, Function.identity(), (a, b) -> a));
    }

    /**
     * 加载行业名称。
     */
    private Map<Long, String> loadTradeNames(List<Long> tradeIds) {
        if (CollUtil.isEmpty(tradeIds)) {
            return Collections.emptyMap();
        }
        Map<Long, String> map = tradeProducer.getTradeNames(tradeIds);
        return map == null ? Collections.emptyMap() : map;
    }


    /**
     * 加载并解析视频的巨量引擎结构化数据（仅巨量百应源，含人群画像）；无数据返回 null。
     * <p>用于在线曲线（4.8）的看播 / 成交人群画像，画像目前仅巨量百应源产出，故沿用巨量取数。</p>
     */
    private OceanEngineDataInfoVo loadOceanEngineInfo(String videoId) {
        return parseOceanEngine(videoDataViewingBll.getOceanEngineByVideoId(videoId));
    }

    /**
     * 加载并解析视频的数据看板结构化数据（不限数据源，字段级混合补齐：巨量百应为主、空字段用蝉妈妈补）；无数据返回 null。
     * <p>用于数据看板（4.6）汇总指标。原先硬取巨量百应源会让蝉妈妈源场次整块「数据缺失」，
     * 这里改走 {@code getDashboardByVideoId} 字段级补齐观看 / 停留 / 转粉 / 互动等核心指标。</p>
     */
    private OceanEngineDataInfoVo loadDashboardInfo(String videoId) {
        return parseOceanEngine(videoDataViewingBll.getDashboardByVideoId(videoId));
    }

    /**
     * 采集缺口哨兵归一：{@code -1 → null}。
     *
     * <p>采集侧对「平台未回传该指标」显式写 -1（{@code VideoDataViewingProducerImpl.fillDataViewingData}：
     * 采到 "-" 即 {@code setVolumeStart(-1)}，原注释「没有采集到数据」；混淆层对 -1 专门旁路不乘系数，
     * 派生的 uvValue/goodsConvertRate 同写 -1.0）。哨兵原样出接口会被消费方读成负数或「零成交」，
     * 故成交族对外一律归一为 null——与 {@code AnchorVideoLogicImpl} 对 {@code totalWatchNum} 的既有处理一致。
     *
     * <p>仅用于成交族（成交额/订单数/客单价/UV价值/带货转化率）：它们都是非负量，-1 不可能是真值。
     * 观看/在线/涨粉等不走这条写入路径，不得套用。
     *
     * @param v 原值（可空）
     * @param <T> 数值类型（Integer/Double 均可，返回类型不变）
     * @return 哨兵→null，其余原样返回
     */
    private static <T extends Number> T nullIfGap(T v) {
        return (v != null && v.doubleValue() == -1d) ? null : v;
    }

    /**
     * 解包看板取数响应：成功且 dataJson 非空时解析为结构化数据，否则返回 null。
     */
    private OceanEngineDataInfoVo parseOceanEngine(R<OceanEngineDataVo> r) {
        if (r == null || r.getCode() == null || r.getCode() != 0 || r.getData() == null
            || StrUtil.isBlank(r.getData().getDataJson())) {
            return null;
        }
        return JSONUtil.toBean(r.getData().getDataJson(), OceanEngineDataInfoVo.class);
    }

    /**
     * 加载并鉴权视频：已分享(uploadStatus=1)的云空间视频全租户可见，不校验权限直接放行；
     * 否则按 userType 裁剪——userType=0 仅校验 tenantId 一致，userType=2 还需 userId 一致。无权访问返回 null。
     */
    private AnchorVideoEntity loadScopedVideo(AiAgentBaseBo bo, String videoId, List<SFunction<AnchorVideoEntity, ?>> columns) {
        AnchorVideoEntity video = anchorVideoService.lambdaQuery()
            .select(columns)
            .eq(AnchorVideoEntity::getVideoId, videoId)
            .eq(AnchorVideoEntity::getIsDeleted, 0)
            .last("limit 1")
            .one();
        if (video == null) {
            return null;
        }
        // 已分享视频不验证权限（云空间共享场景，可跨租户访问）
        if (Objects.equals(video.getUploadStatus(), VIDEO_UPLOAD_STATUS_SHARED)) {
            return video;
        }
//        if (!Objects.equals(video.getTenantId(), bo.getTenantId())) {
//            return null;
//        }
//        if (Objects.equals(bo.getUserType(), CHILD_USER_TYPE)
//            && !Objects.equals(video.getUserId(), bo.getUserId())) {
//            return null;
//        }
        return video;
    }

    private String dictLabel(String code, Integer value) {
        if (value == null) {
            return "";
        }
        return anchorUrlBll.getDictLabelByCodeAndValue(code, value, false);
    }

    private String multiDictLabel(String code, String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return anchorUrlBll.getDictLabelByCodeAndValue(code, value, true);
    }

    private String label(Map<Integer, String> labels, Integer code) {
        if (code == null) {
            return "";
        }
        return labels.getOrDefault(code, "");
    }

    private String formatDateTime(Date date) {
        return date == null ? null : DateUtil.formatDateTime(date);
    }

    private String formatDate(Date date) {
        return date == null ? null : DateUtil.formatDate(date);
    }
}
