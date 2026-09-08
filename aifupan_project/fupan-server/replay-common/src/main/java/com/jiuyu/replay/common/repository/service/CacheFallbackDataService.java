package com.jiuyu.replay.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.common.entity.CacheFallbackDataEntity;
import com.jiuyu.replay.common.enums.CacheFallbackDataTypeEnum;

import java.time.LocalDateTime;

/**
 * <p>
 * 缓存降级数据表 服务类
 * </p>
 *
 * @author RayChou
 * @since 2025-07-10
 */
public interface CacheFallbackDataService extends IService<CacheFallbackDataEntity> {

    /**
     * 保存缓存降级数据
     *
     * @param cacheKey     缓存key
     * @param cacheValue   缓存值（原始对象，将使用Jackson序列化）
     * @param dataTypeEnum 缓存类型枚举
     * @param expireTime   过期时间
     */
    boolean saveCacheFallbackData(String cacheKey, Object cacheValue, CacheFallbackDataTypeEnum dataTypeEnum, LocalDateTime expireTime);

    /**
     * 保存缓存降级数据（带元素类型信息）
     *
     * @param cacheKey     缓存key
     * @param cacheValue   缓存值（原始对象，将使用Jackson序列化）
     * @param dataTypeEnum 缓存类型枚举
     * @param elementType  元素类型（如List<UserDto>中的UserDto类名）
     * @param expireTime   过期时间
     */
    boolean saveCacheFallbackData(String cacheKey, Object cacheValue, CacheFallbackDataTypeEnum dataTypeEnum, String elementType, LocalDateTime expireTime);

    /**
     * 获取缓存值根据缓存key
     *
     * @param cacheKey 缓存key
     * @return
     */
    Object getCacheValueByCache(String cacheKey);

    /**
     * 删除缓存降级数据
     *
     * @param cacheKey 缓存降级key
     * @return
     */
    boolean removeCacheFallbackDataByCacheKey(String cacheKey);

    /**
     * Redis恢复后同步降级期间的缓存临时数据到Redis
     * 将数据库中存储的降级期间缓存数据同步回Redis
     *
     * @return 同步结果信息
     */
    String syncDegradedCacheDataToRedis();
}
