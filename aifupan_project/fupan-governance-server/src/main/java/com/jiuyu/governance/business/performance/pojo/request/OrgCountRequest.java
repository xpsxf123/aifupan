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
 * 组织数量统计请求
 * <p>
 * 使用场景：集团业绩汇总罗盘-详情名称和数量统计，分公司业绩汇总详情-详情名称和数量统计，
 * 部门业绩汇总详情-详情名称和数量统计，小组业绩汇总详情-详情名称和数量统计
 * </p>
 * <p>
 * 根据参数获取对应组织数量的统计：
 * - 如果是租户(tenant)就返回分公司、部门、小组、直播间的数量
 * - 分公司(subCompany)返回部门、小组、直播间的数量
 * - 部门(dept)返回小组、直播间的数量
 * - 小组(team)返回直播间的数量
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
public class OrgCountRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 来源ID
     * - sourceType=tenant时，sourceId随便传
     * - sourceType=subCompany时，sourceId为分公司ID
     * - sourceType=dept时，sourceId为部门ID
     * - sourceType=team时，sourceId为小组ID
     */
    @NotNull(message = "来源ID不能为空")
    private Long sourceId;

    /**
     * 来源类型
     * - tenant: 租户
     * - subCompany: 子公司/分公司
     * - dept: 部门
     * - team: 小组
     */
    @NotNull(message = "来源类型不能为空")
    private String sourceType;

    /**
     * 租户ID（由框架自动注入）
     */
    @JsonIgnore
    private Long tenantId;
}
