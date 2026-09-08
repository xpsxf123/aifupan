package com.jiuyu.replay.third.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "third.tencent.pay")
public class WeChatPayProperties {

    /**
     * 商户号
     */
    private String merchantId;
    /**
     * 商户API私钥路径
     */
    private String privateKeyPath;
    /**
     * 商户证书序列号
     */
    private String merchantSerialNumber;
    /**
     * 商户APIV3密钥
     */
    private String apiKey;
    /**
     * appid
     */
    public String appid;
    /**
     * 支付二维码链接redis-key
     */
    private String codeUrlRedisKey;
}
