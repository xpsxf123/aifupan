package com.jiuyu.replay.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * AI监控报告表(含任务状态)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_script_monitor_report")
@Schema(description = "AI监控报告表(含任务状态)")
public class ScriptMonitorReportEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "ID")
    private Long id;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID")
    private Long tenantId;

    /**
     * 触发者用户ID（手动触发时写入 user.getId()）
     */
    @Schema(description = "触发者用户ID")
    private Long userId;

    /**
     * 资源类型 0录制视频 1上传文件
     */
    @Schema(description = "资源类型 0录制视频 1上传文件")
    private Integer sourceType;

    /**
     * 业务场景 0复盘 1视频分析 2文案预审
     */
    @Schema(description = "业务场景 0复盘 1视频分析 2文案预审")
    private Integer sceneType;

    /**
     * 资源ID
     */
    @Schema(description = "资源ID")
    private String sourceId;

    /**
     * 监控类型 0质检 1还原度 2巡检
     */
    @Schema(description = "监控类型 0质检 1还原度 2巡检")
    private Integer monitorType;

    /**
     * 0未生成 1生成中 2已生成 3失败 4不可生成
     */
    @Schema(description = "0未生成 1生成中 2已生成 3失败 4不可生成")
    private Integer status;

    /**
     * MongoDB ObjectId
     */
    @Schema(description = "MongoDB ObjectId")
    @TableField(updateStrategy = FieldStrategy.ALWAYS) // 忽略字段策略，允许 null 值更新（清空旧 MongoDB 正文指针）
    private String reportBodyId;

    /**
     * 摘要文本（全类型统一存储）。
     *
     * <p>来源于 AI 合并报告 Markdown 中 {@code <summary>...</summary>} 标签内的文本
     * （正则匹配 + trim 首尾空白；标签不区分大小写；多个标签取首个；找不到标签 / 内容为 null 时存 null）。
     * 字段名 {@code summaryJson} 是历史命名（最初存 JSON 子对象 toJSONString），现存
     * Markdown 文本片段；为零迁移成本保留字段名。前端按 monitorType 自行处理。</p>
     */
    @Schema(description = "摘要文本（全类型统一存储；历史命名为 summaryJson，实际存 Markdown 文本片段）")
    @TableField(updateStrategy = FieldStrategy.ALWAYS) // 忽略字段策略，允许 null 值更新（清空旧摘要）
    private String summaryJson;

    /**
     * 不可生成原因
     */
    @Schema(description = "不可生成原因")
    @TableField(updateStrategy = FieldStrategy.ALWAYS) // 忽略字段策略，允许 null 值更新（成功后清空旧失败原因）
    private String unavailableReason;

    /**
     * 是否已读 0未读 1已读（仅录制人查看 detail 接口后置1，重新生成时重置为0）。
     *
     * <p>**不**使用 FieldStrategy.ALWAYS：partial entity updateById 时 null 会被跳过，
     * 避免 restoreOrFail/finishReport/markNotApplicable 把 NOT NULL 列写 null 报错。
     * 重置语义由 ScriptMonitorBll#handleExistingAndReturn 用 LambdaUpdateWrapper
     * 显式 .set(isRead, 0) 完成，不依赖 entity 字段策略。</p>
     */
    @Schema(description = "是否已读 0未读 1已读")
    private Integer isRead;

    /**
     * 录制人首次查看时间戳（已读时间；重新生成时重置为 NULL）。
     *
     * <p>**不**使用 FieldStrategy.ALWAYS：避免 restoreOrFail partial update 误清空。
     * 重置语义由 LambdaUpdateWrapper 显式 .set(confirmedAt, null) 完成。</p>
     */
    @Schema(description = "录制人首次查看时间戳（已读时间；重新生成时重置为 NULL）")
    private Date confirmedAt;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createDate;

    /**
     * 最后修改时间
     */
    @Schema(description = "最后修改时间")
    private Date updateDate;

    /**
     * 是否已删除 0否 1是
     */
    @Schema(description = "是否已删除 0否 1是")
    private Integer isDeleted;

    /**
     * 触发来源 manual-手动触发 auto-自动触发（B7）
     */
    @Schema(description = "触发来源 manual-手动触发 auto-自动触发")
    private String triggerSource;
}
