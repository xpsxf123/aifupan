package com.jiuyu.replay.third.sms.impl;

import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.third.sms.AbstractSmsProvider;
import com.jiuyu.replay.generic.dto.third.SmsResult;
import com.jiuyu.replay.third.sms.SmsService;
import com.jiuyu.replay.third.sms.config.SmsConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 短信服务实现类，支持多个短信服务提供商以及失败后的自动切换功能
 *
 * @author jiuyu
 */
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Autowired
    private SmsConfig smsConfig;

    @Autowired
    private List<AbstractSmsProvider> smsProviders;

    /**
     * 短信服务提供商映射表
     */
    private final Map<String, AbstractSmsProvider> providerMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // 初始化短信服务提供商映射表
        for (AbstractSmsProvider provider : smsProviders) {
            String simpleName = provider.getClass().getSimpleName();
            String name = simpleName.toLowerCase();
            if (name.endsWith("smsprovider")) {
                name = name.substring(0, name.length() - "smsprovider".length());
            }
            providerMap.put(name, provider);
            log.info("注册短信服务提供商: {}, 实现类: {}", name, provider.getClass().getName());
        }
    }

    @Override
    public SmsResult send(String templateId, String mobile, Map<String, String> params, String providerName) {
        return send(templateId, mobile, params, providerName, 0);
    }

    /**
     * 递归发送短信
     *
     * @param templateId   模板ID
     * @param mobile       手机号
     * @param params       参数
     * @param providerName 短信服务提供商名称
     * @param cycleCount   递归次数
     * @return SmsResult
     */
    private SmsResult send(String templateId, String mobile, Map<String, String> params, String providerName, int cycleCount) {
        if (cycleCount > 10) {
            log.error("短信发送次数超出，结束递归，cycleCount = {}", cycleCount);
            return SmsResult.fail("502", "短信全部发送失败");
        }
        if (!smsConfig.isEnabled()) {
            log.info("短信服务已禁用，跳过发送");
            return SmsResult.fail("400", "短信服务已禁用");
        }

        if (mobile == null || mobile.isEmpty()) {
            log.error("短信发送失败：手机号不能为空");
            return SmsResult.fail("400", "手机号不能为空");
        }

        if (templateId == null || templateId.isEmpty()) {
            log.error("短信发送失败：模板ID不能为空");
            return SmsResult.fail("400", "模板ID不能为空");
        }

        // 获取默认提供商
        String defaultProvider = providerName != null ? providerName : smsConfig.getDefaultProvider();
        AbstractSmsProvider provider = providerMap.get(defaultProvider);
        if (provider == null) {
            log.error("短信服务提供商[{}]不存在", defaultProvider);
            return SmsResult.fail("500", "默认短信服务提供商不存在");
        }

        // 尝试使用默认提供商发送
        SmsResult result = provider.doSend(templateId, mobile, params);

        // 如果发送成功，直接返回结果
        if (result.isSuccess()) {
            return result;
        }

        // 发生失败，重新发送
        List<String> fallbackProviders = smsConfig.getFallbackProviders();
        int i = fallbackProviders.indexOf(defaultProvider);
        if (i == -1 || i == fallbackProviders.size() - 1) {
            log.error("短信全部发送失败，mobile = {}", mobile);
            return SmsResult.fail("501", result.getMessage());
        }

        int newI = i + 1;
        if (newI <= fallbackProviders.size() - 1) {
            defaultProvider = fallbackProviders.get(newI);
        } else {
            log.error("短信全部发送失败，mobile = {}", mobile);
            return SmsResult.fail("501", result.getMessage());
        }

        // 如果发送失败，尝试使用备用提供商
        log.info("默认短信服务提供商[{}]发送失败，尝试使用备用提供商", provider.getName());
        return send(templateId, mobile, params, defaultProvider, cycleCount + 1);
    }

    @Override
    public SmsResult send(String templateId, Collection<String> mobileList, Map<String, String> params, String providerName) {
        return send(templateId, mobileList, params, providerName, 0);
    }

    /**
     * 递归发送短信
     *
     * @param templateId   模板ID
     * @param mobileList   手机号列表
     * @param params       参数
     * @param providerName 短信服务提供商名称
     * @param cycleCount   递归次数
     * @return SmsResult
     */
    private SmsResult send(String templateId, Collection<String> mobileList, Map<String, String> params, String providerName, int cycleCount) {
        if (cycleCount > 10) {
            log.error("短信发送次数超出，结束递归，cycleCount = {}", cycleCount);
            return SmsResult.fail("502", "短信全部发送失败");
        }
        if (!smsConfig.isEnabled()) {
            log.info("短信服务已禁用，跳过发送");
            return SmsResult.fail("400", "短信服务已禁用");
        }

        if (mobileList == null || mobileList.isEmpty()) {
            log.error("短信发送失败：手机号列表不能为空");
            return SmsResult.fail("400", "手机号列表不能为空");
        }

        if (templateId == null || templateId.isEmpty()) {
            log.error("短信发送失败：模板ID不能为空");
            return SmsResult.fail("400", "模板ID不能为空");
        }

        // 获取默认提供商
        String defaultProvider = providerName != null ? providerName : smsConfig.getDefaultProvider();
        AbstractSmsProvider provider = providerMap.get(defaultProvider);
        if (provider == null) {
            log.error("默认短信服务提供商[{}]不存在", defaultProvider);
            return SmsResult.fail("500", "默认短信服务提供商不存在");
        }

        // 尝试使用默认提供商发送
        log.info("使用默认短信服务提供商[{}]批量发送短信", provider.getName());
        SmsResult result = provider.doSend(templateId, mobileList, params);

        // 如果发送成功，直接返回结果
        if (result.isSuccess()) {
            return result;
        }

        // 发生失败，重新发送
        List<String> fallbackProviders = smsConfig.getFallbackProviders();
        int i = fallbackProviders.indexOf(defaultProvider);
        if (i == -1 || i == fallbackProviders.size() - 1) {
            log.error("短信全部发送失败，mobileList = {}", JSONUtil.toJsonStr(mobileList));
            return SmsResult.fail("501", "短信全部发送失败");
        }

        int newI = i + 1;
        if (newI <= fallbackProviders.size() - 1) {
            defaultProvider = fallbackProviders.get(newI);
        } else {
            log.error("短信全部发送失败，mobileList = {}", JSONUtil.toJsonStr(mobileList));
            return SmsResult.fail("501", "短信全部发送失败");
        }

        // 如果发送失败，尝试使用备用提供商
        log.info("默认短信服务提供商[{}]批量发送失败，尝试使用备用提供商", provider.getName());
        return send(templateId, mobileList, params, defaultProvider, cycleCount + 1);
    }
}