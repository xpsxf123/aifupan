package com.jiuyu.governance.business.performance.pojo.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 开放接口 - 按场次ID批量查询场次商品（含指标）请求
 * <p>
 * 供外部应用（非同一应用、不同 IP）通过 {@code @APIKey} 调用。
 * 身份字段（userId/tenantId/userType）由调用方在请求体中显式传入；数据隔离以 tenantId 为准。
 * </p>
 *
 * @author HeHui
 * @date 2026-06-22
 */
@Getter
@Setter
public class OpenSessionProductQueryRequest {

    /**
     * 用户ID（调用方传入，作为身份透传）
     */
    @NotNull(message = "userId不能为空")
    private Long userId;

    /**
     * 租户ID（数据隔离边界）
     */
    @NotNull(message = "tenantId不能为空")
    private Long tenantId;

    /**
     * 用户类型（0主账号/1管理员/2子账号，按调用方语义透传）
     */
    private Integer userType;

    /**
     * 场次ID集合
     */
    @NotEmpty(message = "场次ID集合不能为空")
    private List<Long> sessionIds;
}
