package com.jiuyu.governance.openfeign.replay.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 租户资产修改请求
 *
 * @author HeHui
 * @date 2026-04-10 11:11
 */
@Getter
@Setter
public class TenantPropertyQuotaEditRequest {

    /**
     * 租户id
     */
    @NotNull(message = "租户id不能为空")
    private Long tenantId;

    /**
     * 资产类型
     */
    @NotBlank(message = "资产类型不能为空")
    private String code;

    /**
     * 资产数量
     */
    @NotNull(message = "资产数量不能为空")
    private Long quantity;
}
