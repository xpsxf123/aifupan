package com.jiuyu.governance.business.performance.pojo.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ：lujie
 * @date ：2026/4/23 12:23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndicatorResponse <T> {

    /**
     * 指标标签
     */
    private String label;

    /**
     * 指标值
     */
    private T value;

    /**
     * 指标单位
     */
    private String unit;
}
