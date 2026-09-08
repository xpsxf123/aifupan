package com.jiuyu.replay.api.utils;

import com.jiuyu.replay.api.config.SignatureConfig;
import com.jiuyu.replay.api.constant.SignatureConstants;
import com.jiuyu.replay.common.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 签名工具类
 *
 * @author RayChou
 * @date 2025/6/9 14:28
 */
@Component
public class SignatureUtils {

    private static final Logger log = LoggerFactory.getLogger(SignatureUtils.class);

    @Autowired
    private SignatureConfig signatureConfig;

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
     * @return 签名
     */
    public String generateSignature(String appId, String timestamp, String nonce, String fingerprint, String requestId, String body, String signType) {
        // 获取应用密钥
        String appSecret = getAppSecret(appId);
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
        String signTypeToUse = signType != null ? signType : SignatureConstants.DEFAULT_SIGN_TYPE;

        try {
            if (SignatureConstants.SIGN_TYPE_MD5.equals(signTypeToUse)) {
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
        log.debug("[签名工具] 签名生成: URI={}, appId={}, signType={}, 签名值={}",
                RequestContext.getRequestURI(), appId, signTypeToUse, signatureStr);
        return signatureStr;
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
     * 验证签名
     *
     * @param appId       应用ID
     * @param timestamp   时间戳
     * @param nonce       随机字符串
     * @param fingerprint 设备指纹
     * @param requestId   请求ID
     * @param body        请求体
     * @param signature   待验证的签名
     * @param signType    签名方式
     * @return 是否验证通过
     */
    public boolean verifySignature(String appId, String timestamp, String nonce, String fingerprint, String requestId, String body, String signature, String signType) {
        log.debug("[签名工具] 开始验证签名: URI={}, appId={}, requestId={}, signType={}",
                RequestContext.getRequestURI(), appId, requestId, signType);

        // 使用相同的算法生成签名
        String calculatedSignature = generateSignature(appId, timestamp, nonce, fingerprint, requestId, body, signType);

        if (calculatedSignature == null) {
            log.warn("[签名工具] 签名验证失败: 生成签名返回null, appId={}, URI={}",
                    appId, RequestContext.getRequestURI());
            return false;
        }

        // 比较生成的签名和请求中的签名是否匹配
        boolean isValid = calculatedSignature.equals(signature);

        if (!isValid) {
            log.warn("[签名工具] 签名验证失败: 签名不匹配, URI={}, appId={}, 期望签名={}, 实际签名={}",
                    RequestContext.getRequestURI(), appId, calculatedSignature, signature);
        } else {
            log.debug("[签名工具] 签名验证通过: URI={}, appId={}",
                    RequestContext.getRequestURI(), appId);
        }

        return isValid;
    }

    /**
     * 生成签名 - 重载方法，使用默认签名方式
     */
    public String generateSignature(String appId, String timestamp, String nonce, String fingerprint, String requestId, String body) {
        return generateSignature(appId, timestamp, nonce, fingerprint, requestId, body, SignatureConstants.DEFAULT_SIGN_TYPE);
    }

    /**
     * 验证签名 - 重载方法，使用默认签名方式
     */
    public boolean verifySignature(String appId, String timestamp, String nonce, String fingerprint, String requestId, String body, String signature) {
        return verifySignature(appId, timestamp, nonce, fingerprint, requestId, body, signature, SignatureConstants.DEFAULT_SIGN_TYPE);
    }

    /**
     * 根据appId获取对应的密钥
     *
     * @param appId 应用ID
     * @return 应用密钥
     */
    private String getAppSecret(String appId) {
        Optional<SignatureConfig.AppConfig> appConfig = signatureConfig.getApps().stream().filter(app -> app.getAppId().equals(appId)).findFirst();

        return appConfig.map(SignatureConfig.AppConfig::getAppSecret).orElse(null);
    }
} 