package com.jiuyu.governance.business.org.pojo.response;

import com.jiuyu.governance.business.rbac.pojo.response.EmployeeBaseInfo;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 子公司响应信息
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Getter
@Setter
public class SubCompanyResponse {

    /**
     * ID
     */
    private Long id;

    /**
     * 公司名称
     */
    private String name;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private LocalDateTime createDate;

    /**
     * 最后修改时间
     */
    private LocalDateTime updateDate;


    /**
     * 直播间数量
     */
    private Integer roomCount;


    /**
     * 管理员用户信息
     */
    private List<EmployeeBaseInfo> managerUserInfos;
}
