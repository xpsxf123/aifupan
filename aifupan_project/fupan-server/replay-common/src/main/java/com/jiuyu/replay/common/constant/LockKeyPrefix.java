package com.jiuyu.replay.common.constant;

/**
 * 分布式锁前缀
 * 调用getLockKey方法获取完整锁key
 *
 * @author RayChou
 * @date 2025/6/3 11:23
 */
public enum LockKeyPrefix {

    /**
     * 用户服务
     */
    USER("replay:lock:user:", "用户相关操作锁"),

    /**
     * 消息队列幂等锁
     */
    MQ("replay:lock:mq:", "消息队列相关锁"),

    /**
     * 达人相关锁
     */
    INFLUENCER("replay:lock:influencer:", "达人相关操作锁"),

    /**
     * 主播数据相关锁
     */
    ANCHOR("replay:lock:anchor:", "主播数据相关锁"),

    /**
     * 降级缓存
     */
    CACHE_FALLBACK_DATA("replay:lock:cacheFallbackData:", "降级缓存相关锁"),

    /**
     * 热搜邮箱账号
     */
    HOT_SEARCH_EMAIL_ACCOUNT("replay:lock:hotSearch:account:", "热搜邮箱账号相关锁"),

    VIDEO_INFO("replay:lock:videoInfo:", "视频相关操作锁");


    private final String prefix;
    private final String description;

    LockKeyPrefix(String prefix, String description) {
        this.prefix = prefix;
        this.description = description;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 获取完整锁Key
     */
    public String getLockKey(String businessKey) {
        return prefix + businessKey;
    }
}
