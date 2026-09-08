package com.jiuyu.governance.business.performance.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 商品排行聚合统计BO
 * 用于接收SQL按商品聚合查询结果
 *
 * @author lj
 * @date 2026-03-27
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRankingBO {

    /**
     * 商品ID（product.id）
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 销量（SUM）
     */
    private Integer quantity;

    /**
     * 销售额（SUM）
     */
    private BigDecimal salesAmount;

    /**
     * 退单量（SUM）
     */
    private Integer refundQuantity;

    /**
     * 退款额（SUM）
     */
    private BigDecimal refundAmount;

    /**
     * 退款率（计算：退款额/销售额*100）
     */
    private BigDecimal refundRate;

    /**
     * 曝光成交率（AVG）
     */
    private BigDecimal exposureConversionRate;

    /**
     * 曝光点击率（AVG）
     */
    private BigDecimal exposureClickRate;

    /**
     * 千次观看成交额（AVG）
     */
    private BigDecimal gpm;

    /**
     * 点击付款率（AVG）
     */
    private BigDecimal clickPaymentRate;

    /**
     * 关联直播场次数量（COUNT DISTINCT）
     */
    private Integer sessionCount;

    /**
     * 关系子公司数量（COUNT DISTINCT）
     */
    private Integer companyCount;

    /**
     * 商品图片URL
     */
    private String imageUri;
}
