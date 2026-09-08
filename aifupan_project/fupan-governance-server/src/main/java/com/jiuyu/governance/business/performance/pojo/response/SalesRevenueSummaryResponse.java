package com.jiuyu.governance.business.performance.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 销售额汇总响应
 * <p>
 * 使用场景：销售额汇总（分公司、部门、小组、直播间）
 * 用于分公司占比计算、top部门、top小组、top直播间
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesRevenueSummaryResponse {

    /**
     * 维度ID
     * - dimensionType=subCompany时为分公司ID
     * - dimensionType=dept时为部门ID
     * - dimensionType=team时为小组ID
     * - dimensionType=liveRoom时为直播间ID
     */
    private Long id;

    /**
     * 维度名称
     * 对应分公司名称/部门名称/小组名称/直播间名称
     */
    private String name;

    /**
     * 销售额（单位：元）
     */
    private BigDecimal salesRevenue;
}
