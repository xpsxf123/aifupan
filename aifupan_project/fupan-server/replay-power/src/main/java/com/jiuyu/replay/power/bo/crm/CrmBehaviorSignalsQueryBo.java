package com.jiuyu.replay.power.bo.crm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * CRM 客户行为信号批量查询请求对象
 * 用于试用期使用深度接口的入参（单次最多 50 个手机号）
 */
@Data
@Schema(description = "CRM 客户行为信号批量查询请求")
public class CrmBehaviorSignalsQueryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户手机号列表（1~50 个）
     */
    @NotEmpty(message = "手机号列表不能为空")
    @Size(max = 50, message = "手机号数量不能超过50")
    @Schema(description = "手机号列表（1~50 个）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> phones;
}
