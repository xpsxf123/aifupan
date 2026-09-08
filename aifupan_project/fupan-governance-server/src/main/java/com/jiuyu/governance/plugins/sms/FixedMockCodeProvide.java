package com.jiuyu.governance.plugins.sms;

import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 固定mock 短信验证码提供
 *
 * @author HeHui
 * @date 2026-04-01 10:11
 */
@Slf4j
public class FixedMockCodeProvide implements MockCodeProvide {

    private final String code;


    public FixedMockCodeProvide(String code) {
        this.code = code;
        log.info("[验证码] enable mock provide, 固定验证码: {}", code);
    }

    public FixedMockCodeProvide() {
        this(RandomUtil.randomNumbers(6));
    }


    /**
     * 获取手机验证码
     *
     * @param mobile 手机号
     *
     * @return 验证码
     */
    @Override
    public String getCode(String mobile) {
        return code;
    }
}
