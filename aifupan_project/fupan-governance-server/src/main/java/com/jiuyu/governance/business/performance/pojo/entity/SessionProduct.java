package com.jiuyu.governance.business.performance.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 场次商品关联表
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
@TableName("session_product")
public class SessionProduct {

    /**
     * 主键，雪花ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 场次ID
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 商品ID（关联 product.id）
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 销量
     */
    @TableField("quantity")
    private Integer quantity;

    /**
     * 单价
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 销售额
     */
    @TableField("sales_amount")
    private BigDecimal salesAmount;

    /**
     * 曝光点击率(%)
     */
    @TableField("exposure_click_rate")
    private BigDecimal exposureClickRate;

    /**
     * 曝光成交率(%)
     */
    @TableField("exposure_conversion_rate")
    private BigDecimal exposureConversionRate;

    /**
     * 商品千次曝光成交金额
     */
    @TableField("gpm")
    private BigDecimal gpm;

    /**
     * 退单量
     */
    @TableField("refund_quantity")
    private Integer refundQuantity;

    /**
     * 退款额
     */
    @TableField("refund_amount")
    private BigDecimal refundAmount;

    /**
     * 退款率(%)
     */
    @TableField("refund_rate")
    private BigDecimal refundRate;

    /**
     * 点击付款率(%)
     */
    @TableField("click_payment_rate")
    private BigDecimal clickPaymentRate;

    /**
     * 公司ID
     */
    @TableField("company_id")
    private Long companyId;

    /**
     * 部门ID
     */
    @TableField("dept_id")
    private Long deptId;

    /**
     * 小组ID
     */
    @TableField("team_id")
    private Long teamId;

    /**
     * 创建时间
     */
    @TableField(value = "create_date", fill = FieldFill.INSERT)
    private LocalDateTime createDate;

    /**
     * 更新时间
     */
    @TableField(value = "update_date", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;

    /**
     * 创建人
     */
    @TableField("create_by")
    private Long createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private Long updateBy;

    /**
     * 逻辑删除：0-正常，-1-已删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
