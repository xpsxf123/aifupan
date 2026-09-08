package com.jiuyu.governance.plugins.oauth;

import com.jiuyu.framework.oauth.client.annotation.RequiredLogin;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;

import java.lang.annotation.*;

/**
 * 限定只能客户端用户访问
 *
 * @author HeHui
 * @date 2026-03-17 15:51
 */
@RequiredLogin(OauthConstant.CLIENT_USER)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface ClientUser {
}
