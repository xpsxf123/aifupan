package com.jiuyu.replay.power.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "power")
public class PowerProperties {

    /**
     * 手机验证码redis前缀
     */
    private String phoneCodeRedisKey;
    /**
     * 用户登录token redis前缀
     */
    private String userLoginTokenRedisKey;
    /**
     * 重置密码
     */
    private String resetPassword;
    /**
     * 新用户的默认角色id
     */
    private Long defaultRoleId;
    /**
     * 用户登录的临时凭证redis前缀
     */
    private String tempLoginToken;
    /**
     * 平台运营角色的id
     */
    private Long platformOperationRoleId;
    /**
     * 用户登录信息redis前缀
     */
    private String userLoginInfoRedisKey;
}
