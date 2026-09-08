package com.jiuyu.replay.power.vo.crm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * CRM 客户行为信号单项
 * 每条记录对应一个手机号，承载团队维度聚合的使用深度信号
 */
@Data
@Schema(description = "CRM 客户行为信号单项")
public class CrmBehaviorSignalItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 回显入参手机号（必填，按此映射回客户）
     */
    @Schema(description = "回显入参手机号")
    private String phone;

    /**
     * 该手机号是否存在系统中
     */
    @Schema(description = "是否存在")
    private Boolean exists;

    /**
     * 客户团队账号总数（1 主账号 + N 子账号合计）
     */
    @Schema(description = "客户团队账号总数")
    private Integer customerUnitSize;

    /**
     * 团队添加的直播间账号总数
     */
    @Schema(description = "团队添加的直播间账号总数")
    private Integer addedAnchorCount;

    /**
     * 团队"自有账号"数（非授权他人代管）
     */
    @Schema(description = "团队自有账号数")
    private Integer ownAnchorCount;

    /**
     * 团队是否使用过运营助手（历史任一次即 true）
     */
    @Schema(description = "团队是否使用过运营助手")
    private Boolean hasUsedOpAssistant;

    /**
     * 团队是否使用过对比复盘（历史任一有效记录即 true）
     */
    @Schema(description = "团队是否使用过对比复盘")
    private Boolean hasUsedCompareReplay;

    /**
     * 主账号注册至今的服务时长（小时）
     */
    @Schema(description = "服务时长（小时，仅主账号）")
    private Integer serviceDurationHours;

    /**
     * 团队累计已消耗的 AI 语音分析时长（秒）
     */
    @Schema(description = "团队累计 AI 语音分析时长（秒）")
    private Long aiAnalysisTimeConsumed;

    /**
     * 团队历史累计算力消耗（tokens）
     */
    @Schema(description = "团队历史累计算力消耗（tokens）")
    private Long totalAiTokensUsed;

    /**
     * 团队近 30 个自然日算力消耗（tokens）
     */
    @Schema(description = "团队近 30 个自然日算力消耗（tokens）")
    private Long recentAiTokensUsed30d;

    /**
     * 团队近 7 个自然日算力消耗（tokens）
     */
    @Schema(description = "团队近 7 个自然日算力消耗（tokens）")
    private Long recentAiTokensUsed7d;
}
