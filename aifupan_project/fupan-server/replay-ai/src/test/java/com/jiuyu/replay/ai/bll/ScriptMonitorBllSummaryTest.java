package com.jiuyu.replay.ai.bll;

import com.jiuyu.replay.ai.config.ScriptMonitorMqProperties;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorReportBodyRepository;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.ai.vo.QualityReportDetailVo;
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
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * ScriptMonitorBll 摘要装配单测（2026-06-02 summary-flatten 改造）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>AC-1: buildSummaryJson 直接返回 entity.summaryJson（GENERATED 报告）</li>
 *   <li>AC-2: buildSummaryJson 在 summaryJson=null 时返回 null</li>
 *   <li>AC-3: qualityReportDetail 装配 summaryJson 顶层字段（4 count 字段已删除）</li>
 * </ul>
 * </p>
 *
 * @author beta
 * @date 2026-06-02
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScriptMonitorBllSummaryTest {

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

    /**
     * 构造一条 GENERATED 状态的报告实体，summaryJson 由调用方传入。
     *
     * @param summaryJson 摘要 JSON 字符串（可为 null）
     * @return 报告行
     */
    private ScriptMonitorReportEntity generatedReport(String summaryJson) {
        ScriptMonitorReportEntity r = new ScriptMonitorReportEntity();
        r.setId(5L);
        r.setTenantId(100L);
        r.setSourceType(0);
        r.setSceneType(0);
        r.setSourceId("v1");
        r.setMonitorType(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        r.setStatus(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setSummaryJson(summaryJson);
        r.setIsDeleted(0);
        return r;
    }

    /**
     * 反射调用私有方法 buildSummaryJson(ScriptMonitorReportEntity)。
     *
     * @param report 报告实体
     * @return buildSummaryJson 返回值（String 或 null）
     */
    private String invokeBuildSummaryObject(ScriptMonitorReportEntity report) throws Exception {
        Method m = ScriptMonitorBll.class.getDeclaredMethod("buildSummaryJson", ScriptMonitorReportEntity.class);
        m.setAccessible(true);
        return (String) m.invoke(bll, report);
    }

    private AnchorVideoInfoVo video(Long tenantId, Long userId) {
        AnchorVideoInfoVo v = new AnchorVideoInfoVo();
        v.setVideoId("v1");
        v.setTenantId(tenantId);
        v.setUserId(userId);
        return v;
    }

    // ---------- AC-1: buildSummaryJson 直返 summaryJson ----------

    @Test
    @DisplayName("AC-1: buildSummaryJson 直接返回 entity.summaryJson（GENERATED 报告，无分支）")
    void should_return_summaryJson_when_buildSummaryJson_given_GENERATED_report() throws Exception {
        String expectedJson = "{\"crashCount\":2,\"slackCount\":1,\"brandDamageCount\":0,\"afterSalesCount\":3}";
        ScriptMonitorReportEntity report = generatedReport(expectedJson);

        String actual = invokeBuildSummaryObject(report);

        assertEquals(expectedJson, actual, "buildSummaryJson 应返回原始 summaryJson 字符串，不做任何 parse 或包装");
    }

    // ---------- AC-2: summaryJson=null → 返 null ----------

    @Test
    @DisplayName("AC-2: entity.summaryJson=null 时 buildSummaryJson 返回 null（不抛异常）")
    void should_return_null_when_buildSummaryJson_given_nullSummaryJson() throws Exception {
        ScriptMonitorReportEntity report = generatedReport(null);

        String actual = invokeBuildSummaryObject(report);

        assertNull(actual, "summaryJson=null 时 buildSummaryJson 应原样返回 null，不构造空对象");
    }

    // ---------- AC-3: qualityReportDetail 装配 summaryJson 顶层字段 ----------

    @Test
    @DisplayName("AC-3: qualityReportDetail 装配 summaryJson 顶层字段（4 count 字段已删除）")
    void should_populate_summaryJson_when_qualityReportDetail_given_GENERATED_report() {
        String expectedJson = "{\"crashCount\":2,\"slackCount\":1,\"brandDamageCount\":0,\"afterSalesCount\":3}";
        ScriptMonitorReportEntity report = generatedReport(expectedJson);
        when(reportService.getById(5L)).thenReturn(report);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L)));

        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());

        QualityReportDetailVo vo = bll.qualityReportDetail(5L);

        assertNotNull(vo, "qualityReportDetail 不应返回 null");
        // summaryJson 顶层字段透传 entity.summaryJson 原文
        assertEquals(expectedJson, vo.getSummaryJson(),
                "QualityReportDetailVo.summaryJson 应原样透传 entity.summaryJson");
        // 关键回归断言：旧的 4 个顶层 count 字段在 VO 上不存在
        // （由 §3.3 API Contract 保证：crashCount/slackCount/brandDamageCount/afterSalesCount 已删除）
        // 编译期保证：若有人重新添加这些字段，此测试不会自动发现，需配合 §7 AC-007 的 mvn compile 全模块零编译错误。
    }

    @Test
    @DisplayName("AC-3 边界: summaryJson=null 时 qualityReportDetail.summaryJson 返 null")
    void should_return_nullSummaryJson_when_qualityReportDetail_given_nullSummaryJson() {
        ScriptMonitorReportEntity report = generatedReport(null);
        when(reportService.getById(5L)).thenReturn(report);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());

        QualityReportDetailVo vo = bll.qualityReportDetail(5L);

        assertNotNull(vo);
        assertNull(vo.getSummaryJson(), "entity.summaryJson=null 时 VO.summaryJson 应保持 null");
    }
}
