package com.jiuyu.replay.api.controller.common;

import com.jiuyu.replay.api.constant.SignatureConstants;
import com.jiuyu.replay.api.utils.SignatureUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 签名验证测试控制器
 *
 * @author RayChou
 * @date 2025/6/9 14:28
 */
@RestController
@RequestMapping("/replay/common/signature")
public class SignatureTestController {

    @Autowired
    private SignatureUtils signatureUtils;

    /**
     * 测试接口 - POST JSON
     */
    @PostMapping("/test")
    public Map<String, Object> test(@RequestBody(required = false) Map<String, Object> requestBody,
                                    @RequestHeader(SignatureConstants.HEADER_APP_ID) String appId,
                                    @RequestHeader(SignatureConstants.HEADER_TIMESTAMP) String timestamp,
                                    @RequestHeader(SignatureConstants.HEADER_NONCE) String nonce,
                                    @RequestHeader(SignatureConstants.HEADER_FINGERPRINT) String fingerprint,
                                    @RequestHeader(SignatureConstants.HEADER_REQUEST_ID) String requestId,
                                    @RequestHeader(value = SignatureConstants.HEADER_SIGN_TYPE, required = false) String signType) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "签名验证通过");
        result.put("data", requestBody);

        // 返回请求头信息
        Map<String, String> headers = new HashMap<>();
        headers.put("appId", appId);
        headers.put("timestamp", timestamp);
        headers.put("nonce", nonce);
        headers.put("fingerprint", fingerprint);
        headers.put("requestId", requestId);
        headers.put("signType", signType != null ? signType : SignatureConstants.DEFAULT_SIGN_TYPE);
        result.put("headers", headers);

        return result;
    }

    /**
     * 测试接口 - GET
     */
    @GetMapping("/test")
    public Map<String, Object> testGet(@RequestParam Map<String, String> params,
                                       @RequestHeader(SignatureConstants.HEADER_APP_ID) String appId,
                                       @RequestHeader(SignatureConstants.HEADER_TIMESTAMP) String timestamp,
                                       @RequestHeader(SignatureConstants.HEADER_NONCE) String nonce,
                                       @RequestHeader(SignatureConstants.HEADER_FINGERPRINT) String fingerprint,
                                       @RequestHeader(SignatureConstants.HEADER_REQUEST_ID) String requestId,
                                       @RequestHeader(value = SignatureConstants.HEADER_SIGN_TYPE, required = false) String signType) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "GET请求签名验证通过");
        result.put("data", params);

        // 返回请求头信息
        Map<String, String> headers = new HashMap<>();
        headers.put("appId", appId);
        headers.put("timestamp", timestamp);
        headers.put("nonce", nonce);
        headers.put("fingerprint", fingerprint);
        headers.put("requestId", requestId);
        headers.put("signType", signType != null ? signType : SignatureConstants.DEFAULT_SIGN_TYPE);
        result.put("headers", headers);

        return result;
    }

    /**
     * 测试接口 - POST 表单
     */
    @PostMapping(value = "/test-form", consumes = "application/x-www-form-urlencoded")
    public Map<String, Object> testPostForm(@RequestParam Map<String, String> formData,
                                            @RequestHeader(SignatureConstants.HEADER_APP_ID) String appId,
                                            @RequestHeader(SignatureConstants.HEADER_TIMESTAMP) String timestamp,
                                            @RequestHeader(SignatureConstants.HEADER_NONCE) String nonce,
                                            @RequestHeader(SignatureConstants.HEADER_FINGERPRINT) String fingerprint,
                                            @RequestHeader(SignatureConstants.HEADER_REQUEST_ID) String requestId,
                                            @RequestHeader(value = SignatureConstants.HEADER_SIGN_TYPE, required = false) String signType) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "表单请求签名验证通过");
        result.put("data", formData);

        // 返回请求头信息
        Map<String, String> headers = new HashMap<>();
        headers.put("appId", appId);
        headers.put("timestamp", timestamp);
        headers.put("nonce", nonce);
        headers.put("fingerprint", fingerprint);
        headers.put("requestId", requestId);
        headers.put("signType", signType != null ? signType : SignatureConstants.DEFAULT_SIGN_TYPE);
        result.put("headers", headers);

        return result;
    }

    /**
     * 生成签名接口，仅用于测试
     */
    @PostMapping("/generate-signature")
    public Map<String, Object> generateSignature(@RequestBody Map<String, Object> params) {
        String appId = (String) params.get("appId");
        String timestamp = (String) params.get("timestamp");
        String nonce = (String) params.get("nonce");
        String fingerprint = (String) params.get("fingerprint");
        String requestId = (String) params.get("requestId");
        String body = (String) params.get("body");
        String signType = (String) params.get("signType");
        String method = (String) params.get("method");
        String url = (String) params.get("url");
        String contentType = (String) params.get("contentType");

        // 如果未指定签名类型，使用默认签名类型
        if (signType == null || signType.isEmpty()) {
            signType = SignatureConstants.DEFAULT_SIGN_TYPE;
        }

        String signature = signatureUtils.generateSignature(appId, timestamp, nonce, fingerprint, requestId, body, signType);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "生成签名成功");
        result.put("signature", signature);
        result.put("signType", signType);
        result.put("method", method);
        result.put("url", url);
        result.put("contentType", contentType);

        // 返回示例请求头
        Map<String, String> headers = new HashMap<>();
        headers.put(SignatureConstants.HEADER_APP_ID, appId);
        headers.put(SignatureConstants.HEADER_TIMESTAMP, timestamp);
        headers.put(SignatureConstants.HEADER_NONCE, nonce);
        headers.put(SignatureConstants.HEADER_FINGERPRINT, fingerprint);
        headers.put(SignatureConstants.HEADER_REQUEST_ID, requestId);
        headers.put(SignatureConstants.HEADER_SIGNATURE, signature);
        headers.put(SignatureConstants.HEADER_SIGN_TYPE, signType);
        result.put("headers", headers);

        // 展示签名内容
        Map<String, String> params2 = new TreeMap<>();
        params2.put("appId", appId);
        params2.put("timestamp", timestamp);
        params2.put("nonce", nonce);
        if (fingerprint != null) {
            params2.put("fingerprint", fingerprint);
        }
        if (requestId != null) {
            params2.put("requestId", requestId);
        }
        if (body != null && !body.isEmpty()) {
            params2.put("body", body);
        }

        StringBuilder stringToSign = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params2.entrySet()) {
            if (first) {
                first = false;
            } else {
                stringToSign.append(SignatureConstants.PARAM_SEPARATOR);
            }
            stringToSign.append(entry.getKey()).append("=").append(entry.getValue());
        }
        result.put("stringToSign", stringToSign.toString());

        return result;
    }
} 