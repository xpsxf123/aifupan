package com.jiuyu.replay.third.sms.provider;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.third.shlianlu.LianLuSmsHandler;
import com.jiuyu.replay.third.sms.AbstractSmsProvider;
import com.jiuyu.replay.generic.dto.third.SmsResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * 联麓短信服务提供商
 *
 * @author jiuyu
 */
@Slf4j
@Component
public class LianLuSmsProvider extends AbstractSmsProvider {

    @Resource
    private LianLuSmsHandler lianLuSmsHandler;
    @Resource
    private DictDataFeign dictDataFeign;

    @Override
    public String getName() {
        return "联麓短信";
    }

    public String getCode() {
        return "lianlu";
    }

    @Override
    public SmsResult doSend(String templateId, String mobile, Map<String, String> params) {
        return doSend(templateId, Collections.singletonList(mobile), params);
    }

    @Override
    public SmsResult doSend(String templateId, Collection<String> mobileList, Map<String, String> params) {
        // 验证templateId
        DictDataListVo dictDataListVo = dictDataFeign.dictDataByLabel("unified_providers_template_code", templateId);
        if (dictDataListVo == null) {
            log.error("未配置短信的系统templateId={}", templateId);
            return SmsResult.fail("502", "未配置短信的系统templateId=" + templateId);
        }

        String value = dictDataListVo.getValue();
        JSONObject jsonObject = JSONObject.parseObject(value);
        templateId = jsonObject.getString(getCode());

        if (templateId == null) {
            log.error("未配置短信的系统templateId={}, code={}", templateId, getCode());
            return SmsResult.fail("503", StrUtil.format("未配置短信的系统templateId={}, code={}", templateId, getCode()));
        }


        LianLuSmsHandler.Result result = lianLuSmsHandler.send(templateId, mobileList, params);
        return convertResult(result);
    }

    /**
     * 将联麓短信结果转换为统一的短信结果
     *
     * @param result 联麓短信结果
     * @return 统一短信结果
     */
    private SmsResult convertResult(LianLuSmsHandler.Result result) {
        if (result == null) {
            log.error("联麓短信服务返回结果为空");
            return SmsResult.fail("500", "联麓短信服务返回结果为空");
        }

        if (result.success()) {
            return SmsResult.success()
                    .setRequestId(result.getTaskId())
                    .setCount(result.getCount());
        } else {
            return SmsResult.fail(result.getStatus(), result.getMessage())
                    .setRequestId(result.getTaskId())
                    .setCount(result.getCount())
                    .setIllegalMobiles(result.getIllegalMobiles());
        }
    }
} 