package com.jiuyu.governance.business.performance.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 业绩时段统计请求
 * <p>
 * 使用场景：业绩汇总详情页-业绩天、周、月的汇总统计
 * 统计的维度为：租户、分公司、部门、小组、直播间中的一个
 * 统计的业绩表为session_performance
 * </p>
 * <p>
 * 返回数据包含：直播场次（场次数量和直播时长）、场观、销售额、退款、净销售额、投放
 * 时段维度：今天、昨天、本周、上周、本月、上月
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
public class PerformancePeriodStatsRequest implements Serializable {

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
     * 租户ID（由框架自动注入）
     */
    @JsonIgnore
    private Long tenantId;
}
