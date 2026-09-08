package com.jiuyu.governance.business.performance.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 商品排行响应
 *
 * @author lj
 * @date 2026-03-27
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRankingResponse {

    /**
     * 商品ID（product.id）
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 销量（累加）
     */
    private Integer quantity;

    /**
     * 销售额（累加，单位：元）
     */
    private BigDecimal salesAmount;

    /**
     * 退单量（累加）
     */
    private Integer refundQuantity;

    /**
     * 退款额（累加，单位：元）
     */
    private BigDecimal refundAmount;

    /**
     * 退款率（%）
     * 计算方式：退款额总额 / 销售额总额 * 100
     */
    private BigDecimal refundRate;

    /**
     * 曝光成交率（%）平均值
     */
    private BigDecimal exposureConversionRate;

    /**
     * 曝光点击率（%）平均值
     */
    private BigDecimal exposureClickRate;

    /**
     * 千次观看成交额平均值
     */
    private BigDecimal gpm;

    /**
     * 点击付款率（%）平均值
     */
    private BigDecimal clickPaymentRate;

    /**
     * 关联直播场次数量
     */
    private Integer sessionCount;

    /**
     * 关系子公司数量
     */
    private Integer companyCount;

    /**
     * 商品图片URL
     */
    private String imageUri;
}
