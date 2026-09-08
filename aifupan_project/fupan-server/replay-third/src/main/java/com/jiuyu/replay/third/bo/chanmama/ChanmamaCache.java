package com.jiuyu.replay.third.bo.chanmama;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChanmamaCache implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;
    /**
     * 第三方数据平台token
     */
    private String token;
    /**
     * 剩余调用次数
     */
    private Integer remainingQueryNum;
}
