package com.jiuyu.replay.ai.bll;

import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.ai.bo.ScriptMonitorTriggerMsg;
import com.jiuyu.replay.ai.config.ScriptMonitorMqProperties;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.generic.enums.words.ScriptMonitorStatusEnum;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.RedisWithholdVo;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScriptMonitorMqHandler 单测（B4 修补包-3 MQ 接入）。
 *
 * <p>覆盖：happy / 幂等跳过 / 反序列化失败 / 业务异常返 SUCCESS / 系统异常返 FAILURE。</p>
 *
 * @author beta
 * @date 2026-06-01
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScriptMonitorMqHandlerTest {

    private static final String QUALITY_TAG = "Tag_Quality_Inspection";
    private static final String FIDELITY_TAG = "Tag_Fidelity_Monitor";
    private static final String MSG_KEY = "script_monitor_report_42";

    @Mock
    private ScriptMonitorReportService reportService;
    @Mock
    private ScriptMonitorGenerateBll generateBll;
    @Mock
    private ScriptMonitorFidelityGenerateBll fidelityGenerateBll;
    @Mock
    private ScriptMonitorPatrolGenerateBll patrolGenerateBll;
    @Mock
    private ScriptMonitorMqProperties mqProperties;
    @InjectMocks
    private ScriptMonitorMqHandler handler;

    @BeforeEach
    void setUp() {
        ScriptMonitorMqProperties.BusinessTags tags = new ScriptMonitorMqProperties.BusinessTags();
        tags.setTagQualityInspection(QUALITY_TAG);
        tags.setTagFidelityMonitor(FIDELITY_TAG);
        when(mqProperties.getBusinessTags()).thenReturn(tags);
    }

    @Test
    @DisplayName("happy: status=1 → 调 generate 返 SUCCESS")
    void happy_callsGenerate_returnsSuccess() {
        ScriptMonitorReportEntity report = newReport(42L, ScriptMonitorStatusEnum.GENERATING.getCode());
        when(reportService.getById(42L)).thenReturn(report);
        doNothing().when(generateBll).generate(any(), any(), any());

        ConsumeResult result = handler.handle(QUALITY_TAG, buildBody(42L), MSG_KEY);

        assertEquals(ConsumeResult.SUCCESS, result);
        verify(generateBll).generate(any(), any(), any());
    }

    @Test
    @DisplayName("幂等: status=2 GENERATED → 跳过返 SUCCESS")
    void status_generated_skipsAndReturnsSuccess() {
        ScriptMonitorReportEntity report = newReport(42L, ScriptMonitorStatusEnum.GENERATED.getCode());
        when(reportService.getById(42L)).thenReturn(report);

        ConsumeResult result = handler.handle(QUALITY_TAG, buildBody(42L), MSG_KEY);

        assertEquals(ConsumeResult.SUCCESS, result);
        verify(generateBll, never()).generate(any(), any(), any());
    }

    @Test
    @DisplayName("幂等: status=4 NOT_APPLICABLE → 跳过返 SUCCESS")
    void status_notApplicable_skipsAndReturnsSuccess() {
        ScriptMonitorReportEntity report = newReport(42L, ScriptMonitorStatusEnum.NOT_APPLICABLE.getCode());
        when(reportService.getById(42L)).thenReturn(report);

        ConsumeResult result = handler.handle(QUALITY_TAG, buildBody(42L), MSG_KEY);

        assertEquals(ConsumeResult.SUCCESS, result);
        verify(generateBll, never()).generate(any(), any(), any());
    }

    @Test
    @DisplayName("反序列化失败 → 返 SUCCESS（不重试）")
    void invalidBody_returnsSuccess() {
        ConsumeResult result = handler.handle(QUALITY_TAG, "{invalid json", MSG_KEY);
        assertEquals(ConsumeResult.SUCCESS, result);
        verify(generateBll, never()).generate(any(), any(), any());
    }

    @Test
    @DisplayName("报告不存在 → 返 SUCCESS（不重试）")
    void reportNotFound_returnsSuccess() {
        when(reportService.getById(anyLong())).thenReturn(null);
        ConsumeResult result = handler.handle(QUALITY_TAG, buildBody(42L), MSG_KEY);
        assertEquals(ConsumeResult.SUCCESS, result);
    }

    /**
     * Slice B AC-003 配套：fidelity tag → 派发 FidelityGenerateBll.generate（不再 skip 返 SUCCESS）。
     *
     * <p>关键回归：原"B3 预留未实现"路径已删除（Slice B 目标之一），fidelity tag 现在走 doFidelityGenerate。</p>
     */
    @Test
    @DisplayName("Slice B: 还原度 tag → 调 fidelityGenerateBll.generate 返 SUCCESS")
    void fidelityTag_callsFidelityGenerateBll_returnsSuccess() {
        ScriptMonitorReportEntity report = newReport(42L, ScriptMonitorStatusEnum.GENERATING.getCode());
        when(reportService.getById(42L)).thenReturn(report);
        doNothing().when(fidelityGenerateBll).generate(any(), any(), any());

        ConsumeResult result = handler.handle(FIDELITY_TAG, buildBody(42L), MSG_KEY);

        assertEquals(ConsumeResult.SUCCESS, result);
        // 关键断言：fidelityGenerateBll.generate 被调一次（不再 skip）
        verify(fidelityGenerateBll, times(1)).generate(any(), any(), any());
        // 反向断言：质检 / 巡检 generate 不被调（tag 路由隔离）
        verify(generateBll, never()).generate(any(), any(), any());
        verify(patrolGenerateBll, never()).generate(any(), any(), any());
    }

    /**
     * Slice B：fidelity tag 下 fidelityGenerateBll 抛 BusinessException → 已 catch 补偿，返 SUCCESS（不重试）。
     */
    @Test
    @DisplayName("Slice B: 还原度 generate 抛 BusinessException → 返 SUCCESS（已补偿不重试）")
    void fidelityTag_generateBusinessException_returnsSuccess() {
        ScriptMonitorReportEntity report = newReport(42L, ScriptMonitorStatusEnum.GENERATING.getCode());
        when(reportService.getById(42L)).thenReturn(report);
        doThrow(new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), "fidelity biz fail"))
                .when(fidelityGenerateBll).generate(any(), any(), any());

        ConsumeResult result = handler.handle(FIDELITY_TAG, buildBody(42L), MSG_KEY);

        assertEquals(ConsumeResult.SUCCESS, result);
    }

    /**
     * Slice B：fidelity tag 下 fidelityGenerateBll 抛系统异常 → 返 FAILURE（broker 重试）。
     */
    @Test
    @DisplayName("Slice B: 还原度 generate 抛系统异常 → 返 FAILURE（broker 重试）")
    void fidelityTag_generateSystemException_returnsFailure() {
        ScriptMonitorReportEntity report = newReport(42L, ScriptMonitorStatusEnum.GENERATING.getCode());
        when(reportService.getById(42L)).thenReturn(report);
        doThrow(new RuntimeException("DB connection lost"))
                .when(fidelityGenerateBll).generate(any(), any(), any());

        ConsumeResult result = handler.handle(FIDELITY_TAG, buildBody(42L), MSG_KEY);

        assertEquals(ConsumeResult.FAILURE, result);
    }

    @Test
    @DisplayName("generate 抛 BusinessException → 返 SUCCESS（已 catch 补偿，不重试）")
    void generate_businessException_returnsSuccess() {
        ScriptMonitorReportEntity report = newReport(42L, ScriptMonitorStatusEnum.GENERATING.getCode());
        when(reportService.getById(42L)).thenReturn(report);
        doThrow(new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(), "biz fail"))
                .when(generateBll).generate(any(), any(), any());

        ConsumeResult result = handler.handle(QUALITY_TAG, buildBody(42L), MSG_KEY);
        assertEquals(ConsumeResult.SUCCESS, result);
    }

    @Test
    @DisplayName("generate 抛系统异常 → 返 FAILURE（broker 重试）")
    void generate_systemException_returnsFailure() {
        ScriptMonitorReportEntity report = newReport(42L, ScriptMonitorStatusEnum.GENERATING.getCode());
        when(reportService.getById(42L)).thenReturn(report);
        doThrow(new RuntimeException("DB connection lost"))
                .when(generateBll).generate(any(), any(), any());

        ConsumeResult result = handler.handle(QUALITY_TAG, buildBody(42L), MSG_KEY);
        assertEquals(ConsumeResult.FAILURE, result);
    }

    // ---------- 辅助 ----------

    private ScriptMonitorReportEntity newReport(Long id, Integer status) {
        ScriptMonitorReportEntity e = new ScriptMonitorReportEntity();
        e.setId(id);
        e.setStatus(status);
        return e;
    }

    private String buildBody(Long reportId) {
        ScriptMonitorTriggerMsg msg = ScriptMonitorTriggerMsg.builder()
                .reportId(reportId)
                .originalStatus(0)
                .withhold(new RedisWithholdVo())
                .build();
        return JSON.toJSONString(msg);
    }
}
