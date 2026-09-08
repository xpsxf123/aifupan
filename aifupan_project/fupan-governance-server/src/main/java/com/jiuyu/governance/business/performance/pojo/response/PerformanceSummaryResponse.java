package com.jiuyu.governance.business.performance.pojo.response;

import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceMetrics;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 业绩数据汇总响应（分公司/部门/小组/直播间维度）
 * <p>
 * 使用场景：各分公司/部门/小组/直播间业绩数据分页列表
 * 包含：名称、ID、场观、销售额、退款、净销售额、投放、ROI
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
@SuperBuilder
public class PerformanceSummaryResponse extends BasePerformanceMetrics {

    /**
     * 组织ID（分公司ID/部门ID/小组ID/直播间ID）
     */
    private Long id;

    /**
     * 组织名称（分公司名称/部门名称/小组名称/直播间名称）
     */
    private String name;

    /**
     * 直播间主播头像URL
     */
    private String anchorAvatar;
}
