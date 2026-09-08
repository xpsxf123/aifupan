package com.jiuyu.replay.third.sms;

import com.jiuyu.replay.generic.dto.third.SmsResult;

import java.util.Collection;
import java.util.Map;

/**
 * 短信服务接口，定义发送短信的方法
 *
 * @author jiuyu
 */
public interface SmsService {

    /**
     * 发送短信
     *
     * @param templateId 模板ID
     * @param mobile     手机号
     * @param params     模板参数
     * @return 发送结果
     */
    SmsResult send(String templateId, String mobile, Map<String, String> params, String providerName);

    /**
     * 批量发送短信
     *
     * @param templateId 模板ID
     * @param mobileList 手机号列表
     * @param params     模板参数
     * @return 发送结果
     */
    SmsResult send(String templateId, Collection<String> mobileList, Map<String, String> params, String providerName);
} 