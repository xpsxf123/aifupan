package com.jiuyu.governance.plugins.sms;


/**
 * 测试环境用于mock 手机验证码的提供服务
 *
 * @author HeHui
 * @date 2026-04-01 09:56
 */
@FunctionalInterface
public interface MockCodeProvide {


    /**
     * 获取手机验证码
     *
     * @param mobile 手机号
     *
     * @return 验证码
     */
    String getCode(String mobile);
}
