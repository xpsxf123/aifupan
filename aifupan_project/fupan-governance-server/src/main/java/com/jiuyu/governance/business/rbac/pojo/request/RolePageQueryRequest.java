package com.jiuyu.governance.business.rbac.pojo.request;

import com.jiuyu.framework.shandard.PageRequest;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 角色分页查询请求
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Getter
@Setter
public class RolePageQueryRequest extends PageRequest {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色名关键字查询
     */
    private String name;
}
