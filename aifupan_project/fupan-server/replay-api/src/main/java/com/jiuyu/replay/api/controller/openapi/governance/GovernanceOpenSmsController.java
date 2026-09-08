package com.jiuyu.replay.api.controller.openapi.governance;

import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.api.controller.openapi.governance.request.SmsBatchPushRequest;
import com.jiuyu.replay.api.controller.openapi.governance.request.SmsPushRequest;
import com.jiuyu.replay.api.interceptor.FeatureSignature;
import com.jiuyu.replay.generic.dto.third.SmsResult;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.sms.SmsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 对企业管理平台 开发的短信API
 *
 * @author HeHui
 * @date 2026-03-24 18:01
 */
@FeatureSignature(client = "governance")
@RestController
@RequestMapping("/replay/openapi/governance/sms")
public class GovernanceOpenSmsController {

    private final SmsService smsService;

    public GovernanceOpenSmsController(SmsService smsService) {
        this.smsService = smsService;
    }


    /**
     * 推送短信
     *
     * @param request 请求
     *
     * @return {@link R }<{@link SmsResult }>
     */
    @PostMapping("/push")
    public R<SmsResult> push(@RequestBody @Validated SmsPushRequest request) {
        Map<String, String> param = new HashMap<>();
        if (EmptyUtil.isNotEmpty(request.getParams())) {
            for (SmsPushRequest.KV requestParam : request.getParams()) {
                param.put(requestParam.getKey(), requestParam.getValue());
            }
        }
        return R.ok(smsService.send(request.getTemplateId(), request.getMobile(), param, request.getProviderName()));
    }


    /**
     * 批量推送
     *
     * @param request 请求
     *
     * @return {@link R }<{@link SmsResult }>
     */
    @PostMapping("/batch-push")
    public R<SmsResult> batchPush(@RequestBody @Validated SmsBatchPushRequest request) {
        Map<String, String> param = new HashMap<>();
        if (EmptyUtil.isNotEmpty(request.getParams())) {
            for (SmsBatchPushRequest.KV requestParam : request.getParams()) {
                param.put(requestParam.getKey(), requestParam.getValue());
            }
        }
        return R.ok(smsService.send(request.getTemplateId(), request.getMobileList(), param, request.getProviderName()));
    }
}
