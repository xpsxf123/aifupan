package com.jiuyu.replay.power.vo.crm;

import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * CRM 单手机号订单查询响应对象
 * 承载单个手机号对应的客户命中结果和订单信息
 */
@Data
@Schema(description = "CRM 单手机号订单查询响应")
public class CrmOrderQueryByPhoneVo implements Serializable {

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
     * 客户订单列表
     */
    @Schema(description = "订单列表")
    private List<OrderInfoVo> orders = new ArrayList<>();

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
}
