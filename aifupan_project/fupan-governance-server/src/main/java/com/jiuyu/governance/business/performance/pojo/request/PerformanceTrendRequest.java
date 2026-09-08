package com.jiuyu.governance.business.performance.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.framework.shandard.PageRequest;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDate;
import java.util.List;

/**
 * 数据趋势柱形图请求
 * <p>
 * 使用场景：业绩汇总详情页-数据趋势柱形图
 * 返回柱形图数据，y轴是日期(yyyy-MM-dd)，x轴是对应的汇总数据
 * 数据包含：场观、销售额、退款、净销售额、投放
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
public class PerformanceTrendRequest extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 来源ID
     * - sourceType=tenant时，sourceId随便传
     * - sourceType=subCompany时，sourceId为分公司ID
     * - sourceType=dept时，sourceId为部门ID
     * - sourceType=team时，sourceId为小组ID
     * - sourceType=liveRoom时，sourceId为直播间ID
     */
    @NotNull(message = "来源ID不能为空")
    private Long sourceId;

    /**
     * 来源类型
     * - tenant: 租户
     * - subCompany: 子公司/分公司
     * - dept: 部门
     * - team: 小组
     * - liveRoom: 直播间
     */
    @NotNull(message = "来源类型不能为空")
    private String sourceType;

    /**
     * 开始时间（必传，格式：yyyy-MM-dd）
     * 查询 stats_date >= startDate 的数据
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDate startDate;

    /**
     * 结束时间（必传，格式：yyyy-MM-dd）
     * 查询 stats_date <= endDate 的数据
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDate endDate;

    /**
     * 租户ID（由框架自动注入）
     */
    @JsonIgnore
    private Long tenantId;
}
