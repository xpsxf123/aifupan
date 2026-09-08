package com.jiuyu.replay.ai.bll.impl;

import com.jiuyu.replay.ai.bll.ScriptMonitorReportWriteService;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.generic.enums.words.ScriptMonitorStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

// TODO(B9): 若后续引入 micrometer，此处注入 Counter("script_monitor.mark_read.fail") 并在 catch 块 increment

/**
 * AI 监控报告状态写入 Service 实现（C3 修补：独立 Spring Bean，确保 @Transactional AOP 代理正常介入）。
 *
 * <p>三个方法全部加 @Transactional，供 {@link com.jiuyu.replay.ai.bll.ScriptMonitorGenerateBll}
 * 通过 Spring 注入后调用，解决 self-call 导致的事务失效问题。</p>
 *
 * @author beta
 * @date 2026-05-30
 */
@Slf4j
@Service
public class ScriptMonitorReportWriteServiceImpl implements ScriptMonitorReportWriteService {

    private final ScriptMonitorReportService reportService;

    /**
     * 构造器注入。
     *
     * @param reportService 报告 Service
     */
    public ScriptMonitorReportWriteServiceImpl(ScriptMonitorReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 更新报告为 GENERATED 状态（含 summary_json + reportBodyId）。
     *
     * @param reportId    报告 ID
     * @param bodyMongoId MongoDB 正文 ObjectId
     * @param summaryJson 摘要 JSON 字符串（AI summary 子对象 toJSONString()；非法 JSON 时为 null）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishReport(Long reportId, String bodyMongoId, String summaryJson) {
        ScriptMonitorReportEntity update = new ScriptMonitorReportEntity();
        update.setId(reportId);
        update.setStatus(ScriptMonitorStatusEnum.GENERATED.getCode());
        update.setReportBodyId(bodyMongoId);
        update.setSummaryJson(summaryJson);
        update.setUnavailableReason(null);
        update.setUpdateDate(new Date());
        reportService.updateById(update);
    }

    /**
     * 失败补偿：若原状态为 GENERATED 则恢复旧成功报告；否则置 GENERATE_FAILED。
     *
     * <p>openspec §5.2 D-7：失败不覆盖成功。</p>
     *
     * @param reportId       报告 ID
     * @param originalStatus 触发前状态
     * @param oldBodyId      旧报告正文 MongoDB ObjectId
     * @param oldSummaryJson 旧摘要 JSON 字符串
     * @param errorMessage   错误信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreOrFail(Long reportId, Integer originalStatus, String oldBodyId,
                              String oldSummaryJson, String errorMessage) {
        ScriptMonitorReportEntity update = new ScriptMonitorReportEntity();
        update.setId(reportId);
        update.setUpdateDate(new Date());
        if (Objects.equals(originalStatus, ScriptMonitorStatusEnum.GENERATED.getCode())) {
            // 恢复旧成功状态
            update.setStatus(ScriptMonitorStatusEnum.GENERATED.getCode());
            update.setReportBodyId(oldBodyId);
            update.setSummaryJson(oldSummaryJson);
            update.setUnavailableReason(null);
            log.info("质检生成失败，恢复旧成功报告 reportId={}", reportId);
        } else {
            update.setStatus(ScriptMonitorStatusEnum.GENERATE_FAILED.getCode());
            String reason = errorMessage != null && errorMessage.length() > 200
                    ? errorMessage.substring(0, 200) : errorMessage;
            update.setUnavailableReason(reason);
            log.info("质检生成失败，置 GENERATE_FAILED reportId={}", reportId);
        }
        reportService.updateById(update);
    }

    /**
     * 将报告置为 NOT_APPLICABLE 状态。
     *
     * @param reportId 报告 ID
     * @param reason   不可生成原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markNotApplicable(Long reportId, String reason) {
        ScriptMonitorReportEntity update = new ScriptMonitorReportEntity();
        update.setId(reportId);
        update.setStatus(ScriptMonitorStatusEnum.NOT_APPLICABLE.getCode());
        update.setUnavailableReason(reason);
        update.setUpdateDate(new Date());
        reportService.updateById(update);
    }

    /**
     * 幂等写已读：仅当 is_read=0 时执行 UPDATE，同步写 confirmed_at=NOW()。
     *
     * <p>独立事务，写失败不影响调用方（调用方 catch + fail-safe）。
     * tenantId 作为防御性过滤条件，遵循 CLAUDE.md §2 租户隔离硬约束。</p>
     *
     * @param reportId 报告 ID
     * @param tenantId 租户 ID（防御性过滤）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markReadIfNeeded(Long reportId, Long tenantId) {
        reportService.lambdaUpdate()
                .eq(ScriptMonitorReportEntity::getId, reportId)
                .eq(ScriptMonitorReportEntity::getTenantId, tenantId)
                .eq(ScriptMonitorReportEntity::getIsRead, 0)
                .set(ScriptMonitorReportEntity::getIsRead, 1)
                .set(ScriptMonitorReportEntity::getConfirmedAt, new Date())
                .set(ScriptMonitorReportEntity::getUpdateDate, new Date())
                .update();
    }
}
