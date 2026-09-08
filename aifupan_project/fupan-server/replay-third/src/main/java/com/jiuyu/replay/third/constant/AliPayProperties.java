package com.jiuyu.replay.third.constant;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "third.ali.pay")
public class AliPayProperties {

    /**
     * appid
     */
    private String appId;

    /**
     * 应用私钥
     */
    private String PrivateKey;

    /**
     * 应用公钥证书路径
     */
    private String AppCertPath;

    /**
     * 支付宝公钥证书路径
     */
    private String AlipayPublicCertPath;

    /**
     * 支付宝根证书路径
     */
    private String RootCertPath;

    /**
     * 支付宝网关地址
     */
    private String ServerUrl;

    /**
     * 签名类型
     */
    private String SignType;

    /**
     * 数据格式
     */
    private String Format;

    /**
     * 字符编码
     */
    private String Charset;

    /**
     * 支付二维码链接redis-key
     */
    private String codeUrlRedisKey;
}
