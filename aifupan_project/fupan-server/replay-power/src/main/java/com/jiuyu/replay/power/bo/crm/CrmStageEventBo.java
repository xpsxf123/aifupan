package com.jiuyu.replay.power.bo.crm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * CRM 阶段事件回传请求对象
 */
@Data
@Schema(description = "CRM 阶段事件回传请求")
public class CrmStageEventBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户手机号
     */
    @NotBlank(message = "客户手机号不能为空")
    @Schema(description = "客户手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    /**
     * 事件标识
     */
    @NotBlank(message = "事件标识不能为空")
    @Schema(description = "事件标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String eventId;

    /**
     * 来源
     */
    @Schema(description = "来源")
    private String source = "";

    /**
     * 事件发生时间
     */
    @Schema(description = "事件发生时间")
    private String occurredAt = "";

    /**
     * 阶段编码
     */
    @Schema(description = "阶段编码")
    private String stageCode = "";

    /**
     * 阶段名称
     */
    @Schema(description = "阶段名称")
    private String stageLabel = "";

    /**
     * 置信度
     */
    @Schema(description = "置信度")
    private BigDecimal confidence = BigDecimal.ZERO;

    /**
     * 事件摘要
     */
    @Schema(description = "事件摘要")
    private String summary = "";

    /**
     * 关键事实 JSON
     */
    @Schema(description = "关键事实JSON")
    private String factsJson = "";

    /**
     * 原始扩展 JSON
     */
    @Schema(description = "原始扩展JSON")
    private String rawJson = "";
}
