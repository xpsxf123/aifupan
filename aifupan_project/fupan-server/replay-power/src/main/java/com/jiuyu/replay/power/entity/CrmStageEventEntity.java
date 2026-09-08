package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * CRM 阶段事件表
 */
@Data
@TableName("tb_crm_stage_event")
@Schema(description = "CRM 阶段事件表")
public class CrmStageEventEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "客户ID(user_id)")
    private Long userId;
    @Schema(description = "事件唯一标识(event_id)，用于幂等覆盖")
    private String eventId;
    @Schema(description = "来源")
    private String source;
    @Schema(description = "事件发生时间")
    private Date occurredAt;
    @Schema(description = "阶段码(stage_code)")
    private String stageCode;
    @Schema(description = "阶段名称(stage_label)")
    private String stageLabel;
    @Schema(description = "置信度")
    private BigDecimal confidence;
    @Schema(description = "摘要")
    private String summary;
    @Schema(description = "结构化事实JSON")
    private String factsJson;
    @Schema(description = "原始JSON")
    private String rawJson;
    @Schema(description = "创建时间")
    private Date createDate;
}
