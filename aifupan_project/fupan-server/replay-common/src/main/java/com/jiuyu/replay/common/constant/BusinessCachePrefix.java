package com.jiuyu.replay.common.constant;

/**
 * 业务缓存前缀
 *
 * @author RayChou
 * @date 2025/6/7 11:23
 */
public enum BusinessCachePrefix {

    /**
     * 邀请有礼用户录制分析直播场数redis缓存前缀
     */
    USER_ANALYZE_LIVE_BROADCAST_COUNT_CACHE("replay:business:user:analyzeLiveBroadcastCount:", "邀请有礼用户录制分析直播场数缓存前缀"),
    /**
     * 用户token在线状态心跳检查redis缓存前缀
     */
    USER_TOKEN_HEARTBEAT_CACHE("replay:business:user:tokenHeartbeat:", "用户token在线状态心跳检查redis缓存前缀"),

    /**
     * 热搜邮箱账号池-同城池缓存前缀
     * 格式：replay:business:hotSearch:city:{城市}
     * 例如：replay:business:hotSearch:city:上海
     * 数据结构：ZSet
     * - member: accountId
     * - score: 最后使用时间戳（毫秒）
     * - TTL: 7天
     */
    HOT_SEARCH_EMAIL_CITY_POOL("replay:business:hotSearch:city:", "热搜邮箱账号池-同城池"),

    /**
     * 热搜邮箱账号池-全局池缓存前缀
     * 格式：replay:business:hotSearch:global
     * 数据结构：ZSet
     * - member: accountId
     * - score: 最后使用时间戳（毫秒）
     * - TTL: 7天
     */
    HOT_SEARCH_EMAIL_GLOBAL_POOL("replay:business:hotSearch:global", "热搜邮箱账号池-全局池"),

    /**
     * 热搜邮箱账号池-账号详情 Hash
     * 格式：replay:business:hotSearch:accounts
     * 数据结构：Hash
     * - field: accountId
     * - value: JSON {"availableCount": 5, "city": "上海", "lastUseTime": 1732780800000, "lastIp": "1.1.1.1"}
     * - TTL: 7天
     */
    HOT_SEARCH_EMAIL_ACCOUNTS("replay:business:hotSearch:accounts", "热搜邮箱账号池-账号详情"),

    /**
     * 热搜邮箱账号池-IP 映射
     * 格式：replay:business:hotSearch:ip:{ip}
     * 数据结构：String
     * - value: accountId
     * - TTL: 1小时
     */
    HOT_SEARCH_EMAIL_IP_MAPPING("replay:business:hotSearch:ip:", "热搜邮箱账号池-IP映射"),

    /**
     * 热搜邮箱IP-City缓存
     * 热搜邮箱账号池-IP 映射
     * 格式：replay:business:hotSearch:ip:{ip}
     * 数据结构：String
     * - value: accountId
     * - TTL: 1小时
     */
    HOT_SEARCH_EMAIL_IP_CITY("replay:business:hotSearch:ipCity:", "热搜邮箱IP-City缓存"),

    /**
     * 热搜邮箱账号池告警已发送标记
     * 格式：replay:business:hotSearch:alert:sent
     * 数据结构：String
     * - value: "1"
     * - TTL: 30分钟
     */
    HOT_SEARCH_EMAIL_ALERT_SENT("replay:business:hotSearch:alert:sent", "热搜邮箱账号池告警已发送标记"),

    /**
     * 热搜邮箱账号单个IP每天最大失败次数的标记锁
     * 锁格式：replay:business:hotSearch:lock:{ip}
     * 数据结构：String
     * - value: "1"
     * - TTL: 动态截止到23:59:59
     */
    HOT_SEARCH_EMAIL_DAY_LIMIT("replay:business:hotSearch:lock:", "热搜邮箱账号单个IP每天最大失败次数的标记锁");

    public String prefix;
    public String description;

    BusinessCachePrefix(String prefix, String description) {
        this.prefix = prefix;
        this.description = description;
    }
}
