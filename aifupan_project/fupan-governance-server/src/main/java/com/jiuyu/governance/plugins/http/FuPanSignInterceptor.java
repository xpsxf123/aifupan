package com.jiuyu.governance.plugins.http;

import cn.hutool.core.util.IdUtil;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.pojo.ErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.plugins.sign.SignatureConstants;
import com.jiuyu.signature.core.SignGenerate;
import com.jiuyu.signature.core.SignatureModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 爱复盘服务端签名定制版
 *
 * @author HeHui
 * @date 2025-11-13 23:43
 */
public class FuPanSignInterceptor implements ClientHttpRequestInterceptor {


    /**
     * 客户端ID，用于标识请求来源
     */
    private final String clientId;


    /**
     * 密钥对象，用于签名计算
     */
    private final SecretKeySpec keySpec;

    /**
     * 构造函数，初始化签名所需参数
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     */
    public FuPanSignInterceptor(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.keySpec = new SecretKeySpec((EmptyUtil.isNotEmpty(clientSecret) ? clientSecret : "").getBytes(StandardCharsets.UTF_8), SignatureModel.HMAC_SHA256.algorithm);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String requestId = IdUtil.fastSimpleUUID();
        String timestamp = String.valueOf(System.currentTimeMillis());
        // 按ASCII码排序构建签名字符串
        Map<String, String> params = new TreeMap<>();
        params.put(SignatureConstants.HEADER_APP_ID, clientId);
        params.put(SignatureConstants.HEADER_TIMESTAMP, timestamp);
        params.put(SignatureConstants.HEADER_NONCE, requestId);
        params.put(SignatureConstants.HEADER_REQUEST_ID, requestId);
        if (body.length > 0) {
            params.put("body", new String(body, StandardCharsets.UTF_8));
        }
        String signatureValue = getSignatureValue(params);
        HttpHeaders newHeaders = new HttpHeaders(request.getHeaders());
        newHeaders.add(SignatureConstants.HEADER_SIGNATURE, signatureValue);
        newHeaders.add(SignatureConstants.HEADER_TIMESTAMP, timestamp);
        newHeaders.add(SignatureConstants.HEADER_REQUEST_ID, requestId);
        newHeaders.add(SignatureConstants.HEADER_APP_ID, clientId);
        newHeaders.add(SignatureConstants.HEADER_SIGN_TYPE, "HMAC-SHA256");


        return execution.execute(new HttpRequestWrapper(request) {
            @Override
            public HttpHeaders getHeaders() {
                return newHeaders;
            }
        }, body);
    }

    /**
     * 获取签名值
     *
     * @param params 参数
     *
     * @return 签名值
     */
    private String getSignatureValue(Map<String, String> params) {
        Mac mac = null;
        try {
            mac = Mac.getInstance(SignatureModel.HMAC_SHA256.algorithm);
            mac.init(keySpec);
            byte[] result = mac.doFinal(SignGenerate.sortJoin(params).getBytes(StandardCharsets.UTF_8));
            // 对结果进行Base64编码
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new BusinessException(BizErrorCode.GENERAL_FAILED, "签名失败");
        }

    }


}
