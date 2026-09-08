package com.jiuyu.replay.ai.bll;

/**
 * AI 监控报告状态写入 Service 接口（C3 修补：将 ScriptMonitorGenerateBll 自调用的三个事务方法
 * 抽到独立 Spring Bean，确保 @Transactional AOP 代理正常介入）。
 *
 * <p>实现类：{@link impl.ScriptMonitorReportWriteServiceImpl}</p>
 *
 * @author beta
 * @date 2026-05-30
 */
public interface ScriptMonitorReportWriteService {

    /**
     * 更新报告为 GENERATED 状态（含 summary_json + reportBodyId）。
     *
     * <p>2026-06-02：原 4 个 count 参数（crashCount/slackCount/brandDamageCount/afterSalesCount）
     * 合并为 1 个 summaryJson 字符串参数；写入逻辑直存 AI summary 子对象原始 JSON。</p>
     *
     * @param reportId    报告 ID
     * @param bodyMongoId MongoDB 正文 ObjectId
     * @param summaryJson 摘要 JSON 字符串（来自 AI summary 子对象 toJSONString()；非法 JSON 时为 null）
     */
    void finishReport(Long reportId, String bodyMongoId, String summaryJson);

    /**
     * 失败补偿：若原状态为 GENERATED 则恢复旧成功报告；否则置 GENERATE_FAILED。
     *
     * <p>openspec §5.2 D-7：失败不覆盖成功。
     * 2026-06-02：原 4 个 old count 参数合并为 1 个 oldSummaryJson 字符串。</p>
     *
     * @param reportId       报告 ID
     * @param originalStatus 触发前状态
     * @param oldBodyId      旧报告正文 MongoDB ObjectId
     * @param oldSummaryJson 旧摘要 JSON 字符串（D-7 失败时恢复用）
     * @param errorMessage   错误信息
     */
    void restoreOrFail(Long reportId, Integer originalStatus, String oldBodyId,
                       String oldSummaryJson, String errorMessage);

    /**
     * 将报告置为 NOT_APPLICABLE 状态。
     *
     * @param reportId 报告 ID
     * @param reason   不可生成原因
     */
    void markNotApplicable(Long reportId, String reason);

    /**
     * 幂等写已读：仅当 is_read=0 时执行 UPDATE，同步写 confirmed_at=NOW()。
     *
     * <p>独立事务，写失败不影响调用方。调用方需 catch Exception 并 fail-safe 处理。
     * tenantId 作为防御性过滤条件（即使调用方已校验，UPDATE 仍带 tenantId
     * 谓词以遵循 CLAUDE.md §2 租户隔离硬约束）。</p>
     *
     * @param reportId 报告 ID
     * @param tenantId 租户 ID（防御性过滤）
     */
    void markReadIfNeeded(Long reportId, Long tenantId);
}
