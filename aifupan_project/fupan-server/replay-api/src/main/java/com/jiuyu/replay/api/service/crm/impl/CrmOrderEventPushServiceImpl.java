package com.jiuyu.replay.api.service.crm.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.api.bo.crm.CrmOrderEventPushBo;
import com.jiuyu.replay.api.properties.CrmAgentIntegrationProperties;
import com.jiuyu.replay.api.service.crm.CrmOrderEventPushService;
import com.jiuyu.replay.api.vo.crm.CrmOrderEventPushResultVo;
import com.jiuyu.replay.common.utils.HttpUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CRM 订单事件出站推送服务实现
 */
@Service
@AllArgsConstructor
@Slf4j
public class CrmOrderEventPushServiceImpl implements CrmOrderEventPushService {

    private static final String LOG_PREFIX = "[CRM-ORDER-EVENT]";

    private final HttpUtils httpUtils;
    private final CrmAgentIntegrationProperties crmAgentIntegrationProperties;

    /**
     * 推送订单事件到智能体侧接口
     *
     * @param bo 订单事件请求体
     *
     * @return 推送结果
     *
     * @throws Exception HTTP 调用异常
     */
    @Override
    public CrmOrderEventPushResultVo pushOrderEvent(CrmOrderEventPushBo bo) throws Exception {
        String url = buildUrl();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Api-Key", crmAgentIntegrationProperties.getApiKey());
        headers.put("Content-Type", "application/json");
        log.info("{} push start, eventType={}, userId={}, orderId={}",
                LOG_PREFIX,
                bo == null ? null : bo.getEventType(),
                bo == null || bo.getCustomer() == null ? null : bo.getCustomer().getUserId(),
                bo == null || bo.getOrder() == null ? null : bo.getOrder().getOrderId());
        String response = httpUtils.sendHttpPost(url, JSON.toJSONString(bo), headers);
        return parseResult(response);
    }

    private String buildUrl() {
        return safeValue(crmAgentIntegrationProperties.getBaseUrl()) + safeValue(crmAgentIntegrationProperties.getOrderEventPath());
    }

    private CrmOrderEventPushResultVo parseResult(String response) {
        JSONObject jsonObject = JSON.parseObject(response);
        JSONObject data = jsonObject == null ? null : jsonObject.getJSONObject("data");

        CrmOrderEventPushResultVo result = new CrmOrderEventPushResultVo();
        result.setAccepted(data != null && Boolean.TRUE.equals(data.getBoolean("accepted")));
        result.setDedup(data != null && Boolean.TRUE.equals(data.getBoolean("dedup")));
        return result;
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }
}

