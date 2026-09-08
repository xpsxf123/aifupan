package com.jiuyu.governance.business.performance.pojo.response.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.math.BigDecimal;

/**
 * 业绩汇总导出 VO（分公司/部门/小组/直播间通用）
 *
 * @author lj
 * @date 2026-04-02
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceSummaryExport {

    /**
     * 名称（分公司/部门/小组/直播间名称）
     */
    @ExcelProperty("名称")
    private String name;

    /**
     * 场观（累计）
     */
    @ExcelProperty("场观")
    private Integer viewCount;

    /**
     * 销售额（累计，单位：元）
     */
    @ExcelProperty("销售额（元）")
    private BigDecimal salesRevenue;

    /**
     * 退款（累计，单位：元）
     */
    @ExcelProperty("退款（元）")
    private BigDecimal refund;

    /**
     * 净销售额（累计，单位：元）
     */
    @ExcelProperty("净销售额（元）")
    private BigDecimal netSales;

    /**
     * 投放（累计，单位：元）
     */
    @ExcelProperty("投放（元）")
    private BigDecimal investment;

    /**
     * 投资回报率（累计）
     */
    @ExcelProperty("ROI")
    private BigDecimal roi;
}
