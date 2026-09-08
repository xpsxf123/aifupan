package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.utils.DataUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.order.bo.*;
import com.jiuyu.replay.order.entity.*;
import com.jiuyu.replay.order.producer.UserPropertyProducer;
import com.jiuyu.replay.order.repository.service.*;
import com.jiuyu.replay.order.repository.service.impl.OrderDetailServiceImpl;
import com.jiuyu.replay.order.repository.service.impl.PropertyDetailsTokenServiceImpl;
import com.jiuyu.replay.order.vo.*;
import jakarta.annotation.Resource;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * 用户资产
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Service
@Slf4j
public class UserPropertyProducerImpl implements UserPropertyProducer {

    @Resource
    private UserPropertyService userPropertyService;

    @Resource
    private UserPropertyTypeService userPropertyTypeService;

    @Resource
    private CommodityTypeService commodityTypeService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private OrderDetailServiceImpl orderDetailService;

    @Resource
    private OrderService orderService;

    @Autowired
    private PropertyDetailsTokenServiceImpl propertyDetailsTokenService;

    @Resource
    private AiTokenUseRecordService aiTokenUseRecordService;

    @Resource
    private UserFeign userFeign;

    @Resource
    private TypeSurplusService typeSurplusService;


    @Override
    public PageUtils<UserPropertyListVo> queryPage(UserPropertyListBo bo) {
        if (ObjectUtil.isNotEmpty(bo.getCommodityTypeCode())) {
            bo.setCommodityTypeCode(
                    bo.getCommodityTypeCode().stream()
                            .filter(item -> {
                                if (ObjectUtil.isEmpty(item.getCode())) RRException.create("参数错误");
                                return ObjectUtil.isNotEmpty(item.getMin()) || ObjectUtil.isNotEmpty(item.getMax());
                            })
                            .toList()
            );
        }
        // 查询全部的资产，获取符合的资产id
        List<Long> ids = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(bo.getCommodityTypeCode())) {
            List<UserPropertyTypeEntity> userPropertyTypeEntities = userPropertyTypeService.listByIsUse();
            if (ObjectUtil.isEmpty(userPropertyTypeEntities))
                return new PageUtils<>(new ArrayList<>(), 0, bo.getLimit(), bo.getPage());
            Map<Long, List<UserPropertyTypeEntity>> listMap = userPropertyTypeEntities.stream().collect(Collectors.groupingBy(UserPropertyTypeEntity::getPropertyId));
            List<SelectScopeBo> boList = bo.getCommodityTypeCode();

            for (Long key : listMap.keySet()) {
                List<UserPropertyTypeEntity> list = listMap.get(key);
                boolean flag = true;
                for (SelectScopeBo scopeBo : boList) {
                    UserPropertyTypeEntity entity = list.stream().filter(item -> item.getCommodityTypeCode().equals(scopeBo.getCode())).findFirst().orElse(null);
                    if (entity == null || entity.getTotalQuantity() == 0) {
                        flag = false;
                        break;
                    }
                    long surplus = entity.getTotalQuantity() - entity.getUseQuantity();
                    if (ObjectUtil.isNotEmpty(scopeBo.getMin()) && surplus < scopeBo.getMin()) {
                        flag = false;
                        break;
                    }
                    if (ObjectUtil.isNotEmpty(scopeBo.getMax()) && surplus > scopeBo.getMax()) {
                        flag = false;
                        break;
                    }
                }
                if (flag) ids.add(key);
            }
            if (ObjectUtil.isEmpty(ids))
                return new PageUtils<>(new ArrayList<>(), 0, bo.getLimit(), bo.getPage());
        }
        LambdaQueryWrapper<UserPropertyEntity> wrapper = new LambdaQueryWrapper<UserPropertyEntity>()
                .in(ObjectUtil.isNotEmpty(bo.getUserIds()), UserPropertyEntity::getUserId, bo.getUserIds())
                .eq(UserPropertyEntity::getIsUse, 1)
                .in(ObjectUtil.isNotEmpty(ids), UserPropertyEntity::getId, ids);
        IPage<UserPropertyEntity> iPage = userPropertyService.page(new Query<UserPropertyEntity>().getPage(bo.getPage(), bo.getLimit()), wrapper);

        PageUtils<UserPropertyListVo> pageUtils = new PageUtils<>(bo.getPage(), bo.getLimit(), iPage);

        List<UserPropertyEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            // 获取订单获取时间
            List<UserPropertyListVo> result = BeanUtil.copyToList(records, UserPropertyListVo.class);
            List<Long> userId = new ArrayList<>();
            userId.addAll(records.stream().map(UserPropertyEntity::getUserId).toList());
            userId.addAll(result.stream().map(UserPropertyVo::getParentUserId).filter(item -> ObjectUtil.isNotEmpty(item) && item != 0L).toList());
            List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                    .in(OrderEntity::getUserId, userId)
                    .eq(OrderEntity::getCommodityType, 1)
                    .in(OrderEntity::getStatus, 1, 2)
                    .orderByDesc(OrderEntity::getEndDate)
            );
            if (ObjectUtil.isNotEmpty(list)){
                result.forEach(userPropertyListVo ->{
                    Map<Long, OrderEntity> entityMap = list.stream().collect(Collectors.toMap(OrderEntity::getUserId, orderEntity -> orderEntity, (v1, v2) -> v1));
                    Long userid = userPropertyListVo.getParentUserId() != 0L ? userPropertyListVo.getParentUserId() : userPropertyListVo.getUserId();
                    OrderEntity order = entityMap.get(userid);
                    userPropertyListVo.setExpirationTime(order == null ? null : order.getEndDate());
                });
            }
            pageUtils.setList(result);
        }
        return pageUtils;
    }

    @Override
    public UserPropertyInfoVo info(Long id) {

        UserPropertyEntity userPropertyEntity = userPropertyService.getById(id);
        if (userPropertyEntity != null) {
            UserPropertyInfoVo userPropertyInfoVo = new UserPropertyInfoVo();
            BeanUtils.copyProperties(userPropertyEntity, userPropertyInfoVo);
            return userPropertyInfoVo;
        }

        return null;
    }

    /**
     * 新增用户资产
     *
     * @param userPropertyBo 用户资产对象
     * @return
     */
    public UserPropertyInfoVo save(UserPropertyBo userPropertyBo) {

        UserPropertyEntity userPropertyEntity = new UserPropertyEntity();
        BeanUtils.copyProperties(userPropertyBo, userPropertyEntity);
        userPropertyEntity.setId(SnowflakeManager.nextValue());
        userPropertyEntity.setCreateDate(new Date());
        userPropertyEntity.setUpdateDate(new Date());

        userPropertyService.save(userPropertyEntity);

        UserPropertyInfoVo userPropertyInfoVo = new UserPropertyInfoVo();
        BeanUtils.copyProperties(userPropertyEntity, userPropertyInfoVo);

        return userPropertyInfoVo;
    }

    /**
     * 修改用户资产
     *
     * @param userPropertyBo 用户资产对象
     * @return
     */
    public void update(UserPropertyBo userPropertyBo) {

        UserPropertyEntity userPropertyEntity = new UserPropertyEntity();
        BeanUtils.copyProperties(userPropertyBo, userPropertyEntity);
        userPropertyEntity.setUpdateDate(new Date());

        userPropertyService.updateById(userPropertyEntity);
    }

    /**
     * 删除用户资产
     *
     * @param userId 用户资产id
     * @return
     */
    public void deleteByUserId(Long userId) {
        // 删除用户资产
        userPropertyService.remove(new LambdaQueryWrapper<UserPropertyEntity>()
                .eq(UserPropertyEntity::getUserId, userId)
        );

        // 删除用户资产类型
        userPropertyTypeService.remove(new LambdaQueryWrapper<UserPropertyTypeEntity>()
                .eq(UserPropertyTypeEntity::getUserId, userId)
        );

        // 删除资产缓存
        redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, userId, "*")); // 使用资产缓存
        redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, userId, "*")); // 总资产缓存
        redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.userUsePropertyIdCacheKey, userId)); // 使用资产id
//        redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.userPackageLevelCacheKey, userId)); // 用户版本等级
//        redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.userPackageOrderCacheKey, userId)); // 用户版本订单
    }

    @Override
    public UserPropertyInfoVo getOne(Long userId) {
        return this.getOne(userId, 1, 0, null, null);
    }

    @Override
    public UserPropertyInfoVo getOne(Long userId, Integer isUse, Integer type, Long parentId, Long parentUserId) {
        if (ObjectUtil.isEmpty(userId)) RRException.create("用户id不能为空");
        UserPropertyEntity one = userPropertyService.getOne(new LambdaQueryWrapper<UserPropertyEntity>()
                .eq(UserPropertyEntity::getUserId, userId)
                .eq(ObjectUtil.isNotEmpty(isUse), UserPropertyEntity::getIsUse, isUse)
                .eq(ObjectUtil.isNotEmpty(parentId), UserPropertyEntity::getParentId, parentId)
                .eq(ObjectUtil.isNotEmpty(parentUserId), UserPropertyEntity::getParentUserId, parentUserId)
                .eq(ObjectUtil.isNotEmpty(type), UserPropertyEntity::getType, type)
                .last("limit 1")
        );
        if (one != null) {
            return BeanUtil.copyProperties(one, UserPropertyInfoVo.class);
        } else {
            if (type == 0) {
                // 创建对应个人的资产
                return getOrCreate(userId, 1);
            }
            log.error("查询用户的资产表没有找到数据，参数：userId: {}, isUse:{}，parentUserId：{}，type：{}", userId, isUse, parentUserId, type);
            RRException.create("查询用户资产记录失败，请联系管理员");
            return null;
        }
    }

    @Override
    public List<UserPropertyInfoVo> getUserPropertyByUserIds(List<Long> userIds) {
        RRException.isNotEmpty(userIds, "用户id不能为空");
        List<UserPropertyEntity> list = userPropertyService.list(new LambdaQueryWrapper<UserPropertyEntity>()
                .in(UserPropertyEntity::getUserId, userIds)
                .eq(UserPropertyEntity::getType, 0)
                .eq(UserPropertyEntity::getParentId, 0)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, UserPropertyInfoVo.class);
        }
        return new ArrayList<>();
    }

    @Synchronized
    public UserPropertyInfoVo getOrCreate(Long userId, Integer isUse) {
        UserPropertyEntity userProperty = userPropertyService.getOne(new LambdaQueryWrapper<UserPropertyEntity>()
                .eq(UserPropertyEntity::getUserId, userId)
                .eq(UserPropertyEntity::getType, 0)
                .last("limit 1")
        );
        if (userProperty != null) {
            return BeanUtil.copyProperties(userProperty, UserPropertyInfoVo.class);
        }
        // 创建用户资产
        Date now = new Date();
        UserPropertyEntity userPropertyEntity = new UserPropertyEntity();
        userPropertyEntity.setId(SnowflakeManager.nextValue());
        userPropertyEntity.setUserId(userId);
        userPropertyEntity.setIsUse(isUse);
        userPropertyEntity.setParentId(0L);
        userPropertyEntity.setParentUserId(0L);
        userPropertyEntity.setType(0);
        userPropertyEntity.setCreateDate(now);
        userPropertyEntity.setUpdateDate(now);

        List<CommodityTypeEntity> typeEntityList = commodityTypeService.list();
        // 创建用户资产对应的数量
        ArrayList<UserPropertyTypeEntity> propertyTypeList = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(typeEntityList)) {
            typeEntityList.forEach(typeEntity -> {
                UserPropertyTypeEntity e = new UserPropertyTypeEntity();
                e.setId(SnowflakeManager.nextValue());
                e.setUserId(userId);
                e.setParentId(0L);
                e.setPropertyId(userPropertyEntity.getId());
                e.setCommodityTypeId(typeEntity.getId());
                e.setCommodityTypeCode(typeEntity.getCode());
                e.setCommodityTypeName(typeEntity.getName());
                e.setCommodityTypeUnit(typeEntity.getUnit());
                e.setCommodityTypeReset(typeEntity.getIsReset());
                e.setUseQuantity(0L);
                e.setTotalQuantity(0L);
                e.setTotalUseQuantity(0L);
                e.setCreateDate(now);
                e.setUpdateDate(now);
                propertyTypeList.add(e);
            });
        }
        userPropertyService.save(userPropertyEntity);
        userPropertyTypeService.saveBatch(propertyTypeList);
        if (isUse == 1) {
            redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.userUsePropertyIdCacheKey, userId), userPropertyEntity.getId());
        }
        return BeanUtil.copyProperties(userPropertyEntity, UserPropertyInfoVo.class);
    }

    @Override
    public void statisticsProperty(UserPropertyInfoVo userProperty) {
        userPropertyService.update(new LambdaUpdateWrapper<UserPropertyEntity>()
                .eq(UserPropertyEntity::getUserId, userProperty.getUserId())
                .set(UserPropertyEntity::getIsUse, 0)
        );
        userPropertyService.update(new LambdaUpdateWrapper<UserPropertyEntity>()
                .eq(UserPropertyEntity::getId, userProperty.getId())
                .set(UserPropertyEntity::getIsUse, 1)
        );
        // 把用户使用的资产id添加到缓存总中
        redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.userUsePropertyIdCacheKey, userProperty.getUserId()), userProperty.getId());
        // 查询用户资产类型的总量和总使用量
        List<UserPropertyTypeEntity> typeEntities = userPropertyTypeService.list(new LambdaQueryWrapper<UserPropertyTypeEntity>()
                .eq(UserPropertyTypeEntity::getPropertyId, userProperty.getId())
        );
        List<UserPropertyTypeEntity> add = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(typeEntities)) {
            List<UserPropertyTypeEntity> list = userPropertyTypeService.statisticsProperty(userProperty.getId());
            log.debug("统计用户的资产：{}", JSONUtil.toJsonStr(list));
            List<OrderDetailEntity> totalNumberList = orderDetailService.statisticsTotalNumberByUserId(userProperty.getUserId());

            for (UserPropertyTypeEntity item : typeEntities) {
                item.setUseQuantity(0L);
                item.setTotalQuantity(0L);
                if (item.getCommodityTypeReset() == 1 && !item.getCommodityTypeCode().startsWith("child_")) {
                    if (ObjectUtil.isNotEmpty(list)) {
                        UserPropertyTypeEntity propertyTypeEntity = list.stream()
                                .filter(val -> val.getCommodityTypeId().equals(item.getCommodityTypeId()))
                                .findFirst()
                                .orElse(null);
                        if (ObjectUtil.isNotEmpty(propertyTypeEntity)) {
                            item.setUseQuantity(propertyTypeEntity.getUseQuantity());
                            item.setTotalQuantity(propertyTypeEntity.getTotalQuantity());
                            log.debug("找到对应的资产，在set值：{}", JSONUtil.toJsonStr(item));
                        }
                    }
                } else {
                    Long value = Convert.toLong(redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, userProperty.getUserId(), item.getCommodityTypeCode())));
                    if (ObjectUtil.isNotEmpty(value)) {
                        item.setUseQuantity(value);
                    }
                    OrderDetailEntity orderDetailEntity = totalNumberList.stream().filter(val -> val.getCommodityTypeId().equals(item.getCommodityTypeId()))
                            .findFirst().orElse(null);
                    if (orderDetailEntity != null) {
                        item.setTotalQuantity(orderDetailEntity.getTotalNumber());
                    }
                }
                add.add(item);
            }
            if (ObjectUtil.isNotEmpty(add)) {
                boolean b = userPropertyTypeService.updateBatchById(add);
                if (b) {
                    // 添加redis缓存
                    HashMap<String, Object> map = new HashMap<>();
                    add.forEach(item -> {
                        map.put(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, item.getUserId(), item.getCommodityTypeCode()),
                                item.getUseQuantity().toString());
                        map.put(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, item.getUserId(), item.getCommodityTypeCode()),
                                item.getTotalQuantity().toString());
                    });
                    if (!map.isEmpty()) {
                        redisTemplate.opsForValue().multiSet(map);
                    }
                }
            }
        }
    }


    /**
     * 批量统计用户资产
     * <p>
     * 该方法用于批量更新用户资产的使用状态，并根据资产类型进行数量统计，同时将结果缓存到 Redis 中。
     * 主要包括以下操作：
     * 1. 更新用户资产的 isUse 字段（标记是否被使用）；
     * 2. 将用户当前使用的资产 ID 缓存至 Redis；
     * 3. 查询相关资产类型的记录并重置其使用量和总量；
     * 4. 根据是否需要重置数据分别处理资产类型数量；
     * 5. 批量更新资产类型数据；
     * 6. 将资产类型使用量和总量缓存至 Redis。
     *
     * @param userPropertyList 用户资产信息列表，不能为空。每个元素包含用户ID、资产ID等信息。
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void batchStatisticsProperty(List<UserPropertyInfoVo> userPropertyList) {
        if (EmptyUtil.isEmpty(userPropertyList)) {
            return;
        }

        // 提取用户ID和资产ID列表，用于后续数据库操作
        Date now = new Date();
        List<Long> userIds = userPropertyList.stream().map(UserPropertyInfoVo::getUserId).distinct().toList();
        List<Long> ids = userPropertyList.stream().map(UserPropertyInfoVo::getId).distinct().toList();

        // 更新不在本次统计范围内的资产为未使用状态
        userPropertyService.lambdaUpdate().in(UserPropertyEntity::getUserId, userIds)
                .notIn(UserPropertyEntity::getId, ids)
                .set(UserPropertyEntity::getUpdateDate, now)
                .set(UserPropertyEntity::getIsUse, 0).update();

        // 更新在本次统计范围内的资产为使用状态
        userPropertyService.lambdaUpdate().in(UserPropertyEntity::getId, ids)
                .set(UserPropertyEntity::getIsUse, 1)
                .set(UserPropertyEntity::getUpdateDate, now)
                .update();

        // 构造用户使用资产ID的缓存映射，并批量写入Redis
        Map<String, Long> setCacheMap = userPropertyList.stream()
                .collect(Collectors.toMap(
                        userProperty -> RedisCacheKey.getRedisKey(RedisCacheKey.userUsePropertyIdCacheKey, userProperty.getUserId()),
                        UserPropertyInfoVo::getId,
                        (v1, v2) -> v1));
        redisTemplate.opsForValue().multiSet(setCacheMap);

        // 查询与这些资产相关的资产类型数据
        List<UserPropertyTypeEntity> userPropertyTypeList = userPropertyTypeService.lambdaQuery()
                .in(UserPropertyTypeEntity::getPropertyId, ids).list();
        if (EmptyUtil.isEmpty(userPropertyTypeList)) {
            return;
        }

        // 初始化所有资产类型的使用量和总量为0
        userPropertyTypeList.forEach(type -> {
            type.setUseQuantity(0L);
            type.setTotalQuantity(0L);
            type.setUpdateDate(now);
        });

        // 处理需要重置数据的资产类型：从 surplus 表中统计使用量和总量
        List<UserPropertyTypeEntity> restPropertyTypeList = userPropertyTypeList.stream()
                .filter(type -> type.getCommodityTypeReset() == 1 && !type.getCommodityTypeCode().startsWith("child_")).toList();
        if (EmptyUtil.isNotEmpty(restPropertyTypeList)) {
            List<Long> propertyIds = restPropertyTypeList.stream().map(UserPropertyTypeEntity::getPropertyId).toList();
            Map<Long, Map<Long, Map<Long, List<TypeSurplusEntity>>>> propertyTypeSurplusMap = typeSurplusService.lambdaQuery()
                    .in(TypeSurplusEntity::getPropertyId, propertyIds)
                    .in(TypeSurplusEntity::getUserId, userIds)
                    .eq(TypeSurplusEntity::getTimeStatus, 0)
                    .in(TypeSurplusEntity::getUseStatus, 0, 1)
                    .list().stream().collect(Collectors.groupingBy(TypeSurplusEntity::getUserId,Collectors.groupingBy(TypeSurplusEntity::getPropertyId, Collectors.groupingBy(TypeSurplusEntity::getCommodityTypeId))));

            restPropertyTypeList.forEach(type -> {
                List<TypeSurplusEntity> propertyTypeSurplus = propertyTypeSurplusMap.getOrDefault(type.getUserId(), Map.of()).getOrDefault(type.getPropertyId(), Map.of()).getOrDefault(type.getCommodityTypeId(), List.of());
                if (EmptyUtil.isNotEmpty(propertyTypeSurplus)) {
                    type.setUseQuantity(propertyTypeSurplus.stream().mapToLong(TypeSurplusEntity::getUseNumber).sum());
                    type.setTotalQuantity(propertyTypeSurplus.stream().mapToLong(TypeSurplusEntity::getTotalNumber).sum());
                }
            });
        }

        // 处理不需要重置数据的资产类型
        List<UserPropertyTypeEntity> notPropertyTypeList = userPropertyTypeList.stream()
                .filter(type -> type.getCommodityTypeReset() != 1 || type.getCommodityTypeCode().startsWith("child_")).toList();
        this.processNotRestProperty(notPropertyTypeList);

        // 批量更新资产类型统计数据
        userPropertyTypeService.updateBatchById(userPropertyTypeList);

        // 构造资产类型使用量和总量的缓存映射，并批量写入Redis
        HashMap<String, Long> map = new HashMap<>();
        userPropertyTypeList.forEach(item -> {
            map.put(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, item.getUserId(), item.getCommodityTypeCode()),
                    item.getUseQuantity());
            map.put(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, item.getUserId(), item.getCommodityTypeCode()),
                    item.getTotalQuantity());
        });
        if (!map.isEmpty()) {
            redisTemplate.opsForValue().multiSet(map);
        }
    }




    /**
     * 处理非重置类用户属性数据，主要用于计算用户的使用数量和总数量。
     *
     * @param notPropertyTypeList 非重置类型的用户属性实体列表
     */
    private void processNotRestProperty(List<UserPropertyTypeEntity> notPropertyTypeList) {
        // 如果传入的列表为空，则直接返回
        if (EmptyUtil.isEmpty(notPropertyTypeList)) {
            return;
        }

        // 提取所有用户ID，用于后续查询订单信息
        List<Long> userIds = notPropertyTypeList.stream().map(UserPropertyTypeEntity::getUserId).toList();

        // 查询状态为2的订单，并按用户ID分组，构建用户ID到订单ID列表的映射
        Map<Long, List<Long>> userNotPropertyOrderMap = orderService.lambdaQuery().select(OrderEntity::getId, OrderEntity::getUserId)
            .in(OrderEntity::getUserId, userIds).eq(OrderEntity::getStatus, 2)
            .list().stream().collect(Collectors.groupingBy(OrderEntity::getUserId, Collectors.mapping(OrderEntity::getId, Collectors.toList())));

        // 如果没有符合条件的订单信息，则直接返回
        if (EmptyUtil.isEmpty(userNotPropertyOrderMap)) {
            return;
        }

        // 根据订单ID查询订单详情，并按订单ID和商品类型ID进行分组
        Map<Long, Map<Long, List<OrderDetailEntity>>> orderCommodityTypeMap = orderDetailService.lambdaQuery().select(OrderDetailEntity::getCommodityTypeId, OrderDetailEntity::getTotalNumber, OrderDetailEntity::getOrderId)
            .in(OrderDetailEntity::getOrderId, userNotPropertyOrderMap.values().stream().flatMap(Collection::stream).distinct().toList())
            .eq(OrderDetailEntity::getStatus, 1).list()
            .stream().collect(Collectors.groupingBy(OrderDetailEntity::getOrderId, Collectors.groupingBy(OrderDetailEntity::getCommodityTypeId)));

        // 如果没有符合条件的订单详情信息，则直接返回
        if (EmptyUtil.isEmpty(orderCommodityTypeMap)) {
            return;
        }

        // 构建Redis缓存键列表，用于批量获取缓存中的使用数量
        List<String> cacheKeys = notPropertyTypeList.stream().map(item -> {
            return RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, item.getUserId(), item.getCommodityTypeCode());
        }).toList();

        // 批量从Redis中获取缓存值
        List<Object> objectValueList = redisTemplate.opsForValue().multiGet(cacheKeys);

        // 如果缓存值不为空，则遍历并设置到对应的实体中
        if (EmptyUtil.isNotEmpty(objectValueList)) {
            for (int i = 0; i < objectValueList.size(); i++) {
                Object value = objectValueList.get(i);
                if (value == null) {
                    continue;
                }
                UserPropertyTypeEntity entity = notPropertyTypeList.get(i);
                if (entity == null) {
                    continue;
                }
                if (value instanceof Long useQuantity) {
                    entity.setUseQuantity(useQuantity);
                } else if (value instanceof String useQuantity) {
                    entity.setUseQuantity(Convert.toLong(useQuantity));
                } else if (value instanceof Integer useQuantity) {
                    entity.setUseQuantity(Convert.toLong(useQuantity));
                }
            }
        }

        // 遍历每个用户属性实体，计算其总数量
        notPropertyTypeList.forEach(type -> {
            userNotPropertyOrderMap.getOrDefault(type.getUserId(), List.of())
                .stream().flatMap(orderId -> {
                    return orderCommodityTypeMap.getOrDefault(orderId, Map.of()).getOrDefault(type.getCommodityTypeId(), List.of()).stream();
                }).map(OrderDetailEntity::getTotalNumber).reduce(Long::sum).ifPresent(type::setTotalQuantity);
        });
    }


    @Override
    public void statisticsChildProperty(UserPropertyInfoVo userPropertyInfoVo) {
        // 1.先查询出主账号的资产
        List<UserPropertyTypeEntity> parentList = userPropertyTypeService.list(new LambdaQueryWrapper<UserPropertyTypeEntity>()
                .eq(UserPropertyTypeEntity::getPropertyId, userPropertyInfoVo.getId())
        );

        // 2.查询出子账号的资产
        // 2.1先查询出子账号的资产id-查出的资产是单前账号使用的
        List<UserPropertyEntity> list = userPropertyService.list(new LambdaQueryWrapper<UserPropertyEntity>()
                .eq(UserPropertyEntity::getParentId, userPropertyInfoVo.getId())
                .eq(UserPropertyEntity::getParentUserId, userPropertyInfoVo.getUserId())
                .eq(UserPropertyEntity::getIsUse, 1)
        );
        if (ObjectUtil.isEmpty(list)) return;
        // 2.2在用子账号的资产id查询出子账号的资产，parentId字段有值的就不更新
        List<Long> userPropertyIds = list.stream().map(UserPropertyEntity::getId).toList();
        List<UserPropertyTypeEntity> userPropertyTypeEntities = userPropertyTypeService.list(new LambdaQueryWrapper<UserPropertyTypeEntity>()
                .in(UserPropertyTypeEntity::getPropertyId, userPropertyIds)
                .eq(UserPropertyTypeEntity::getParentId, 0)
        );
        if (ObjectUtil.isEmpty(userPropertyTypeEntities)) return;
        Map<Long, List<UserPropertyTypeEntity>> listMap = userPropertyTypeEntities.stream().collect(Collectors.groupingBy(UserPropertyTypeEntity::getPropertyId));

        // 3.修改子账号资产的总数量字段，使用数量字段不用管
        listMap.forEach((key, value) -> {
            UserPropertyEntity userChileProperty = list.stream().filter(item -> item.getId().equals(key)).findFirst().orElse(null);
            if (ObjectUtil.isNotEmpty(userChileProperty) && ObjectUtil.isNotEmpty(value)) {
                value.forEach(item -> {
                    if (ObjectUtil.isEmpty(item.getParentId()) || item.getParentId() == 0) {
                        UserPropertyTypeEntity propertyTypeEntity = parentList.stream()
                                .filter(val -> val.getCommodityTypeCode().equals(StrUtil.format("child_{}", item.getCommodityTypeCode())))
                                .findFirst().orElse(null);
                        if (ObjectUtil.isNotEmpty(propertyTypeEntity)) {
                            item.setTotalQuantity(propertyTypeEntity.getTotalQuantity());
                        }
                    }
                });
            }
        });

        // 4.查询子账号要重置的资产
        // 找到子账号中要重置资产的类型，并且设置为0，
        Map<String, CommodityTypeEntity> commodityTypeMap = getResetChildProperty();
        for (UserPropertyTypeEntity userPropertyTypeEntity : userPropertyTypeEntities) {
            if (commodityTypeMap.containsKey(userPropertyTypeEntity.getCommodityTypeCode())) {
                userPropertyTypeEntity.setUseQuantity(0L);
                userPropertyTypeEntity.setTotalQuantity(0L);
            }
        }
        // 查询重置的资产剩余和总数
        List<TypeSurplusEntity> statisticsTypeSurplusList = typeSurplusService.list(new QueryWrapper<TypeSurplusEntity>()
                .select("property_id", "commodity_type_id", "sum( use_number ) AS use_number", "sum( total_number ) AS total_number")
                .lambda()
                .in(TypeSurplusEntity::getPropertyId, userPropertyIds)
                .eq(TypeSurplusEntity::getTimeStatus, 0)
                .in(TypeSurplusEntity::getUseStatus, 0, 1)
                .groupBy(TypeSurplusEntity::getPropertyId, TypeSurplusEntity::getCommodityTypeId)
        );
        // 把查询出TypeSurplus的值赋值到资产中
        if (ObjectUtil.isNotEmpty(statisticsTypeSurplusList)) {
            statisticsTypeSurplusList.forEach(item -> userPropertyTypeEntities.stream()
                    .filter(temp -> ObjectUtil.equals(item.getPropertyId(), temp.getPropertyId()) && ObjectUtil.equals(temp.getCommodityTypeId(), item.getCommodityTypeId()))
                    .forEach(value -> {
                        value.setUseQuantity(item.getUseNumber());
                        value.setTotalQuantity(item.getTotalNumber());
                    }));
        }


        // 4.批量修改
        boolean b = userPropertyTypeService.updateBatchById(userPropertyTypeEntities);
        if (b) {
            // 添加redis缓存
            HashMap<String, Object> map = new HashMap<>();
            userPropertyTypeEntities.forEach(item -> {
                map.put(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, item.getUserId(), item.getCommodityTypeCode()),
                        item.getUseQuantity().toString());
                map.put(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, item.getUserId(), item.getCommodityTypeCode()),
                        item.getTotalQuantity().toString());
            });
            if (!map.isEmpty()) {
                redisTemplate.opsForValue().multiSet(map);
            }
        }
    }


    /**
     * 批量统计子用户资产
     * <p>
     * 该方法用于批量更新子用户资产的总数量，使其与主账户的"child_"类型资产保持一致。
     * 主要包括以下操作：
     * 1. 查询主账户资产类型数据；
     * 2. 查询子账户资产数据；
     * 3. 根据主账户资产更新子账户资产总数量；
     * 4. 批量更新数据库并同步更新Redis缓存。
     *
     * @param userPropertyList 用户资产信息列表，不能为空。每个元素包含用户ID、资产ID等信息。
     */
    @Override
    public void batchStatisticsChildProperty(List<UserPropertyInfoVo> userPropertyList) {
        if (EmptyUtil.isEmpty(userPropertyList)) {
            return;
        }
        Map<Long, Long> propertyUserMap = userPropertyList.stream().collect(Collectors.toMap(UserPropertyInfoVo::getId, UserPropertyInfoVo::getUserId));
        Set<Long> ids = propertyUserMap.keySet();
        // 1. 查询出主账号的资产
        List<UserPropertyTypeEntity> parentList = userPropertyTypeService.lambdaQuery().in(UserPropertyTypeEntity::getPropertyId, ids).list();
        if (EmptyUtil.isEmpty(parentList)) {
            return;
        }
        Collection<Long> userIds = propertyUserMap.values();
        // 2. 查询出子账号的资产
        // 2.1 先查询出子账号的资产id-查出的资产是单前账号使用的
        List<UserPropertyEntity> list = new BatchQuery<>((limit, idx) -> {
            return userPropertyService.lambdaQuery().in(UserPropertyEntity::getParentId, ids)
                .le(idx != null, UserPropertyEntity::getId, idx)
                .in(UserPropertyEntity::getParentUserId, userIds)
                .eq(UserPropertyEntity::getIsUse, 1)
                .last("limit " + limit)
                .orderByDesc(UserPropertyEntity::getId)
                .list();
        }, UserPropertyEntity::getId).batch(1000).get();
        if (EmptyUtil.isEmpty(list)) {
            return;
        }
        List<UserPropertyEntity> childPropertyList = list.stream().filter(item -> {
            Long parentUserId = propertyUserMap.get(item.getParentId());
            return Objects.equals(parentUserId, item.getParentUserId());
        }).toList();
        if (EmptyUtil.isEmpty(childPropertyList)) {
            return;
        }
        Map<Long, UserPropertyEntity> userPropertyMap = childPropertyList.stream().collect(Collectors.toMap(UserPropertyEntity::getId, Function.identity()));
        // 2.2 再用子账号的资产id查询出子账号的资产，parentId字段有值的就不更新
        List<UserPropertyTypeEntity> userPropertyTypeEntities = userPropertyTypeService.lambdaQuery().in(UserPropertyTypeEntity::getPropertyId, userPropertyMap.keySet())
            .eq(UserPropertyTypeEntity::getParentId, 0).list();
        if (EmptyUtil.isEmpty(userPropertyTypeEntities)) {
            return;
        }

        // 4.查询子账号要重置的资产
        Map<String, TypeSurplusEntity> typeSurplusMap = typeSurplusService.list(new QueryWrapper<TypeSurplusEntity>()
                        .select("property_id", "commodity_type_id", "sum( use_number ) AS use_number", "sum( total_number ) AS total_number")
                        .lambda()
                        .in(TypeSurplusEntity::getPropertyId, userPropertyMap.keySet())
                        .eq(TypeSurplusEntity::getTimeStatus, 0)
                        .in(TypeSurplusEntity::getUseStatus, 0, 1)
                        .groupBy(TypeSurplusEntity::getPropertyId, TypeSurplusEntity::getCommodityTypeId)
                ).stream()
                .collect(Collectors.toMap(value -> value.getPropertyId() + "_" + value.getCommodityTypeId(), Function.identity(), (a, b) -> a));
        // 找到子账号中要重置资产的类型
        Map<String, CommodityTypeEntity> commodityTypeMap = getResetChildProperty();

        // 构建parentList的映射，按propertyId分组，方便后续查找
        Map<Long, List<UserPropertyTypeEntity>> parentListMap = parentList.stream()
            .collect(Collectors.groupingBy(UserPropertyTypeEntity::getPropertyId));
        Date now = new Date();
        // 过滤出需要更新的资产类型记录，并设置总数量
        List<UserPropertyTypeEntity> updateList = userPropertyTypeEntities.stream()
            .filter(item -> ObjectUtil.isEmpty(item.getParentId()) || item.getParentId() == 0)
            .filter(item -> userPropertyMap.containsKey(item.getPropertyId()))
            .map(item -> {
                UserPropertyEntity userChildProperty = userPropertyMap.get(item.getPropertyId());
                if (Objects.isNull(userChildProperty)) {
                    return null;
                }
                // 获取该子账户对应的主账户资产类型列表
                List<UserPropertyTypeEntity> currentParentList = parentListMap.get(userChildProperty.getParentId());
                if (EmptyUtil.isEmpty(currentParentList)) {
                    return null;
                }
                Optional<UserPropertyTypeEntity> currentParentPropertyType = currentParentList.stream()
                    .filter(val -> val.getCommodityTypeCode().equals(StrUtil.format("child_{}", item.getCommodityTypeCode())))
                    .findFirst();
                if (currentParentPropertyType.isEmpty()) {
                    return null;
                }
                item.setTotalQuantity(currentParentPropertyType.get().getTotalQuantity());
                item.setUpdateDate(now);

                if (commodityTypeMap.containsKey(item.getCommodityTypeCode())) {
                    item.setUseQuantity(0L);
                    item.setTotalUseQuantity(0L);
                }
                // 把查询出TypeSurplus的值赋值到资产中
                if (typeSurplusMap.containsKey(item.getPropertyId() + "_" + item.getCommodityTypeId())) {
                    TypeSurplusEntity statisticsTypeSurplus = typeSurplusMap.get(item.getPropertyId() + "_" + item.getCommodityTypeId());
                    if (ObjectUtil.isNotEmpty(statisticsTypeSurplus)) {
                        item.setUseQuantity(statisticsTypeSurplus.getUseNumber());
                        item.setTotalQuantity(statisticsTypeSurplus.getTotalNumber());
                    }
                }

                return item;
            }).filter(Objects::nonNull)
            .toList();
        if (EmptyUtil.isEmpty(updateList)) {
            return;
        }
        // 4.批量修改
        boolean b = userPropertyTypeService.updateBatchById(updateList);
        if (b) {
            // 添加redis缓存
            HashMap<String, Long> map = new HashMap<>();
            updateList.forEach(item -> {
                map.put(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, item.getUserId(), item.getCommodityTypeCode()),
                    item.getUseQuantity());
                map.put(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, item.getUserId(), item.getCommodityTypeCode()),
                    item.getTotalQuantity());
            });
            if (!map.isEmpty()) {
                redisTemplate.opsForValue().multiSet(map);
            }
        }
    }

    /**
     * 获取子账号中要重置资产的类型
     *
     * @return
     */
    private Map<String, CommodityTypeEntity> getResetChildProperty() {
        // 资产类型
        List<CommodityTypeEntity> commodityTypeList = commodityTypeService.listAllByCache();
        // 找到子账号中要重置资产的类型
        return commodityTypeList
                .stream()
                .filter(item -> {
                    if (!item.getCode().startsWith("child_")) {
                        CommodityTypeEntity commodityType = commodityTypeList.stream()
                                .filter(k -> k.getCode().equals("child_" + item.getCode()))
                                .findAny().orElse(null);
                        return commodityType != null && ObjectUtil.equals(commodityType.getSubAccountHave(), 1) && ObjectUtil.equals(commodityType.getIsReset(), 1);
                    }
                    return false;
                })
                .collect(Collectors.toMap(CommodityTypeEntity::getCode, Function.identity(), (a, b) -> a));
    }


    @Override
    public void checkUserPropertyAndCreate(List<Long> userIds, Integer isUse) {
        List<UserPropertyEntity> list = userPropertyService.list(new LambdaQueryWrapper<UserPropertyEntity>()
                .in(UserPropertyEntity::getUserId, userIds)
                .eq(UserPropertyEntity::getType, 0)
        );
        List<Long> user2 = new ArrayList<>();
        if (ObjectUtil.isEmpty(list)) {
            List<Long> ids = list.stream().map(UserPropertyEntity::getUserId).toList();
            user2.addAll(userIds.stream().filter(item -> !ids.contains(item)).toList());
        } else {
            user2.addAll(userIds);
        }
        if (ObjectUtil.isNotEmpty(user2)) {
            Date now = new Date();
            List<CommodityTypeEntity> typeEntityList = commodityTypeService.list();
            ArrayList<UserPropertyTypeEntity> propertyTypeList = new ArrayList<>();
            List<UserPropertyEntity> list1 = user2.stream().map(item -> {
                UserPropertyEntity userPropertyEntity = new UserPropertyEntity();
                userPropertyEntity.setId(SnowflakeManager.nextValue());
                userPropertyEntity.setUserId(item);
                userPropertyEntity.setIsUse(isUse);
                userPropertyEntity.setParentId(0L);
                userPropertyEntity.setParentUserId(0L);
                userPropertyEntity.setType(0);
                userPropertyEntity.setCreateDate(now);
                userPropertyEntity.setUpdateDate(now);

                if (ObjectUtil.isNotEmpty(typeEntityList)) {
                    typeEntityList.forEach(typeEntity -> {
                        UserPropertyTypeEntity e = new UserPropertyTypeEntity();
                        e.setId(SnowflakeManager.nextValue());
                        e.setUserId(item);
                        e.setParentId(0L);
                        e.setPropertyId(userPropertyEntity.getId());
                        e.setCommodityTypeId(typeEntity.getId());
                        e.setCommodityTypeCode(typeEntity.getCode());
                        e.setCommodityTypeName(typeEntity.getName());
                        e.setCommodityTypeUnit(typeEntity.getUnit());
                        e.setCommodityTypeReset(typeEntity.getIsReset());
                        e.setUseQuantity(0L);
                        e.setTotalQuantity(0L);
                        e.setTotalUseQuantity(0L);
                        e.setCreateDate(now);
                        e.setUpdateDate(now);
                        propertyTypeList.add(e);
                    });
                }
                return userPropertyEntity;
            }).toList();
            // 批量保存资产表
            if (userPropertyService.saveBatch(list1)) {
                list1.forEach(item -> {
                    if (item.getIsUse() == 1) {
                        redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.userUsePropertyIdCacheKey, item.getUserId()), item.getId());
                    }
                });
            }

            // 批量保存资产类型表
            userPropertyTypeService.saveBatch(propertyTypeList);
        }
    }

    @Override
    public void addUserPropertyCache() {
        List<UserPropertyEntity> list = userPropertyService.list(new LambdaQueryWrapper<UserPropertyEntity>()
                .eq(UserPropertyEntity::getIsUse, 1)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            List<Long> ids = list.stream().map(UserPropertyEntity::getId).toList();
            List<UserPropertyTypeEntity> userPropertyTypeEntities = userPropertyTypeService.list(new LambdaQueryWrapper<UserPropertyTypeEntity>()
                    .in(UserPropertyTypeEntity::getPropertyId, ids)
            );
            if (ObjectUtil.isNotEmpty(userPropertyTypeEntities)) {
                List<UserPropertyInfoVo> propertyList = BeanUtil.copyToList(list, UserPropertyInfoVo.class);
                List<UserPropertyTypeInfoVo> typeList = BeanUtil.copyToList(userPropertyTypeEntities, UserPropertyTypeInfoVo.class);
                Map<Long, List<UserPropertyTypeInfoVo>> collect = typeList.stream().collect(Collectors.groupingBy(UserPropertyTypeInfoVo::getPropertyId));
                // 把对应的资产类型数据设置到对应的资产对象中
                DataUtils.setFieldMap(propertyList, "id", "userPropertyTypeList", collect);
                ValueOperations<String, Object> ops = redisTemplate.opsForValue();
                redisTemplate.executePipelined((RedisCallback<?>) connection -> {
                    propertyList.forEach(item -> {
                        // 缓存用户id和资产id的映射关系
                        ops.set(RedisCacheKey.getRedisKey(RedisCacheKey.userUsePropertyIdCacheKey, item.getUserId()), item.getId());
                        // 添加新缓存
                        if (ObjectUtil.isNotEmpty(item.getUserPropertyTypeList())) {
                            item.getUserPropertyTypeList().forEach(val -> {
                                // 先生成
                                redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, item.getUserId(),
                                        val.getCommodityTypeCode()));
                                redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, item.getUserId(),
                                        val.getCommodityTypeCode()));
                                if (ObjectUtil.isEmpty(val.getParentId()) || val.getParentId() == 0) {
                                    // 缓存资产类型id和资产类型数据的映射关系
                                    ops.set(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, item.getUserId(),
                                            val.getCommodityTypeCode()), val.getUseQuantity().toString());

                                    // 缓存资产类型id和资产类型数据的映射关系
                                    ops.set(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, item.getUserId(),
                                            val.getCommodityTypeCode()), val.getTotalQuantity().toString());
                                }
                            });
                        }
                    });
                    return null;
                });

            }
        }


    }

    @Override
    public void assetsMinusOrPlus(AssetsMinusOrPlusBo assets) {
        // 判断当前的用户是子用户吗
    }

    @Override
    @Transactional
    public List<UserPropertyTypeInfoVo> getUserProperty(Long userId) {
        List<UserPropertyTypeInfoVo> list = new ArrayList<>();
        // 获取当前用户的上级用户id，有值就获取上级用户的资产
        Long parentId = ResultUtil.getResult(userFeign.getUserParentId(userId));
        if (parentId != null) {
            Long parentPropertyId = (Long) redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.userUsePropertyIdCacheKey, parentId));
            if (parentPropertyId != null) {
                // 获取已经使用的资产信息
                setFieldValue(list, RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, parentId, "{}"), false);
                // 获取全部资产信息
                setFieldValue(list, RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, parentId, "{}"), true);

                // 去掉前缀叫child_的商品类型
                list = list.stream().filter(item -> !item.getCommodityTypeCode().startsWith("child_")).collect(Collectors.toList());
                list = list.stream().filter(item -> !item.getCommodityTypeCode().startsWith("subAccountCount")).collect(Collectors.toList());
            }
        }

        // 获取当前用户的资产信息
        setFieldValue(list, RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, userId, "{}"), false);
        // 获取全部资产信息
        setFieldValue(list, RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, userId, "{}"), true);

        // 获取资产名称、单位
        List<CommodityTypeEntity> typeList = commodityTypeService.listAllByCache();
        if (ObjectUtil.isNotEmpty(typeList)) {
            list.forEach(item -> {
                CommodityTypeEntity temp = typeList.stream().filter(type -> type.getCode().equals(item.getCommodityTypeCode())).findFirst().orElse(null);
                if (temp != null) {
                    item.setCommodityTypeName(temp.getName());
                    item.setCommodityTypeUnit(temp.getUnit());
                    item.setCommodityTypeReset(temp.getIsReset());
                    item.setCommodityTypeId(temp.getId());
                }
                item.setTotalQuantity(ObjectUtil.defaultIfNull(item.getTotalQuantity(), 0L));
                item.setUseQuantity(ObjectUtil.defaultIfNull(item.getUseQuantity(), 0L));
            });
        }

        // 检查资产信息是否存在
        if (ObjectUtil.isEmpty(list)){
            Long temp = userId;
            if (parentId != null) {
                temp = parentId;
            }
            UserPropertyInfoVo infoVo = getOne(temp);

            statisticsProperty(infoVo);
            statisticsChildProperty(infoVo);

            return this.getUserProperty(userId);
        }

        return list;
    }



    /**
     * 获取用户的资产信息-如果有就用用的，没有则创建
     *
     * @param id       当前用户id
     * @param parentId 父用户id
     * @return
     */
    @Override
    public UserPropertyInfoVo getUserPropertyOrCreate(Long id, Long parentId) {
        // 查询当前用户是否已经存在
        UserPropertyEntity one = userPropertyService.getOne(new LambdaQueryWrapper<UserPropertyEntity>()
                .eq(UserPropertyEntity::getUserId, id)
                .eq(UserPropertyEntity::getParentUserId, parentId)
                .last("limit 1")
        );
        if (ObjectUtil.isNotEmpty(one)) {
            return BeanUtil.copyProperties(one, UserPropertyInfoVo.class);
        }

        // 创建
        UserPropertyEntity userProperty = new UserPropertyEntity();
        userProperty.setId(SnowflakeManager.nextValue());
        userProperty.setUserId(id);
        userProperty.setIsUse(0);
        userProperty.setParentId(parentId);
        UserPropertyEntity parentProperty = userPropertyService.getOne(new LambdaQueryWrapper<UserPropertyEntity>()
                .eq(UserPropertyEntity::getUserId, parentId)
                .eq(UserPropertyEntity::getIsUse, 1)
                .eq(UserPropertyEntity::getType, 0)
                .eq(UserPropertyEntity::getParentId, 0)
                .last("limit 1")
        );
        if (ObjectUtil.isEmpty(parentProperty)) RRException.create("父账号资产查询失败");
        userProperty.setParentId(parentProperty.getId());
        userProperty.setParentUserId(parentId);
        userProperty.setType(1);
        userProperty.setCreateDate(new Date());
        userProperty.setUpdateDate(new Date());

        boolean save = userPropertyService.save(userProperty);
        if (save) {
            List<CommodityTypeEntity> typeEntityList = commodityTypeService.list();
            List<UserPropertyTypeEntity> propertyTypeList = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(typeEntityList)) {
                // 查询父资产对应的数据
                List<UserPropertyTypeEntity> parentTypeList = userPropertyTypeService.list(new LambdaQueryWrapper<UserPropertyTypeEntity>()
                        .eq(UserPropertyTypeEntity::getPropertyId, parentProperty.getId())
                        .eq(UserPropertyTypeEntity::getParentId, 0)
                );
                if (ObjectUtil.isEmpty(parentTypeList)) RRException.create("父资产查询失败");
                typeEntityList.forEach(typeEntity -> {
                    UserPropertyTypeEntity e = new UserPropertyTypeEntity();
                    e.setId(SnowflakeManager.nextValue());
                    e.setUserId(id);
                    if (typeEntity.getSubAccountHave() == 1) {
                        e.setParentId(0L);
                    } else {
                        UserPropertyTypeEntity first = parentTypeList.stream()
                                .filter(val -> val.getCommodityTypeId().equals(typeEntity.getId()))
                                .findFirst().orElse(null);
                        if (ObjectUtil.isEmpty(first)) RRException.create("父资产查询失败");
                        e.setParentId(first.getId());
                    }
                    e.setPropertyId(userProperty.getId());
                    e.setCommodityTypeId(typeEntity.getId());
                    e.setCommodityTypeCode(typeEntity.getCode());
                    e.setCommodityTypeName(typeEntity.getName());
                    e.setCommodityTypeUnit(typeEntity.getUnit());
                    e.setCommodityTypeReset(typeEntity.getIsReset());
                    e.setUseQuantity(0L);
                    e.setTotalQuantity(0L);
                    e.setTotalUseQuantity(0L);
                    e.setCreateDate(new Date());
                    e.setUpdateDate(new Date());
                    propertyTypeList.add(e);
                });
            }
            if (ObjectUtil.isNotEmpty(propertyTypeList)) {
                // 批量保存资产类型表
                userPropertyTypeService.saveBatch(propertyTypeList);
            }
        }
        return BeanUtil.copyProperties(userProperty, UserPropertyInfoVo.class);
    }

    /**
     * 更新子账号资产
     *
     * @param id
     */
    @Override
    public void useChildUserProperty(Long id) {
        // 查询资产
        UserPropertyEntity property = userPropertyService.getById(id);
        if (ObjectUtil.isEmpty(property)) RRException.create("用户资产查询出错");

        // 查询资产类型
        List<UserPropertyTypeEntity> userPropertyTypeList = userPropertyTypeService.list(new LambdaQueryWrapper<UserPropertyTypeEntity>()
                .eq(UserPropertyTypeEntity::getPropertyId, property.getId())
        );
        if (ObjectUtil.isEmpty(userPropertyTypeList)) RRException.create("用户资产类型查询出错");

        // 取消资产使用状态
        userPropertyService.update(new LambdaUpdateWrapper<UserPropertyEntity>()
                .eq(UserPropertyEntity::getUserId, property.getUserId())
                .set(UserPropertyEntity::getIsUse, 0)
        );

        // 更新资产使用状态
        userPropertyService.update(new LambdaUpdateWrapper<UserPropertyEntity>()
                .eq(UserPropertyEntity::getId, property.getId())
                .set(UserPropertyEntity::getIsUse, 1)
        );
        // 更新redis缓存
        redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.userUsePropertyIdCacheKey, property.getUserId()), property.getId());

        // 查询父资产类型用量
        List<UserPropertyTypeEntity> parentUserTypeList = userPropertyTypeService.list(new LambdaUpdateWrapper<UserPropertyTypeEntity>()
                .eq(property.getParentId() > 1, UserPropertyTypeEntity::getPropertyId, property.getParentId())
                .eq(property.getParentId() == 0, UserPropertyTypeEntity::getId, 0)
        );

        // 更新子账号资产类型用量
        userPropertyTypeList.forEach(item -> {
            if (item.getParentId() == 0) {
                UserPropertyTypeEntity parent = parentUserTypeList.stream()
                        .filter(val -> StrUtil.format("child_{}", item.getCommodityTypeCode()).equals(val.getCommodityTypeCode()))
                        .findFirst().orElse(null);
                if (ObjectUtil.isNotEmpty(parent)) {
                    item.setTotalQuantity(parent.getTotalQuantity());
                    userPropertyTypeService.updateById(item);

                }
                // 更新redis缓存
                redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(
                        RedisCacheKey.userPropertyTypeCacheKey, property.getUserId(),
                        item.getCommodityTypeCode()), item.getUseQuantity().toString());
                redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(
                        RedisCacheKey.userPropertyTypeTotalCacheKey, property.getUserId(),
                        item.getCommodityTypeCode()), item.getTotalQuantity().toString());
            } else {
                // 因为子账号里有parenId字段的都是用父账号资产，所以把无用的资产类型清空
                redisTemplate.delete(RedisCacheKey.getRedisKey(
                        RedisCacheKey.userPropertyTypeCacheKey, property.getUserId(),
                        item.getCommodityTypeCode()));
                redisTemplate.delete(RedisCacheKey.getRedisKey(
                        RedisCacheKey.userPropertyTypeTotalCacheKey, property.getUserId(),
                        item.getCommodityTypeCode()));
            }
        });
    }

    @Override
    public boolean updateUserProperty(Long userId, String code, Long quantity) {
        // 获取用户id
        userId = getCurrentUserId(userId, code);
        Long propertyId = (Long) redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.userUsePropertyIdCacheKey, userId));
        if (ObjectUtil.isEmpty(propertyId)) RRException.create("获取用户资产失败");

        // 获取当前值, 判断是否一致
        Long currentVal = convertLong(redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, userId, code)), 0L);
        if (ObjectUtil.equals(currentVal, quantity)) {
            // 缓存一致, 返回成功
            return true;
        }
        // 更新数据库
        if (updateUserPropertyType(userId, propertyId, code, quantity)) {
            // 更新redis缓存

            try {
                redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(
                                RedisCacheKey.userPropertyTypeCacheKey, userId, code),
                        quantity.toString());
            } catch (Exception e) {
                log.error("更新redis缓存失败 userId = {}, code = {}, quantity = {}", userId, code, quantity);
                throw new BusinessException(StatusCode.REDIS_OPERATE_ERROR);
            }
            return true;
        }
        return false;
    }

    /**
     * 转换为long
     *
     * @param object       值
     * @param defaultValue 默认值
     * @return long值
     */
    private long convertLong(Object object, long defaultValue) {
        if (object == null) {
            return defaultValue;
        }
        return Convert.toLong(object, defaultValue);
    }

    @Override
    public boolean updateUserPropertyType(Long userId, Long propertyId, String code, Long quantity) {
        return userPropertyTypeService.update(new LambdaUpdateWrapper<UserPropertyTypeEntity>()
                .eq(UserPropertyTypeEntity::getUserId, userId)
                .eq(UserPropertyTypeEntity::getPropertyId, propertyId)
                .eq(UserPropertyTypeEntity::getCommodityTypeCode, code)
                .set(UserPropertyTypeEntity::getUseQuantity, quantity)
        );
    }

    @Override
    public Long getTotalUserIdPropertyByCode(Long userId, String code) {
        userId = getCurrentUserId(userId, code);
        // 查询redis缓存
        Long value = Convert.toLong(redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, userId, code)));
        return ObjectUtil.defaultIfNull(value, 0L);
    }

    @Override
    public List<AiTokenUseRecordInfoVo> aiTokenUseRecordByDetailId(Long propertyDetailsId) {
        List<PropertyDetailsTokenEntity> list = propertyDetailsTokenService.list(new LambdaQueryWrapper<PropertyDetailsTokenEntity>()
                .eq(PropertyDetailsTokenEntity::getPropertyDetailsId, propertyDetailsId)
        );
        if (ObjectUtil.isEmpty(list)) return new ArrayList<>();

        List<Long> aiTokenIds = list.stream().map(PropertyDetailsTokenEntity::getAiTokenId).distinct().toList();

        List<AiTokenUseRecordEntity> result = aiTokenUseRecordService.list(new LambdaQueryWrapper<AiTokenUseRecordEntity>()
                .in(AiTokenUseRecordEntity::getId, aiTokenIds)
        );

        if (ObjectUtil.isNotEmpty(result)){
            return BeanUtil.copyToList(result, AiTokenUseRecordInfoVo.class);
        }
        return new ArrayList<>();
    }

    /**
     * 获取当前资产的实际id
     * @param userId
     * @param code
     * @return
     */
    @Override
    public Long getCurrentUserId(Long userId, String code) {
        CommodityTypeEntity one = commodityTypeService.getOne(new LambdaQueryWrapper<CommodityTypeEntity>()
                .eq(CommodityTypeEntity::getCode, code)
                .last("limit 1")
        );
        RRException.isNotEmpty(one, "资产类型查询出错");
        if (one.getSubAccountHave() == 0){
            Long userId1 = ResultUtil.getResult(userFeign.getUserParentId(userId));

            userId = ObjectUtil.defaultIfNull(userId1, userId);
        }
        return userId;
    }

    @Override
    @Transactional
    public void useSubscribeProperty(List<UpdateUserPropertyVo> list) {
        // redis
        HashMap<String, Object> map = new HashMap<>();
        for (UpdateUserPropertyVo vo : list) {
            Long sharePropertyId = (Long) redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.userUsePropertyIdCacheKey, vo.getUserId()));
            if (ObjectUtil.isEmpty(sharePropertyId)) {
                RRException.create("获取用户资产失败");
            }
            boolean isShareUpdate = this.updateUserPropertyType(vo.getUserId(), sharePropertyId, vo.getCode(), vo.getQuantity());
            if (!isShareUpdate) {
                throw new BusinessException(StatusCode.DATA_UPDATE_ERROR.getCode(), "更新资产失败");
            }
            map.put(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, vo.getUserId(), vo.getCode()), vo.getQuantity());
        }
        redisTemplate.opsForValue().multiSet(map);
    }

    private void setFieldValue(List<UserPropertyTypeInfoVo> list, String redisKeys, boolean isTotal) {
        Map<String, Object> userPropertyNum = new HashMap<>();
        List<CommodityTypeEntity> typeList = commodityTypeService.listAllByCache();
        if (ObjectUtil.isNotEmpty(typeList)){
            for (CommodityTypeEntity item : typeList) {
                String key = StrUtil.format(redisKeys, item.getCode());
                Object o = redisTemplate.opsForValue().get(key);
                if (o == null) continue;
                userPropertyNum.put(key, o);
            }
        }

        if (ObjectUtil.isNotEmpty(userPropertyNum)) {
            userPropertyNum.forEach((key, val) -> {
                Long value = Convert.toLong(val);
                if (value != null) {
                    String[] split = key.split(":");
                    String code = split[split.length - 1];
                    UserPropertyTypeInfoVo typeInfoVo = list.stream()
                            .filter(item -> item.getCommodityTypeCode().equals(code))
                            .findFirst().orElse(null);
                    CommodityTypeEntity commodityType = typeList.stream()
                            .filter(item -> code.equals(item.getCode())).findFirst().orElse(null);
                    if (commodityType != null){
                        // 判断是否存在
                        if (typeInfoVo == null) {
                            typeInfoVo = new UserPropertyTypeInfoVo();
                            typeInfoVo.setCommodityTypeCode(code);
                            if (isTotal) {
                                typeInfoVo.setTotalQuantity(value);
                            } else {
                                typeInfoVo.setUseQuantity(value);
                            }
                            list.add(typeInfoVo);
                        } else {
                            // 如果存在了，这个就是子账号，在看这个资产子账号是否拥有
                            if (isTotal) {
//                                typeInfoVo.setTotalQuantity(value);
                                if (typeInfoVo.getTotalQuantity() == null || commodityType.getSubAccountHave() == 1){
                                    typeInfoVo.setTotalQuantity(value);
                                }
                            } else {
                                if (typeInfoVo.getUseQuantity() == null || commodityType.getSubAccountHave() == 1){
                                    typeInfoVo.setUseQuantity(value);
                                }
                            }
                        }
                    }
                }
            });
        }
    }


    /**
     * 根据userId 获取该用户正在试用的资产id
     * @param userId
     * @return
     */
    @Override
    public Long clintGetData(Long userId) {
        List<UserPropertyEntity> userPropertyEntities = userPropertyService.lambdaQuery()
                .eq(UserPropertyEntity::getUserId, userId)
                //用户id 去查资产，且在使用的
                .eq(UserPropertyEntity::getIsUse, 1).list();
        if (userPropertyEntities.isEmpty()) {
            //如果没有资产，则直接返回
            throw new BusinessException(5001,"用户无在用资产");
        }
        return userPropertyEntities.get(0).getId();
    }


    /**
     * 清空用户资产-不是公用的资产
     * @param userIds 用户id
     * @param code 资产类型code
     */
    @Override
    public void clearPrivateProperty(List<Long> userIds, String code) {
        if (EmptyUtil.isEmpty(userIds) || EmptyUtil.isEmpty(code)) {
            throw new RRException("参数错误");
        }
        if (!commodityTypeService.lambdaQuery().eq(CommodityTypeEntity::getCode, code).last("limit 1").exists()) {
            throw new RRException("资产类型不存在");
        }
        List<Object> propertyObjs =  redisTemplate.opsForValue().multiGet(userIds.stream().map(userId -> RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, userId, code)).toList());
        if (EmptyUtil.isEmpty(propertyObjs)) {
            return;
        }
        Map<Long, Long> userPropertyIdMap = IntStream.range(0, userIds.size())
            .boxed().collect(Collectors.toMap(userIds::get, index -> {
                Object propertyId = propertyObjs.get(index);
                if (propertyId instanceof Long l) {
                    return l;
                }
                if (propertyId instanceof Integer i) {
                    return Long.valueOf(i);
                }
                return -1L;
            }));

        // 更新数据库
        userPropertyTypeService.clearUserPrivateProperty(userPropertyIdMap, code);

        try {
            // 更新redis缓存
            redisTemplate.opsForValue().multiSet(userIds.stream().collect(Collectors.toMap(userId -> RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, userId, code), userId -> 0)));
        } catch (Exception e) {
            log.error("清空用户资产缓存失败 userIds = {}, code = {}", userIds, code);
            throw new BusinessException(StatusCode.REDIS_OPERATE_ERROR);
        }
    }

    @Override
    public PageUtils<UserPropertyDetailsInfoVo> userPropertyDetails(UserPropertyDetailsList bo) {


        return null;
    }
}

