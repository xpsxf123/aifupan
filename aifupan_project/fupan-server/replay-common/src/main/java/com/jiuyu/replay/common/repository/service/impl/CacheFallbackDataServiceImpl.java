package com.jiuyu.replay.common.repository.service.impl;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import com.jiuyu.replay.common.entity.CacheFallbackDataEntity;
import com.jiuyu.replay.common.enums.CacheFallbackDataTypeEnum;
import com.jiuyu.replay.common.repository.dao.CacheFallbackDataDao;
import com.jiuyu.replay.common.repository.service.CacheFallbackDataService;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * 缓存降级数据表 服务实现类
 * </p>
 *
 * @author RayChou
 * @since 2025-07-10
 */
@Service
@Slf4j
public class CacheFallbackDataServiceImpl extends ServiceImpl<CacheFallbackDataDao, CacheFallbackDataEntity> implements CacheFallbackDataService {

    @Resource
    private ResilientRedisTemplate<String, Object> resilientRedisTemplate;

    /**
     * 使用Spring容器中的ObjectMapper
     */
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean saveCacheFallbackData(String cacheKey, Object cacheValue, CacheFallbackDataTypeEnum dataTypeEnum, LocalDateTime expireTime) {
        // 调用带elementType的重载方法，elementType为null表示自动推断
        return saveCacheFallbackData(cacheKey, cacheValue, dataTypeEnum, null, expireTime);
    }

    @Override
    public boolean saveCacheFallbackData(String cacheKey, Object cacheValue, CacheFallbackDataTypeEnum dataTypeEnum, String elementType, LocalDateTime expireTime) {
        if (StrUtil.isBlank(cacheKey)) {
            log.error("[缓存降级持久化] cacheKey is null");
            return false;
        }
        if (Objects.isNull(cacheValue)) {
            log.error("[缓存降级持久化] cacheValue is null");
            return false;
        }
        if (Objects.isNull(dataTypeEnum)) {
            log.error("[缓存降级持久化] dataTypeEnum is null");
            return false;
        }
        if (Objects.isNull(expireTime) || LocalDateTime.now().isAfter(expireTime)) {
            log.error("[缓存降级持久化] expireTime is null or now time is after expireTime");
            return false;
        }

        try {
            // 如果业务方没有传入elementType，则尝试自动推断
            if (elementType == null || elementType.trim().isEmpty()) {
                elementType = getElementType(cacheValue, dataTypeEnum);
            }

            CacheFallbackDataEntity cacheFallbackDataEntity = new CacheFallbackDataEntity();
            cacheFallbackDataEntity.setId(SnowflakeManager.nextValue());
            cacheFallbackDataEntity.setCacheKey(cacheKey.trim());
            cacheFallbackDataEntity.setCacheValue(objectMapper.writeValueAsString(cacheValue));
            cacheFallbackDataEntity.setDataType(dataTypeEnum.getCode());
            cacheFallbackDataEntity.setElementType(elementType);
            cacheFallbackDataEntity.setExpireTime(expireTime);
            cacheFallbackDataEntity.setCreateDate(LocalDateTime.now());
            cacheFallbackDataEntity.setUpdateDate(LocalDateTime.now());

            log.debug("[缓存降级持久化] 保存数据: cacheKey={}, dataType={}, elementType={}",
                    cacheKey, dataTypeEnum, elementType);

            return save(cacheFallbackDataEntity);
        } catch (Exception e) {
            log.error("[缓存降级持久化] 序列化失败, cacheKey: {}, cacheValue: {}", cacheKey, cacheValue, e);
            return false;
        }
    }

    @Override
    public Object getCacheValueByCache(String cacheKey) {
        if (StrUtil.isBlank(cacheKey)) {
            log.error("[缓存降级持久化] cacheKey is null.");
            return null;
        }
        List<CacheFallbackDataEntity> fallbackDataEntityList = list(new LambdaQueryWrapper<>(CacheFallbackDataEntity.class).eq(CacheFallbackDataEntity::getCacheKey, cacheKey).ge(CacheFallbackDataEntity::getExpireTime, LocalDateTime.now()).orderByDesc(CacheFallbackDataEntity::getCreateDate));
        if (CollectionUtil.isEmpty(fallbackDataEntityList)) {
            return null;
        }
        int size = fallbackDataEntityList.size();
        if (size > 1) {
            log.warn("[缓存降级持久化] get more than one fallbackDataEntity cacheKey:{} size:{}", cacheKey, size);
        }
        CacheFallbackDataEntity entity = fallbackDataEntityList.get(0);
        String cacheValue = entity.getCacheValue();
        CacheFallbackDataTypeEnum dataType = CacheFallbackDataTypeEnum.getByCode(entity.getDataType().intValue());
        String elementType = entity.getElementType();

        // 使用新的精确反序列化方法
        return deserializeWithTypeInfo(cacheValue, dataType, elementType);
    }

    @Override
    public boolean removeCacheFallbackDataByCacheKey(String cacheKey) {
        if (StrUtil.isBlank(cacheKey)) {
            log.error("[缓存降级持久化] cacheKey is null..");
            return false;
        }
        return remove(new LambdaQueryWrapper<>(CacheFallbackDataEntity.class).eq(CacheFallbackDataEntity::getCacheKey, cacheKey));
    }

    @Override
    public String syncDegradedCacheDataToRedis() {
        log.info("[Redis降级] 开始同步降级期间的缓存数据到Redis...");

        if (resilientRedisTemplate.isDegraded()) {
            log.warn("[Redis降级] Redis仍处于降级状态，无法执行同步操作");
            return "Redis仍处于降级状态，同步失败";
        }

        int syncCount = 0;
        int errorCount = 0;

        try {
            // 查询所有有效的降级缓存数据（未过期的）
            List<CacheFallbackDataEntity> validCacheData = list(new LambdaQueryWrapper<CacheFallbackDataEntity>().gt(CacheFallbackDataEntity::getExpireTime, LocalDateTime.now()).eq(CacheFallbackDataEntity::getIsDeleted, false));

            log.info("[Redis降级] 找到 {} 条有效的降级缓存数据", validCacheData.size());

            for (CacheFallbackDataEntity cacheEntity : validCacheData) {
                try {
                    String cacheKey = cacheEntity.getCacheKey();
                    String cacheValue = cacheEntity.getCacheValue();
                    CacheFallbackDataTypeEnum dataType = CacheFallbackDataTypeEnum.getByCode(cacheEntity.getDataType().intValue());

                    // 检查Redis中是否已存在，避免覆盖
                    if (!resilientRedisTemplate.getRedisTemplate().hasKey(cacheKey)) {
                        // 计算剩余过期时间
                        long remainingSeconds = Duration.between(LocalDateTime.now(), cacheEntity.getExpireTime()).getSeconds();
                        if (remainingSeconds > 0) {
                            // 根据数据类型同步到Redis
                            String elementType = cacheEntity.getElementType();
                            boolean syncSuccess = syncCacheDataByType(cacheKey, cacheValue, dataType, elementType, remainingSeconds);
                            if (syncSuccess) {
                                syncCount++;
                                log.debug("[Redis降级] 同步缓存数据成功: {}", cacheKey);
                            } else {
                                errorCount++;
                                log.warn("[Redis降级] 同步缓存数据失败: {}", cacheKey);
                            }
                        } else {
                            log.debug("[Redis降级] 缓存数据已过期，跳过同步: {}", cacheKey);
                        }
                    } else {
                        log.debug("[Redis降级] Redis中已存在该key，跳过同步: {}", cacheKey);
                    }

                } catch (Exception e) {
                    errorCount++;
                    log.warn("[Redis降级] 同步单条缓存数据失败, cacheKey: {}, 错误: {}", cacheEntity.getCacheKey(), e.getMessage());
                }
            }

            String result = String.format("同步完成 - 成功: %d条, 错误: %d条", syncCount, errorCount);
            log.info("[Redis降级] {}", result);
            return result;

        } catch (Exception e) {
            log.error("[Redis降级] 同步降级期间缓存数据失败: {}", e.getMessage(), e);
            return "同步失败: " + e.getMessage();
        }
    }

    /**
     * 根据数据类型同步缓存数据到Redis
     * 使用新的精确反序列化方法
     */
    private boolean syncCacheDataByType(String cacheKey, String cacheValue, CacheFallbackDataTypeEnum dataType, String elementType, long expireSeconds) {
        try {
            Duration expireDuration = Duration.ofSeconds(expireSeconds);

            // 使用新的精确反序列化方法
            Object deserializedValue = deserializeWithTypeInfo(cacheValue, dataType, elementType);

            switch (dataType) {
                case STRING:
                    // 直接设置反序列化后的值
                    resilientRedisTemplate.getRedisTemplate().opsForValue().set(cacheKey, deserializedValue, expireDuration);
                    break;

                case LIST:
                    // 检查类型并设置List
                    if (deserializedValue instanceof List) {
                        List<?> list = (List<?>) deserializedValue;
                        resilientRedisTemplate.getRedisTemplate().opsForList().rightPushAll(cacheKey, list.toArray());
                        resilientRedisTemplate.getRedisTemplate().expire(cacheKey, expireDuration);
                    } else {
                        log.warn("[Redis降级] 期望List类型，但反序列化得到: {}", deserializedValue.getClass());
                        return false;
                    }
                    break;

                case HASH:
                    // 检查类型并设置Map
                    if (deserializedValue instanceof Map) {
                        resilientRedisTemplate.getRedisTemplate().opsForHash().putAll(cacheKey, (Map<?, ?>) deserializedValue);
                        resilientRedisTemplate.getRedisTemplate().expire(cacheKey, expireDuration);
                    } else {
                        log.warn("[Redis降级] 期望Map类型，但反序列化得到: {}", deserializedValue.getClass());
                        return false;
                    }
                    break;

                case SET:
                    // 检查类型并设置Set
                    if (deserializedValue instanceof Set) {
                        resilientRedisTemplate.getRedisTemplate().opsForSet().add(cacheKey, ((Set<?>) deserializedValue).toArray());
                        resilientRedisTemplate.getRedisTemplate().expire(cacheKey, expireDuration);
                    } else {
                        log.warn("[Redis降级] 期望Set类型，但反序列化得到: {}", deserializedValue.getClass());
                        return false;
                    }
                    break;

                case ZSET:
                    // ZSET处理：期望数据格式为Map<String, Double>（元素->分数）
                    if (deserializedValue instanceof Map) {
                        Map<?, ?> zsetMap = (Map<?, ?>) deserializedValue;
                        if (!zsetMap.isEmpty()) {
                            // 遍历Map，将每个元素和分数添加到ZSET
                            for (Map.Entry<?, ?> entry : zsetMap.entrySet()) {
                                Object member = entry.getKey();
                                Object scoreObj = entry.getValue();

                                // 确保分数是数字类型
                                double score;
                                if (scoreObj instanceof Number) {
                                    score = ((Number) scoreObj).doubleValue();
                                } else {
                                    // 尝试解析字符串为数字
                                    try {
                                        score = Double.parseDouble(scoreObj.toString());
                                    } catch (NumberFormatException e) {
                                        log.warn("[Redis降级] ZSET分数解析失败，使用默认分数0: {}", scoreObj);
                                        score = 0.0;
                                    }
                                }

                                resilientRedisTemplate.getRedisTemplate().opsForZSet().add(cacheKey, member, score);
                            }
                            resilientRedisTemplate.getRedisTemplate().expire(cacheKey, expireDuration);
                            log.debug("[Redis降级] ZSET同步成功，元素数量: {}", zsetMap.size());
                        }
                    } else {
                        log.warn("[Redis降级] 期望Map类型（ZSET），但反序列化得到: {}", deserializedValue.getClass());
                        return false;
                    }
                    break;

                default:
                    log.warn("[Redis降级] 未知的数据类型: {}, cacheKey: {}", dataType, cacheKey);
                    return false;
            }

            return true;

        } catch (Exception e) {
            log.error("[Redis降级] Redis序列化器反序列化失败, cacheKey: {}, dataType: {}, 错误: {}", cacheKey, dataType, e.getMessage());
            return false;
        }
    }


    /**
     * 获取元素类型信息，用于精确反序列化
     *
     * @param cacheValue   缓存值
     * @param dataTypeEnum 数据类型枚举
     * @return 元素类型字符串
     */
    private String getElementType(Object cacheValue, CacheFallbackDataTypeEnum dataTypeEnum) {
        if (cacheValue == null) {
            return null;
        }

        switch (dataTypeEnum) {
            case LIST:
                if (cacheValue instanceof List) {
                    List<?> list = (List<?>) cacheValue;
                    if (!list.isEmpty()) {
                        Object firstElement = list.get(0);
                        return firstElement.getClass().getName();
                    }
                }
                return "java.lang.Object";  // 默认类型

            case HASH:
                return "java.util.Map";  // Map类型

            case SET:
                if (cacheValue instanceof Set) {
                    Set<?> set = (Set<?>) cacheValue;
                    if (!set.isEmpty()) {
                        Object firstElement = set.iterator().next();
                        return firstElement.getClass().getName();
                    }
                }
                return "java.lang.Object";  // 默认类型

            case ZSET:
                // ZSET通常存储为Map<String, Double>格式
                if (cacheValue instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) cacheValue;
                    if (!map.isEmpty()) {
                        // 获取第一个key的类型作为元素类型
                        Object firstKey = map.keySet().iterator().next();
                        return firstKey.getClass().getName();
                    }
                }
                return "java.lang.String";  // ZSET元素默认为String类型

            case STRING:
                return cacheValue.getClass().getName();

            default:
                return cacheValue.getClass().getName();
        }
    }

    /**
     * 根据数据类型和元素类型进行精确反序列化
     *
     * @param jsonValue   JSON字符串
     * @param dataType    数据类型枚举
     * @param elementType 元素类型（可为null）
     * @return 反序列化后的对象
     */
    private Object deserializeWithTypeInfo(String jsonValue, CacheFallbackDataTypeEnum dataType, String elementType) {
        if (jsonValue == null || jsonValue.trim().isEmpty()) {
            return null;
        }

        try {
            switch (dataType) {
                case LIST:
                    return deserializeList(jsonValue, elementType);

                case HASH:
                    return deserializeMap(jsonValue);

                case SET:
                    return deserializeSet(jsonValue, elementType);

                case ZSET:
                    // ZSET通常存储为Map<String, Double>（元素->分数）
                    return deserializeZSet(jsonValue);

                case STRING:
                default:
                    return deserializeString(jsonValue, elementType);
            }

        } catch (Exception e) {
            log.warn("[缓存降级] 精确反序列化失败，使用通用方式: {}", e.getMessage());
            // 降级到通用反序列化
            try {
                Object result = objectMapper.readValue(jsonValue, Object.class);
                log.debug("[缓存降级] 通用反序列化成功");
                return result;
            } catch (Exception e2) {
                log.error("[缓存降级] 所有反序列化方式都失败: {}", e2.getMessage());
            }
        }

        return null;
    }

    /**
     * 反序列化List类型
     */
    private Object deserializeList(String jsonValue, String elementType) throws Exception {
        if (elementType != null && !elementType.isEmpty() && !"java.lang.Object".equals(elementType)) {
            try {
                // 尝试使用具体的元素类型
                Class<?> elementClass = Class.forName(elementType);
                com.fasterxml.jackson.databind.type.CollectionType listType =
                        objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass);
                List<?> result = objectMapper.readValue(jsonValue, listType);
                log.debug("[缓存降级] List<{}>反序列化成功，大小: {}", elementClass.getSimpleName(), result.size());
                return result;
            } catch (ClassNotFoundException e) {
                log.warn("[缓存降级] 元素类型{}未找到，使用Object类型", elementType);
            }
        }

        // 降级到List<Object>
        List<Object> result = objectMapper.readValue(jsonValue,
                new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {
                });
        log.debug("[缓存降级] List<Object>反序列化成功，大小: {}", result.size());
        return result;
    }

    /**
     * 反序列化Map类型
     */
    private Object deserializeMap(String jsonValue) throws Exception {
        Map<String, Object> result = objectMapper.readValue(jsonValue,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                });
        log.debug("[缓存降级] Map反序列化成功，键数量: {}", result.size());
        return result;
    }

    /**
     * 反序列化Set类型
     */
    private Object deserializeSet(String jsonValue, String elementType) throws Exception {
        if (elementType != null && !elementType.isEmpty() && !"java.lang.Object".equals(elementType)) {
            try {
                // 尝试使用具体的元素类型
                Class<?> elementClass = Class.forName(elementType);
                com.fasterxml.jackson.databind.type.CollectionType setType =
                        objectMapper.getTypeFactory().constructCollectionType(Set.class, elementClass);
                Set<?> result = objectMapper.readValue(jsonValue, setType);
                log.debug("[缓存降级] Set<{}>反序列化成功，大小: {}", elementClass.getSimpleName(), result.size());
                return result;
            } catch (ClassNotFoundException e) {
                log.warn("[缓存降级] 元素类型{}未找到，使用Object类型.", elementType);
            }
        }

        // 降级到Set<Object>
        Set<Object> result = objectMapper.readValue(jsonValue,
                new com.fasterxml.jackson.core.type.TypeReference<Set<Object>>() {
                });
        log.debug("[缓存降级] Set<Object>反序列化成功，大小: {}", result.size());
        return result;
    }

    /**
     * 反序列化ZSet类型（存储为Map<元素, 分数>格式）
     */
    private Object deserializeZSet(String jsonValue) throws Exception {
        // 先尝试解析为Map<String, Double>（最常见的情况）
        try {
            Map<String, Double> result = objectMapper.readValue(jsonValue,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Double>>() {
                    });
            log.debug("[缓存降级] ZSet反序列化成功（String->Double），元素数量: {}", result.size());
            return result;
        } catch (Exception e) {
            log.debug("[缓存降级] String->Double格式解析失败，尝试通用Map格式: {}", e.getMessage());
        }

        // 降级到通用Map<Object, Object>，然后转换分数为Double
        Map<String, Object> rawMap = objectMapper.readValue(jsonValue,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                });

        Map<String, Double> result = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 转换分数为Double
            double score;
            if (value instanceof Number) {
                score = ((Number) value).doubleValue();
            } else {
                try {
                    score = Double.parseDouble(value.toString());
                } catch (NumberFormatException ex) {
                    log.warn("[缓存降级] ZSet分数转换失败，使用默认分数0: key={}, value={}", key, value);
                    score = 0.0;
                }
            }
            result.put(key, score);
        }

        log.debug("[缓存降级] ZSet通用格式反序列化成功，元素数量: {}", result.size());
        return result;
    }

    /**
     * 反序列化String/基本类型
     */
    private Object deserializeString(String jsonValue, String elementType) throws Exception {
        if (elementType != null && !elementType.isEmpty()) {
            try {
                Class<?> targetClass = Class.forName(elementType);
                Object result = objectMapper.readValue(jsonValue, targetClass);
                log.debug("[缓存降级] {}类型反序列化成功", targetClass.getSimpleName());
                return result;
            } catch (ClassNotFoundException e) {
                log.warn("[缓存降级] 目标类型{}未找到，使用Object类型", elementType);
            }
        }

        // 降级到Object
        Object result = objectMapper.readValue(jsonValue, Object.class);
        log.debug("[缓存降级] Object类型反序列化成功: {}", result.getClass().getSimpleName());
        return result;
    }
}
