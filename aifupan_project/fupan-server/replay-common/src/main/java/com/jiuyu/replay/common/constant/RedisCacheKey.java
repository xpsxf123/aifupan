package com.jiuyu.replay.common.constant;

import cn.hutool.core.util.StrUtil;

/**
 * redis缓存的key的前缀
 */
public class RedisCacheKey {

    /**
     * websocket错误记录缓存key, 格式为 replay:message:webhook:{}
     */
    public static final String websocketErrorCacheKey = "replay:message:webhook:{}";
    /**
     * 视频contextId缓存key, 格式为 replay:ai-video-context-id:{用户id}:{类型}:{自定义key}
     */
    public static final String aiVideoContextIdCacheKey = "replay:ai-client-context-id:{}:{}:{}";
    /**
     * 视频contextId缓存key, 格式为 replay:ai-video-context-id-new:{用户id}:{自定义key}
     */
    public static final String aiVideoContextIdNewCacheKey = "replay:ai-video-context-id-new:{}:{}";
    /**
     * 实体缓存key, 格式为 replay:entity-cache:{实体类名}:{主键}
     */
    public static final String aiContextIdCacheKey = "replay:ai-context-id:{}";

    /**
     * AI 上下文缓存key, 格式为: replay:ai-model-context-id:{uuid}:{模型id}
     */
    public static final String aiModelContextIdCacheKey = "replay:ai-context-id:{}:{}";
    /**
     * 实体缓存key, 格式为 replay:entity-cache:{实体类名}:{主键}
     */
    public static final String aiSessionIdCacheKey = "replay:ai-session-id:{}";

    /**
     * AI上下文session缓存，附带模型名称
     */
    public static final String aiModelSessionIdCacheKey = "replay:ai-session-id:{}:{}";

    /**
     * 临时ark token缓存key, 格式为 replay:ai-temp-ark-token
     */
    public static final String aiTempArkTokenCacheKey = "replay:ai-temp-ark-token";

    /**
     * 实体缓存key, 格式为 replay:entity-cache:{实体类名}:{主键}
     */
    public static final String entityCacheKey = "replay:entity-cache:{}:{}";

    /**
     * 用户资产类型缓存key, 格式为 replay:user-property-type-cache:{用户id}:{资产类型code}
     */
    public static final String userPropertyTypeCacheKey = "replay:user-property-type-cache:{}:{}";

    /**
     * 用户资产类型统计缓存key, 格式为 replay:user-property-type-total-cache:{用户id}:{资产类型code}
     */
    public static final String userPropertyTypeTotalCacheKey = "replay:user-property-type-total-cache:{}:{}";

    /**
     * 用户使用的资产id缓存key, 格式为 replay:user-property-id:{用户id}
     */
    public static final String userUsePropertyIdCacheKey = "replay:user-property-id:{}";

    /**
     * 用户套餐等级缓存key, 格式为 replay:user-package-level:{用户id}
     */
    public static final String userPackageLevelCacheKey = "replay:user-order:package-level:{}";

    /**
     * 用户套餐id缓存key, 格式为 replay:user-package-id:{用户id}
     */
    public static final String userPackageIdCacheKey = "replay:user-order:package-id:{}";

    /**
     * 用户订单(套餐类型)缓存key, 格式为 replay:user:package-order:{用户id}
     */
    public static final String userPackageOrderCacheKey = "replay:user-order:package-order:{}";

    /**
     * 临时用户资产类型缓存key, 格式为 replay:temp-user-property-type-cache:{redis缓存id}:{资产id}:{资产类型}
     */
    public static final String tempUserPropertyTypeCacheKey = "replay:temp-user-property-type-cache:{}:{}:{}";

    /**
     * 临时用户资产类型缓存key 格式为 replay:user-property:{用户id}
     */
    public static final String tempUserPropertyTypeHashCacheKey = "replay:withhold:temp-user-property-type-hash-cache:{}";

    /**
     * 为了关联redisId而设置的键
     */
    public static final String tempUserPropertyTypeAssociationCacheKey = "replay:withhold:temp-user-property-type-association-cache:{}";

    /**
     * websocket数据缓存key, 格式为 replay:words:websocket-data-cache:{视频id}
     */
    public static final String websocketDataCacheKey = "replay:words:websocket-data-cache:{}";

    /**
     * oceanEngineData数据缓存key, 格式为 replay:words:oceanEngine-data-cache:{视频id}
     */
    public static final String oceanEngineDataCacheKey = "replay:words:oceanEngine-data-cache:{}";

    /**
     * 弹幕助手中缓存key, 格式为 replay:words:barrage:{userId}:{key}
     */
    public static final String barrageRedisKey = "replay:words:barrage:{}:{}:{}";

    /**
     * 在线数据缓存key, 格式为 replay:words:onlineChartData:{videoId}
     */
    public static final String onlineChartDataKey = "replay:words:onlineChartData:{}";

    /**
     * 视频对应全部的弹幕数据缓存key, 格式为 replay:words:barrageDataCache:{videoId}
     */
    public static final String barrageDataCacheKey = "replay:words:barrageDataCache:{}";

    /**
     * 视频对应数据看板查询屏障, 格式为 replay:words:dataDashboardBarrierSet:{videoId}
     */
    public static final String dataDashboardBarrierSetKey = "replay:words:dataDashboardBarrierSet:{}";

    /**
     * 获取redis的key
     * @param key       缓存key
     * @param args      参数
     * @return          缓存key
     */
    public static String getRedisKey(String key, Object... args){
        return StrUtil.format(key, args);
    }
}
