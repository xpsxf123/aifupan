package com.jiuyu.replay.ai.repository.mongo;

import com.jiuyu.replay.ai.entity.ScriptMonitorReportBodyEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * AI监控报告正文 MongoDB Repository。
 *
 * <p>提供按 {@code reportId} 查询和删除能力，用于报告的读取与覆盖写入。</p>
 *
 * @author beta
 * @date 2026-05-30
 */
@Component
public interface ScriptMonitorReportBodyRepository extends MongoRepository<ScriptMonitorReportBodyEntity, String> {

    /**
     * 按报告ID查询正文。
     *
     * @param reportId MySQL tb_script_monitor_report.id
     * @return 报告正文（Optional）
     */
    Optional<ScriptMonitorReportBodyEntity> findByReportId(Long reportId);

    /**
     * 按报告ID删除正文（重新生成时先删旧）。
     *
     * @param reportId MySQL tb_script_monitor_report.id
     */
    void deleteByReportId(Long reportId);
}
