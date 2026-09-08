package com.jiuyu.replay.power.vo.crm;

import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * CRM 客户聚合结果项
 * 对应单个手机号的客户、销售和订单聚合信息
 */
@Data
@Schema(description = "CRM 客户聚合结果项")
public class CrmCustomerAggregateItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 是否命中客户
     */
    @Schema(description = "是否存在客户")
    private Boolean exists;

    /**
     * 未命中客户时的缺失原因
     */
    @Schema(description = "缺失原因")
    private String missingReason;

    /**
     * 客户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 客户名称
     */
    @Schema(description = "客户名称")
    private String customerName;

    /**
     * 归属销售负责人
     */
    @Schema(description = "销售负责人")
    private SalesInfoVo sales;

    /**
     * 客户订单列表
     */
    @Schema(description = "订单列表")
    private List<OrderInfoVo> orders = new ArrayList<>();
}
