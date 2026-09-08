package com.jiuyu.replay.power.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 租户选项
 *
 * @author HeHui
 * @date 2026-03-05 10:23
 */
@Getter
@Setter
public class TenantOptionVo {

    /**
     * 租户id
     */
    private Long tenantId;

    /**
     * 主账户名称
     */
    private String accountName;

    /**
     * 主账户手机号 (脱敏)
     */
    private String accountMobile;
}
