package com.jiuyu.governance.openfeign.replay.response;

import lombok.Data;

import java.util.Date;

/**
 * 系统键值对响应
 *
 * @author lujie
 * @date 2026/4/23
 */
@Data
public class SystemKvResponse {

    /**
     * id
     */
    private Long id;

    /**
     * code
     */
    private String kvKey;

    /**
     * value
     */
    private String kvValue;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 最后修改时间
     */
    private Date updateDate;
}
