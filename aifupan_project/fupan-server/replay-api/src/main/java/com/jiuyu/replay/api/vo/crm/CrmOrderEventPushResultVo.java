package com.jiuyu.replay.api.vo.crm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * CRM 订单事件出站响应结果
 */
@Data
@Schema(description = "CRM 订单事件出站响应结果")
public class CrmOrderEventPushResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否接受")
    private Boolean accepted;
    @Schema(description = "是否幂等去重命中")
    private Boolean dedup;
}
