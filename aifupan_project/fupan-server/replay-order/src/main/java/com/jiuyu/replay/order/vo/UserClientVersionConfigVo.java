package com.jiuyu.replay.order.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/4/7 15:00
 */
@Data
public class UserClientVersionConfigVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 版本未知
     */
    private String clientVersion;

    /**
     * 是否选择版本 0-不选版本，1-选择版本
     */
    private Integer isVersionSelect;

    /**
     * 当前账号类型
     */
    private Integer userType;
}
