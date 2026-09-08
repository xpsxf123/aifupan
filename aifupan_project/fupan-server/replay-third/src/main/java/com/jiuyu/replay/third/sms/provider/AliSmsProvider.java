package com.jiuyu.replay.third.sms.provider;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONObject;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.Common;
import com.aliyun.teautil.models.RuntimeOptions;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.third.constant.AliMsgProperties;
import com.jiuyu.replay.third.sms.AbstractSmsProvider;
import com.jiuyu.replay.generic.dto.third.SmsResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 阿里云短信服务提供商
 *
 * @author jiuyu
 */
@Slf4j
@Component
public class AliSmsProvider extends AbstractSmsProvider {

    @Resource
    private AliMsgProperties properties;
    @Resource
    private DictDataFeign dictDataFeign;

    @Override
    public String getName() {
        return "阿里云短信";
    }

    public String getCode() {
        return "ali";
    }

    @Override
    public SmsResult doSend(String templateId, String mobile, Map<String, String> params) {
        if (!PhoneUtil.isMobile(mobile)) {
            log.error("手机号码格式错误 {}", mobile);
            return SmsResult.fail("400", "手机号码格式错误");
        }
        if (CharSequenceUtil.isBlank(templateId)) {
            log.error("缺少模板ID {}", templateId);
            return SmsResult.fail("400", "缺少模板ID");
        }
        return doSend(templateId, List.of(mobile), params);
    }

    @Override
    public SmsResult doSend(String templateId, Collection<String> mobileList, Map<String, String> params) {
        if (CollUtil.isEmpty(mobileList)) {
            log.error("手机号码列表为空 {}", mobileList);
            return SmsResult.fail("400", "手机号码列表为空");
        }

        // 验证templateId
        DictDataListVo dictDataListVo = dictDataFeign.dictDataByLabel("unified_providers_template_code", templateId);
        if (dictDataListVo == null) {
            log.error("未配置短信的系统templateId {}", templateId);
            return SmsResult.fail("502", "未配置短信的系统templateId=" + templateId);
        }

        String value = dictDataListVo.getValue();
        JSONObject jsonObject = JSONObject.parseObject(value);
        templateId = jsonObject.getString(getCode());

        if (templateId == null) {
            log.error("未配置短信的系统templateId = {}, code = {}", templateId, getCode());
            return SmsResult.fail("503", StrUtil.format("未配置短信的系统templateId={}, code={}", templateId, getCode()));
        }

        // 验证手机号格式
        List<String> validMobiles = mobileList.stream()
                .filter(PhoneUtil::isMobile)
                .distinct()
                .collect(Collectors.toList());

        if (validMobiles.size() != mobileList.size()) {
            List<String> invalidMobiles = new ArrayList<>(mobileList);
            invalidMobiles.removeAll(validMobiles);
            log.error("存在无效的手机号码 {}", invalidMobiles);
            return SmsResult.fail("400", "存在无效的手机号码")
                    .setIllegalMobiles(invalidMobiles);
        }

        try {
            // 配置阿里云客户端
            Config config = new Config();
            config.setAccessKeyId(properties.getAccessKeyId());
            config.setAccessKeySecret(properties.getAccessKeySecret());
            config.endpoint = properties.getEndpoint();
            Client client = new Client(config);

            // 构建请求
            SendSmsRequest sendSmsRequest = new SendSmsRequest();
            sendSmsRequest.setPhoneNumbers(String.join(",", validMobiles));
            sendSmsRequest.setSignName(properties.getSignName());
            sendSmsRequest.setTemplateCode(templateId);
            if (params != null && !params.isEmpty()) {
                sendSmsRequest.setTemplateParam(JSONUtil.toJsonStr(params));
            }

            // 发送短信
            SendSmsResponse response = client.sendSmsWithOptions(sendSmsRequest, new RuntimeOptions());

            // 处理响应
            if (response.getStatusCode() == 200 && "OK".equals(response.getBody().getCode())) {
                log.info("阿里云短信发送成功, 手机号: {}, 模板ID: {}", validMobiles, templateId);
                return SmsResult.success()
                        .setRequestId(response.getBody().getRequestId())
                        .setCount(validMobiles.size());
            } else {
                log.error("阿里云短信发送失败, 手机号: {}, 模板ID: {}, 错误: {}",
                        validMobiles, templateId, Common.toJSONString(response));
                return SmsResult.fail(
                        response.getBody().getCode(),
                        response.getBody().getMessage()
                ).setRequestId(response.getBody().getRequestId());
            }
        } catch (Exception e) {
            log.error("阿里云短信发送异常, 手机号: {}, 模板ID: {}", validMobiles, templateId, e);
            return SmsResult.fail("500", "短信发送异常: " + e.getMessage());
        }
    }
} 