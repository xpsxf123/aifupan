package com.jiuyu.replay.common.vo;

import lombok.Data;

@Data
public class CheckSurplusVo {

    /**
     * id，用于加回余量
     */
    private Long id;
    /**
     * secretId，用于加回余量
     */
    private String secretId;
}
