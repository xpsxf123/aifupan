package com.jiuyu.replay.common.context;

/**
 * 请求上下文，用于线程内共享请求信息
 *
 * @author RayChou
 * @date 2025/6/9 14:28
 */
public class RequestContext {

    /**
     * 请求信息实体类
     */
    public static class RequestInfo {
        private String requestURI;
        private String requestId;
        private String fingerprint;
        private String appId;
        private String ipAddress;
        private String timestamp;
        private String nonce;
        private String signType;
        private Long userId;

        // 可以根据需要添加更多字段

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getFingerprint() {
            return fingerprint;
        }

        public void setFingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getNonce() {
            return nonce;
        }

        public void setNonce(String nonce) {
            this.nonce = nonce;
        }

        public String getSignType() {
            return signType;
        }

        public void setSignType(String signType) {
            this.signType = signType;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getRequestURI() {
            return requestURI;
        }

        public void setRequestURI(String requestURI) {
            this.requestURI = requestURI;
        }
    }

    /**
     * 使用RequestInfo实体类作为ThreadLocal的泛型类型
     */
    private static final ThreadLocal<RequestInfo> CONTEXT = new ThreadLocal<>();

    /**
     * 初始化上下文
     */
    public static void init() {
        if (CONTEXT.get() == null) {
            CONTEXT.set(new RequestInfo());
        }
    }

    /**
     * 获取当前请求信息
     *
     * @return 请求信息
     */
    public static RequestInfo getCurrentRequestInfo() {
        init();
        return CONTEXT.get();
    }

    /**
     * 获取请求ID
     *
     * @return 请求ID
     */
    public static String getRequestId() {
        RequestInfo info = CONTEXT.get();
        return info != null ? info.getRequestId() : null;
    }

    /**
     * 设置请求ID
     *
     * @param requestId 请求ID
     */
    public static void setRequestId(String requestId) {
        init();
        CONTEXT.get().setRequestId(requestId);
    }

    /**
     * 获取设备指纹
     *
     * @return 设备指纹
     */
    public static String getFingerprint() {
        RequestInfo info = CONTEXT.get();
        return info != null ? info.getFingerprint() : null;
    }

    /**
     * 设置设备指纹
     *
     * @param fingerprint 设备指纹
     */
    public static void setFingerprint(String fingerprint) {
        init();
        CONTEXT.get().setFingerprint(fingerprint);
    }

    /**
     * 获取应用ID
     *
     * @return 应用ID
     */
    public static String getAppId() {
        RequestInfo info = CONTEXT.get();
        return info != null ? info.getAppId() : null;
    }

    /**
     * 设置应用ID
     *
     * @param appId 应用ID
     */
    public static void setAppId(String appId) {
        init();
        CONTEXT.get().setAppId(appId);
    }

    /**
     * 获取IP地址
     *
     * @return IP地址
     */
    public static String getIpAddress() {
        RequestInfo info = CONTEXT.get();
        return info != null ? info.getIpAddress() : null;
    }

    /**
     * 设置IP地址
     *
     * @param ipAddress IP地址
     */
    public static void setIpAddress(String ipAddress) {
        init();
        CONTEXT.get().setIpAddress(ipAddress);
    }

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
    public static String getTimestamp() {
        RequestInfo info = CONTEXT.get();
        return info != null ? info.getTimestamp() : null;
    }

    /**
     * 设置时间戳
     *
     * @param timestamp 时间戳
     */
    public static void setTimestamp(String timestamp) {
        init();
        CONTEXT.get().setTimestamp(timestamp);
    }

    /**
     * 获取随机字符串
     *
     * @return 随机字符串
     */
    public static String getNonce() {
        RequestInfo info = CONTEXT.get();
        return info != null ? info.getNonce() : null;
    }

    /**
     * 设置随机字符串
     *
     * @param nonce 随机字符串
     */
    public static void setNonce(String nonce) {
        init();
        CONTEXT.get().setNonce(nonce);
    }

    /**
     * 获取签名类型
     *
     * @return 签名类型
     */
    public static String getSignType() {
        RequestInfo info = CONTEXT.get();
        return info != null ? info.getSignType() : null;
    }

    /**
     * 设置签名类型
     *
     * @param signType 签名类型
     */
    public static void setSignType(String signType) {
        init();
        CONTEXT.get().setSignType(signType);
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        RequestInfo info = CONTEXT.get();
        return info != null ? info.getUserId() : null;
    }

    /**
     * 设置用户ID
     *
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        init();
        CONTEXT.get().setUserId(userId);
    }

    /**
     * 获取签名类型
     *
     * @return 签名类型
     */
    public static String getRequestURI() {
        RequestInfo info = CONTEXT.get();
        return info != null ? info.getRequestURI() : null;
    }

    /**
     * 设置签名类型
     *
     * @param requestURI 签名类型
     */
    public static void setRequestURI(String requestURI) {
        init();
        CONTEXT.get().setRequestURI(requestURI);
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }
} 