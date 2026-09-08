package com.jiuyu.replay.ai.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI监控报告正文（MongoDB 存储）。
 *
 * <p>对应集合 {@code script_monitor_report_body}，每条记录保存一份最终报告的 Markdown 正文。
 * 重新生成时按 {@code reportId} 覆盖（先删旧再插新）。</p>
 *
 * @author beta
 * @date 2026-05-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI监控报告正文（MongoDB）")
@Document(collection = "script_monitor_report_body")
public class ScriptMonitorReportBodyEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * MongoDB ObjectId（String 形式）
     */
    @Id
    private String id;

    /**
     * 关联 MySQL tb_script_monitor_report.id
     */
    @Indexed(unique = true)
    @Field("reportId")
    @Schema(description = "关联MySQL报告记录ID")
    private Long reportId;

    /**
     * 最终报告 Markdown 正文
     */
    @Field("content")
    @Schema(description = "最终报告Markdown正文")
    private String content;
}
