package com.jiuyu.replay.ai.bll;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.jiuyu.replay.ai.bo.BatchReportStatusBo;
import com.jiuyu.replay.ai.bo.ReportStatusBo;
import com.jiuyu.replay.ai.config.ScriptMonitorMqProperties;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorReportBodyRepository;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.ai.vo.MonitorTypeStatusVo;
import com.jiuyu.replay.ai.vo.ScriptMonitorReportStatusVo;
import com.jiuyu.replay.common.bll.RocketMqBll;
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
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ScriptMonitorBll reportStatus / batchReportStatus 端到端单测（2026-06-02 summary-flatten 改造）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>AC-4: reportStatus 装配 summary 字段（String 透传）+ monitorEnabled（secUid 命中）</li>
 *   <li>AC-5: 非 GENERATED 状态时 buildMonitorStatus 不写 summary（保持 null）</li>
 *   <li>AC-6: batchReportStatus 多 source 装配 summary + monitorEnabled</li>
 * </ul>
 * </p>
 *
 * @author beta
 * @date 2026-06-02
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScriptMonitorBllReportStatusTest {

    @Mock
    private ScriptMonitorReportService reportService;
    @Mock
    private ScriptMonitorReportWriteService writeService;
    @Mock
    private ScriptMonitorReportBodyRepository reportBodyRepository;
    @Mock
    private AnchorUrlUserFeign anchorUrlUserFeign;
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
    }

    // ---------- helpers ----------

    private AnchorVideoInfoVo video(Long tenantId, Long userId) {
        AnchorVideoInfoVo v = new AnchorVideoInfoVo();
        v.setVideoId("v1");
        v.setTenantId(tenantId);
        v.setUserId(userId);
        return v;
    }

    /**
     * 构造一条 GENERATED 状态的质检报告。
     *
     * @param sourceId    资源 ID
     * @param summaryJson 摘要 JSON 字符串
     * @return 报告行
     */
    private ScriptMonitorReportEntity generatedQualityReport(String sourceId, String summaryJson) {
        ScriptMonitorReportEntity r = new ScriptMonitorReportEntity();
        r.setId(5L);
        r.setTenantId(100L);
        r.setSourceType(0);
        r.setSceneType(0);
        r.setSourceId(sourceId);
        r.setMonitorType(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        r.setStatus(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setSummaryJson(summaryJson);
        r.setIsDeleted(0);
        return r;
    }

    /**
     * 构造一条指定状态的质检报告。
     *
     * @param sourceId 资源 ID
     * @param status   报告状态
     * @return 报告行
     */
    private ScriptMonitorReportEntity reportWithStatus(String sourceId, Integer status) {
        ScriptMonitorReportEntity r = new ScriptMonitorReportEntity();
        r.setId(6L);
        r.setTenantId(100L);
        r.setSourceType(0);
        r.setSceneType(0);
        r.setSourceId(sourceId);
        r.setMonitorType(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        r.setStatus(status);
        // 即使非 GENERATED 也填一段 summaryJson，验证门控逻辑（应被 buildMonitorStatus 过滤为 null）
        r.setSummaryJson("{\"crashCount\":99}");
        r.setIsDeleted(0);
        return r;
    }

    /**
     * 构造 AnchorUrlUserVo（三开关）。
     *
     * @param secUid                  主播唯一标识
     * @param scriptQualityInspection 话术质检开关
     * @param scriptFidelityMonitor   还原度开关
     * @param interactionPatrol       巡检开关
     * @return AnchorUrlUserVo
     */
    private AnchorUrlUserVo anchorUrlUserVo(String secUid, Integer scriptQualityInspection,
                                            Integer scriptFidelityMonitor, Integer interactionPatrol) {
        AnchorUrlUserVo vo = new AnchorUrlUserVo();
        vo.setAnchorUrlSecUid(secUid);
        vo.setIsScriptQualityInspection(scriptQualityInspection);
        vo.setIsScriptFidelityMonitor(scriptFidelityMonitor);
        vo.setIsInteractionPatrol(interactionPatrol);
        return vo;
    }

    private ReportStatusBo statusBo(Integer sourceType, Integer sceneType, String sourceId, String secUid) {
        ReportStatusBo bo = new ReportStatusBo();
        bo.setSourceType(sourceType);
        bo.setSceneType(sceneType);
        bo.setSourceId(sourceId);
        bo.setSecUid(secUid);
        return bo;
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

    /**
     * 按 monitorType 在响应里找对应的 MonitorTypeStatusVo。
     *
     * @param vo          响应
     * @param monitorType 监控类型
     * @return MonitorTypeStatusVo
     */
    private MonitorTypeStatusVo pickMonitor(ScriptMonitorReportStatusVo vo, MonitorTypeEnum monitorType) {
        return vo.getMonitors().stream()
                .filter(m -> Objects.equals(m.getMonitorType(), monitorType.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 monitorType=" + monitorType.getCode()));
    }

    // ---------- AC-4: reportStatus 端到端装配 summary + monitorEnabled ----------

    @Test
    @DisplayName("AC-4: reportStatus 带 secUid，monitors[质检].summary 为 String，monitorEnabled 装配三开关")
    void should_populate_monitorEnabled_and_summaryJson_when_reportStatus_given_secUid_andRecord() {
        String secUid = "sec_abc";
        String expectedSummary = "{\"crashCount\":2,\"slackCount\":1,\"brandDamageCount\":0,\"afterSalesCount\":3}";

        // 1. 视频权限校验通过
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L)));

        // 2. 报告查询返回一条 GENERATED 质检报告
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> rq = mockReportQuery();
        when(rq.list()).thenReturn(List.of(generatedQualityReport("v1", expectedSummary)));

        // 3. anchorUrlUserFeign 返回三开关命中
        when(anchorUrlUserFeign.listBySecUidsAndUser(any(), anyLong(), anyLong()))
                .thenReturn(R.ok(List.of(anchorUrlUserVo(secUid, 1, 0, 1))));

        ScriptMonitorReportStatusVo vo = bll.reportStatus(statusBo(0, 0, "v1", secUid));

        assertNotNull(vo);
        assertEquals(3, vo.getMonitors().size(), "monitors 固定 3 项");

        MonitorTypeStatusVo qualityMonitor = pickMonitor(vo, MonitorTypeEnum.QUALITY_INSPECTION);
        // 关键断言: summary 是 String（不是 Object）且与 entity.summaryJson 原文一致
        assertEquals(expectedSummary, qualityMonitor.getSummary(),
                "monitors[质检].summary 应为 entity.summaryJson 原始 String");
        assertEquals(Integer.valueOf(1), qualityMonitor.getMonitorEnabled(),
                "monitors[质检].monitorEnabled 应来自 isScriptQualityInspection");
        assertEquals(ScriptMonitorStatusEnum.GENERATED.getCode(), qualityMonitor.getStatus());

        MonitorTypeStatusVo fidelityMonitor = pickMonitor(vo, MonitorTypeEnum.FIDELITY_MONITOR);
        assertEquals(Integer.valueOf(0), fidelityMonitor.getMonitorEnabled(),
                "monitors[还原度].monitorEnabled 应来自 isScriptFidelityMonitor");
        assertNull(fidelityMonitor.getSummary(), "还原度无报告时 summary 应为 null");

        MonitorTypeStatusVo patrolMonitor = pickMonitor(vo, MonitorTypeEnum.INTERACTION_PATROL);
        assertEquals(Integer.valueOf(1), patrolMonitor.getMonitorEnabled(),
                "monitors[巡检].monitorEnabled 应来自 isInteractionPatrol");
        assertNull(patrolMonitor.getSummary(), "巡检无报告时 summary 应为 null");
    }

    // ---------- AC-5: status 门控 — 非 GENERATED 状态 summary 返 null ----------

    @Test
    @DisplayName("AC-5: 非 GENERATED 状态时 buildMonitorStatus 返 summary=null（status 门控）")
    void should_return_nullSummary_when_status_not_GENERATED() {
        // 报告处于 GENERATING（生成中），即使 entity.summaryJson 有值也应被门控为 null
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L)));
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> rq = mockReportQuery();
        when(rq.list()).thenReturn(List.of(reportWithStatus("v1", ScriptMonitorStatusEnum.GENERATING.getCode())));

        ScriptMonitorReportStatusVo vo = bll.reportStatus(statusBo(0, 0, "v1", null));

        MonitorTypeStatusVo qualityMonitor = pickMonitor(vo, MonitorTypeEnum.QUALITY_INSPECTION);
        assertEquals(ScriptMonitorStatusEnum.GENERATING.getCode(), qualityMonitor.getStatus());
        assertNull(qualityMonitor.getSummary(),
                "非 GENERATED 状态时 summary 必须为 null，即使 entity.summaryJson 有值也不返回");
    }

    @Test
    @DisplayName("AC-5 边界: 未传 secUid 时 monitorEnabled 全为 null（不查 anchor_url_user）")
    void should_return_nullMonitorEnabled_when_reportStatus_given_no_secUid() {
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L)));
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> rq = mockReportQuery();
        when(rq.list()).thenReturn(new ArrayList<>());

        ScriptMonitorReportStatusVo vo = bll.reportStatus(statusBo(0, 0, "v1", null));

        assertNotNull(vo);
        assertTrue(vo.getMonitors().stream().allMatch(m -> m.getMonitorEnabled() == null),
                "未传 secUid 时 monitorEnabled 应全为 null（业务语义：不适用）");
    }

    // ---------- B9 AC-003: reportStatus monitors[].isRead 取自报告主表 is_read 字段 ----------

    @Test
    @DisplayName("AC-003: reportStatus 响应 monitors[质检].isRead 来源 report.getIsRead()（非旧 tb_script_monitor_read）")
    void should_populate_isRead_from_report_entity_when_reportStatus() {
        // given: report.is_read=1 in main table (代替旧 tb_script_monitor_read 子表查询)
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L)));
        ScriptMonitorReportEntity report = generatedQualityReport("v1", "{\"crashCount\":0}");
        report.setIsRead(1);
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> rq = mockReportQuery();
        when(rq.list()).thenReturn(List.of(report));

        ScriptMonitorReportStatusVo vo = bll.reportStatus(statusBo(0, 0, "v1", null));

        // AC-003: monitors[质检].isRead 必须与 report.isRead 一致（来源主表字段）
        MonitorTypeStatusVo qualityMonitor = pickMonitor(vo, MonitorTypeEnum.QUALITY_INSPECTION);
        assertEquals(Integer.valueOf(1), qualityMonitor.getIsRead(),
                "monitors[质检].isRead 应取自 report.getIsRead() 主表字段");
        // 无报告的还原度/巡检在 report=null 分支走，isRead 默认 0
        MonitorTypeStatusVo fidelityMonitor = pickMonitor(vo, MonitorTypeEnum.FIDELITY_MONITOR);
        assertEquals(Integer.valueOf(0), fidelityMonitor.getIsRead(), "report=null 时 isRead 默认 0");
    }

    @Test
    @DisplayName("AC-003 边界: report.isRead=null 时 monitors[].isRead 兜底为 0")
    void should_default_isRead_to_zero_when_report_isRead_null() {
        // given: report.is_read=null（历史脏数据/未 backfill 场景，兜底逻辑 report.getIsRead() != null ? : 0）
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L)));
        ScriptMonitorReportEntity report = generatedQualityReport("v1", "{\"crashCount\":0}");
        report.setIsRead(null);
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> rq = mockReportQuery();
        when(rq.list()).thenReturn(List.of(report));

        ScriptMonitorReportStatusVo vo = bll.reportStatus(statusBo(0, 0, "v1", null));

        MonitorTypeStatusVo qualityMonitor = pickMonitor(vo, MonitorTypeEnum.QUALITY_INSPECTION);
        assertEquals(Integer.valueOf(0), qualityMonitor.getIsRead(),
                "report.isRead=null 时 monitors[质检].isRead 应兜底为 0（NPE 防御）");
    }

    // ---------- AC-6: batchReportStatus 多 source 装配 ----------

    @Test
    @DisplayName("AC-6: batchReportStatus 多 source 装配 secUid + summary（String 透传）")
    void should_handle_batch_with_secUid_and_summary() {
        String secUidA = "sec_a";
        String secUidB = "sec_b";
        String summaryA = "{\"crashCount\":3}";

        BatchReportStatusBo bo = new BatchReportStatusBo();
        List<BatchReportStatusBo.SourceItemBo> items = new ArrayList<>();

        BatchReportStatusBo.SourceItemBo s1 = new BatchReportStatusBo.SourceItemBo();
        s1.setSourceType(0);
        s1.setSceneType(0);
        s1.setSourceId("v1");
        s1.setSecUid(secUidA);
        items.add(s1);

        BatchReportStatusBo.SourceItemBo s2 = new BatchReportStatusBo.SourceItemBo();
        s2.setSourceType(0);
        s2.setSceneType(0);
        s2.setSourceId("v2");
        s2.setSecUid(secUidB);
        items.add(s2);

        bo.setSources(items);

        // listByVideoIds 批量加载视频
        when(anchorVideoFeign.listByVideoIds(any()))
                .thenReturn(R.ok(List.of(video(100L, 1L), buildVideo("v2", 100L, 1L))));

        // 批量报告查询：v1 有质检报告，v2 无报告
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> rq = mockReportQuery();
        when(rq.list()).thenReturn(List.of(generatedQualityReport("v1", summaryA)));

        // anchor_url_user 命中两条 secUid（v1 质检开，v2 质检关）
        when(anchorUrlUserFeign.listBySecUidsAndUser(any(), anyLong(), anyLong()))
                .thenReturn(R.ok(List.of(
                        anchorUrlUserVo(secUidA, 1, 0, 0),
                        anchorUrlUserVo(secUidB, 0, 0, 1)
                )));

        List<ScriptMonitorReportStatusVo> result = bll.batchReportStatus(bo);

        assertEquals(2, result.size(), "两个 source 都应在结果中");

        // v1: 有质检报告 + 质检开关=1
        ScriptMonitorReportStatusVo v1Result = result.stream()
                .filter(r -> "v1".equals(r.getSourceId())).findFirst().orElseThrow();
        MonitorTypeStatusVo v1Quality = pickMonitor(v1Result, MonitorTypeEnum.QUALITY_INSPECTION);
        assertEquals(summaryA, v1Quality.getSummary(), "v1 质检 summary 应为 entity.summaryJson 原文");
        assertEquals(Integer.valueOf(1), v1Quality.getMonitorEnabled());

        // v2: 无报告 + 巡检开关=1
        ScriptMonitorReportStatusVo v2Result = result.stream()
                .filter(r -> "v2".equals(r.getSourceId())).findFirst().orElseThrow();
        MonitorTypeStatusVo v2Quality = pickMonitor(v2Result, MonitorTypeEnum.QUALITY_INSPECTION);
        assertNull(v2Quality.getSummary(), "v2 无报告 summary 应为 null");
        assertEquals(Integer.valueOf(0), v2Quality.getMonitorEnabled(), "v2 质检开关=0");
        MonitorTypeStatusVo v2Patrol = pickMonitor(v2Result, MonitorTypeEnum.INTERACTION_PATROL);
        assertEquals(Integer.valueOf(1), v2Patrol.getMonitorEnabled(), "v2 巡检开关=1");
    }

    /**
     * 构造另一个视频实例（便于 batch 测试）。
     */
    private AnchorVideoInfoVo buildVideo(String videoId, Long tenantId, Long userId) {
        AnchorVideoInfoVo v = new AnchorVideoInfoVo();
        v.setVideoId(videoId);
        v.setTenantId(tenantId);
        v.setUserId(userId);
        return v;
    }
}
