package com.jiuyu.replay.ai.bll;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.ai.bo.BatchReportStatusBo;
import com.jiuyu.replay.ai.bo.ReportStatusBo;
import com.jiuyu.replay.ai.bo.ScriptMonitorTriggerMsg;
import com.jiuyu.replay.ai.bo.TriggerReportBo;
import com.jiuyu.replay.ai.config.ScriptMonitorMqProperties;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorReportBodyRepository;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.ai.vo.FidelityReportDetailVo;
import com.jiuyu.replay.ai.vo.InteractionPatrolReportDetailVo;
import com.jiuyu.replay.ai.vo.MonitorTypeStatusVo;
import com.jiuyu.replay.ai.vo.QualityReportDetailVo;
import com.jiuyu.replay.ai.vo.ScriptMonitorReportStatusVo;
import com.jiuyu.replay.common.bll.RocketMqBll;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.enums.words.MonitorSceneTypeEnum;
import com.jiuyu.replay.generic.enums.words.MonitorSourceTypeEnum;
import com.jiuyu.replay.generic.enums.words.MonitorTypeEnum;
import com.jiuyu.replay.generic.enums.words.ScriptMonitorStatusEnum;
import com.jiuyu.replay.generic.feign.order.AiTokenWithholdFeign;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.words.AnchorUrlUserFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoFeign;
import com.jiuyu.replay.generic.feign.words.BasicSettingsFeign;
import com.jiuyu.replay.generic.feign.words.SensitiveWordsFeign;
import com.jiuyu.replay.generic.feign.words.StandardScriptFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.RedisWithholdVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.BasicSettingsVo;
import com.jiuyu.replay.generic.vo.words.StandardScriptInfoVo;
import com.jiuyu.replay.generic.vo.words.UploadFileSimpleInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI监控报告状态查询、手动触发编排
 *
 * <p>B9 改造（2026-06-08）：删除 confirmRead 端点 + 两张子表（tb_script_monitor_read /
 * tb_script_monitor_role_confirm），已读语义内置到 detail 接口：
 * 录制人首次打开 qualityReportDetail / patrolReportDetail 时，自动写已读（独立事务 + fail-safe）。</p>
 *
 * @author beta
 * @date 2026-05-29
 */
@Slf4j
@Component
public class ScriptMonitorBll {

    /**
     * 批量查询上限
     */
    private static final int BATCH_LIMIT = 100;
    /**
     * AI Token 资产 code
     */
    private static final String AI_TOKEN_CODE = "aiTokenNum";
    /**
     * 手动触发 Token 余额门槛（预扣量）
     */
    private static final long AI_TOKEN_PREHOLD = 100000L;

    private final ScriptMonitorReportService reportService;
    private final ScriptMonitorReportWriteService writeService;
    private final ScriptMonitorReportBodyRepository reportBodyRepository;
    private final AnchorUrlUserFeign anchorUrlUserFeign;
    private final AnchorVideoFeign anchorVideoFeign;
    private final SensitiveWordsFeign sensitiveWordsFeign;
    private final UserFeign userFeign;
    private final UserPropertyFeign userPropertyFeign;
    private final AiTokenWithholdFeign aiTokenWithholdFeign;
    private final RocketMqBll rocketMqBll;
    private final ScriptMonitorMqProperties scriptMonitorMqProperties;
    /**
     * 标准稿跨模块查询 SPI（Slice B）：triggerReport monitorType=1 时按 secUid 校验有效标准稿。
     */
    private final StandardScriptFeign standardScriptFeign;
    /**
     * 基础设置跨模块查询 SPI：anchorBasicConfig 合并 tb_basic_settings 中的 BaseDto 12 字段（含 livingMode）。
     */
    private final BasicSettingsFeign basicSettingsFeign;

    /**
     * 构造器注入。
     *
     * <p>B4 修补包-3（2026-06-01）：移除直注入 {@code ScriptMonitorGenerateBll} 与
     * {@code ThreadPoolTaskExecutor}，改为通过 RocketMQ 异步触发；generate 由
     * {@link com.jiuyu.replay.ai.bll.ScriptMonitorMqHandler} 在 consumer 侧调用。</p>
     *
     * <p>B5 返工（2026-06-02）：加入 {@code AnchorUrlUserFeign}，供 anchorBasicConfig 走 SPI 取配置。</p>
     *
     * <p>B9（2026-06-08）：移除 {@code readService}（ScriptMonitorReadService）与
     * {@code roleConfirmService}（ScriptMonitorRoleConfirmService），对应两张子表已删除；
     * 加入 {@code writeService}（ScriptMonitorReportWriteService）供 detail 接口写已读使用。</p>
     *
     * @param reportService             报告 Service
     * @param writeService              报告状态写入 Service（含 markReadIfNeeded 独立事务）
     * @param reportBodyRepository      报告正文 MongoDB Repository（B4-3）
     * @param anchorUrlUserFeign        直播间用户配置 SPI（B5）
     * @param anchorVideoFeign          视频 Feign
     * @param sensitiveWordsFeign       上传文件 Feign（B4-4）
     * @param userFeign                 用户 Feign
     * @param userPropertyFeign         资产 Feign
     * @param aiTokenWithholdFeign      Token 预扣 SPI（B4-0）
     * @param rocketMqBll               本地消息表通用入口（B4 修补包-3 MQ 接入）
     * @param scriptMonitorMqProperties 独立 RocketMQ 配置（B4 修补包-3）
     * @param standardScriptFeign       标准稿跨模块查询 SPI（Slice B）
     * @param basicSettingsFeign        基础设置跨模块查询 SPI（anchorBasicConfig 合并 BaseDto 12 字段）
     */
    public ScriptMonitorBll(ScriptMonitorReportService reportService,
                            ScriptMonitorReportWriteService writeService,
                            ScriptMonitorReportBodyRepository reportBodyRepository,
                            AnchorUrlUserFeign anchorUrlUserFeign,
                            AnchorVideoFeign anchorVideoFeign,
                            SensitiveWordsFeign sensitiveWordsFeign,
                            UserFeign userFeign,
                            UserPropertyFeign userPropertyFeign,
                            AiTokenWithholdFeign aiTokenWithholdFeign,
                            RocketMqBll rocketMqBll,
                            ScriptMonitorMqProperties scriptMonitorMqProperties,
                            StandardScriptFeign standardScriptFeign,
                            BasicSettingsFeign basicSettingsFeign) {
        this.reportService = reportService;
        this.writeService = writeService;
        this.reportBodyRepository = reportBodyRepository;
        this.anchorUrlUserFeign = anchorUrlUserFeign;
        this.anchorVideoFeign = anchorVideoFeign;
        this.sensitiveWordsFeign = sensitiveWordsFeign;
        this.userFeign = userFeign;
        this.userPropertyFeign = userPropertyFeign;
        this.aiTokenWithholdFeign = aiTokenWithholdFeign;
        this.rocketMqBll = rocketMqBll;
        this.scriptMonitorMqProperties = scriptMonitorMqProperties;
        this.standardScriptFeign = standardScriptFeign;
        this.basicSettingsFeign = basicSettingsFeign;
    }

    /**
     * 查询单个资源的三类监控报告状态。
     *
     * <p>当 {@code bo.secUid} 非空时，按 (userId, tenantId, secUid) 查
     * tb_anchor_url_user 三开关，装配到响应 monitors[].monitorEnabled；
     * 未传 / 查无记录 → 该字段为 null。</p>
     *
     * @param bo 报告状态查询请求
     * @return 报告状态（含 monitorEnabled）
     */
    public ScriptMonitorReportStatusVo reportStatus(ReportStatusBo bo) {
        if (bo == null || bo.getSourceType() == null || bo.getSceneType() == null || bo.getSourceId() == null) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), "资源参数不完整");
        }
        UserCacheVo user = currentUser();
        AnchorVideoInfoVo video = loadSingleVideo(bo.getSourceType(), bo.getSourceId());
        // 区分「资源不存在/不支持」与「无读权限」两种语义
        if (video == null) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), "资源不存在或暂不支持该资源类型");
        }
        if (!hasReadPermission(user, video)) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), "无数据读取权限");
        }
        Long tenantId = user.getActiveTenantId();
        Map<String, Map<Integer, ScriptMonitorReportEntity>> reportMap =
                loadReportMap(tenantId, Collections.singletonList(bo.getSourceId()));
        Map<String, AnchorUrlUserVo> enabledMap = loadEnabledMap(
                bo.getSecUid() == null ? Collections.emptySet() : Collections.singleton(bo.getSecUid()),
                user.getId(), tenantId);
        return assemble(bo.getSourceType(), bo.getSceneType(), bo.getSourceId(),
                bo.getSecUid(), reportMap, enabledMap);
    }

    /**
     * 批量查询报告状态，跳过无权限/不存在的资源。批量加载资源+报告，避免 N+1
     *
     * @param bo 批量查询请求
     * @return 报告状态列表
     */
    public List<ScriptMonitorReportStatusVo> batchReportStatus(BatchReportStatusBo bo) {
        if (bo == null || CollUtil.isEmpty(bo.getSources())) {
            return new ArrayList<>();
        }
        List<BatchReportStatusBo.SourceItemBo> sources = bo.getSources();
        if (sources.size() > BATCH_LIMIT) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "批量查询最多 " + BATCH_LIMIT + " 条");
        }
        UserCacheVo user = currentUser();
        Long tenantId = user.getActiveTenantId();

        // 1. 按 sourceType 分组批量加载资源
        // 1a. sourceType=0 录制视频：批量走 AnchorVideoFeign
        List<String> videoIds = sources.stream()
                .filter(s -> Objects.equals(s.getSourceType(), MonitorSourceTypeEnum.RECORD_VIDEO.getCode()))
                .map(BatchReportStatusBo.SourceItemBo::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, AnchorVideoInfoVo> videoMap = batchLoadVideos(videoIds);

        // 1b. sourceType=1 上传文件：单查 SensitiveWordsFeign（上限 BATCH_LIMIT 可接受循环）
        List<String> fileIds = sources.stream()
                .filter(s -> Objects.equals(s.getSourceType(), MonitorSourceTypeEnum.UPLOAD_FILE.getCode()))
                .map(BatchReportStatusBo.SourceItemBo::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, AnchorVideoInfoVo> uploadFileMap = batchLoadUploadFiles(fileIds);

        // 1c. 合并两类资源图
        Map<String, AnchorVideoInfoVo> permissionMap = new HashMap<>(videoMap);
        permissionMap.putAll(uploadFileMap);

        // 2. 过滤有读权限的资源（无权限/不存在跳过，不返回）
        List<BatchReportStatusBo.SourceItemBo> permitted = sources.stream()
                .filter(s -> hasReadPermission(user, permissionMap.get(s.getSourceId())))
                .collect(Collectors.toList());
        if (permitted.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 批量查报告
        List<String> permittedSourceIds = permitted.stream()
                .map(BatchReportStatusBo.SourceItemBo::getSourceId).distinct().collect(Collectors.toList());
        Map<String, Map<Integer, ScriptMonitorReportEntity>> reportMap = loadReportMap(tenantId, permittedSourceIds);

        // 4. 批量按 secUid 集合查三开关（用于 monitorEnabled 装配；未传 secUid 的 source 不参与查询）
        Set<String> secUids = permitted.stream()
                .map(BatchReportStatusBo.SourceItemBo::getSecUid)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        Map<String, AnchorUrlUserVo> enabledMap = loadEnabledMap(secUids, user.getId(), tenantId);

        // 5. 内存装配
        return permitted.stream()
                .map(s -> assemble(s.getSourceType(), s.getSceneType(), s.getSourceId(),
                        s.getSecUid(), reportMap, enabledMap))
                .collect(Collectors.toList());
    }

    /**
     * 批量按 secUid 集合查 tb_anchor_url_user 三开关，构造 secUid → vo 索引。
     *
     * <p>用于 reportStatus / batchReportStatus 装配 monitorEnabled 字段。空集合直接返空 map。</p>
     *
     * @param secUids  secUid 集合（已去重去空）
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return secUid → AnchorUrlUserVo 映射（查无记录的 secUid 不在 map 中）
     */
    private Map<String, AnchorUrlUserVo> loadEnabledMap(Set<String> secUids, Long userId, Long tenantId) {
        if (CollUtil.isEmpty(secUids)) {
            return Collections.emptyMap();
        }
        R<List<AnchorUrlUserVo>> r = anchorUrlUserFeign.listBySecUidsAndUser(new ArrayList<>(secUids), userId, tenantId);
        List<AnchorUrlUserVo> vos = (r != null && r.success() && r.getData() != null) ? r.getData() : Collections.emptyList();
        if (CollUtil.isEmpty(vos)) {
            return Collections.emptyMap();
        }
        return vos.stream()
                .filter(v -> v.getAnchorUrlSecUid() != null)
                .collect(Collectors.toMap(AnchorUrlUserVo::getAnchorUrlSecUid, Function.identity(), (a, b) -> a));
    }

    /**
     * 查询质检报告详情：包含报告摘要、MongoDB 正文、主播信息、已读状态。
     *
     * <p>B9 改造（2026-06-08）：录制人首次查看时自动写已读（独立事务 + fail-safe），
     * 删除角色已知晓（confirmedRecords）+ canConfirm 字段；isRead 直接取 report.isRead。</p>
     *
     * <p>权限规则：同租户可读；跨租户仅当对应视频 uploadStatus=1（已上传云空间）时可读，
     * 否则抛 70011。sourceType=1 上传文件场景 video.uploadStatus 恒为 null（不参与云空间分享）。</p>
     *
     * @param reportId 报告ID
     * @return 质检报告详情
     */
    public QualityReportDetailVo qualityReportDetail(Long reportId) {
        if (reportId == null) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), "reportId 不能为空");
        }
        UserCacheVo user = currentUser();
        ScriptMonitorReportEntity report = reportService.getById(reportId);
        if (report == null || Objects.equals(report.getIsDeleted(), 1)) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_REPORT_NOT_EXIST);
        }
        // 加载视频信息前移到权限校验之前（用于判断是否云空间公开分享）
        AnchorVideoInfoVo video = loadSingleVideo(report.getSourceType(), report.getSourceId());
        // 读权限：同租户 OR 视频已上传云空间（uploadStatus=1，公开分享）
        boolean sameTenant = Objects.equals(report.getTenantId(), user.getActiveTenantId());
        boolean publicShared = video != null && Objects.equals(video.getUploadStatus(), 1);
        if (!sameTenant && !publicShared) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), "无数据读取权限");
        }
        if (video == null) {
            log.warn("qualityReportDetail: video not found, reportId={}, sourceType={}, sourceId={}",
                    reportId, report.getSourceType(), report.getSourceId());
        }

        // 判断是否可确认（录制人本人）
        boolean canConfirm = hasConfirmPermission(user, video);

        // B9：录制人首次查看时写已读（独立事务 + fail-safe，写失败不影响详情返回）
        if (canConfirm && Objects.equals(report.getIsRead(), 0)) {
            try {
                writeService.markReadIfNeeded(reportId, user.getActiveTenantId());
                // 写成功后更新内存，使响应 isRead=1 而非 stale 0
                report.setIsRead(1);
                report.setConfirmedAt(new Date());
            } catch (Exception e) {
                // TODO(B9): inject micrometer Counter once registered
                log.warn("[B9] markReadIfNeeded 写已读失败 reportId={}", reportId, e);
            }
        }

        // 查 MongoDB 报告正文
        String reportContent = reportBodyRepository.findByReportId(reportId)
                .map(body -> body.getContent())
                .orElse(null);

        // 装配 VO
        QualityReportDetailVo vo = new QualityReportDetailVo();
        vo.setReportId(report.getId());
        vo.setSourceType(report.getSourceType());
        vo.setSceneType(report.getSceneType());
        vo.setSourceId(report.getSourceId());
        vo.setStatus(report.getStatus());
        vo.setSummaryJson(report.getSummaryJson());
        vo.setReportContent(reportContent);
        vo.setIsRead(report.getIsRead() != null ? report.getIsRead() : 0);
        // confirmedAt 直接取 report.confirmedAt（首次查看后为 NOW()；is_read=0 时为 null）
        vo.setConfirmedAt(report.getConfirmedAt());
        vo.setCreateDate(report.getCreateDate());
        // 主播/直播信息：按 sourceType 分支映射
        if (Objects.equals(report.getSourceType(), MonitorSourceTypeEnum.UPLOAD_FILE.getCode())) {
            // sourceType=1 上传文件：用文件名占位 anchorName/liveTitle，uploadTime 映射 liveTime
            fillAnchorFieldsFromUploadFile(vo, report.getSourceId());
        } else if (video != null) {
            // sourceType=0 录制视频
            vo.setLiveTitle(video.getLiveTitle());
            vo.setLiveTime(video.getStartTime());
            // anchorName 优先取 anchorInfo.anchorName，无则取 userNickName
            if (video.getAnchorInfo() != null && video.getAnchorInfo().getAnchorName() != null) {
                vo.setAnchorName(video.getAnchorInfo().getAnchorName());
            } else {
                vo.setAnchorName(video.getUserNickName());
            }
        }
        return vo;
    }

    /**
     * 查询互动巡检报告详情。
     *
     * <p>B9 改造（2026-06-08）：录制人首次查看时自动写已读（独立事务 + fail-safe）；
     * confirmedAt 直接取 report.confirmedAt；新增 isRead 字段返回；删除 canConfirm 字段。</p>
     *
     * <p>权限规则：同租户可读；跨租户仅当对应视频 uploadStatus=1（已上传云空间）时可读，
     * 否则抛 70011。录制人本人触发写已读，非录制人（含跨租户分享接收方）查看不写。</p>
     *
     * @param reportId 报告 ID
     * @return 互动巡检报告详情
     */
    public InteractionPatrolReportDetailVo patrolReportDetail(Long reportId) {
        if (reportId == null) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), "reportId 不能为空");
        }
        UserCacheVo user = currentUser();
        ScriptMonitorReportEntity report = reportService.getById(reportId);
        if (report == null || Objects.equals(report.getIsDeleted(), 1)) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_REPORT_NOT_EXIST);
        }
        // 加载视频信息前移到权限校验之前（用于判断是否云空间公开分享）
        AnchorVideoInfoVo video = loadSingleVideo(report.getSourceType(), report.getSourceId());
        // 读权限：同租户 OR 视频已上传云空间（uploadStatus=1，公开分享）
        boolean sameTenant = Objects.equals(report.getTenantId(), user.getActiveTenantId());
        boolean publicShared = video != null && Objects.equals(video.getUploadStatus(), 1);
        if (!sameTenant && !publicShared) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), "无数据读取权限");
        }
        if (video == null) {
            log.warn("patrolReportDetail: video not found, reportId={}, sourceType={}, sourceId={}",
                    reportId, report.getSourceType(), report.getSourceId());
        }
        // canConfirm 仅录制人
        boolean canConfirm = hasConfirmPermission(user, video);

        // B9：录制人首次查看时写已读（独立事务 + fail-safe，写失败不影响详情返回）
        if (canConfirm && Objects.equals(report.getIsRead(), 0)) {
            try {
                writeService.markReadIfNeeded(reportId, user.getActiveTenantId());
                // 写成功后更新内存，使响应 isRead=1, confirmedAt=now 而非 stale 0/null
                report.setIsRead(1);
                report.setConfirmedAt(new Date());
            } catch (Exception e) {
                // TODO(B9): inject micrometer Counter once registered
                log.warn("[B9] markReadIfNeeded 写已读失败 reportId={}", reportId, e);
            }
        }

        // 查 MongoDB 报告正文
        String reportContent = reportBodyRepository.findByReportId(reportId)
                .map(body -> body.getContent())
                .orElse(null);
        // 装配 VO
        InteractionPatrolReportDetailVo vo = new InteractionPatrolReportDetailVo();
        vo.setReportId(report.getId());
        vo.setSourceType(report.getSourceType());
        vo.setSceneType(report.getSceneType());
        vo.setSourceId(report.getSourceId());
        vo.setStatus(report.getStatus());
        vo.setSummaryJson(report.getSummaryJson());
        vo.setReportContent(reportContent);
        // confirmedAt 直接取 report.confirmedAt（首次查看后为 NOW()；is_read=0 时为 null）
        vo.setConfirmedAt(report.getConfirmedAt());
        vo.setIsRead(report.getIsRead() != null ? report.getIsRead() : 0);
        vo.setCreateDate(report.getCreateDate());
        // 主播/直播信息（sourceType=0 录制视频）
        if (video != null) {
            vo.setLiveTitle(video.getLiveTitle());
            vo.setLiveTime(video.getStartTime());
            if (video.getAnchorInfo() != null && video.getAnchorInfo().getAnchorName() != null) {
                vo.setAnchorName(video.getAnchorInfo().getAnchorName());
            } else {
                vo.setAnchorName(video.getUserNickName());
            }
        }
        return vo;
    }

    /**
     * 查询话术还原度报告详情：包含报告摘要、MongoDB 正文、主播信息、已读状态。
     *
     * <p>Slice C（2026-06-13）：1:1 mirror {@link #qualityReportDetail(Long)}，还原度专属接口。
     * 录制人首次查看时自动写已读（独立事务 + fail-safe，写失败不影响详情返回）。</p>
     *
     * <p>权限规则：同租户可读；跨租户仅当对应视频 uploadStatus=1（已上传云空间）时可读，
     * 否则抛 70011。sourceType=1 异常数据不参与云空间分享（video 恒为 null）。</p>
     *
     * <p>sourceType 固定 0（录制视频）；如出现 sourceType=1 的还原度异常数据，log.warn 兜底不抛。</p>
     *
     * @param reportId 报告 ID
     * @return 话术还原度报告详情
     */
    public FidelityReportDetailVo fidelityReportDetail(Long reportId) {
        if (reportId == null) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), "reportId 不能为空");
        }
        UserCacheVo user = currentUser();
        ScriptMonitorReportEntity report = reportService.getById(reportId);
        if (report == null || Objects.equals(report.getIsDeleted(), 1)) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_REPORT_NOT_EXIST);
        }
        // 加载视频信息前移到权限校验之前（还原度 sourceType 固定 0；sourceType=1 为异常数据，log.warn 兜底）
        AnchorVideoInfoVo video = null;
        if (Objects.equals(report.getSourceType(), MonitorSourceTypeEnum.UPLOAD_FILE.getCode())) {
            log.warn("fidelityReportDetail: unexpected sourceType=1 (upload file) for fidelity report, reportId={}",
                    reportId);
        } else {
            video = loadSingleVideo(report.getSourceType(), report.getSourceId());
            if (video == null) {
                log.warn("fidelityReportDetail: video not found, reportId={}, sourceType={}, sourceId={}",
                        reportId, report.getSourceType(), report.getSourceId());
            }
        }
        // 读权限：同租户 OR 视频已上传云空间（uploadStatus=1，公开分享）
        boolean sameTenant = Objects.equals(report.getTenantId(), user.getActiveTenantId());
        boolean publicShared = video != null && Objects.equals(video.getUploadStatus(), 1);
        if (!sameTenant && !publicShared) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), "无数据读取权限");
        }

        // 判断是否为录制人（可确认）
        boolean canConfirm = hasConfirmPermission(user, video);

        // Slice C：录制人首次查看时写已读（独立事务 + fail-safe，写失败不影响详情返回）
        if (canConfirm && Objects.equals(report.getIsRead(), 0)) {
            try {
                writeService.markReadIfNeeded(reportId, user.getActiveTenantId());
                // 写成功后更新内存，使响应 isRead=1 而非 stale 0
                report.setIsRead(1);
                report.setConfirmedAt(new Date());
            } catch (Exception e) {
                log.warn("[SliceC] markReadIfNeeded 写已读失败 reportId={}", reportId, e);
            }
        }

        // 查 MongoDB 报告正文
        String reportContent = reportBodyRepository.findByReportId(reportId)
                .map(body -> body.getContent())
                .orElse(null);

        // 装配 VO
        FidelityReportDetailVo vo = new FidelityReportDetailVo();
        vo.setReportId(report.getId());
        vo.setSourceType(report.getSourceType());
        vo.setSceneType(report.getSceneType());
        vo.setSourceId(report.getSourceId());
        vo.setStatus(report.getStatus());
        vo.setSummaryJson(report.getSummaryJson());
        vo.setReportContent(reportContent);
        vo.setIsRead(report.getIsRead() != null ? report.getIsRead() : 0);
        // confirmedAt 直接取 report.confirmedAt（首次查看后为 NOW()；is_read=0 时为 null）
        vo.setConfirmedAt(report.getConfirmedAt());
        vo.setCreateDate(report.getCreateDate());
        // 主播/直播信息：sourceType=0 录制视频
        if (video != null) {
            vo.setLiveTitle(video.getLiveTitle());
            vo.setLiveTime(video.getStartTime());
            if (video.getAnchorInfo() != null && video.getAnchorInfo().getAnchorName() != null) {
                vo.setAnchorName(video.getAnchorInfo().getAnchorName());
            } else {
                vo.setAnchorName(video.getUserNickName());
            }
        }
        return vo;
    }

    /**
     * 手动触发报告生成：前置校验通过后预扣 Token，投递异步生成任务。
     *
     * <p>同步立即返回，前端通过轮询 reportStatus 查看进度。</p>
     *
     * @param bo 触发参数
     */
    public void triggerReport(TriggerReportBo bo) {
        if (bo == null || bo.getSourceType() == null || bo.getSceneType() == null
                || bo.getSourceId() == null || bo.getMonitorType() == null) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), "触发参数不完整");
        }
        // HOTFIX 2026-06-06: 防御 sourceType-sceneType 非法组合（避免写入畸形 entity 导致后续查询失败）
        if (!MonitorSceneTypeEnum.isLegalCombination(bo.getSourceType(), bo.getSceneType())) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "sourceType=" + bo.getSourceType() + " 与 sceneType=" + bo.getSceneType() + " 组合非法");
        }
        // Slice C：还原度不支持上传文件场景（sourceType=1 + monitorType=1 拒绝）
        if (Objects.equals(bo.getMonitorType(), MonitorTypeEnum.FIDELITY_MONITOR.getCode())
                && Objects.equals(bo.getSourceType(), MonitorSourceTypeEnum.UPLOAD_FILE.getCode())) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_FEATURE_NOT_AVAILABLE.getCode(),
                    "还原度暂不支持上传文件场景");
        }
        UserCacheVo user = currentUser();
        Integer sourceType = bo.getSourceType();
        Integer monitorType = bo.getMonitorType();

        // 1. 资源归属校验（sourceType=0 校验 tenantId + userId 匹配）
        AnchorVideoInfoVo video = loadSingleVideo(sourceType, bo.getSourceId());
        if (video == null) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), "资源不存在或暂不支持该资源类型");
        }
        if (!hasConfirmPermission(user, video)) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), "无操作权限");
        }

        // 2. 自有账号校验
        // TODO(script-monitor:B3) 校验自有账号(accountType=0，竞品账号抛 70004)，需 words 暴露按 secUid/anchorUrlUserId 取 accountType 的 SPI（与 addOrUpdateAnchor 账号保护同源）

        // 3. 授权量校验
        // 手动触发场景按 PRD R-P0-003 不校验监控位授权量（仅校验套餐功能权限、自有账号和 Token 余额）
        // 监控位授权量校验由 addOrUpdateAnchor 开关开启时（B6）和自动触发（B7）负责

        // 4. Token 余额校验 ≥ 100000（在预扣前做快速余额判断，避免先创建任务再被 withholdAiToken 拒绝）
        if (aiTokenBalance(user.getId()) < AI_TOKEN_PREHOLD) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH);
        }

        // 5. 监控类型特定前置校验
        if (Objects.equals(monitorType, MonitorTypeEnum.FIDELITY_MONITOR.getCode())) {
            // Slice B：按 (tenantId+userId+secUid) 校验有效标准稿；无则抛 70005
            String secUid = video.getSecUid();
            StandardScriptInfoVo standardScript = standardScriptFeign.findValid(
                    user.getActiveTenantId(), user.getId(), secUid);
            if (standardScript == null) {
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_UNCONFIRMED);
            }
        }
        if (Objects.equals(monitorType, MonitorTypeEnum.INTERACTION_PATROL.getCode())) {
            // 注意：不能用 video.getExistBarrage() —— 单视频查询 GetByVideoId 不填充该字段（始终 null）
            // 必须走 hasBarrage SPI 查 tb_socket_collect_message 实时判定
            Boolean hasBarrage = ResultUtil.getResult(anchorVideoFeign.hasBarrage(bo.getSourceId()));
            if (!Boolean.TRUE.equals(hasBarrage)) {
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_NO_BARRAGE);
            }
        }

        // 6. 查询现有报告，记录触发前原始状态（D-7：originalStatus 用于异步失败时恢复旧成功报告）
        ScriptMonitorReportEntity existBefore = findReport(user.getActiveTenantId(), bo);
        Integer originalStatus = existBefore != null ? existBefore.getStatus()
                : ScriptMonitorStatusEnum.NOT_GENERATED.getCode();

        // 7. 预扣 Token（余额不足时抛 70001；先扣再置 GENERATING，避免 GENERATING 死锁 M5）
        RedisWithholdVo withhold = aiTokenWithholdFeign.withholdAiToken(user.getId(), AI_TOKEN_PREHOLD);

        // 8. 防重复 + 创建/重置 GENERATING 任务（唯一键防并发）
        ScriptMonitorReportEntity report = createGeneratingTaskAndReturn(user, bo, existBefore, TRIGGER_SOURCE_MANUAL);

        // 9. 投递 MQ 异步生成（写入本地消息表立即返回；XXL-Job 扫描发 RocketMQ；consumer 调 generate）
        try {
            String tag = resolveMqTag(monitorType);
            if (tag == null) {
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "未配置该监控类型的 MQ tag");
            }
            ScriptMonitorTriggerMsg msg = ScriptMonitorTriggerMsg.builder()
                    .reportId(report.getId())
                    .originalStatus(originalStatus)
                    .withhold(withhold)
                    .build();
            // messageKey 必须每次消息唯一（RocketMqBll.getMessageRecordByMessageKey 用 selectOne），
            // 用 UUID（跟项目现网范式一致：IdUtil.simpleUUID()）；reportId 会因 FAILED 重试/GENERATED
            // 重新触发被复用，不能作 messageKey。
            String messageKey = IdUtil.simpleUUID();
            boolean sent = rocketMqBll.syncSendAndDeliverToTopic(
                    user.getId(),
                    scriptMonitorMqProperties.getTopic(),
                    tag,
                    messageKey,
                    JSON.toJSONString(msg));
            if (!sent) {
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "报告生成任务提交失败，请稍后重试");
            }
        } catch (Exception ex) {
            // MQ 消息入库失败：返还预扣 Token + 回滚状态
            log.error("MQ 消息投递失败 reportId={}", report.getId(), ex);
            try {
                aiTokenWithholdFeign.returnAiToken(withhold);
            } catch (Exception re) {
                log.error("returnAiToken 失败(MQ 投递失败) reportId={}", report.getId(), re);
            }
            report.setStatus(originalStatus);
            report.setUpdateDate(new Date());
            reportService.updateById(report);
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "报告生成任务提交失败，请稍后重试");
        }
    }

    /**
     * 自动触发报告生成（B7 — 不走 GlobalObject.getLocalUser()，跳过用户态校验）。
     *
     * <p>前提：调用方（{@code ScriptMonitorApi}）已完成开关 + hasAuth + Token 三道校验；
     * 本方法仅负责报告创建 + MQ 投递，不重复校验。</p>
     *
     * @param videoId     视频 ID（来自 tb_anchor_video）
     * @param userId      用户 ID（视频归属）
     * @param tenantId    租户 ID
     * @param monitorType 监控类型（0=质检 / 1=还原度 / 2=巡检）
     * @return 是否成功触发（写表 + 投递均完成返回 true；失败不抛，由外层 ScriptMonitorApi catch）
     */
    public boolean autoTriggerReport(String videoId, Long userId, Long tenantId, Integer monitorType) {
        // 1. 构造 TriggerReportBo（sourceType=0 录制视频对应 sceneType=0 复盘场景；
        //    HOTFIX 2026-06-06：原硬编码 sceneType=1 视频分析为错误组合，
        //    导致 reportStatus(sourceType=0,sceneType=0) 查不到自动生成的报告）
        TriggerReportBo bo = new TriggerReportBo();
        bo.setSourceType(MonitorSourceTypeEnum.RECORD_VIDEO.getCode());
        bo.setSceneType(MonitorSceneTypeEnum.REPLAY.getCode());
        bo.setSourceId(videoId);
        bo.setMonitorType(monitorType);

        // 2. 查现有报告（防重复）
        ScriptMonitorReportEntity existBefore = findReport(tenantId, bo);

        // 3. GENERATING 中：直接 skip（防重复自动触发）
        if (existBefore != null
                && Objects.equals(existBefore.getStatus(), ScriptMonitorStatusEnum.GENERATING.getCode())) {
            log.warn("[B7 自动触发] 报告已在生成中，跳过 videoId={} monitorType={}", videoId, monitorType);
            return false;
        }

        // 4. 创建/重置 GENERATING 任务（trigger_source='auto'）
        // 构造临时 UserCacheVo（autoTriggerReport 不走 GlobalObject）
        UserCacheVo tempUser = new UserCacheVo();
        tempUser.setId(userId);
        tempUser.setActiveTenantId(tenantId);
        ScriptMonitorReportEntity report;
        try {
            report = createGeneratingTaskAndReturn(tempUser, bo, existBefore, TRIGGER_SOURCE_AUTO);
        } catch (BusinessException be) {
            // 并发防重 / 资源校验等业务异常 — 降级为 warn，避免误导排障
            log.warn("[B7 自动触发] 业务校验拒绝 videoId={} monitorType={} reason={}",
                    videoId, monitorType, be.getMessage());
            return false;
        } catch (Exception e) {
            log.error("[B7 自动触发] 创建报告任务失败 videoId={} monitorType={}", videoId, monitorType, e);
            return false;
        }
        log.info("[B7 自动触发] 报告已创建 reportId={} monitorType={} triggerSource=auto", report.getId(), monitorType);

        // 5. 解析 MQ tag
        String tag = resolveMqTag(monitorType);
        if (tag == null) {
            log.error("[B7 自动触发] 未配置该监控类型的 MQ tag monitorType={}", monitorType);
            return false;
        }

        // 6. 构造 MQ 消息（自动触发不预扣 Token，withhold=null）
        ScriptMonitorTriggerMsg msg = ScriptMonitorTriggerMsg.builder()
                .reportId(report.getId())
                .originalStatus(existBefore != null ? existBefore.getStatus()
                        : ScriptMonitorStatusEnum.NOT_GENERATED.getCode())
                .withhold(null)
                .build();
        String messageKey = IdUtil.simpleUUID();

        // 7. 写表 + 业务直发（syncSendAndDeliverToTopic 含 @Transactional，直发失败保 status=0 等 XXL-Job）
        boolean sent = rocketMqBll.syncSendAndDeliverToTopic(
                userId,
                scriptMonitorMqProperties.getTopic(),
                tag,
                messageKey,
                JSON.toJSONString(msg));
        if (!sent) {
            log.error("[B7 自动触发] MQ 消息表写入失败 videoId={} monitorType={}", videoId, monitorType);
            return false;
        }
        return true;
    }

    /**
     * 按 monitorType 解析对应 MQ tag（B4 修补包-3）。
     *
     * @param monitorType 监控类型（{@link MonitorTypeEnum}）
     * @return 对应 tag；未配置返回 null
     */
    private String resolveMqTag(Integer monitorType) {
        ScriptMonitorMqProperties.BusinessTags tags = scriptMonitorMqProperties.getBusinessTags();
        if (Objects.equals(monitorType, MonitorTypeEnum.QUALITY_INSPECTION.getCode())) {
            return tags.getTagQualityInspection();
        }
        if (Objects.equals(monitorType, MonitorTypeEnum.FIDELITY_MONITOR.getCode())) {
            return tags.getTagFidelityMonitor();
        }
        if (Objects.equals(monitorType, MonitorTypeEnum.INTERACTION_PATROL.getCode())) {
            return tags.getTagInteractionPatrol();
        }
        return null;
    }

    /**
     * 内存装配单资源三类监控状态
     *
     * @param user       当前用户
     * @param sourceType 资源类型
     * @param sceneType  场景类型
     * @param sourceId   资源 ID
     * @param secUid     主播唯一标识（可为 null）
     * @param video      资源信息
     * @param reportMap  报告图
     * @param enabledMap 三开关图
     * @return 单资源监控状态
     */
    private ScriptMonitorReportStatusVo assemble(Integer sourceType, Integer sceneType,
                                                 String sourceId, String secUid,
                                                 Map<String, Map<Integer, ScriptMonitorReportEntity>> reportMap,
                                                 Map<String, AnchorUrlUserVo> enabledMap) {
        ScriptMonitorReportStatusVo vo = new ScriptMonitorReportStatusVo();
        vo.setSourceType(sourceType);
        vo.setSceneType(sceneType);
        vo.setSourceId(sourceId);

        Map<Integer, ScriptMonitorReportEntity> byType =
                reportMap.getOrDefault(sourceKey(sourceType, sceneType, sourceId), Collections.emptyMap());
        // 未传 secUid / 查无记录 → AnchorUrlUserVo 为 null，monitorEnabled 全 null
        AnchorUrlUserVo enabledVo = (secUid == null || enabledMap == null) ? null : enabledMap.get(secUid);
        List<MonitorTypeStatusVo> monitors = new ArrayList<>();
        for (MonitorTypeEnum type : MonitorTypeEnum.values()) {
            monitors.add(buildMonitorStatus(type, byType.get(type.getCode()), enabledVo));
        }
        vo.setMonitors(monitors);
        return vo;
    }

    /**
     * 构建单个监控类型状态 VO。
     *
     * <p>B9 改造：isRead 直接取 report.getIsRead()，不再查 tb_script_monitor_read；
     * canConfirm 字段已下线（detail 接口自动写已读，前端不再需要"是否可确认"语义）。</p>
     *
     * @param type      监控类型枚举
     * @param report    报告实体（可为 null，表示未生成）
     * @param enabledVo 直播间用户配置（含三开关字段），可能为 null
     * @return 监控类型状态 VO
     */
    private MonitorTypeStatusVo buildMonitorStatus(MonitorTypeEnum type, ScriptMonitorReportEntity report,
                                                   AnchorUrlUserVo enabledVo) {
        MonitorTypeStatusVo m = new MonitorTypeStatusVo();
        m.setMonitorType(type.getCode());
        m.setMonitorTypeText(type.getRemarks());
        m.setMonitorEnabled(pickMonitorEnabled(type, enabledVo));
        if (report == null) {
            m.setStatus(ScriptMonitorStatusEnum.NOT_GENERATED.getCode());
            m.setStatusText(ScriptMonitorStatusEnum.NOT_GENERATED.getRemarks());
            m.setIsRead(0);
            return m;
        }
        m.setStatus(report.getStatus());
        m.setStatusText(statusText(report.getStatus()));
        m.setReportId(report.getId());
        // 仅 GENERATED 状态返回 summary 对象；生成中/失败/不可生成时返 null（契约 v1.3 §3.1 要求）
        m.setSummary(Objects.equals(report.getStatus(), ScriptMonitorStatusEnum.GENERATED.getCode())
                ? buildSummaryJson(report)
                : null);
        m.setUnavailableReason(report.getUnavailableReason());
        // B9：isRead 直接取主表字段，不再查 tb_script_monitor_read
        m.setIsRead(report.getIsRead() != null ? report.getIsRead() : 0);
        return m;
    }

    /**
     * 按 monitorType 从 anchor_url_user 三开关字段挑对应字段。
     *
     * <p>enabledVo 为 null（未传 secUid / 查无记录） → 返 null（前端语义"不适用"）。</p>
     *
     * @param type      监控类型枚举
     * @param enabledVo 直播间用户配置（含三开关字段），可能为 null
     * @return 0=关 / 1=开 / null=不适用
     */
    private Integer pickMonitorEnabled(MonitorTypeEnum type, AnchorUrlUserVo enabledVo) {
        if (enabledVo == null) {
            return null;
        }
        if (Objects.equals(type.getCode(), MonitorTypeEnum.QUALITY_INSPECTION.getCode())) {
            return enabledVo.getIsScriptQualityInspection();
        }
        if (Objects.equals(type.getCode(), MonitorTypeEnum.FIDELITY_MONITOR.getCode())) {
            return enabledVo.getIsScriptFidelityMonitor();
        }
        if (Objects.equals(type.getCode(), MonitorTypeEnum.INTERACTION_PATROL.getCode())) {
            return enabledVo.getIsInteractionPatrol();
        }
        return null;
    }

    /**
     * AI Token 余额 = totalQuantity - useQuantity
     *
     * @param userId 用户 ID
     * @return Token 余额
     */
    private long aiTokenBalance(Long userId) {
        List<UserPropertyTypeInfoVo> props = userPropertyFeign.getUserProperty(userId);
        if (CollUtil.isEmpty(props)) {
            return 0L;
        }
        return props.stream()
                .filter(p -> AI_TOKEN_CODE.equals(p.getCommodityTypeCode()))
                .findAny()
                .map(p -> Math.max(0L, safeLong(p.getTotalQuantity()) - safeLong(p.getUseQuantity())))
                .orElse(0L);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 触发来源常量：手动触发
     */
    private static final String TRIGGER_SOURCE_MANUAL = "manual";
    /**
     * 触发来源常量：B7 自动触发
     */
    private static final String TRIGGER_SOURCE_AUTO = "auto";

    /**
     * 防重复 + 创建/重置 GENERATING 任务，返回报告行供后续投递异步使用。
     *
     * <p>唯一键 uk_tenant_source_monitor 兜底并发。</p>
     *
     * @param user          当前用户
     * @param bo            触发参数
     * @param existBefore   调用方预查的现有报告（null 表示首次创建），已记录原始 status 供回滚
     * @param triggerSource 触发来源（{@link #TRIGGER_SOURCE_MANUAL} 或 {@link #TRIGGER_SOURCE_AUTO}）
     * @return 已置 GENERATING 的报告行（含旧成功报告字段用于 D-7 失败回滚）
     */
    private ScriptMonitorReportEntity createGeneratingTaskAndReturn(UserCacheVo user, TriggerReportBo bo,
                                                                    ScriptMonitorReportEntity existBefore,
                                                                    String triggerSource) {
        if (existBefore != null) {
            return handleExistingAndReturn(existBefore, user.getId(), triggerSource);
        }
        Date now = new Date();
        ScriptMonitorReportEntity report = ScriptMonitorReportEntity.builder()
                .id(SnowflakeManager.nextValue())
                .tenantId(user.getActiveTenantId())
                .userId(user.getId())
                .sourceType(bo.getSourceType())
                .sceneType(bo.getSceneType())
                .sourceId(bo.getSourceId())
                .monitorType(bo.getMonitorType())
                .status(ScriptMonitorStatusEnum.GENERATING.getCode())
                .triggerSource(triggerSource)
                .createDate(now)
                .updateDate(now)
                .isDeleted(0)
                .build();
        try {
            reportService.save(report);
        } catch (DuplicateKeyException e) {
            // 并发已创建，重查统一按 existing 处理
            ScriptMonitorReportEntity concurrent = findReport(user.getActiveTenantId(), bo);
            if (concurrent != null) {
                return handleExistingAndReturn(concurrent, user.getId(), triggerSource);
            }
        }
        return report;
    }

    /**
     * 处理已存在报告的重新触发，返回带旧成功态字段的报告行（供 D-7 失败回滚用）：
     * <ul>
     *   <li>GENERATING：防重复，拒绝</li>
     *   <li>GENERATED：旧成功字段保留在 entity 内存中（异步失败时恢复），不提前清除</li>
     *   <li>FAILED / NOT_APPLICABLE：安全重置为 GENERATING</li>
     * </ul>
     *
     * <p>B9 改造：重新触发时同步重置 is_read=0、confirmed_at=NULL（报告将重新生成，已读清零）。</p>
     *
     * @param exist         已有报告行
     * @param userId        当前触发者用户 ID（更新 userId 字段，确保 settleAiToken 传正确 userId）
     * @param triggerSource 触发来源（{@link #TRIGGER_SOURCE_MANUAL} 或 {@link #TRIGGER_SOURCE_AUTO}），覆写旧值
     * @return 报告行（GENERATED 时状态仍为 GENERATING 但旧字段保留供回滚）
     */
    private ScriptMonitorReportEntity handleExistingAndReturn(ScriptMonitorReportEntity exist, Long userId,
                                                              String triggerSource) {
        Integer status = exist.getStatus();
        if (Objects.equals(status, ScriptMonitorStatusEnum.GENERATING.getCode())) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), "报告生成中，请勿重复触发");
        }
        // FAILED / NOT_APPLICABLE → 无成功报告可破坏，安全重置为 GENERATING 重试
        // GENERATED → 旧成功字段（reportBodyId/counts）保留在 entity 内存，
        //             generate() 里 originalStatus=2 确保失败时恢复（D-7）；
        //             此处只更新 status → GENERATING（不动摘要字段）
        Date now = new Date();
        // B9 hotfix：用 LambdaUpdateWrapper 显式 set，绕过 entity FieldStrategy 默认 NOT_NULL 策略，
        // 确保 confirmedAt=null 能写入 DB；同时避免 partial entity update 把 isRead/confirmedAt 误写 null
        reportService.lambdaUpdate()
                .eq(ScriptMonitorReportEntity::getId, exist.getId())
                .set(ScriptMonitorReportEntity::getStatus, ScriptMonitorStatusEnum.GENERATING.getCode())
                .set(ScriptMonitorReportEntity::getUserId, userId)
                .set(ScriptMonitorReportEntity::getTriggerSource, triggerSource)
                .set(ScriptMonitorReportEntity::getUnavailableReason, null)
                .set(ScriptMonitorReportEntity::getIsRead, 0)
                .set(ScriptMonitorReportEntity::getConfirmedAt, null)
                .set(ScriptMonitorReportEntity::getUpdateDate, now)
                .update();
        // 同步内存值（保持 entity 与 DB 一致，调用方 generate() 仍要读 entity）
        exist.setStatus(ScriptMonitorStatusEnum.GENERATING.getCode());
        exist.setUserId(userId);
        exist.setTriggerSource(triggerSource);
        exist.setUnavailableReason(null);
        exist.setIsRead(0);
        exist.setConfirmedAt(null);
        exist.setUpdateDate(now);
        return exist;
    }

    private ScriptMonitorReportEntity findReport(Long tenantId, TriggerReportBo bo) {
        return reportService.lambdaQuery()
                .eq(ScriptMonitorReportEntity::getTenantId, tenantId)
                .eq(ScriptMonitorReportEntity::getSourceType, bo.getSourceType())
                .eq(ScriptMonitorReportEntity::getSceneType, bo.getSceneType())
                .eq(ScriptMonitorReportEntity::getSourceId, bo.getSourceId())
                .eq(ScriptMonitorReportEntity::getMonitorType, bo.getMonitorType())
                .eq(ScriptMonitorReportEntity::getIsDeleted, 0)
                .last("LIMIT 1")
                .one();
    }

    /**
     * 批量加载录制视频资源（B2-a 仅 sourceType=0，复用 AnchorVideoFeign.listByVideoIds）
     *
     * @param videoIds 视频 ID 列表
     * @return videoId → AnchorVideoInfoVo 映射
     */
    private Map<String, AnchorVideoInfoVo> batchLoadVideos(List<String> videoIds) {
        if (CollUtil.isEmpty(videoIds)) {
            return Collections.emptyMap();
        }
        List<AnchorVideoInfoVo> videos = ResultUtil.getResult(anchorVideoFeign.listByVideoIds(videoIds));
        if (CollUtil.isEmpty(videos)) {
            return Collections.emptyMap();
        }
        return videos.stream()
                .filter(v -> v.getVideoId() != null)
                .collect(Collectors.toMap(AnchorVideoInfoVo::getVideoId, Function.identity(), (a, b) -> a));
    }

    /**
     * 批量加载上传文件资源（sourceType=1），将文件信息桥接到 AnchorVideoInfoVo。
     *
     * <p>上传文件暂无批量 SPI，循环单查（上限 BATCH_LIMIT=100 可接受）。</p>
     *
     * @param fileIds 文件 ID 列表
     * @return fileId → AnchorVideoInfoVo (仅 tenantId/userId 有值) 的映射
     */
    private Map<String, AnchorVideoInfoVo> batchLoadUploadFiles(List<String> fileIds) {
        if (CollUtil.isEmpty(fileIds)) {
            return Collections.emptyMap();
        }
        Map<String, AnchorVideoInfoVo> result = new HashMap<>(fileIds.size() * 2);
        for (String fileId : fileIds) {
            UploadFileSimpleInfoVo fileInfo = ResultUtil.getResult(sensitiveWordsFeign.getUploadFileInfo(fileId));
            if (fileInfo != null) {
                AnchorVideoInfoVo bridge = new AnchorVideoInfoVo();
                bridge.setTenantId(fileInfo.getTenantId());
                bridge.setUserId(fileInfo.getUserId());
                result.put(fileId, bridge);
            }
        }
        return result;
    }

    /**
     * 按 sourceType 加载资源信息，用于鉴权字段（tenantId/userId）填充。
     *
     * <p>sourceType=0：从 AnchorVideoFeign 取录制视频信息，直接返回 AnchorVideoInfoVo。
     * sourceType=1：从 SensitiveWordsFeign 取上传文件信息，将 tenantId/userId 桥接到
     * dummy AnchorVideoInfoVo（仅鉴权字段有值，视频专属字段为 null）。</p>
     *
     * @param sourceType 资源类型 0录制视频 1上传文件
     * @param sourceId   资源唯一标识
     * @return 资源信息（鉴权字段已填充），不存在时返回 null
     */
    private AnchorVideoInfoVo loadSingleVideo(Integer sourceType, String sourceId) {
        if (Objects.equals(sourceType, MonitorSourceTypeEnum.RECORD_VIDEO.getCode()) && sourceId != null) {
            return ResultUtil.getResult(anchorVideoFeign.GetByVideoId(sourceId));
        }
        if (Objects.equals(sourceType, MonitorSourceTypeEnum.UPLOAD_FILE.getCode()) && sourceId != null) {
            UploadFileSimpleInfoVo fileInfo = ResultUtil.getResult(sensitiveWordsFeign.getUploadFileInfo(sourceId));
            if (fileInfo == null) {
                return null;
            }
            // 将上传文件鉴权字段桥接到 AnchorVideoInfoVo，视频专属字段保持 null
            AnchorVideoInfoVo bridge = new AnchorVideoInfoVo();
            bridge.setTenantId(fileInfo.getTenantId());
            bridge.setUserId(fileInfo.getUserId());
            return bridge;
        }
        return null;
    }

    /**
     * 批量查报告：tenantId + sourceId IN，内存按 (sourceType,sceneType,sourceId) → monitorType 分组
     *
     * @param tenantId  租户 ID
     * @param sourceIds 资源 ID 列表
     * @return 报告图
     */
    private Map<String, Map<Integer, ScriptMonitorReportEntity>> loadReportMap(Long tenantId, List<String> sourceIds) {
        if (CollUtil.isEmpty(sourceIds)) {
            return Collections.emptyMap();
        }
        List<ScriptMonitorReportEntity> reports = reportService.lambdaQuery()
                .eq(ScriptMonitorReportEntity::getTenantId, tenantId)
                .in(ScriptMonitorReportEntity::getSourceId, sourceIds)
                .eq(ScriptMonitorReportEntity::getIsDeleted, 0)
                .list();
        Map<String, Map<Integer, ScriptMonitorReportEntity>> map = new HashMap<>();
        for (ScriptMonitorReportEntity r : reports) {
            String key = sourceKey(r.getSourceType(), r.getSceneType(), r.getSourceId());
            map.computeIfAbsent(key, k -> new HashMap<>()).put(r.getMonitorType(), r);
        }
        return map;
    }

    private String sourceKey(Integer sourceType, Integer sceneType, String sourceId) {
        return sourceType + "_" + sceneType + "_" + sourceId;
    }

    /**
     * 读权限：同租户即可读（含云空间分享场景）
     *
     * @param user  当前用户
     * @param video 资源信息
     * @return 是否有读权限
     */
    private boolean hasReadPermission(UserCacheVo user, AnchorVideoInfoVo video) {
        return video != null && Objects.equals(video.getTenantId(), user.getActiveTenantId());
    }

    /**
     * 确认/操作权限：必须是资源归属账号(录制人本人)且同租户
     *
     * @param user  当前用户
     * @param video 资源信息
     * @return 是否有确认权限
     */
    private boolean hasConfirmPermission(UserCacheVo user, AnchorVideoInfoVo video) {
        return video != null
                && Objects.equals(video.getTenantId(), user.getActiveTenantId())
                && Objects.equals(video.getUserId(), user.getId());
    }

    private UserCacheVo currentUser() {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        if (user == null || user.getId() == null) {
            throw new BusinessException(StatusCode.UNAUTHORIZED);
        }
        return user;
    }

    private String statusText(Integer status) {
        for (ScriptMonitorStatusEnum e : ScriptMonitorStatusEnum.values()) {
            if (Objects.equals(e.getCode(), status)) {
                return e.getRemarks();
            }
        }
        return "";
    }

    /**
     * 列表摘要：直接返回 entity.summaryJson（String 原始 JSON），不再按 monitorType 分支构造嵌套 VO。
     *
     * <p>2026-06-02 改造：9 列摘要字段合并为 1 列 summary_json TEXT；后端不解析具体字段，
     * 前端按 monitorType 自行 JSON.parse。仅在 report.status=GENERATED 时调用（调用方做门控）。</p>
     *
     * @param report 报告实体（非 null）
     * @return 原始 summary JSON 字符串；null 表示该报告未生成 summary（AI 返回非法或缺 summary key）
     */
    private String buildSummaryJson(ScriptMonitorReportEntity report) {
        return report.getSummaryJson();
    }

    /**
     * 查询直播间基础配置 + 三个 AI 监控开关
     *
     * <p>通过 AnchorUrlUserFeign SPI 跨模块取 tb_anchor_url_user 数据，并通过 BasicSettingsFeign
     * 合并 tb_basic_settings 中 BasicSettingsBaseDto 12 字段（含 livingMode / accountType 等），
     * 修复 VO 继承字段始终为 null 的问题。basic 合并采用 fail-safe：查询失败/无记录时主响应正常返回，
     * BaseDto 字段保持 null（等价于本次修复前的行为）。</p>
     *
     * <p>standardScriptId B5 阶段恒返 null（B3 标准稿能力实现后再填）。</p>
     *
     * @param secUid 主播唯一标识
     * @return AnchorUrlUserVo（含话术质检/还原度/互动巡检三类开关 + BaseDto 12 字段）
     */
    public R<AnchorUrlUserVo> anchorBasicConfig(String secUid) {
        UserCacheVo user = currentUser();
        R<AnchorUrlUserVo> result = anchorUrlUserFeign.getBySecUidAndUser(secUid, user.getId(), user.getActiveTenantId());
        if (result == null || result.getCode() != 0 || result.getData() == null) {
            return result != null ? result : R.error(30000, "记录不存在");
        }
        AnchorUrlUserVo vo = result.getData();
        // 合并 tb_basic_settings 12 字段（fail-safe；失败/无记录时 BaseDto 字段保持 null）
        try {
            R<BasicSettingsVo> basicR = basicSettingsFeign.getByAnchor(secUid, user.getId(), user.getActiveTenantId());
            BasicSettingsVo basic = (basicR != null && basicR.getCode() == 0) ? basicR.getData() : null;
            if (basic != null) {
                // 显式排除 BasicSettingsVo 自有 7 字段，原因有二，缺一不可：
                //   1) id / sourceId / sourceType / userId / tenantId — 防污染 anchor 维度同名字段
                //      （vo.id / vo.userId / vo.tenantId 来自 tb_anchor_url_user，不可被 basic 覆盖）
                //   2) createDate / updateDate — BasicSettingsVo 为 LocalDateTime，AnchorUrlUserVo 为 Date，
                //      类型不兼容；BeanUtils 遇类型不匹配会静默跳过，但显式排除让意图明确，避免未来精简列表
                //      时误删导致行为隐性退化
                BeanUtils.copyProperties(basic, vo,
                        "id", "sourceId", "sourceType", "userId", "tenantId", "createDate", "updateDate");
            }
        } catch (Exception e) {
            log.warn("anchorBasicConfig: 合并 tb_basic_settings 失败 secUid={}", secUid, e);
        }
        // standardScriptId B5 阶段恒返 null（B3 标准稿能力实现后再填）
        vo.setStandardScriptId(null);
        return result;
    }

    /**
     * sourceType=1 时从上传文件信息填充主播/直播 anchor 字段。
     *
     * <p>anchorName = 文件名（前端按 sceneType 切换 label），liveTitle = 文件名，
     * liveTime = uploadTime 解析结果（格式 yyyy-MM-dd HH:mm:ss）。
     * 取不到文件信息或解析失败时字段返 null + log.warn，与 sourceType=0 取不到 anchor 信息同款行为。</p>
     *
     * @param vo       待填充的详情 VO
     * @param sourceId 文件 ID
     */
    private void fillAnchorFieldsFromUploadFile(QualityReportDetailVo vo, String sourceId) {
        UploadFileSimpleInfoVo fileInfo;
        try {
            fileInfo = ResultUtil.getResult(sensitiveWordsFeign.getUploadFileInfo(sourceId));
        } catch (Exception e) {
            log.warn("fillAnchorFieldsFromUploadFile: 取上传文件信息失败, sourceId={}, error={}", sourceId, e.getMessage());
            return;
        }
        if (fileInfo == null) {
            log.warn("fillAnchorFieldsFromUploadFile: 上传文件不存在, sourceId={}", sourceId);
            return;
        }
        vo.setAnchorName(fileInfo.getFileName());
        vo.setLiveTitle(fileInfo.getFileName());
        if (fileInfo.getUploadTime() != null) {
            try {
                // 使用线程安全的 DateTimeFormatter 替代 SimpleDateFormat（M11 修补）
                LocalDateTime uploadLdt = LocalDateTime.parse(fileInfo.getUploadTime(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                vo.setLiveTime(java.util.Date.from(
                        uploadLdt.atZone(java.time.ZoneId.systemDefault()).toInstant()));
            } catch (DateTimeParseException e) {
                log.warn("fillAnchorFieldsFromUploadFile: uploadTime 解析失败, sourceId={}, uploadTime={}",
                        sourceId, fileInfo.getUploadTime());
            }
        }
    }
}
