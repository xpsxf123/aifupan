package com.jiuyu.replay.third.bo;

import lombok.Data;

@Data
public class WechatPayCallbackHandleBo {

    /**
     * 证书序列号
     */
    private String serialNo;
    /**
     * 随机字符串
     */
    private String nonceStr;
    /**
     * 时间戳
     */
    private String timestamp;
    /**
     * 签名
     */
    private String wechatSign;
    /**
     * 请求体
     */
    private String body;

}
