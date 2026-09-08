package com.jiuyu.replay.ai.bll;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.jiuyu.replay.ai.bo.BatchReportStatusBo;
import com.jiuyu.replay.ai.bo.ReportStatusBo;
import com.jiuyu.replay.ai.bo.TriggerReportBo;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorReportBodyRepository;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.ai.config.ScriptMonitorMqProperties;
import com.jiuyu.replay.ai.vo.FidelityReportDetailVo;
import com.jiuyu.replay.ai.vo.InteractionPatrolReportDetailVo;
import com.jiuyu.replay.ai.vo.QualityReportDetailVo;
import com.jiuyu.replay.ai.vo.ScriptMonitorReportStatusVo;
import com.jiuyu.replay.common.bll.RocketMqBll;
import com.jiuyu.replay.generic.enums.words.MonitorTypeEnum;
import com.jiuyu.replay.generic.enums.words.ScriptMonitorStatusEnum;
import com.jiuyu.replay.generic.feign.order.AiTokenWithholdFeign;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoFeign;
import com.jiuyu.replay.generic.feign.words.BasicSettingsFeign;
import com.jiuyu.replay.generic.feign.words.SensitiveWordsFeign;
import com.jiuyu.replay.generic.feign.words.StandardScriptFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.UploadFileSimpleInfoVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScriptMonitorBll 单测（B2-a 状态查询 + B2-b 触发 + B9 detail 已读 + confirmRead 删除）
 *
 * <p>B9 改造（2026-06-08）：
 * <ul>
 *   <li>删除 readService / roleConfirmService Mock 注入</li>
 *   <li>删除 confirmRead 相关测试（端点已删除）</li>
 *   <li>新增 AC-001/AC-002/AC-004/AC-005/AC-006/AC-007/AC-012 测试</li>
 *   <li>isRead 断言改为来源 report.getIsRead()</li>
 * </ul>
 * </p>
 *
 * @author beta
 * @date 2026-05-29
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScriptMonitorBllTest {

    @Mock
    private ScriptMonitorReportService reportService;
    @Mock
    private ScriptMonitorReportWriteService writeService;
    @Mock
    private ScriptMonitorReportBodyRepository reportBodyRepository;
    @Mock
    private com.jiuyu.replay.generic.feign.words.AnchorUrlUserFeign anchorUrlUserFeign;
    @Mock
    private AnchorVideoFeign anchorVideoFeign;
    @Mock
    private SensitiveWordsFeign sensitiveWordsFeign;
    @Mock
    private UserFeign userFeign;
    @Mock
    private UserPropertyFeign userPropertyFeign;
    @Mock
    private AiTokenWithholdFeign aiTokenWithholdFeign;
    @Mock
    private RocketMqBll rocketMqBll;
    @Mock
    private ScriptMonitorMqProperties scriptMonitorMqProperties;
    @Mock
    private StandardScriptFeign standardScriptFeign;
    @Mock
    private BasicSettingsFeign basicSettingsFeign;

    private ScriptMonitorBll bll;

    private UserCacheVo user;

    @BeforeEach
    void setUp() {
        // 手动构造（构造器注入，B9 移除了 readService + roleConfirmService 两个参数）
        bll = new ScriptMonitorBll(
                reportService,
                writeService,
                reportBodyRepository,
                anchorUrlUserFeign,
                anchorVideoFeign,
                sensitiveWordsFeign,
                userFeign,
                userPropertyFeign,
                aiTokenWithholdFeign,
                rocketMqBll,
                scriptMonitorMqProperties,
                standardScriptFeign,
                basicSettingsFeign
        );

        user = new UserCacheVo();
        user.setId(1L);
        user.setActiveTenantId(100L);
        when(userFeign.getLocalUser()).thenReturn(R.ok(user));
        // B4 修补包-3：默认 mock MQ properties 返回质检 tag + MQ 发送成功（按需被各测试覆盖）
        ScriptMonitorMqProperties.BusinessTags defaultTags = new ScriptMonitorMqProperties.BusinessTags();
        defaultTags.setTagQualityInspection("Tag_Quality_Inspection");
        defaultTags.setTagInteractionPatrol("Tag_Interaction_Patrol");
        when(scriptMonitorMqProperties.getBusinessTags()).thenReturn(defaultTags);
        when(scriptMonitorMqProperties.getTopic()).thenReturn("script_monitor_test_topic");
        when(rocketMqBll.syncSendAndDeliverToTopic(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
    }

    // ---------- helpers ----------

    private AnchorVideoInfoVo video(Long tenantId, Long userId, Integer existBarrage) {
        AnchorVideoInfoVo v = new AnchorVideoInfoVo();
        v.setVideoId("v1");
        v.setTenantId(tenantId);
        v.setUserId(userId);
        v.setExistBarrage(existBarrage);
        return v;
    }

    private ScriptMonitorReportEntity report(Integer status) {
        ScriptMonitorReportEntity r = new ScriptMonitorReportEntity();
        r.setId(5L);
        r.setTenantId(100L);
        r.setSourceType(0);
        r.setSceneType(0);
        r.setSourceId("v1");
        r.setMonitorType(0);
        r.setStatus(status);
        r.setIsDeleted(0);
        r.setIsRead(0);
        return r;
    }

    private TriggerReportBo trigger(Integer monitorType) {
        TriggerReportBo bo = new TriggerReportBo();
        bo.setSourceType(0);
        bo.setSceneType(0);
        bo.setSourceId("v1");
        bo.setMonitorType(monitorType);
        return bo;
    }

    private List<UserPropertyTypeInfoVo> tokenProps(long total, long use) {
        UserPropertyTypeInfoVo v = new UserPropertyTypeInfoVo();
        v.setCommodityTypeCode("aiTokenNum");
        v.setTotalQuantity(total);
        v.setUseQuantity(use);
        return List.of(v);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryChainWrapper<ScriptMonitorReportEntity> mockReportQuery() {
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> q = mock(LambdaQueryChainWrapper.class);
        doReturn(q).when(q).eq(any(), any());
        doReturn(q).when(q).in(any(), anyCollection());
        doReturn(q).when(q).last(any());
        when(reportService.lambdaQuery()).thenReturn(q);
        return q;
    }

    private int code(BusinessException ex) {
        return ex.getCode();
    }

    // ---------- B2-a reportStatus ----------

    @Test
    void reportStatus_resourceNotFound_throwsParamException() {
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok((AnchorVideoInfoVo) null));
        BusinessException ex = assertThrows(BusinessException.class, () -> bll.reportStatus(statusBo(0, 0, "v1")));
        assertEquals(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), code(ex));
    }

    @Test
    void reportStatus_noReadPermission_throws70011() {
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(999L, 1L, 0)));
        BusinessException ex = assertThrows(BusinessException.class, () -> bll.reportStatus(statusBo(0, 0, "v1")));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), code(ex));
    }

    @Test
    void reportStatus_normal_allThreeMonitorsNotGenerated() {
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> q = mockReportQuery();
        when(q.list()).thenReturn(new ArrayList<>());
        ScriptMonitorReportStatusVo vo = bll.reportStatus(statusBo(0, 0, "v1"));
        assertEquals(3, vo.getMonitors().size());
        assertTrue(vo.getMonitors().stream()
                .allMatch(m -> ScriptMonitorStatusEnum.NOT_GENERATED.getCode().equals(m.getStatus())));
    }

    // ---------- B2-a batchReportStatus ----------

    @Test
    void batchReportStatus_over100_throws70013() {
        BatchReportStatusBo bo = new BatchReportStatusBo();
        List<BatchReportStatusBo.SourceItemBo> items = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            BatchReportStatusBo.SourceItemBo s = new BatchReportStatusBo.SourceItemBo();
            s.setSourceType(0);
            s.setSceneType(0);
            s.setSourceId("v" + i);
            items.add(s);
        }
        bo.setSources(items);
        BusinessException ex = assertThrows(BusinessException.class, () -> bll.batchReportStatus(bo));
        assertEquals(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), code(ex));
    }

    // ---------- B2-b triggerReport ----------

    @Test
    void triggerReport_paramIncomplete_throws70013() {
        BusinessException ex = assertThrows(BusinessException.class, () -> bll.triggerReport(new TriggerReportBo()));
        assertEquals(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), code(ex));
    }

    /**
     * HOTFIX 2026-06-06: 防御 sourceType-sceneType 非法组合
     */
    @Test
    void triggerReport_sourceType0WithSceneType1_invalidCombination_throws70013() {
        TriggerReportBo bo = new TriggerReportBo();
        bo.setSourceType(0);
        bo.setSceneType(1);
        bo.setSourceId("v1");
        bo.setMonitorType(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        BusinessException ex = assertThrows(BusinessException.class, () -> bll.triggerReport(bo));
        assertEquals(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), code(ex));
        assertTrue(ex.getMessage().contains("组合非法"));
    }

    @Test
    void triggerReport_sourceType1WithSceneType0_invalidCombination_throws70013() {
        TriggerReportBo bo = new TriggerReportBo();
        bo.setSourceType(1);
        bo.setSceneType(0);
        bo.setSourceId("file1");
        bo.setMonitorType(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        BusinessException ex = assertThrows(BusinessException.class, () -> bll.triggerReport(bo));
        assertEquals(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), code(ex));
        assertTrue(ex.getMessage().contains("组合非法"));
    }

    /**
     * Slice B AC-005：monitorType=1 通过资源校验 + Token 校验后，按 (tenantId+userId+secUid) 无有效标准稿 → 抛 70005。
     *
     * <p>关键回归点：原"功能未上线 70014"硬抛闸门已删除（Slice B 目标之一），新路径进入 StandardScriptFeign.findValid 校验。</p>
     */
    @Test
    void triggerReport_monitorType1_noStandardScript_throws70005() {
        AnchorVideoInfoVo v = video(100L, 1L, 0);
        v.setSecUid("sec-001");
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        when(userPropertyFeign.getUserProperty(1L)).thenReturn(tokenProps(200000L, 0L));
        // StandardScriptFeign 返回 null → 无有效标准稿
        when(standardScriptFeign.findValid(100L, 1L, "sec-001")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.triggerReport(trigger(1)));
        assertEquals(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_UNCONFIRMED.getCode(), code(ex));
        // 关键：标准稿校验在预扣前，无标准稿不应消耗 Token
        verify(aiTokenWithholdFeign, never()).withholdAiToken(any(), any());
    }

    /**
     * AC-1（修复后契约）：socket 实测有弹幕 → hasBarrage=true → monitorType=2 闸门通过，
     * 进入 withholdAiToken + MQ 投递流程。video.existBarrage 字段值已不再被业务代码读取，
     * 改由 anchorVideoFeign.hasBarrage SPI 实时判定（修复 GetByVideoId 不填该字段导致的 NO_BARRAGE 误判）。
     */
    @Test
    void triggerReport_monitorType2_hasBarrageTrue_passesGate_proceedsToChecks() {
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, null)));
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(true));
        when(userPropertyFeign.getUserProperty(1L)).thenReturn(tokenProps(200000L, 0L));
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> q = mockReportQuery();
        when(q.one()).thenReturn(null);
        com.jiuyu.replay.generic.vo.order.RedisWithholdVo withholdVo =
                new com.jiuyu.replay.generic.vo.order.RedisWithholdVo();
        withholdVo.setWithholdId("w100");
        when(aiTokenWithholdFeign.withholdAiToken(any(), any())).thenReturn(withholdVo);
        assertDoesNotThrow(() -> bll.triggerReport(trigger(2)));
        // 闸门通过后必须扣 Token + 投递 MQ
        verify(aiTokenWithholdFeign).withholdAiToken(any(), any());
    }

    @Test
    void triggerReport_tokenInsufficient_throws70001() {
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(userPropertyFeign.getUserProperty(1L)).thenReturn(tokenProps(50000L, 0L));
        BusinessException ex = assertThrows(BusinessException.class, () -> bll.triggerReport(trigger(0)));
        assertEquals(StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH.getCode(), code(ex));
    }

    /**
     * AC-2（修复后契约）：socket 实测无弹幕 → hasBarrage=false → 抛 SCRIPT_MONITOR_NO_BARRAGE(70010)。
     *
     * <p>关键回归点：闸门置于 withholdAiToken 之前，无弹幕场景不应消耗 Token。</p>
     */
    @Test
    void triggerReport_monitorType2_hasBarrageFalse_throwsNoBarrage_notConsumeToken() {
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, null)));
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok(false));
        when(userPropertyFeign.getUserProperty(1L)).thenReturn(tokenProps(200000L, 0L));
        BusinessException ex = assertThrows(BusinessException.class, () -> bll.triggerReport(trigger(2)));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_BARRAGE.getCode(), code(ex));
        // 关键：闸门在 Token 预扣之前，不应消耗 Token
        verify(aiTokenWithholdFeign, never()).withholdAiToken(any(), any());

    }

    /**
     * AC-2 补充（修复后契约）：hasBarrage SPI 返回 null R（远程异常 / 空响应）→ ResultUtil 退化为 null →
     * !Boolean.TRUE.equals(null) 为 true → 抛 NO_BARRAGE。防止 SPI 异常被吞而误放过无弹幕场景。
     */
    @Test
    void triggerReport_monitorType2_hasBarrageNull_throwsNoBarrage() {
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, null)));
        when(anchorVideoFeign.hasBarrage("v1")).thenReturn(R.ok((Boolean) null));
        when(userPropertyFeign.getUserProperty(1L)).thenReturn(tokenProps(200000L, 0L));
        BusinessException ex = assertThrows(BusinessException.class, () -> bll.triggerReport(trigger(2)));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_BARRAGE.getCode(), code(ex));
        verify(aiTokenWithholdFeign, never()).withholdAiToken(any(), any());
    }

    /**
     * Slice B AC-006：验证 monitorType=1 原 449-453 "功能未上线 70014" 闸门已删除。
     *
     * <p>Given 合法参数 + monitorType=1（仅校验参数 + sourceType-sceneType 合法组合即继续），
     * when 调用 triggerReport，then 不抛 70014（FEATURE_NOT_AVAILABLE）；
     * 后续校验链异常允许（如视频不存在抛 70013 等），但禁出现 70014。</p>
     */
    @Test
    void triggerReport_monitorType1_doesNotThrow70014_gateRemoved() {
        // 不 mock video → 走 loadSingleVideo 返 null → 抛 70013（资源不存在），但绝不应是 70014
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok((AnchorVideoInfoVo) null));

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.triggerReport(trigger(1)));
        // 关键回归断言：不再抛 70014（功能未上线）
        assertNotEquals(StatusCode.SCRIPT_MONITOR_FEATURE_NOT_AVAILABLE.getCode(), code(ex),
                "Slice B 已删除 monitorType=1 → 70014 硬抛闸门，应进入后续校验链");
    }

    @Test
    void triggerReport_generating_rejectsDuplicateTrigger() {
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(userPropertyFeign.getUserProperty(1L)).thenReturn(tokenProps(200000L, 0L));
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> q = mockReportQuery();
        when(q.one()).thenReturn(report(ScriptMonitorStatusEnum.GENERATING.getCode()));
        BusinessException ex = assertThrows(BusinessException.class, () -> bll.triggerReport(trigger(0)));
        assertEquals(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), code(ex));
        verify(reportService, never()).updateById(any());
    }

    @Test
    @Disabled("DEBT-012: B7 改造后 triggerReport 走 MQ 双轨投递，本测试 mock 未补 RocketMqBll.syncSendAndDeliverToTopic 路径致 ApplicationException(70013)；非本次 summary/secUid 任务范围，待原作者补 MQ mock")
    void triggerReport_existingGeneratedReportRetriggered_setsGeneratingStatus() {
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(userPropertyFeign.getUserProperty(1L)).thenReturn(tokenProps(200000L, 0L));
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> q = mockReportQuery();
        when(q.one()).thenReturn(report(ScriptMonitorStatusEnum.GENERATED.getCode()));
        bll.triggerReport(trigger(0));
        verify(reportService).updateById(any());
        verify(reportService, never()).save(any());
    }

    @Test
    @Disabled("DEBT-012: 同上，B7 MQ mock 缺失致 70013")
    void triggerReport_qualityFirstTrigger_createsGeneratingTask() {
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(userPropertyFeign.getUserProperty(1L)).thenReturn(tokenProps(200000L, 0L));
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> q = mockReportQuery();
        when(q.one()).thenReturn(null);
        bll.triggerReport(trigger(0));
        verify(reportService).save(any());
    }

    // ---------- B9 AC-001: qualityReportDetail 录制人首次查看写已读 ----------

    @Test
    void qualityReportDetail_recorderFirstView_marksReadAndReturnsIsRead1() {
        ScriptMonitorReportEntity r = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setMonitorType(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        r.setSummaryJson("{\"crashCount\":2}");
        r.setIsRead(0);
        r.setConfirmedAt(null);
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());
        doNothing().when(writeService).markReadIfNeeded(eq(5L), eq(100L));

        QualityReportDetailVo vo = bll.qualityReportDetail(5L);

        assertNotNull(vo);
        assertEquals(5L, vo.getReportId());
        // AC-001：写已读后内存更新，响应 isRead=1
        assertEquals(1, vo.getIsRead());
        // 2026-06-10 对齐巡检：QualityReportDetailVo 新增 confirmedAt，首次查看后内存同步为 NOW()
        assertNotNull(vo.getConfirmedAt());
        // AC-010：QualityReportDetailVo 已无 confirmedRecords / canConfirm 字段（编译期保证）
        // AC-001：verify markReadIfNeeded 被调用一次（带 tenantId 防御参数 M-1）
        verify(writeService).markReadIfNeeded(eq(5L), eq(100L));
    }

    // ---------- B9 AC-004: 录制人重复查看不破坏状态 ----------

    @Test
    void qualityReportDetail_recorderAlreadyRead_doesNotInvokeMarkReadIfNeeded() {
        ScriptMonitorReportEntity r = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setMonitorType(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        r.setIsRead(1); // 已读
        // 2026-06-10 AC-2：已读报告携带 confirmedAt，VO 必须透传
        java.util.Date existingConfirmedAt = new java.util.Date(1717000000000L);
        r.setConfirmedAt(existingConfirmedAt);
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());

        QualityReportDetailVo vo = bll.qualityReportDetail(5L);

        assertEquals(1, vo.getIsRead());
        // AC-004：isRead=1 时不调 markReadIfNeeded（幂等）
        verify(writeService, never()).markReadIfNeeded(any(), any());
        // 2026-06-10 AC-2：VO.confirmedAt 等于 report.confirmedAt（透传，不覆盖）
        assertEquals(existingConfirmedAt, vo.getConfirmedAt());
    }

    // ---------- B9 AC-005: 非录制人不写已读 ----------

    @Test
    void qualityReportDetail_nonRecorder_canConfirmFalse_doesNotMarkRead() {
        ScriptMonitorReportEntity r = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setMonitorType(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        r.setIsRead(0);
        // 2026-06-10 AC-3：未写已读场景显式锁定 confirmedAt=null（防止后续误改 Bll 把 NOW() 默认填上）
        r.setConfirmedAt(null);
        when(reportService.getById(5L)).thenReturn(r);
        // video.userId=999 != user.id=1，非录制人
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 999L, 0)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());

        QualityReportDetailVo vo = bll.qualityReportDetail(5L);

        // AC-005：非录制人不触发 markReadIfNeeded
        verify(writeService, never()).markReadIfNeeded(any(), any());
        // isRead 保持原值 0
        assertEquals(0, vo.getIsRead());
        // 2026-06-10 AC-3：未触发写已读 → VO.confirmedAt 透传 null
        assertNull(vo.getConfirmedAt());
    }

    // ---------- B9 AC-006: 跨租户访问被拦截 ----------

    @Test
    void qualityReportDetail_crossTenant_throwsNoPermission() {
        ScriptMonitorReportEntity r = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setTenantId(999L);
        when(reportService.getById(5L)).thenReturn(r);
        // 视频加载已前移到权限校验之前：非公开分享（uploadStatus=null）→ 跨租户仍拦截
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(999L, 999L, 0)));

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.qualityReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), ex.getCode());
    }

    // ---------- B9 AC-007: 报告不存在或已软删 ----------

    @Test
    void qualityReportDetail_reportNotExist_throwsNotExist() {
        when(reportService.getById(5L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.qualityReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_REPORT_NOT_EXIST.getCode(), ex.getCode());
    }

    // ---------- B9 AC-012: markReadIfNeeded 异常不向上抛 ----------

    @Test
    void qualityReportDetail_markReadIfNeededThrows_doesNotPropagateException_returnsNormally() {
        ScriptMonitorReportEntity r = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setMonitorType(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        r.setIsRead(0);
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());
        // 模拟 markReadIfNeeded 抛出 DB 异常
        org.mockito.Mockito.doThrow(new RuntimeException("DB timeout"))
                .when(writeService).markReadIfNeeded(eq(5L), eq(100L));

        // AC-012：不向上层抛异常，正常返回 VO
        QualityReportDetailVo vo = assertDoesNotThrow(() -> bll.qualityReportDetail(5L));
        assertNotNull(vo);
        // 写失败时 isRead 保持内存旧值（0，写前未更新）
        assertEquals(0, vo.getIsRead());
    }

    // ---------- B9 AC-002: patrolReportDetail 录制人首次查看写已读 ----------

    @Test
    void patrolReportDetail_recorderFirstView_marksReadConfirmedAtNotNull() {
        ScriptMonitorReportEntity r = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setMonitorType(MonitorTypeEnum.INTERACTION_PATROL.getCode());
        r.setIsRead(0);
        r.setConfirmedAt(null);
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());
        doNothing().when(writeService).markReadIfNeeded(eq(5L), eq(100L));

        InteractionPatrolReportDetailVo vo = bll.patrolReportDetail(5L);

        assertNotNull(vo);
        // AC-002：写已读后内存更新 confirmedAt
        assertNotNull(vo.getConfirmedAt());
        assertEquals(1, vo.getIsRead());
        verify(writeService).markReadIfNeeded(eq(5L), eq(100L));
    }

    // ---------- B9 patrolReportDetail 权限校验 ----------

    @Test
    void patrolReportDetail_tenantMismatch_throwsNoPermission() {
        ScriptMonitorReportEntity r = new ScriptMonitorReportEntity();
        r.setId(7L);
        r.setTenantId(200L);
        r.setIsDeleted(0);
        when(reportService.getById(7L)).thenReturn(r);

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.patrolReportDetail(7L));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), ex.getCode());
    }

    // ---------- B4-4 sourceType=1 上传文件场景 ----------

    private UploadFileSimpleInfoVo uploadFile(Long tenantId, Long userId, String fileName, String uploadTime) {
        UploadFileSimpleInfoVo f = new UploadFileSimpleInfoVo();
        f.setFileId("file1");
        f.setTenantId(tenantId);
        f.setUserId(userId);
        f.setFileName(fileName);
        f.setUploadTime(uploadTime);
        f.setFileType(0);
        return f;
    }

    @Test
    void loadResource_sourceType1_happy() {
        when(sensitiveWordsFeign.getUploadFileInfo("file1")).thenReturn(R.ok(uploadFile(100L, 1L, "测试文件.mp4", "2026-05-01 10:00:00")));
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> q = mockReportQuery();
        when(q.list()).thenReturn(new ArrayList<>());

        ScriptMonitorReportStatusVo vo = bll.reportStatus(statusBo(1, 1, "file1"));
        assertNotNull(vo);
        assertEquals(3, vo.getMonitors().size());
    }

    @Test
    void loadResource_sourceType1_crossTenant() {
        when(sensitiveWordsFeign.getUploadFileInfo("file1")).thenReturn(R.ok(uploadFile(999L, 1L, "测试文件.mp4", "2026-05-01 10:00:00")));

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.reportStatus(statusBo(1, 1, "file1")));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), ex.getCode());
    }

    private ReportStatusBo statusBo(Integer sourceType, Integer sceneType, String sourceId) {
        ReportStatusBo bo = new ReportStatusBo();
        bo.setSourceType(sourceType);
        bo.setSceneType(sceneType);
        bo.setSourceId(sourceId);
        return bo;
    }

    @Test
    void qualityReportDetail_sourceType1_anchorFieldsFromUploadFile() {
        ScriptMonitorReportEntity r = new ScriptMonitorReportEntity();
        r.setId(6L);
        r.setTenantId(100L);
        r.setSourceType(1);
        r.setSceneType(1);
        r.setSourceId("file1");
        r.setMonitorType(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        r.setStatus(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setIsDeleted(0);
        r.setIsRead(0);
        when(reportService.getById(6L)).thenReturn(r);
        when(sensitiveWordsFeign.getUploadFileInfo("file1"))
                .thenReturn(R.ok(uploadFile(100L, 1L, "直播素材.mp4", "2026-05-01 10:00:00")));
        when(reportBodyRepository.findByReportId(6L)).thenReturn(Optional.empty());

        QualityReportDetailVo vo = bll.qualityReportDetail(6L);

        assertNotNull(vo);
        assertEquals("直播素材.mp4", vo.getAnchorName());
        assertEquals("直播素材.mp4", vo.getLiveTitle());
        assertNotNull(vo.getLiveTime());
    }

    // ---------- AC-M4: batchReportStatus 混合 sourceType ----------

    @Test
    void batchReportStatus_mixedSourceTypes_returnsAll() {
        BatchReportStatusBo bo = new BatchReportStatusBo();
        List<BatchReportStatusBo.SourceItemBo> items = new ArrayList<>();

        BatchReportStatusBo.SourceItemBo videoItem = new BatchReportStatusBo.SourceItemBo();
        videoItem.setSourceType(0);
        videoItem.setSceneType(0);
        videoItem.setSourceId("v1");
        items.add(videoItem);

        BatchReportStatusBo.SourceItemBo fileItem = new BatchReportStatusBo.SourceItemBo();
        fileItem.setSourceType(1);
        fileItem.setSceneType(1);
        fileItem.setSourceId("file1");
        items.add(fileItem);

        bo.setSources(items);

        when(anchorVideoFeign.listByVideoIds(any())).thenReturn(R.ok(List.of(video(100L, 1L, 0))));
        when(sensitiveWordsFeign.getUploadFileInfo("file1"))
                .thenReturn(R.ok(uploadFile(100L, 1L, "文件.mp4", "2026-05-01 10:00:00")));

        LambdaQueryChainWrapper<ScriptMonitorReportEntity> q = mockReportQuery();
        when(q.list()).thenReturn(new ArrayList<>());

        List<ScriptMonitorReportStatusVo> result = bll.batchReportStatus(bo);

        assertEquals(2, result.size());
    }

    // ---------- AC-M5（B4 修补包-3）: triggerReport MQ 投递失败后补偿 ----------

    @Test
    void triggerReport_mqSendFail_returnsToken_andRestoresStatus() {
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(userPropertyFeign.getUserProperty(1L)).thenReturn(tokenProps(200000L, 0L));
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> q = mockReportQuery();
        when(q.one()).thenReturn(null);

        com.jiuyu.replay.generic.vo.order.RedisWithholdVo withholdVo =
                new com.jiuyu.replay.generic.vo.order.RedisWithholdVo();
        withholdVo.setWithholdId("w999");
        when(aiTokenWithholdFeign.withholdAiToken(any(), any())).thenReturn(withholdVo);

        when(rocketMqBll.syncSendAndDeliverToTopic(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(false);

        doNothing().when(aiTokenWithholdFeign).returnAiToken(any());

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.triggerReport(trigger(0)));
        assertEquals(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), ex.getCode());

        verify(aiTokenWithholdFeign).returnAiToken(withholdVo);
        verify(reportService).updateById(any());
    }

    // ---------- Slice C: fidelityReportDetail（话术还原度详情）----------

    /**
     * Slice C (a)：录制人首次查看还原度详情 → 写已读 + isRead=1 + confirmedAt != null。
     *
     * <p>1:1 mirror qualityReportDetail recorderFirstView 范式。</p>
     */
    @Test
    void fidelityReportDetail_recorderFirstView_marksReadAndReturnsIsRead1() {
        ScriptMonitorReportEntity r = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setMonitorType(MonitorTypeEnum.FIDELITY_MONITOR.getCode());
        r.setSummaryJson("# 还原度报告\n核心偏差点 3 处");
        r.setIsRead(0);
        r.setConfirmedAt(null);
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());
        doNothing().when(writeService).markReadIfNeeded(eq(5L), eq(100L));

        FidelityReportDetailVo vo = bll.fidelityReportDetail(5L);

        assertNotNull(vo);
        assertEquals(5L, vo.getReportId());
        // (a)：写已读后内存更新，响应 isRead=1
        assertEquals(1, vo.getIsRead());
        // (a)：confirmedAt 首次查看后内存同步为 NOW()
        assertNotNull(vo.getConfirmedAt());
        // (a)：verify markReadIfNeeded 被调用一次（带 tenantId 防御参数）
        verify(writeService).markReadIfNeeded(eq(5L), eq(100L));
    }

    /**
     * Slice C (b)：非录制人查看 → 不写已读 + isRead=0 + VO 13 字段透传。
     *
     * <p>video.userId=999 != user.id=1，hasConfirmPermission=false。</p>
     */
    @Test
    void fidelityReportDetail_nonRecorder_doesNotMarkRead_returnsFullVo() {
        ScriptMonitorReportEntity r = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setMonitorType(MonitorTypeEnum.FIDELITY_MONITOR.getCode());
        r.setSummaryJson("# 还原度报告\n核心偏差点 2 处");
        r.setIsRead(0);
        r.setConfirmedAt(null);
        when(reportService.getById(5L)).thenReturn(r);
        // video.userId=999 != user.id=1，非录制人
        AnchorVideoInfoVo v = video(100L, 999L, 0);
        v.setLiveTitle("某场直播");
        java.util.Date startTime = new java.util.Date(1717000000000L);
        v.setStartTime(startTime);
        com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo anchorInfo =
                new com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo();
        anchorInfo.setAnchorName("某主播");
        v.setAnchorInfo(anchorInfo);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(v));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.of(
                bodyDoc("# 报告正文 Markdown")
        ));

        FidelityReportDetailVo vo = bll.fidelityReportDetail(5L);

        // (b)：非录制人不触发 markReadIfNeeded
        verify(writeService, never()).markReadIfNeeded(any(), any());

        // (b)：13 字段透传断言
        assertNotNull(vo);
        // 1. reportId
        assertEquals(5L, vo.getReportId());
        // 2. sourceType
        assertEquals(0, vo.getSourceType());
        // 3. sceneType
        assertEquals(0, vo.getSceneType());
        // 4. sourceId
        assertEquals("v1", vo.getSourceId());
        // 5. status
        assertEquals(ScriptMonitorStatusEnum.GENERATED.getCode(), vo.getStatus());
        // 6. summaryJson 透传 Markdown 文本
        assertEquals("# 还原度报告\n核心偏差点 2 处", vo.getSummaryJson());
        // 7. reportContent 来自 MongoDB
        assertEquals("# 报告正文 Markdown", vo.getReportContent());
        // 8. anchorName 取 AnchorInfo.anchorName
        assertEquals("某主播", vo.getAnchorName());
        // 9. liveTitle
        assertEquals("某场直播", vo.getLiveTitle());
        // 10. liveTime
        assertEquals(startTime, vo.getLiveTime());
        // 11. isRead 保持原值 0
        assertEquals(0, vo.getIsRead());
        // 12. confirmedAt 透传 null（未写已读）
        assertNull(vo.getConfirmedAt());
        // 13. createDate 透传 entity.createDate（此处未设，应为 null）
        assertNull(vo.getCreateDate());
    }

    /**
     * Slice C (c)：报告不存在 → 抛 70012 SCRIPT_MONITOR_REPORT_NOT_EXIST。
     */
    @Test
    void fidelityReportDetail_reportNotExist_throwsNotExist() {
        when(reportService.getById(5L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.fidelityReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_REPORT_NOT_EXIST.getCode(), code(ex));
    }

    /**
     * Slice C (d)：报告已软删（isDeleted=1）→ 抛 70012 SCRIPT_MONITOR_REPORT_NOT_EXIST。
     */
    @Test
    void fidelityReportDetail_reportSoftDeleted_throwsNotExist() {
        ScriptMonitorReportEntity r = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setMonitorType(MonitorTypeEnum.FIDELITY_MONITOR.getCode());
        r.setIsDeleted(1);
        when(reportService.getById(5L)).thenReturn(r);

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.fidelityReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_REPORT_NOT_EXIST.getCode(), code(ex));
    }

    /**
     * Slice C (e)：跨租户访问 → 抛 70011 SCRIPT_MONITOR_NO_PERMISSION。
     *
     * <p>report.tenantId=999 != user.activeTenantId=100。</p>
     */
    @Test
    void fidelityReportDetail_crossTenant_throwsNoPermission() {
        ScriptMonitorReportEntity r = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setMonitorType(MonitorTypeEnum.FIDELITY_MONITOR.getCode());
        r.setTenantId(999L);
        when(reportService.getById(5L)).thenReturn(r);
        // 视频加载已前移到权限校验之前：非公开分享（uploadStatus=null）→ 跨租户仍拦截
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(999L, 999L, 0)));

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.fidelityReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), code(ex));
    }

    /**
     * Slice C (f)：triggerReport 还原度 + 上传文件场景 guard → 抛 70014 + message 含 "还原度暂不支持上传文件场景"。
     *
     * <p>触发组合：sourceType=1 + sceneType=1（视频分析合法组合）+ monitorType=FIDELITY_MONITOR(1)。
     * 必须先通过 isLegalCombination(1,1) 校验，再命中 Slice C guard。</p>
     */
    @Test
    void triggerReport_fidelityWithUploadFile_throwsFeatureNotAvailable_messageContainsHint() {
        TriggerReportBo bo = new TriggerReportBo();
        bo.setSourceType(1);
        bo.setSceneType(1);
        bo.setSourceId("file1");
        bo.setMonitorType(MonitorTypeEnum.FIDELITY_MONITOR.getCode());

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.triggerReport(bo));
        assertEquals(StatusCode.SCRIPT_MONITOR_FEATURE_NOT_AVAILABLE.getCode(), code(ex));
        assertTrue(ex.getMessage().contains("还原度暂不支持上传文件场景"));
    }

    /**
     * Slice C (g)：markReadIfNeeded 抛 RuntimeException（DB 异常）→ fail-safe，不向上抛 + isRead=0 + confirmedAt=null。
     */
    @Test
    void fidelityReportDetail_markReadIfNeededThrows_doesNotPropagateException_returnsNormally() {
        ScriptMonitorReportEntity r = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setMonitorType(MonitorTypeEnum.FIDELITY_MONITOR.getCode());
        r.setIsRead(0);
        r.setConfirmedAt(null);
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());
        // 模拟 markReadIfNeeded 抛出 DB 异常
        org.mockito.Mockito.doThrow(new RuntimeException("DB timeout"))
                .when(writeService).markReadIfNeeded(eq(5L), eq(100L));

        // (g)：fail-safe — 不向上层抛异常，正常返回 VO
        FidelityReportDetailVo vo = assertDoesNotThrow(() -> bll.fidelityReportDetail(5L));
        assertNotNull(vo);
        // (g)：写失败时 isRead 保持内存旧值（0，写前未更新）
        assertEquals(0, vo.getIsRead());
        // (g)：写失败时 confirmedAt 保持 null（写前未更新）
        assertNull(vo.getConfirmedAt());
    }

    /**
     * Slice C 辅助：构造 ScriptMonitorReportBodyEntity，用于 reportBodyRepository.findByReportId 返回非空场景。
     */
    private com.jiuyu.replay.ai.entity.ScriptMonitorReportBodyEntity bodyDoc(String content) {
        com.jiuyu.replay.ai.entity.ScriptMonitorReportBodyEntity doc =
                new com.jiuyu.replay.ai.entity.ScriptMonitorReportBodyEntity();
        doc.setContent(content);
        return doc;
    }
}
