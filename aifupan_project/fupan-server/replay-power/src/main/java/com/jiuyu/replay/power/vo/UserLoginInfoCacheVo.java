package com.jiuyu.replay.power.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginInfoCacheVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 登录来源
     */
    private String source;
    /**
     * 最后一次请求时间
     */
    private Long lastRequestTime;
    /**
     * 登陆时携带的指纹
     */
    private String fingerprint;
}
