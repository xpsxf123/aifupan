package com.jiuyu.governance.business.performance.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 商品关联分公司BO
 * 用于接收SQL查询结果
 *
 * @author lj
 * @date 2026-03-27
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCompanyBO {

    /**
     * 分公司ID
     */
    private Long companyId;

    /**
     * 分公司名称
     */
    private String companyName;

    /**
     * 销量
     */
    private Integer quantity;

    /**
     * 销售额
     */
    private BigDecimal salesAmount;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 场观
     */
    private Integer viewCount;
}
