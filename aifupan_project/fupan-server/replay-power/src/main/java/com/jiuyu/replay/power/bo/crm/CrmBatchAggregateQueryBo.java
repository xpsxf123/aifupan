package com.jiuyu.replay.power.bo.crm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * CRM 批量聚合查询请求对象
 * 用于承接销售智能体发起的客户批量读取请求
 */
@Data
@Schema(description = "CRM 批量聚合查询请求")
public class CrmBatchAggregateQueryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户手机号列表
     */
    @NotEmpty(message = "手机号列表不能为空")
    @Schema(description = "手机号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> phones;
}
