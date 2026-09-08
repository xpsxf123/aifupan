package com.jiuyu.governance.business.performance.pojo.response.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.math.BigDecimal;

/**
 * 排班业绩导出 VO（班次维度）
 *
 * @author lj
 * @date 2026-04-03
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePerformanceExport {

    /**
     * 开始时间
     */
    @ExcelProperty("日期")
    private String date;

    /**
     * 结束时间
     */
    @ExcelProperty("直播时段")
    private String timeSlot;

    /**
     * 参与人员（格式："岗位 姓名"，多人换行分隔）
     */
    @ExcelProperty("人员")
    private String staffNames;

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

    /**
     * 数据来源（系统 / 手动）
     */
    @ExcelProperty("数据来源")
    private String source;
}
