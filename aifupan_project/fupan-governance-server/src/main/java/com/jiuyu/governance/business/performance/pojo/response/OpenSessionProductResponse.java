package com.jiuyu.governance.business.performance.pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 开放接口 - 场次商品查询响应（按场次ID聚合）
 * <p>
 * 每个场次ID一行，内含该场次下的商品指标集合。商品名称与图片URL由 product 表反查回填。
 * </p>
 *
 * @author HeHui
 * @date 2026-06-22
 */
@Getter
@Setter
public class OpenSessionProductResponse {

    /**
     * 场次ID
     */
    private Long sessionId;

    /**
     * 该场次下的商品指标集合
     */
    private List<ProductMetric> products;

    /**
     * 商品指标
     */
    @Getter
    @Setter
    public static class ProductMetric {

        /**
         * 商品ID（product.id）
         */
        private Long productId;

        /**
         * 商品名称（反查 product 回填）
         */
        private String productName;

        /**
         * 商品图片URL（反查 product 回填）
         */
        private String productImageUrl;

        /**
         * 销量
         */
        private Integer quantity;

        /**
         * 单价
         */
        private BigDecimal price;

        /**
         * 销售额
         */
        private BigDecimal salesAmount;

        /**
         * 曝光点击率(%)
         */
        private BigDecimal exposureClickRate;

        /**
         * 曝光成交率(%)
         */
        private BigDecimal exposureConversionRate;

        /**
         * 商品千次曝光成交金额
         */
        private BigDecimal gpm;

        /**
         * 退单量
         */
        private Integer refundQuantity;

        /**
         * 退款额
         */
        private BigDecimal refundAmount;

        /**
         * 退款率(%)
         */
        private BigDecimal refundRate;

        /**
         * 点击付款率(%)
         */
        private BigDecimal clickPaymentRate;

        /**
         * 所属场次ID（仅用于分组，不对外输出）
         */
        @JsonIgnore
        private Long sessionId;
    }
}
