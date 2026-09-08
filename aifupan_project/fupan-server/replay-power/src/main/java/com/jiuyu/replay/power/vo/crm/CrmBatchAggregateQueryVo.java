package com.jiuyu.replay.power.vo.crm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * CRM 批量聚合查询响应对象
 * 承载按手机号返回的客户聚合结果集合
 */
@Data
@Schema(description = "CRM 批量聚合查询响应")
public class CrmBatchAggregateQueryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户聚合结果列表
     */
    @Schema(description = "客户聚合结果")
    private List<CrmCustomerAggregateItemVo> customers = new ArrayList<>();
}
