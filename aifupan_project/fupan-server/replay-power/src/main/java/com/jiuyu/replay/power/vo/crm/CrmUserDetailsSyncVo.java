package com.jiuyu.replay.power.vo.crm;

import lombok.Data;

import java.io.Serializable;

/**
 * CRM 结构化画像同步结果
 */
@Data
public class CrmUserDetailsSyncVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String userAmbition;
    private Integer userBelongType;
}
