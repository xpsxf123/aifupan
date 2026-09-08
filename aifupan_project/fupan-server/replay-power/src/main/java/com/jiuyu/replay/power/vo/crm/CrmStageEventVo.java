package com.jiuyu.replay.power.vo.crm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * CRM 阶段事件回传响应对象
 */
@Data
@Schema(description = "CRM 阶段事件回传响应")
public class CrmStageEventVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户ID
     */
    @Schema(description = "客户ID")
    private Long userId;

    /**
     * 客户手机号
     */
    @Schema(description = "客户手机号")
    private String phone;

    /**
     * 事件标识
     */
    @Schema(description = "事件标识")
    private String eventId;

    /**
     * 是否刷新当前销售状态
     */
    @Schema(description = "是否刷新当前销售状态")
    private Boolean businessUpdated;
}
