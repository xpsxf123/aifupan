package com.jiuyu.governance.business.performance.pojo.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/4/23 11:35
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientPerformanceResponse {
    /**
     * 指标标签
     */
    private String label;
    /**
     * 指标值
     */
    private List<IndicatorResponse> value;
}
