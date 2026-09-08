package com.jiuyu.replay.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * rocketmq配置参数信息
 *
 * @author RayChou
 * @date 2025/6/7 9:51
 */
@Data
@ConfigurationProperties(prefix = "rocketmq.producer.business-tags")
public class RocketMqProperties {

    /**
     * 用户邀请活动tag
     */
    private String tagUserInviteActivity;

    /**
     * 用户注册后首单tag
     */
    private String tagUserFirstOrder;

    /**
     * 用户注册后发送短信
     */
    private String tagUserRegisterSendEmail;
}
