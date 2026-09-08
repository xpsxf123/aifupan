package com.jiuyu.governance.business.performance.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售额汇总请求
 * <p>
 * 使用场景：分公司占比(前端自己计算占比)、top部门、top小组、top直播间
 * 以(分公司、部门、小组、直播间)为维度汇总销售额
 * </p>
 * <p>
 * 层级筛选规则：
 * - dimensionType=subCompany时，返回当前租户下所有分公司的销售额，忽略companyId、deptId、teamId
 * - dimensionType=dept时，若传入companyId则仅统计该分公司下的部门；若不传则统计租户下所有部门
 * - dimensionType=team时，若传入deptId则仅统计该部门下的小组；若传入companyId则进一步筛选
 * - dimensionType=liveRoom时，若传入teamId则仅统计该小组下的直播间；若传入deptId或companyId则进一步筛选
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
public class SalesRevenueSummaryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分公司ID（精确匹配，可不传）
     * 传入时配合dimensionType进行层级筛选
     */
    private Long companyId;

    /**
     * 部门ID（精确匹配，可不传）
     * 传入时配合dimensionType进行层级筛选
     */
    private Long deptId;

    /**
     * 小组ID（精确匹配，可不传）
     * 传入时配合dimensionType进行层级筛选
     */
    private Long teamId;

    /**
     * 统计维度类型（必传）
     * - subCompany: 分公司维度
     * - dept: 部门维度
     * - team: 小组维度
     * - liveRoom: 直播间维度
     */
    @NotNull(message = "维度类型不能为空")
    private String dimensionType;

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
