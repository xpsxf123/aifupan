package com.jiuyu.replay.power.bo.crm;

import lombok.Data;

import java.io.Serializable;

/**
 * CRM 结构化画像同步到用户详情表的最小写入对象
 */
@Data
public class CrmUserDetailsSyncBo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String userAmbition;
    private Integer userBelongType;
}
