package com.jiuyu.replay.third.sms;

import com.jiuyu.replay.generic.dto.third.SmsResult;

import java.util.Collection;
import java.util.Map;

/**
 * 短信服务提供商抽象类
 *
 * @author jiuyu
 */
public abstract class AbstractSmsProvider {

    /**
     * 获取提供商名称
     *
     * @return 提供商名称
     */
    public abstract String getName();

    /**
     * 发送短信
     *
     * @param templateId 模板ID
     * @param mobile     手机号
     * @param params     模板参数
     * @return 发送结果
     */
    public abstract SmsResult doSend(String templateId, String mobile, Map<String, String> params);

    /**
     * 批量发送短信
     *
     * @param templateId 模板ID
     * @param mobileList 手机号列表
     * @param params     模板参数
     * @return 发送结果
     */
    public abstract SmsResult doSend(String templateId, Collection<String> mobileList, Map<String, String> params);
} 