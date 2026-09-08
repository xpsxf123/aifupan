package com.jiuyu.governance.business.rbac.pojo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 人员开启/关闭录制权限
 *
 * @author HeHui
 * @date 2026-03-18 16:04
 */
@Getter
@Setter
public class EmployOnRecRequest {

    /**
     * 人员
     */
    @NotNull(message = "缺少ID")
    private Long id;
}
