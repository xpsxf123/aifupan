package com.jiuyu.replay.generic.feign.third;

import com.jiuyu.replay.generic.dto.third.SmsResult;

import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/10/11 上午11:44
 */
public interface SmsServiceFeign {

    /**
     * 发送短信
     *
     * @param templateId 模板ID
     * @param mobile     手机号
     * @param params     模板参数
     * @return 发送结果
     */
    SmsResult send(String templateId, String mobile, Map<String, String> params, String providerName);

}
