package com.jiuyu.replay.third.api;

import com.jiuyu.replay.generic.dto.third.SmsResult;
import com.jiuyu.replay.generic.feign.third.SmsServiceFeign;
import com.jiuyu.replay.third.sms.SmsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author ：lujie
 * @date ：2025/10/11 17:00
 */
@Component
@AllArgsConstructor
public class SmsServiceApi implements SmsServiceFeign {

    private SmsService smsService;

    @Override
    public SmsResult send(String templateId, String mobile, Map<String, String> params, String providerName) {
        return smsService.send(templateId, mobile, params, providerName);
    }
}
