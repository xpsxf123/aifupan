package com.jiuyu.governance.business.performance.pojo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 组织数量统计响应
 * <p>
 * 使用场景：业绩汇总详情页-获取组织下对应的分公司、部门、小组、直播间数量统计
 * </p>
 * <p>
 * 返回规则：
 * - 租户维度：返回分公司、部门、小组、直播间数量，sourceId=0，sourceName=租户名称
 * - 分公司维度：返回部门、小组、直播间数量（公司数量为null），sourceId=分公司ID
 * - 部门维度：返回小组、直播间数量（公司、部门数量为null），sourceId=部门ID
 * - 小组维度：返回直播间数量（公司、部门、小组数量为null），sourceId=小组ID
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgCountResponse {

    /**
     * 来源ID
     * - 租户维度时为0
     * - 其他维度为对应的组织ID
     */
    private Long sourceId;

    /**
     * 来源名称
     * - 租户维度时为"集团"或租户名称
     * - 其他维度为对应的组织名称
     */
    private String sourceName;

    /**
     * 子公司名称
     * 当前组织所属的子公司名称
     */
    private String companyName;

    /**
     * 部门名称
     * 当前组织所属的部门名称
     */
    private String deptName;

    /**
     * 小组名称
     * 当前组织所属的小组名称
     */
    private String teamName;

    /**
     * 分公司数量
     * 仅租户维度返回
     */
    private Integer companyCount;

    /**
     * 部门数量
     * 租户、分公司维度返回
     */
    private Integer deptCount;

    /**
     * 小组数量
     * 租户、分公司、部门维度返回
     */
    private Integer teamCount;

    /**
     * 直播间数量
     * 所有维度都返回
     */
    private Integer liveRoomCount;
}
