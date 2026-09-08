package com.jiuyu.governance.business.performance.pojo.response.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.math.BigDecimal;

/**
 * 员工业绩导出 VO（按天维度）
 *
 * @author lj
 * @date 2026-04-03
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDailyPerformanceExport {

    /**
     * 日期
     */
    @ExcelProperty("日期")
    private String statsDate;

    /**
     * 场次（格式："N场(X小时)"）
     */
    @ExcelProperty("场次")
    private String schedule;

    /**
     * 参与直播间（多个、分隔）
     */
    @ExcelProperty("直播间")
    private String liveRooms;

    /**
     * 场观
     */
    @ExcelProperty("场观")
    private Integer viewCount;

    /**
     * 销售额（元）
     */
    @ExcelProperty("销售额（元）")
    private BigDecimal salesRevenue;

    /**
     * 退款（元）
     */
    @ExcelProperty("退款（元）")
    private BigDecimal refund;

    /**
     * 净销售额（元）
     */
    @ExcelProperty("净销售额（元）")
    private BigDecimal netSales;

    /**
     * 投放（元）
     */
    @ExcelProperty("投放（元）")
    private BigDecimal investment;

    /**
     * 投资回报率（ROI）
     */
    @ExcelProperty("ROI")
    private BigDecimal roi;
}
