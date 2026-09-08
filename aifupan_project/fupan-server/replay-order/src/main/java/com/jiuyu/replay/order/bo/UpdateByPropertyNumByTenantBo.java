package com.jiuyu.replay.order.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/4/9 19:59
 */
@Data
public class UpdateByPropertyNumByTenantBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户id
     */
    @NotNull(message = "租户id不能为空")
    private Long tenantId;

    /**
     * 资产类型
     */
    @NotNull(message = "资产类型不能为空")
    private String code;

    /**
     * 资产数量
     */
    @NotNull(message = "资产数量不能为空")
    private Long quantity;
}
