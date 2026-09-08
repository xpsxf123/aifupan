package com.jiuyu.governance.business.performance.pojo.response.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日业绩数据导出 VO
 *
 * @author lj
 * @date 2026-04-01
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyPerformanceExport {

    /**
     * 统计日期（格式：yyyy-MM-dd）
     */
    @ExcelProperty("日期")
    private LocalDate date;

    /**
     * 场观
     */
    @ExcelProperty("场观")
    private Integer viewCount;

    /**
     * 销售额（单位：元）
     */
    @ExcelProperty("销售额（元）")
    private BigDecimal salesRevenue;

    /**
     * 退款（单位：元）
     */
    @ExcelProperty("退款（元）")
    private BigDecimal refund;

    /**
     * 净销售额（单位：元）
     */
    @ExcelProperty("净销售额（元）")
    private BigDecimal netSales;

    /**
     * 投放（单位：元）
     */
    @ExcelProperty("投放（元）")
    private BigDecimal investment;
}
