package com.jiuyu.governance.business.org.pojo.bo;

import lombok.Getter;
import lombok.Setter;

/**
 * 小组信息
 *
 * @author HeHui
 * @date 2026-03-26 10:26
 */
@Getter
@Setter
public class TeamInfo {

    /**
     * ID
     */
    private Long id;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 所属公司ID
     */
    private Long companyId;


    /**
     * 小组名称
     */
    private String name;

    /**
     * 排序
     */
    private Integer sort;
}
