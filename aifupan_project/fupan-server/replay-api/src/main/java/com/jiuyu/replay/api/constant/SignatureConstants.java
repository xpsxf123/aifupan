package com.jiuyu.replay.api.constant;

/**
 * 签名验证相关常量
 *
 * @author RayChou
 * @date 2025/6/9 14:28
 */
public class SignatureConstants {

    /**
     * 请求头中的应用ID
     */
    public static final String HEADER_APP_ID = "X-App-Id";

    /**
     * 请求头中的时间戳
     */
    public static final String HEADER_TIMESTAMP = "X-Timestamp";

    /**
     * 请求头中的随机字符串
     */
    public static final String HEADER_NONCE = "X-Nonce";

    /**
     * 请求头中的签名
     */
    public static final String HEADER_SIGNATURE = "X-Signature";

    /**
     * 请求头中的设备指纹
     */
    public static final String HEADER_FINGERPRINT = "X-Fingerprint";

    /**
     * 请求头中的请求ID
     */
    public static final String HEADER_REQUEST_ID = "X-RequestId";

    /**
     * 请求头中的签名方式
     */
    public static final String HEADER_SIGN_TYPE = "X-Sign-Type";

    /**
     * 请求头中的签名方式
     */
    public static final String HEADER_TOKEN = "token";

    /**
     * 签名算法 - HMAC-SHA256
     */
    public static final String ALGORITHM_HMAC_SHA256 = "HmacSHA256";

    /**
     * 签名算法 - MD5
     */
    public static final String ALGORITHM_MD5 = "MD5";

    /**
     * 签名方式 - HMAC-SHA256
     */
    public static final String SIGN_TYPE_HMAC_SHA256 = "HMAC-SHA256";

    /**
     * 签名方式 - MD5
     */
    public static final String SIGN_TYPE_MD5 = "MD5";

    /**
     * 默认签名方式
     */
    public static final String DEFAULT_SIGN_TYPE = SIGN_TYPE_HMAC_SHA256;

    /**
     * 参数分隔符
     */
    public static final String PARAM_SEPARATOR = "&";
} 