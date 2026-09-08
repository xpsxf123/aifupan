package com.jiuyu.replay.third.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "third.ali.msg")
public class AliMsgProperties {

    /**
     * aliMsgAccessKeyId
     */
    private String accessKeyId;
    /**
     * aliMsgAccessKeySecret
     */
    private String accessKeySecret;
    /**
     * aliMsgEndpoint
     */
    private String endpoint;
    /**
     * aliMsgSignName
     */
    private String signName;
    /**
     * aliMsgTemplateCode
     */
    private String templateCode;

    /**
     * 主播下播模板code
     */
    private String templateCodeDownAnchor;

    /**
     * 主播上线模板code
     */
    private String templateCodeUpAnchor;

    /**
     * 绑定子账号模板code
     */
    private String templateCodeBindingAccount;
}
