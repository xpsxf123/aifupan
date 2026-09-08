package com.jiuyu.governance.plugins.sign;

import com.jiuyu.framework.json.JsonTemplate;
import com.jiuyu.framework.sign.SignRequest;
import com.jiuyu.framework.sign.SignatureCalculate;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.signature.core.SignatureModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 为了减少前端的工程量，使用原签名方式替换默认的签名计算
 *
 * @author HeHui
 * @date 2026-03-17 16:59
 */
@Slf4j
@Service
public class ReplaySignatureCalculate implements SignatureCalculate {


    /**
     * 验证签名
     *
     * @param request   请求对象
     * @param model     签名类型
     * @param body      body参数
     * @param params    参数
     * @param secretKey 加密密钥(加密盐)，为防止被穷举。为空则不加密
     * @param sign      签名
     * @param timestamp 时间戳
     * @param paths     路径
     *
     * @return true 验证成功
     *
     * @throws SignatureException 签名异常
     */
    @Override
    public boolean verify(SignRequest request, SignatureModel model, String body, Map<String, ?> params, String secretKey, String sign, long timestamp, String... paths) throws SignatureException {
        String requestId = request.getHeader(SignatureConstants.HEADER_REQUEST_ID);
        if (EmptyUtil.isEmpty(requestId)) {
            return false;
        }
        String appId = request.getHeader(SignatureConstants.HEADER_APP_ID);
        String switchSignatureStr = switchSignatureStr(body, params);
        String calcSignatureStr = generateSignature(appId, appId, String.valueOf(timestamp), request.getHeader(SignatureConstants.HEADER_NONCE), request.getHeader(SignatureConstants.HEADER_FINGERPRINT), requestId, switchSignatureStr, model);
        return Objects.equals(calcSignatureStr, sign);
    }


    private String switchSignatureStr(String body, Map<String, ?> params) {
        if (JsonTemplate.isJson(body)) {
            return body;
        }
        return mapToString(params);
    }


    /**
     * 将Map转换为字符串
     */
    private String mapToString(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }

        return sb.toString();
    }


    /**
     * 生成签名
     *
     * @param appId       应用ID
     * @param timestamp   时间戳
     * @param nonce       随机字符串
     * @param fingerprint 设备指纹
     * @param requestId   请求ID
     * @param body        请求体
     * @param signType    签名方式
     *
     * @return 签名
     */
    public String generateSignature(String appId, String appSecret, String timestamp, String nonce, String fingerprint, String requestId, String body, SignatureModel signType) {
        // 获取应用密钥
        if (appSecret == null) {
            log.warn("[签名工具] 生成签名失败: 未找到应用的密钥, appId={}", appId);
            return null;
        }

        // 按ASCII码排序构建签名字符串
        Map<String, String> params = new TreeMap<>();
        params.put(SignatureConstants.HEADER_APP_ID, appId);
        params.put(SignatureConstants.HEADER_TIMESTAMP, timestamp);
        params.put(SignatureConstants.HEADER_NONCE, nonce);

        if (fingerprint != null) {
            params.put(SignatureConstants.HEADER_FINGERPRINT, fingerprint);
        }

        if (requestId != null) {
            params.put(SignatureConstants.HEADER_REQUEST_ID, requestId);
        }

        if (body != null && !body.isEmpty()) {
            params.put("body", body);
        }

        // 构建待签名字符串，使用&连接
        StringBuilder stringToSign = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first) {
                first = false;
            } else {
                stringToSign.append(SignatureConstants.PARAM_SEPARATOR);
            }
            stringToSign.append(entry.getKey()).append("=").append(entry.getValue());
        }

        String signatureStr = null;
        SignatureModel signTypeToUse = signType != null ? signType : SignatureModel.HMAC_SHA256;

        try {
            if (SignatureModel.MD5.equals(signTypeToUse)) {
                // 使用MD5算法进行签名
                signatureStr = generateMD5Signature(stringToSign.toString(), appSecret);
                log.debug("[签名工具] 使用MD5算法生成签名, 待签名字符串: {}", stringToSign);
            } else {
                // 默认使用HMAC-SHA256算法进行签名
                signatureStr = generateHmacSha256Signature(stringToSign.toString(), appSecret);
                log.debug("[签名工具] 使用HMAC-SHA256算法生成签名, 待签名字符串: {}", stringToSign);
            }
        } catch (Exception e) {
            log.error("[签名工具] 生成签名异常: 异常类型={}, 异常信息={}", e.getClass().getName(), e.getMessage(), e);
            return null;
        }

        return signatureStr;
    }


    /**
     * 生成MD5签名
     */
    private String generateMD5Signature(String data, String secret) throws NoSuchAlgorithmException {
        // 将密钥拼接到数据前后
        String dataWithSecret = secret + data + secret;

        MessageDigest md = MessageDigest.getInstance(SignatureConstants.ALGORITHM_MD5);
        byte[] digest = md.digest(dataWithSecret.getBytes(StandardCharsets.UTF_8));

        // 将byte数组转换为16进制字符串
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    /**
     * 生成HMAC-SHA256签名
     */
    private String generateHmacSha256Signature(String data, String secret) throws Exception {
        Mac hmacSha256 = Mac.getInstance(SignatureConstants.ALGORITHM_HMAC_SHA256);
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SignatureConstants.ALGORITHM_HMAC_SHA256);
        hmacSha256.init(secretKey);

        byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // 对结果进行Base64编码
        return Base64.getEncoder().encodeToString(hash);
    }
}
