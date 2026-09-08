package com.jiuyu.replay.ai.bll.impl;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.generic.enums.words.ScriptMonitorStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScriptMonitorReportWriteServiceImpl 单元测试。
 *
 * <p>核心保护：</p>
 * <ol>
 *   <li>entity 反射断言 — {@code unavailableReason} / {@code summaryJson} 必须带
 *       {@code @TableField(updateStrategy = FieldStrategy.ALWAYS)}，否则
 *       MyBatis-Plus 默认 NOT_NULL 策略会静默丢弃 setNull，导致
 *       "status=2 + unavailable_reason 残留" 同类状态机字段残留 bug 复活。</li>
 *   <li>service 调用层 — {@code finishReport} 必须在 update 时把
 *       {@code unavailableReason} / {@code summaryJson} 一同写入（即使为 null），
 *       防止后续重构忘记清旧值。</li>
 * </ol>
 *
 * <p>实际 SQL 层（"DB 行真被 UPDATE ... unavailable_reason = NULL"）的端到端验证
 * 走手工 verify SQL，由 QA 在测试环境复跑（trigger 已残留行 → 查 DB 字段变化）。</p>
 *
 * @author beta
 * @date 2026-06-07
 */
@ExtendWith(MockitoExtension.class)
class ScriptMonitorReportWriteServiceImplTest {

    @Mock
    private ScriptMonitorReportService reportService;

    @InjectMocks
    private ScriptMonitorReportWriteServiceImpl writeService;

    @Test
    @DisplayName("entity 反射 — unavailableReason 必须带 @TableField(updateStrategy=ALWAYS)")
    void unavailableReason_shouldHaveAlwaysUpdateStrategy() throws NoSuchFieldException {
        Field field = ScriptMonitorReportEntity.class.getDeclaredField("unavailableReason");
        TableField annotation = field.getAnnotation(TableField.class);
        assertNotNull(annotation, "unavailableReason 必须显式标注 @TableField，否则 setNull 被默认 NOT_NULL 策略吃掉");
        assertEquals(FieldStrategy.ALWAYS, annotation.updateStrategy(),
                "unavailableReason updateStrategy 必须是 ALWAYS，让 setNull 真正进 UPDATE SQL");
    }

    @Test
    @DisplayName("entity 反射 — summaryJson 必须带 @TableField(updateStrategy=ALWAYS)")
    void summaryJson_shouldHaveAlwaysUpdateStrategy() throws NoSuchFieldException {
        Field field = ScriptMonitorReportEntity.class.getDeclaredField("summaryJson");
        TableField annotation = field.getAnnotation(TableField.class);
        assertNotNull(annotation, "summaryJson 必须显式标注 @TableField，否则 setNull 被默认 NOT_NULL 策略吃掉");
        assertEquals(FieldStrategy.ALWAYS, annotation.updateStrategy(),
                "summaryJson updateStrategy 必须是 ALWAYS，让 setNull 真正进 UPDATE SQL");
    }

    @Test
    @DisplayName("entity 反射 — reportBodyId 必须带 @TableField(updateStrategy=ALWAYS)（与 summaryJson 一致性）")
    void reportBodyId_shouldHaveAlwaysUpdateStrategy() throws NoSuchFieldException {
        Field field = ScriptMonitorReportEntity.class.getDeclaredField("reportBodyId");
        TableField annotation = field.getAnnotation(TableField.class);
        assertNotNull(annotation, "reportBodyId 必须显式标注 @TableField，避免清 summaryJson 但留旧正文指针的脏字段");
        assertEquals(FieldStrategy.ALWAYS, annotation.updateStrategy(),
                "reportBodyId updateStrategy 必须是 ALWAYS，跟 summaryJson 同步清空 / 写入");
    }

    @Test
    @DisplayName("finishReport — UPDATE entity 的 unavailableReason 写 null + summaryJson 透传")
    void finishReport_shouldSetUnavailableReasonNull_andTransparentSummary() {
        when(reportService.updateById(any(ScriptMonitorReportEntity.class))).thenReturn(true);

        writeService.finishReport(1001L, "mongo-oid-xyz", "崩盘 0 处...");

        ArgumentCaptor<ScriptMonitorReportEntity> captor = ArgumentCaptor.forClass(ScriptMonitorReportEntity.class);
        verify(reportService).updateById(captor.capture());
        ScriptMonitorReportEntity actual = captor.getValue();
        assertEquals(1001L, actual.getId());
        assertEquals(ScriptMonitorStatusEnum.GENERATED.getCode(), actual.getStatus());
        assertEquals("mongo-oid-xyz", actual.getReportBodyId());
        assertEquals("崩盘 0 处...", actual.getSummaryJson());
        assertNull(actual.getUnavailableReason(),
                "finishReport 必须显式 setUnavailableReason(null)，依赖 entity 注解让 null 真进 SQL");
        assertNotNull(actual.getUpdateDate());
    }

    @Test
    @DisplayName("finishReport — summaryJson 为 null 时也透传 null（AI 无 <aifupan-data-block> 标签场景）")
    void finishReport_shouldPassNullSummary_whenAiTagMissing() {
        when(reportService.updateById(any(ScriptMonitorReportEntity.class))).thenReturn(true);

        writeService.finishReport(2002L, "mongo-oid-abc", null);

        ArgumentCaptor<ScriptMonitorReportEntity> captor = ArgumentCaptor.forClass(ScriptMonitorReportEntity.class);
        verify(reportService).updateById(captor.capture());
        ScriptMonitorReportEntity actual = captor.getValue();
        assertNull(actual.getSummaryJson(),
                "summaryJson=null 必须透传，依赖 entity 注解清空 DB 旧摘要");
        assertNull(actual.getUnavailableReason());
    }

    @Test
    @DisplayName("restoreOrFail — GENERATED 恢复分支：setUnavailableReason(null) 透传")
    void restoreOrFail_generatedBranch_shouldSetUnavailableReasonNull() {
        when(reportService.updateById(any(ScriptMonitorReportEntity.class))).thenReturn(true);

        writeService.restoreOrFail(3003L,
                ScriptMonitorStatusEnum.GENERATED.getCode(),
                "old-mongo-oid", "old-summary", "AI 服务超时");

        ArgumentCaptor<ScriptMonitorReportEntity> captor = ArgumentCaptor.forClass(ScriptMonitorReportEntity.class);
        verify(reportService).updateById(captor.capture());
        ScriptMonitorReportEntity actual = captor.getValue();
        assertEquals(ScriptMonitorStatusEnum.GENERATED.getCode(), actual.getStatus());
        assertEquals("old-mongo-oid", actual.getReportBodyId());
        assertEquals("old-summary", actual.getSummaryJson());
        assertNull(actual.getUnavailableReason(),
                "GENERATED 恢复分支必须清旧 unavailableReason（D-7 失败补偿）");
    }

    @Test
    @DisplayName("restoreOrFail — 非 GENERATED 分支：unavailableReason 写错误信息（非 null）")
    void restoreOrFail_failedBranch_shouldSetUnavailableReason() {
        when(reportService.updateById(any(ScriptMonitorReportEntity.class))).thenReturn(true);

        writeService.restoreOrFail(4004L,
                ScriptMonitorStatusEnum.NOT_GENERATED.getCode(),
                null, null, "AI 调用 5xx");

        ArgumentCaptor<ScriptMonitorReportEntity> captor = ArgumentCaptor.forClass(ScriptMonitorReportEntity.class);
        verify(reportService).updateById(captor.capture());
        ScriptMonitorReportEntity actual = captor.getValue();
        assertEquals(ScriptMonitorStatusEnum.GENERATE_FAILED.getCode(), actual.getStatus());
        assertEquals("AI 调用 5xx", actual.getUnavailableReason());
    }

    @Test
    @DisplayName("restoreOrFail — FAILED 分支 errorMessage 超 200 字符 → 截断到 200")
    void restoreOrFail_failedBranch_shouldTruncateLongErrorMessage() {
        when(reportService.updateById(any(ScriptMonitorReportEntity.class))).thenReturn(true);
        // 构造 250 字符的超长错误信息
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 250; i++) {
            sb.append('x');
        }
        String longError = sb.toString();

        writeService.restoreOrFail(5005L,
                ScriptMonitorStatusEnum.NOT_GENERATED.getCode(),
                null, null, longError);

        ArgumentCaptor<ScriptMonitorReportEntity> captor = ArgumentCaptor.forClass(ScriptMonitorReportEntity.class);
        verify(reportService).updateById(captor.capture());
        String actual = captor.getValue().getUnavailableReason();
        assertEquals(200, actual.length(),
                "errorMessage 超 200 字符必须截断到 200，防止超出 DB unavailable_reason 字段长度");
    }

    @Test
    @DisplayName("restoreOrFail — FAILED 分支 errorMessage=null → unavailableReason 透传 null")
    void restoreOrFail_failedBranch_shouldPassNullErrorMessage() {
        when(reportService.updateById(any(ScriptMonitorReportEntity.class))).thenReturn(true);

        writeService.restoreOrFail(6006L,
                ScriptMonitorStatusEnum.NOT_GENERATED.getCode(),
                null, null, null);

        ArgumentCaptor<ScriptMonitorReportEntity> captor = ArgumentCaptor.forClass(ScriptMonitorReportEntity.class);
        verify(reportService).updateById(captor.capture());
        ScriptMonitorReportEntity actual = captor.getValue();
        assertEquals(ScriptMonitorStatusEnum.GENERATE_FAILED.getCode(), actual.getStatus());
        assertNull(actual.getUnavailableReason(),
                "errorMessage=null 时 unavailableReason 应保持 null（防御 NPE + substring 边界）");
    }

    @Test
    @DisplayName("markNotApplicable — status 置 NOT_APPLICABLE + unavailableReason 写原因")
    void markNotApplicable_shouldSetStatusAndReason() {
        when(reportService.updateById(any(ScriptMonitorReportEntity.class))).thenReturn(true);

        writeService.markNotApplicable(7007L, "ASR 数据为空，无法生成质检报告");

        ArgumentCaptor<ScriptMonitorReportEntity> captor = ArgumentCaptor.forClass(ScriptMonitorReportEntity.class);
        verify(reportService).updateById(captor.capture());
        ScriptMonitorReportEntity actual = captor.getValue();
        assertEquals(7007L, actual.getId());
        assertEquals(ScriptMonitorStatusEnum.NOT_APPLICABLE.getCode(), actual.getStatus());
        assertEquals("ASR 数据为空，无法生成质检报告", actual.getUnavailableReason());
        assertNotNull(actual.getUpdateDate());
    }
}
