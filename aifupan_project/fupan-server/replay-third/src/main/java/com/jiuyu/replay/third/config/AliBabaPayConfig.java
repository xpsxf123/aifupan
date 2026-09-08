package com.jiuyu.replay.third.config;

import com.alipay.api.AlipayConfig;
import com.jiuyu.replay.third.constant.AliPayProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class AliBabaPayConfig {

    @Resource
    private AliPayProperties aliPayProperties;

    public AlipayConfig getAlipayConfig() {
        AlipayConfig alipayConfig = new AlipayConfig();
        // 数据格式
        alipayConfig.setFormat(aliPayProperties.getFormat());
        // 字符编码
        alipayConfig.setCharset(aliPayProperties.getCharset());
        //支付宝网关地址
        alipayConfig.setServerUrl(aliPayProperties.getServerUrl());
        //appId
        alipayConfig.setAppId(aliPayProperties.getAppId());
        //设置签名类型
        alipayConfig.setSignType(aliPayProperties.getSignType());
        //应用私钥
        alipayConfig.setPrivateKey(aliPayProperties.getPrivateKey());
        //设置应用公钥证书路径
        alipayConfig.setAppCertPath(aliPayProperties.getAppCertPath());
        //设置支付宝公钥证书路径
        alipayConfig.setAlipayPublicCertPath(aliPayProperties.getAlipayPublicCertPath());
        //设置支付宝根证书路径
        alipayConfig.setRootCertPath(aliPayProperties.getRootCertPath());
        return alipayConfig;
    }

}
