package com.jiuyu.governance.business.rbac.pojo.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 角色响应对象
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Getter
@Setter
public class RoleResponse {

    /**
     * ID
     */
    private Long id;


    /**
     * 角色名
     */
    private String name;


    /**
     * 创建时间
     */
    private LocalDateTime createDate;

    /**
     * 最后修改时间
     */
    private LocalDateTime updateDate;


    /**
     * 用户数量
     */
    private Integer userCount;
}
