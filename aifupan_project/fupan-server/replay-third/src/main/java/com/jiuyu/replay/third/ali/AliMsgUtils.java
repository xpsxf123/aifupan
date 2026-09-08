package com.jiuyu.replay.third.ali;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.Common;
import com.aliyun.teautil.models.RuntimeOptions;
import com.jiuyu.replay.third.constant.AliMsgProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class AliMsgUtils {

    @Resource
    private AliMsgProperties properties;

    /**
     * 发送绑定子账号短信
     * @param phone
     * @return
     */
    public boolean sendBindingAccountMsg(String phone) {
        return sendAliMsg(phone, properties.getSignName(), properties.getTemplateCodeBindingAccount(), null);
    }

    /**
     * 发生阿里云短信
     * @param phone
     * @param signName
     * @param templateCode
     * @param params
     * @return
     */
    public boolean sendAliMsg(String phone, String signName, String templateCode, Map<String, String> params) {
        try {
            Config config = new Config();
            config.setAccessKeyId(properties.getAccessKeyId());
            config.setAccessKeySecret(properties.getAccessKeySecret());
            config.endpoint = properties.getEndpoint();
            Client client = new Client(config);

            SendSmsRequest sendSmsRequest = new SendSmsRequest();
            sendSmsRequest.setPhoneNumbers(phone);
            sendSmsRequest.setSignName(signName);
            sendSmsRequest.setTemplateCode(templateCode);
            if (ObjectUtil.isNotEmpty(params)) sendSmsRequest.setTemplateParam(JSONUtil.toJsonStr(params));
            SendSmsResponse sendSmsResponse = client.sendSmsWithOptions(sendSmsRequest, new RuntimeOptions());
            if (sendSmsResponse.getStatusCode() == 200 && "OK".equals(sendSmsResponse.getBody().getCode())){
                log.info("{}短信发送成功：{}", phone, Common.toJSONString(sendSmsResponse));
                return true;
            }else{
                log.info("{}短信发送失败：{}", phone, Common.toJSONString(sendSmsResponse));
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
