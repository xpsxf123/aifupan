package com.jiuyu.governance.business.performance.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 视频商品表
 *
 * @author lj
 * @date 2026-03-19
 */
@Getter
@Setter
@TableName("video_product")
public class VideoProduct {

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
     * 爱复盘视频ID
     */
    @TableField("video_id")
    private String videoId;

    /**
     * 直播批次号
     */
    @TableField("batch_number")
    private String batchNumber;

    /**
     * 商品ID
     */
    @TableField("product_id")
    private String productId;

    /**
     * 商品标题
     */
    @TableField("title")
    private String title;

    /**
     * 商品图片URL
     */
    @TableField("image_uri")
    private String imageUri;

    /**
     * 到手价（元）
     */
    @TableField("market_price")
    private BigDecimal marketPrice;

    /**
     * 直播间上架时间
     */
    @TableField("product_bind_time")
    private LocalDateTime productBindTime;

    /**
     * 直播间下架时间
     */
    @TableField("product_down_time")
    private LocalDateTime productDownTime;

    /**
     * 讲解次数
     */
    @TableField("explain_cnt")
    private Integer explainCnt;

    /**
     * 商品曝光人数
     */
    @TableField("product_show_ucnt")
    private Long productShowUcnt;

    /**
     * 商品点击人数
     */
    @TableField("product_click_ucnt")
    private Long productClickUcnt;

    /**
     * 曝光-点击转化率
     */
    @TableField("product_show_click_ucnt_ratio")
    private BigDecimal productShowClickUcntRatio;

    /**
     * 曝光-成交转化率
     */
    @TableField("product_show_pay_ucnt_ratio")
    private BigDecimal productShowPayUcntRatio;

    /**
     * 点击-成交转化率
     */
    @TableField("product_click_pay_ucnt_ratio")
    private BigDecimal productClickPayUcntRatio;

    /**
     * 商品千次曝光成交金额（元）
     */
    @TableField("gpm")
    private BigDecimal gpm;

    /**
     * 累计成交金额（元）
     */
    @TableField("pay_amt")
    private BigDecimal payAmt;

    /**
     * 分钟最高成交金额（元）
     */
    @TableField("avg_max_pay_amt_min")
    private BigDecimal avgMaxPayAmtMin;

    /**
     * 累计成交件数
     */
    @TableField("pay_combo_cnt")
    private Long payComboCnt;

    /**
     * 累计成交订单数
     */
    @TableField("pay_cnt")
    private Long payCnt;

    /**
     * 创建订单数
     */
    @TableField("create_cnt")
    private Long createCnt;

    /**
     * 订单支付率
     */
    @TableField("create_pay_ucnt_ratio")
    private BigDecimal createPayUcntRatio;

    /**
     * 预售订单数
     */
    @TableField("pay_deposit_pre_order_cnt")
    private Long payDepositPreOrderCnt;

    /**
     * 预售定金金额（元）
     */
    @TableField("presale_depay_deamt")
    private BigDecimal presaleDepayDeamt;

    /**
     * 预售全款金额（元）
     */
    @TableField("pay_deposit_pre_order_amt")
    private BigDecimal payDepositPreOrderAmt;

    /**
     * 退款订单数
     */
    @TableField("refund_cnt")
    private Integer refundCnt;

    /**
     * 退款金额（元）
     */
    @TableField("real_refund_amt")
    private BigDecimal realRefundAmt;

    /**
     * 退款率
     */
    @TableField("refund_rate")
    private BigDecimal refundRate;

    /**
     * 商品过程数据OSS路径
     */
    @TableField("product_oss_key")
    private String productOssKey;

    /**
     * 曝光观看率 — 商品曝光人数/直播间观看人数
     */
    @TableField("product_view_show_ratio")
    private BigDecimal productViewShowRatio;

    /**
     * 成交单价（元）
     */
    @TableField("avg_pay_amt_per_order")
    private BigDecimal avgPayAmtPerOrder;

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
     * 逻辑删除：0-正常，-1-已删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 排序，从1开始
     */
    @TableField("sort")
    private Integer sort;
}
