package com.jiuyu.replay.ai.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * AI监控中间报告（MongoDB 存储）。
 *
 * <p>对应集合 {@code script_monitor_intermediate_report}，每条记录保存一份中间报告。
 * 同一个 {@code reportId} 下按 {@code index} 区分多份中间报告（质检/还原度各3份，巡检N份）。
 * 重新生成时先按 {@code reportId} 删旧再插新。</p>
 *
 * @author beta
 * @date 2026-05-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI监控中间报告（MongoDB）")
@Document(collection = "script_monitor_intermediate_report")
@CompoundIndex(name = "uk_report_index", def = "{'reportId':1, 'index':1}", unique = true)
public class ScriptMonitorIntermediateReportEntity implements Serializable {

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
    @Field("reportId")
    @Schema(description = "关联MySQL报告记录ID")
    private Long reportId;

    /**
     * 中间报告序号（0/1/2 对应质检/还原度的3份；巡检可多份）
     */
    @Field("index")
    @Schema(description = "中间报告序号，从0开始")
    private Integer index;

    /**
     * 中间报告 HTML 正文
     */
    @Field("content")
    @Schema(description = "中间报告HTML正文")
    private String content;

    /**
     * 调用模型信息，包含 modelCode（模型编码）和 promptUsed（实际使用的提示词）等字段
     */
    @Field("modelInfo")
    @Schema(description = "调用模型信息（modelCode/promptUsed等）")
    private Map<String, Object> modelInfo;
}
