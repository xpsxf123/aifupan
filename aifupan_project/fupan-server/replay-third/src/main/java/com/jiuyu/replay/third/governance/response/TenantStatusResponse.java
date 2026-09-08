package com.jiuyu.replay.third.governance.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 租户状态响应
 *
 * @author HeHui
 * @date 2026-04-07 16:54
 */
@Getter
@Setter
public class TenantStatusResponse {

    /**
     * 租户ID
     */
    private Long id;

    /**
     * 账户状态 0停用，1正常,2 冻结
     */
    private Integer accountStatus;
}
