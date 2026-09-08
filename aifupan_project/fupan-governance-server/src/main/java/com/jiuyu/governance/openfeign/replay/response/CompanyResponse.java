package com.jiuyu.governance.openfeign.replay.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 公司表信息
 */
@Getter
@Setter
public class CompanyResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;
    /**
     * 公司名称
     */
    private String name;
    /**
     * 注册号，唯一标识公司
     */
    private String registrationNumber;
    /**
     * 行业 id
     */
    private Long tradeId;
    /**
     * 规模：人数
     */
    private String scales;
    /**
     * 公司联系人
     */
    private String linkman;
    /**
     * 公司联系电话
     */
    private String phones;
    /**
     * 公司地址
     */
    private String address;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 成立日期
     */
    private Date foundingDate;
    /**
     * 年收入
     */
    private String annualRevenue;
    /**
     * 公司状态（0 正常，1 暂停营业）
     */
    private Integer status;
    /**
     * 公司描述
     */
    private String description;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 更新时间
     */
    private Date updateDate;
    /**
     * 是否删除（0 未删除 1 删除）
     */
    private Integer isDeleted;


}
