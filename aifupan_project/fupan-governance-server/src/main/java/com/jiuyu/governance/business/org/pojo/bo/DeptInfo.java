package com.jiuyu.governance.business.org.pojo.bo;

import lombok.Getter;
import lombok.Setter;

/**
 * 部门信息
 *
 * @author HeHui
 * @date 2026-03-26 10:26
 */
@Getter
@Setter
public class DeptInfo {

    /**
     * ID
     */
    private Long id;


    /**
     * 所属公司ID
     */
    private Long companyId;


    /**
     * 部门名称
     */
    private String name;

    /**
     * 排序
     */
    private Integer sort;
}
