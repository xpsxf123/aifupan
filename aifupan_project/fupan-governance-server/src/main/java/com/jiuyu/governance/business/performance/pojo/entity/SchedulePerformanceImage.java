package com.jiuyu.governance.business.performance.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 排班业绩凭证图片表
 *
 * @author lj
 * @date 2026-03-28
 */
@Getter
@Setter
@TableName("schedule_performance_image")
public class SchedulePerformanceImage {

    /**
     * 主键，关联schedule_performance.id
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 场观图片ossKey
     */
    @TableField("view_count_image_url")
    private String viewCountImageUrl;

    /**
     * 销售额图片ossKey
     */
    @TableField("sales_revenue_image_url")
    private String salesRevenueImageUrl;

    /**
     * 退款图片ossKey
     */
    @TableField("refund_image_url")
    private String refundImageUrl;

    /**
     * 投放图片ossKey
     */
    @TableField("investment_image_url")
    private String investmentImageUrl;

    /**
     * 退款数量图片ossKey
     */
    @TableField("refund_quantity_image_url")
    private String refundQuantityImageUrl;

    /**
     * 销售单量图片ossKey
     */
    @TableField("pay_combo_cnt_image_url")
    private String payComboCntImageUrl;

    /**
     * 曝光次数图片ossKey
     */
    @TableField("exposure_count_image_url")
    private String exposureCountImageUrl;

    /**
     * 涨粉人数图片ossKey
     */
    @TableField("follow_count_image_url")
    private String followCountImageUrl;

    /**
     * 点击-成交率图片ossKey
     */
    @TableField("click_payment_rate_image_url")
    private String clickPaymentRateImageUrl;

    /**
     * 互动率图片ossKey
     */
    @TableField("interaction_rate_image_url")
    private String interactionRateImageUrl;

    /**
     * 最高在线图片ossKey
     */
    @TableField("max_online_image_url")
    private String maxOnlineImageUrl;

    /**
     * 净销售额图片ossKey
     */
    @TableField("net_sales_image_url")
    private String netSalesImageUrl;

    /**
     * ROI图片ossKey
     */
    @TableField("roi_image_url")
    private String roiImageUrl;

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
     * 逻辑删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean isDeleted;
}
