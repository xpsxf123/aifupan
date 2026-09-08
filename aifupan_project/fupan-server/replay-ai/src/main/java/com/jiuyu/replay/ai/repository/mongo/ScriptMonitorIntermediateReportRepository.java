package com.jiuyu.replay.ai.repository.mongo;

import com.jiuyu.replay.ai.entity.ScriptMonitorIntermediateReportEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI监控中间报告 MongoDB Repository。
 *
 * <p>提供按 {@code reportId} 查询、排序和删除能力。
 * 合并拼接中间报告时用 {@code findByReportIdOrderByIndexAsc} 保证顺序一致。</p>
 *
 * @author beta
 * @date 2026-05-30
 */
@Component
public interface ScriptMonitorIntermediateReportRepository extends MongoRepository<ScriptMonitorIntermediateReportEntity, String> {

    /**
     * 按报告ID查询所有中间报告，按序号升序排列（用于合并拼接 prompt）。
     *
     * @param reportId MySQL tb_script_monitor_report.id
     * @return 中间报告列表（按 index 升序）
     */
    List<ScriptMonitorIntermediateReportEntity> findByReportIdOrderByIndexAsc(Long reportId);

    /**
     * 按报告ID查询所有中间报告（无序）。
     *
     * @param reportId MySQL tb_script_monitor_report.id
     * @return 中间报告列表
     */
    List<ScriptMonitorIntermediateReportEntity> findByReportId(Long reportId);

    /**
     * 按报告ID删除所有中间报告（重新生成时先删旧）。
     *
     * @param reportId MySQL tb_script_monitor_report.id
     */
    void deleteByReportId(Long reportId);
}
