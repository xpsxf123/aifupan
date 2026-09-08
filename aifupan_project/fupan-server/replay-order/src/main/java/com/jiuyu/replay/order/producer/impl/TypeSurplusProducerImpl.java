package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.util.FunctionUtil;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.order.OrderDetailInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderDetailVo;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.order.bo.TypeSurplusBo;
import com.jiuyu.replay.order.bo.TypeSurplusListBo;
import com.jiuyu.replay.order.entity.*;
import com.jiuyu.replay.order.producer.TypeSurplusProducer;
import com.jiuyu.replay.order.repository.service.*;
import com.jiuyu.replay.order.vo.ClintGetDataVo;
import com.jiuyu.replay.order.vo.TypeSurplusInfoVo;
import com.jiuyu.replay.order.vo.TypeSurplusListVo;
import com.jiuyu.replay.order.vo.UserPropertyInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * 商品类型资产剩余表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Service
@Slf4j
public class TypeSurplusProducerImpl implements TypeSurplusProducer {

    @Resource
    private TypeSurplusService typeSurplusService;

    @Resource
    private UserPropertyService userPropertyService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private CommodityTypeService commodityTypeService;

    @Resource
    private OrderService orderService;

    @Resource
    private OrderDetailService orderDetailService;



    @Override
    public PageUtils<TypeSurplusListVo> queryPage(TypeSurplusListBo typeSurplusListBo) {
        QueryWrapper<TypeSurplusEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(typeSurplusListBo.getKeyword())) {
            wrapper.like("name", typeSurplusListBo.getKeyword());
        }

        IPage<TypeSurplusEntity> iPage = typeSurplusService.page(new Query<TypeSurplusEntity>().getPage(typeSurplusListBo.getPage(), typeSurplusListBo.getLimit()), wrapper);

        PageUtils<TypeSurplusListVo> pageUtils = new PageUtils<>(typeSurplusListBo.getPage(), typeSurplusListBo.getLimit(), iPage);

        List<TypeSurplusEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
            List<TypeSurplusListVo> vos = records.stream().map(item -> {
                TypeSurplusListVo typeSurplusVo = new TypeSurplusListVo();
                BeanUtils.copyProperties(item, typeSurplusVo);
                return typeSurplusVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public TypeSurplusInfoVo info(java.lang.Long id) {

        TypeSurplusEntity typeSurplusEntity = typeSurplusService.getById(id);
        if (typeSurplusEntity != null) {
            TypeSurplusInfoVo typeSurplusInfoVo = new TypeSurplusInfoVo();
            BeanUtils.copyProperties(typeSurplusEntity, typeSurplusInfoVo);
            return typeSurplusInfoVo;
        }

        return null;
    }

    /**
     * 新增商品类型资产剩余表
     *
     * @param typeSurplusBo 商品类型资产剩余表对象
     * @return
     */
    public TypeSurplusInfoVo save(TypeSurplusBo typeSurplusBo) {

        TypeSurplusEntity typeSurplusEntity = new TypeSurplusEntity();
        BeanUtils.copyProperties(typeSurplusBo, typeSurplusEntity);
        typeSurplusEntity.setId(SnowflakeManager.nextValue());

        typeSurplusService.save(typeSurplusEntity);

        TypeSurplusInfoVo typeSurplusInfoVo = new TypeSurplusInfoVo();
        BeanUtils.copyProperties(typeSurplusEntity, typeSurplusInfoVo);

        return typeSurplusInfoVo;
    }

    /**
     * 修改商品类型资产剩余表
     *
     * @param typeSurplusBo 商品类型资产剩余表对象
     * @return
     */
    public void update(TypeSurplusBo typeSurplusBo) {

        TypeSurplusEntity typeSurplusEntity = new TypeSurplusEntity();
        BeanUtils.copyProperties(typeSurplusBo, typeSurplusEntity);

        typeSurplusService.updateById(typeSurplusEntity);
    }

    /**
     * 删除商品类型资产剩余表
     *
     * @param id 商品类型资产剩余表id
     * @return
     */
    public void deleteById(java.lang.Long id) {

        typeSurplusService.removeById(id);
    }

    @Override
    public void payAddTypeSurplus(OrderInfoVo nowOrder, UserPropertyInfoVo userPropertyInfoVo) {
        // 获取上个订单中商品类型,
        List<TypeSurplusEntity> typeSurplusEntities = null;
        if (ObjectUtil.isNotEmpty(nowOrder) && (nowOrder.getOrderType() == 1)) {
            List<OrderDetailEntity> orderDetailEntities = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                    .eq(OrderDetailEntity::getOrderId, nowOrder.getBeforeUpgrading())
            );
            if (ObjectUtil.isNotEmpty(orderDetailEntities)){
                typeSurplusEntities = typeSurplusService.list(new LambdaQueryWrapper<TypeSurplusEntity>()
                        .eq(TypeSurplusEntity::getUserId, nowOrder.getUserId())
                        .in(TypeSurplusEntity::getOrderDetailId, orderDetailEntities.stream().map(OrderDetailEntity::getId).toList())
                );
            }
        }
        // 获取用户资产id
        if (ObjectUtil.isNotEmpty(nowOrder.getOrderDetailList())) {
            ArrayList<TypeSurplusEntity> entityList = new ArrayList<>();
            for (OrderDetailInfoVo item : nowOrder.getOrderDetailList()) {
                // 如果是重置用量的商品，则跳过
                if (item.getCommodityTypeReset() == null || item.getCommodityTypeReset() != 1 || item.getCommodityTypeCode().startsWith("child_"))
                    continue;
                TypeSurplusEntity typeSurplusEntity = new TypeSurplusEntity();
                typeSurplusEntity.setId(SnowflakeManager.nextValue());
                typeSurplusEntity.setCommodityTypeId(item.getCommodityTypeId());
                typeSurplusEntity.setCommodityTypeCode(item.getCommodityTypeCode());
                typeSurplusEntity.setPropertyId(userPropertyInfoVo.getId());
                typeSurplusEntity.setUserId(nowOrder.getUserId());
                typeSurplusEntity.setOrderDetailId(item.getId());
                // 获取固定的使用资产数量，比如主播录制数、监控位，这些数据在redis中
                Long surplusNumber = 0L;
//                if (nowOrder.getCommodityType() == 1 && item.getCommodityTypeReset() == 0) {
//                    surplusNumber = Convert.toLong(redisTemplate.opsForValue()
//                            .get(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, userPropertyInfoVo.getId(), item.getCommodityTypeCode())));
//                }
                if (nowOrder.getCommodityType() == 1 && ObjectUtil.isNotEmpty(typeSurplusEntities) && nowOrder.getOrderType() == 1) {
                    TypeSurplusEntity temp = typeSurplusEntities.stream()
                            .filter(val -> val.getCommodityTypeId().equals(item.getCommodityTypeId()))
                            .findFirst().orElse(null);
                    if (ObjectUtil.isNotEmpty(temp)) {
                        surplusNumber = temp.getUseNumber();
                    }
                }
                typeSurplusEntity.setUseNumber(surplusNumber == null ? 0 : surplusNumber);
                if (typeSurplusEntity.getUseNumber() > item.getTotalNumber()) {
                    typeSurplusEntity.setUseNumber(item.getTotalNumber());
                }
                typeSurplusEntity.setTotalNumber(item.getTotalNumber());
                if (nowOrder.getCommodityType() == 1) {
                    typeSurplusEntity.setStartTime(item.getCommodityTypeReset() == 1 ? item.getResetDate() : item.getStartDate());
                    typeSurplusEntity.setEndTime(item.getCommodityTypeReset() == 1 ? item.getNextReset() : item.getExpirationDate());
                } else {
                    typeSurplusEntity.setStartTime(item.getStartDate());
                    typeSurplusEntity.setEndTime(item.getExpirationDate());
                }
                typeSurplusEntity.setUseStatus(0);
                typeSurplusEntity.setTimeStatus(0);
                entityList.add(typeSurplusEntity);
            }
            typeSurplusService.saveBatch(entityList);
        }
    }

    @Override
    public void payAddTypeSurplus1(OrderInfoVo nowOrder, UserPropertyInfoVo userPropertyInfoVo) {
        if (nowOrder == null || ObjectUtil.isEmpty(nowOrder.getOrderDetailList())) {
            return;
        }
        List<Long> orderDetailsIds = typeSurplusService.list(new LambdaQueryWrapper<TypeSurplusEntity>()
                .eq(TypeSurplusEntity::getUserId, nowOrder.getUserId())
                .in(TypeSurplusEntity::getOrderDetailId, nowOrder.getOrderDetailList().stream().map(OrderDetailInfoVo::getId).toList())
                .groupBy(TypeSurplusEntity::getOrderDetailId)
                .select(TypeSurplusEntity::getOrderDetailId)
        ).stream().map(TypeSurplusEntity::getOrderDetailId).toList();

        // 获取用户资产id
        ArrayList<TypeSurplusEntity> entityList = new ArrayList<>();
        for (OrderDetailInfoVo item : nowOrder.getOrderDetailList()) {
            // 如果是重置用量的商品，则跳过
            if (item.getCommodityTypeReset() == null || item.getCommodityTypeReset() != 1 || item.getCommodityTypeCode().startsWith("child_") || orderDetailsIds.contains(item.getId())) {
                continue;
            }
            TypeSurplusEntity typeSurplusEntity = new TypeSurplusEntity();
            typeSurplusEntity.setId(SnowflakeManager.nextValue());
            typeSurplusEntity.setCommodityTypeId(item.getCommodityTypeId());
            typeSurplusEntity.setCommodityTypeCode(item.getCommodityTypeCode());
            typeSurplusEntity.setPropertyId(userPropertyInfoVo.getId());
            typeSurplusEntity.setUserId(nowOrder.getUserId());
            typeSurplusEntity.setOrderDetailId(item.getId());
            // 获取固定的使用资产数量，比如主播录制数、监控位，这些数据在redis中
            typeSurplusEntity.setUseNumber(0L);
            typeSurplusEntity.setTotalNumber(item.getTotalNumber());
            if (nowOrder.getCommodityType() == 1) {
                typeSurplusEntity.setStartTime(item.getCommodityTypeReset() == 1 ? item.getResetDate() : item.getStartDate());
                typeSurplusEntity.setEndTime(item.getCommodityTypeReset() == 1 ? item.getNextReset() : item.getExpirationDate());
            } else {
                typeSurplusEntity.setStartTime(item.getStartDate());
                typeSurplusEntity.setEndTime(item.getExpirationDate());
            }
            typeSurplusEntity.setUseStatus(0);
            typeSurplusEntity.setTimeStatus(0);
            entityList.add(typeSurplusEntity);
        }
        if (!entityList.isEmpty()) {
            typeSurplusService.saveBatch(entityList);
        }
    }


    /**
     * 支付后批量处理
     *
     * @param orderPropertyMap 订单和用户资产
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void batchPayAddTypeSurplus(Map<OrderInfoVo, UserPropertyInfoVo> orderPropertyMap) {
        if (EmptyUtil.isEmpty(orderPropertyMap)) {
            return;
        }
        
        // 构建用户资产与类型剩余实体的映射关系
        Map<UserPropertyInfoVo, List<TypeSurplusEntity>> propertyTypeSurplusMap = buildPropertyTypeSurplusMap(orderPropertyMap);
        
        // 构建需要保存的实体列表
        List<TypeSurplusEntity> entityList = buildTypeSurplusEntities(orderPropertyMap, propertyTypeSurplusMap);
        
        // 批量保存实体
        if (EmptyUtil.isNotEmpty(entityList)) {
            typeSurplusService.saveBatch(entityList, 1000);
        }
    }

    /**
     * 批量补救漏建的主账号 type_surplus
     * <p>以商品类型表的 is_reset 字段为准判断哪些明细应建资产，对比库中已存在的记录，补建缺失部分。
     * 不依赖订单详情表的 commodity_type_reset 字段，避免该字段数据异常导致漏建无法兜底。</p>
     *
     * @param orderPropertyMap 订单和用户资产映射
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void remedyMissedTypeSurplus(Map<OrderInfoVo, UserPropertyInfoVo> orderPropertyMap) {
        if (EmptyUtil.isEmpty(orderPropertyMap)) {
            return;
        }

        // 主账号需要重置清零的商品类型 code 集合（以商品类型表 is_reset 为准，非 child_ 前缀）
        Set<String> resetCodes = commodityTypeService.list().stream()
                .filter(type -> type.getIsReset() != null && type.getIsReset() == 1
                        && StrUtil.isNotEmpty(type.getCode()) && !type.getCode().startsWith("child_"))
                .map(CommodityTypeEntity::getCode)
                .collect(Collectors.toSet());
        if (resetCodes.isEmpty()) {
            return;
        }

        // 应建 type_surplus 的明细：code 属于重置类型，并记住所属订单与用户资产
        Map<Long, Map.Entry<OrderInfoVo, UserPropertyInfoVo>> shouldBuildMap = orderPropertyMap.entrySet().stream()
                .filter(entry -> EmptyUtil.isNotEmpty(entry.getKey().getOrderDetailList()))
                .flatMap(entry -> entry.getKey().getOrderDetailList().stream()
                        .filter(item -> resetCodes.contains(item.getCommodityTypeCode()))
                        .map(item -> Map.entry(item.getId(), entry)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
        if (shouldBuildMap.isEmpty()) {
            return;
        }

        // 库中已存在的 type_surplus 明细 id
        Set<Long> existingIds = typeSurplusService.lambdaQuery()
                .in(TypeSurplusEntity::getOrderDetailId, shouldBuildMap.keySet())
                .select(TypeSurplusEntity::getOrderDetailId)
                .list().stream()
                .map(TypeSurplusEntity::getOrderDetailId)
                .collect(Collectors.toSet());

        // 补建漏掉的明细
        List<TypeSurplusEntity> remedyList = shouldBuildMap.entrySet().stream()
                .filter(entry -> !existingIds.contains(entry.getKey()))
                .map(entry -> {
                    OrderInfoVo order = entry.getValue().getKey();
                    UserPropertyInfoVo userProperty = entry.getValue().getValue();
                    OrderDetailInfoVo item = order.getOrderDetailList().stream()
                            .filter(detail -> detail.getId().equals(entry.getKey()))
                            .findFirst().orElse(null);
                    if (item == null) {
                        return null;
                    }
                    TypeSurplusEntity entity = createTypeSurplusEntity(order, userProperty, null, item);
                    // 补救以商品类型表 is_reset 判定为重置类型，时间用 resetDate/nextReset 对齐主账号口径
                    if (order.getCommodityType() != null && order.getCommodityType() == 1) {
                        entity.setStartTime(item.getResetDate());
                        entity.setEndTime(item.getNextReset());
                    }
                    return entity;
                })
                .filter(Objects::nonNull)
                .toList();
        if (EmptyUtil.isNotEmpty(remedyList)) {
            typeSurplusService.saveBatch(remedyList, 1000);
        }
    }

    /**
     * 构建用户资产与类型剩余实体的映射关系
     *
     * @param orderPropertyMap 订单和用户资产映射
     * @return 用户资产与类型剩余实体的映射关系
     */
    private Map<UserPropertyInfoVo, List<TypeSurplusEntity>> buildPropertyTypeSurplusMap(
            Map<OrderInfoVo, UserPropertyInfoVo> orderPropertyMap) {
        
        Map<UserPropertyInfoVo, List<TypeSurplusEntity>> propertyTypeSurplusMap = new HashMap<>(orderPropertyMap.size(), 1F);
        
        // 获取所有需要升级的订单ID
        List<Long> beforeUpgradingIds = extractBeforeUpgradingIds(orderPropertyMap);
        if (EmptyUtil.isEmpty(beforeUpgradingIds)) {
            return propertyTypeSurplusMap;
        }
        
        // 获取升级订单的详情ID映射
        Map<Long, List<Long>> beforeUpgradingOrderDetailMap = getBeforeUpgradingOrderDetailMap(beforeUpgradingIds);
        if (EmptyUtil.isEmpty(beforeUpgradingOrderDetailMap)) {
            return propertyTypeSurplusMap;
        }
        
        // 获取类型剩余实体列表
        Map<Long, TypeSurplusEntity> typeSurplusList = getTypeSurplusList(orderPropertyMap, beforeUpgradingOrderDetailMap);
        if (EmptyUtil.isEmpty(typeSurplusList)) {
            return propertyTypeSurplusMap;
        }
        
        // 构建用户资产与类型剩余实体的映射关系
        buildUserTypeSurplusMapping(orderPropertyMap, propertyTypeSurplusMap, beforeUpgradingOrderDetailMap, typeSurplusList);
        
        return propertyTypeSurplusMap;
    }
    
    /**
     * 提取需要升级的订单ID列表
     *
     * @param orderPropertyMap 订单和用户资产映射
     * @return 需要升级的订单ID列表
     */
    private List<Long> extractBeforeUpgradingIds(Map<OrderInfoVo, UserPropertyInfoVo> orderPropertyMap) {
        return orderPropertyMap.keySet().stream()
                .filter(order -> order.getOrderType() == 1 && EmptyUtil.isNotEmpty(order.getOrderDetailList()))
                .map(OrderInfoVo::getBeforeUpgrading)
                .filter(Objects::nonNull)
                .filter(beforeUpgradingId -> beforeUpgradingId > 0)
                .distinct()
                .toList();
    }
    
    /**
     * 获取升级订单的详情ID映射
     *
     * @param beforeUpgradingIds 升级订单ID列表
     * @return 订单ID与详情ID列表的映射关系
     */
    private Map<Long, List<Long>> getBeforeUpgradingOrderDetailMap(List<Long> beforeUpgradingIds) {
        return orderDetailService.lambdaQuery()
                .in(OrderDetailEntity::getOrderId, beforeUpgradingIds)
                .select(OrderDetailEntity::getOrderId, OrderDetailEntity::getId)
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        OrderDetailEntity::getOrderId, 
                        Collectors.mapping(OrderDetailEntity::getId, Collectors.toList())
                ));
    }
    
    /**
     * 获取类型剩余实体列表
     *
     * @param orderPropertyMap 订单和用户资产映射
     * @param beforeUpgradingOrderDetailMap 升级订单详情ID映射
     * @return 类型剩余实体映射
     */
    private Map<Long, TypeSurplusEntity> getTypeSurplusList(
            Map<OrderInfoVo, UserPropertyInfoVo> orderPropertyMap,
            Map<Long, List<Long>> beforeUpgradingOrderDetailMap) {
        
        List<Long> orderDetailIds = beforeUpgradingOrderDetailMap.values()
                .stream()
                .flatMap(Collection::stream)
                .toList();
        
        List<Long> userIds = orderPropertyMap.keySet()
                .stream()
                .map(OrderInfoVo::getUserId)
                .toList();
        
        return typeSurplusService.lambdaQuery()
                .in(TypeSurplusEntity::getOrderDetailId, orderDetailIds)
                .in(TypeSurplusEntity::getUserId, userIds)
                .list()
                .stream()
                .collect(Collectors.toMap(TypeSurplusEntity::getId, Function.identity()));
    }
    
    /**
     * 构建用户资产与类型剩余实体的映射关系
     *
     * @param orderPropertyMap 订单和用户资产映射
     * @param propertyTypeSurplusMap 用户资产与类型剩余实体映射结果
     * @param beforeUpgradingOrderDetailMap 升级订单详情ID映射
     * @param typeSurplusList 类型剩余实体列表
     */
    private void buildUserTypeSurplusMapping(
            Map<OrderInfoVo, UserPropertyInfoVo> orderPropertyMap,
            Map<UserPropertyInfoVo, List<TypeSurplusEntity>> propertyTypeSurplusMap,
            Map<Long, List<Long>> beforeUpgradingOrderDetailMap,
            Map<Long, TypeSurplusEntity> typeSurplusList) {
        
        orderPropertyMap.forEach((order, userProperty) -> {
            List<TypeSurplusEntity> userTypeSurplusList = buildUserTypeSurplusList(
                    order, userProperty, beforeUpgradingOrderDetailMap, typeSurplusList);
            
            if (EmptyUtil.isNotEmpty(userTypeSurplusList)) {
                propertyTypeSurplusMap.put(userProperty, userTypeSurplusList);
            }
        });
    }
    
    /**
     * 构建单个用户的类型剩余实体列表
     *
     * @param order 订单信息
     * @param userProperty 用户资产信息
     * @param beforeUpgradingOrderDetailMap 升级订单详情ID映射
     * @param typeSurplusList 类型剩余实体列表
     * @return 用户的类型剩余实体列表
     */
    private List<TypeSurplusEntity> buildUserTypeSurplusList(
            OrderInfoVo order,
            UserPropertyInfoVo userProperty,
            Map<Long, List<Long>> beforeUpgradingOrderDetailMap,
            Map<Long, TypeSurplusEntity> typeSurplusList) {
        
        Long beforeUpgradingId = order.getBeforeUpgrading();
        if (beforeUpgradingId == null || beforeUpgradingId <= 0) {
            return List.of();
        }
        
        List<Long> orderDetailIds = beforeUpgradingOrderDetailMap.getOrDefault(beforeUpgradingId, List.of());
        return orderDetailIds.stream()
                .map(typeSurplusList::get)
                .filter(Objects::nonNull)
                .filter(typeSurplus -> Objects.equals(typeSurplus.getUserId(), order.getUserId()))
                .toList();
    }
    
    /**
     * 构建类型剩余实体列表
     *
     * @param orderPropertyMap 订单和用户资产映射
     * @param propertyTypeSurplusMap 用户资产与类型剩余实体映射
     * @return 类型剩余实体列表
     */
    private List<TypeSurplusEntity> buildTypeSurplusEntities(
            Map<OrderInfoVo, UserPropertyInfoVo> orderPropertyMap,
            Map<UserPropertyInfoVo, List<TypeSurplusEntity>> propertyTypeSurplusMap) {
        
        return orderPropertyMap.entrySet().stream()
                .filter(entry -> EmptyUtil.isNotEmpty(entry.getKey().getOrderDetailList()))
                .flatMap(entry -> buildOrderTypeSurplusEntities(entry, propertyTypeSurplusMap))
                .toList();
    }
    
    /**
     * 构建单个订单的类型剩余实体列表
     *
     * @param entry 订单和用户资产映射项
     * @param propertyTypeSurplusMap 用户资产与类型剩余实体映射
     * @return 订单的类型剩余实体列表
     */
    private Stream<TypeSurplusEntity> buildOrderTypeSurplusEntities(
            Map.Entry<OrderInfoVo, UserPropertyInfoVo> entry,
            Map<UserPropertyInfoVo, List<TypeSurplusEntity>> propertyTypeSurplusMap) {
        
        OrderInfoVo nowOrder = entry.getKey();
        UserPropertyInfoVo userPropertyInfoVo = entry.getValue();
        List<TypeSurplusEntity> typeSurplusEntities = propertyTypeSurplusMap.get(userPropertyInfoVo);
        
        return nowOrder.getOrderDetailList().stream()
                .filter(item -> item.getCommodityTypeReset() != null && item.getCommodityTypeReset() == 1 && !item.getCommodityTypeCode().startsWith("child_"))
                .map(item -> createTypeSurplusEntity(nowOrder, userPropertyInfoVo, typeSurplusEntities, item));
    }
    
    /**
     * 创建类型剩余实体
     *
     * @param nowOrder 当前订单
     * @param userPropertyInfoVo 用户资产信息
     * @param typeSurplusEntities 类型剩余实体列表
     * @param item 订单详情项
     * @return 类型剩余实体
     */
    private TypeSurplusEntity createTypeSurplusEntity(
            OrderInfoVo nowOrder,
            UserPropertyInfoVo userPropertyInfoVo,
            List<TypeSurplusEntity> typeSurplusEntities,
            OrderDetailInfoVo item) {
        
        TypeSurplusEntity typeSurplusEntity = new TypeSurplusEntity();
        typeSurplusEntity.setId(SnowflakeManager.nextValue());
        typeSurplusEntity.setCommodityTypeId(item.getCommodityTypeId());
        typeSurplusEntity.setCommodityTypeCode(item.getCommodityTypeCode());
        typeSurplusEntity.setPropertyId(userPropertyInfoVo.getId());
        typeSurplusEntity.setUserId(nowOrder.getUserId());
        typeSurplusEntity.setOrderDetailId(item.getId());
        
        // 获取固定的使用资产数量，比如主播录制数、监控位，这些数据在redis中
        long surplusNumber = getSurplusNumber(nowOrder, typeSurplusEntities, item);
        typeSurplusEntity.setUseNumber(surplusNumber);
        
        // 确保使用数量不超过总数量
        if (typeSurplusEntity.getUseNumber() > item.getTotalNumber()) {
            typeSurplusEntity.setUseNumber(item.getTotalNumber());
        }
        
        typeSurplusEntity.setTotalNumber(item.getTotalNumber());
        
        // 设置开始和结束时间
        setStartAndEndTime(typeSurplusEntity, nowOrder, item);
        
        typeSurplusEntity.setUseStatus(0);
        typeSurplusEntity.setTimeStatus(0);
        
        return typeSurplusEntity;
    }
    
    /**
     * 获取剩余数量
     *
     * @param nowOrder 当前订单
     * @param typeSurplusEntities 类型剩余实体列表
     * @param item 订单详情项
     * @return 剩余数量
     */
    private long getSurplusNumber(OrderInfoVo nowOrder, List<TypeSurplusEntity> typeSurplusEntities, OrderDetailInfoVo item) {
        long surplusNumber = 0L;
        
        if (nowOrder.getCommodityType() == 1 && ObjectUtil.isNotEmpty(typeSurplusEntities) && nowOrder.getOrderType() == 1) {
            TypeSurplusEntity temp = typeSurplusEntities.stream()
                    .filter(val -> val.getCommodityTypeId().equals(item.getCommodityTypeId()))
                    .findFirst()
                    .orElse(null);
            
            if (EmptyUtil.isNotEmpty(temp)) {
                surplusNumber = temp.getUseNumber() == null ? 0 : temp.getUseNumber();
            }
        }
        
        return surplusNumber;
    }
    
    /**
     * 设置开始和结束时间
     *
     * @param typeSurplusEntity 类型剩余实体
     * @param nowOrder 当前订单
     * @param item 订单详情项
     */
    private void setStartAndEndTime(TypeSurplusEntity typeSurplusEntity, OrderInfoVo nowOrder, OrderDetailInfoVo item) {
        if (nowOrder.getCommodityType() == 1) {
            typeSurplusEntity.setStartTime(item.getCommodityTypeReset() == 1 ? item.getResetDate() : item.getStartDate());
            typeSurplusEntity.setEndTime(item.getCommodityTypeReset() == 1 ? item.getNextReset() : item.getExpirationDate());
        } else {
            typeSurplusEntity.setStartTime(item.getStartDate());
            typeSurplusEntity.setEndTime(item.getExpirationDate());
        }
    }
    
    @Override
    public void payAddChildTypeSurplus(OrderInfoVo order) {
        if (ObjectUtil.isNotEmpty(order.getUserId())) {
            // 查询子用户的资产id
            List<UserPropertyEntity> userPropertyList = userPropertyService.list(new LambdaQueryWrapper<UserPropertyEntity>()
                    .eq(UserPropertyEntity::getParentUserId, order.getUserId())
                    .eq(UserPropertyEntity::getType, 1)
                    .eq(UserPropertyEntity::getIsUse, 1)
            );
            List<CommodityTypeEntity> commodityTypeList = commodityTypeService.list();
            if (ObjectUtil.isNotEmpty(userPropertyList) && ObjectUtil.isNotEmpty(order.getOrderDetailList())) {
                ArrayList<TypeSurplusEntity> entityList = new ArrayList<>();

                List<String> orderDetailsIds = typeSurplusService.list(new LambdaQueryWrapper<TypeSurplusEntity>()
                                .in(TypeSurplusEntity::getUserId, userPropertyList.stream().map(UserPropertyEntity::getUserId).distinct().toList())
                                .in(TypeSurplusEntity::getOrderDetailId, order.getOrderDetailList().stream().map(OrderDetailInfoVo::getId).toList())
                                .groupBy(TypeSurplusEntity::getOrderDetailId, TypeSurplusEntity::getUserId)
                                .select(TypeSurplusEntity::getOrderDetailId))
                        .stream()
                        .map(item -> StrUtil.format("{}_{}", item.getUserId(), item.getOrderDetailId())).toList();

                for (UserPropertyEntity userProperty : userPropertyList) {
                    List<OrderDetailInfoVo> orderDetailList = order.getOrderDetailList();

                    for (OrderDetailInfoVo orderDetailInfoVo : orderDetailList) {
                        CommodityTypeEntity currentCommodityType = commodityTypeList.stream()
                                .filter(item -> item.getCode().equals(orderDetailInfoVo.getCommodityTypeCode()))
                                .findAny()
                                .orElse(null);
                        // 是null、不要重置、子账号没有的跳过
                        if (currentCommodityType == null ||
                                ObjectUtil.isEmpty(currentCommodityType.getCode()) ||
                                !currentCommodityType.getCode().startsWith("child_") ||
                                !ObjectUtil.equals(currentCommodityType.getIsReset(), 1) ||
                                !ObjectUtil.equals(currentCommodityType.getSubAccountHave(), 1) ||
                                orderDetailsIds.contains(StrUtil.format("{}_{}", userProperty.getUserId(), orderDetailInfoVo.getId()))
                        ) {
                            continue;
                        }
                        CommodityTypeEntity parentCommodityType = commodityTypeList.stream()
                                .filter(item -> orderDetailInfoVo.getCommodityTypeCode().equals("child_" + item.getCode()))
                                .findAny()
                                .orElse(null);

                        TypeSurplusEntity typeSurplusEntity = new TypeSurplusEntity();
                        typeSurplusEntity.setId(SnowflakeManager.nextValue());
                        typeSurplusEntity.setCommodityTypeId(parentCommodityType.getId());
                        typeSurplusEntity.setCommodityTypeCode(parentCommodityType.getCode());
                        typeSurplusEntity.setPropertyId(userProperty.getId());
                        typeSurplusEntity.setUserId(userProperty.getUserId());
                        typeSurplusEntity.setOrderDetailId(orderDetailInfoVo.getId());
                        // 获取固定的使用资产数量，比如主播录制数、监控位，这些数据在redis中
                        typeSurplusEntity.setUseNumber(0L);
                        typeSurplusEntity.setTotalNumber(orderDetailInfoVo.getTotalNumber());
                        typeSurplusEntity.setStartTime(orderDetailInfoVo.getStartDate());
                        typeSurplusEntity.setEndTime(orderDetailInfoVo.getExpirationDate());
                        typeSurplusEntity.setUseStatus(0);
                        typeSurplusEntity.setTimeStatus(0);
                        entityList.add(typeSurplusEntity);
                    }
                }
                log.info("新增子账号资产：{}", entityList);
                typeSurplusService.saveBatch(entityList);
            }
        }
    }

    /**
     * 批量添加子用户资产
     *
     * @param orderInfoVos 订单信息列表，包含用户ID和订单详情列表
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void batchPayAddChildTypeSurplus(Collection<OrderInfoVo> orderInfoVos) {
        // 如果订单列表为空，则直接返回
        if (EmptyUtil.isEmpty(orderInfoVos)) {
            return;
        }

        // 按用户ID分组，合并每个用户的订单详情列表
        Map<Long, List<OrderDetailInfoVo>> userOrderMap = orderInfoVos.stream()
            .filter(order -> EmptyUtil.isNotEmpty(order.getUserId()) && EmptyUtil.isNotEmpty(order.getOrderDetailList()))
            .collect(Collectors.toMap(
                OrderInfoVo::getUserId,
                OrderInfoVo::getOrderDetailList,
                (v1, v2) -> Stream.concat(v1.stream(), v2.stream()).toList()
            ));

        // 如果没有有效的用户订单映射，则直接返回
        if (EmptyUtil.isEmpty(userOrderMap)) {
            return;
        }

        // 查询所有商品类型数据
        List<CommodityTypeEntity> commodityTypeList = commodityTypeService.list();
        if (EmptyUtil.isEmpty(commodityTypeList)) {
            throw new RRException("商品类型不存在");
        }

        // 筛选出需要重置且子账户可用的商品类型，并构建code到实体的映射
        Map<String, CommodityTypeEntity> restTypeMap = commodityTypeList.stream()
                .filter(type -> type.getIsReset() != null && type.getCode() != null && type.getIsReset() == 1 && type.getSubAccountHave() == 1 && type.getCode().startsWith("child_"))
            .collect(Collectors.toMap(
                CommodityTypeEntity::getCode,
                Function.identity(),
                FunctionUtil::mergeFirst
            ));

        // 如果没有符合条件的商品类型，则直接返回
        if (EmptyUtil.isEmpty(restTypeMap)) {
            return;
        }

        Map<String, CommodityTypeEntity> commodityTypeAllMap = commodityTypeList.stream()
                .collect(Collectors.toMap(CommodityTypeEntity::getCode, Function.identity(), FunctionUtil::mergeFirst));

        // 查询相关用户的资产信息，并按用户ID分组
        Map<Long, List<UserPropertyEntity>> userPropertyMap = userPropertyService.lambdaQuery()
                .in(UserPropertyEntity::getParentUserId, userOrderMap.keySet())
            .eq(UserPropertyEntity::getType, 1)
                .eq(UserPropertyEntity::getIsUse, 1)
            .list().stream()
                .collect(Collectors.groupingBy(UserPropertyEntity::getParentUserId));

        // 如果没有查询到用户资产信息，则直接返回
        if (EmptyUtil.isEmpty(userPropertyMap)) {
            return;
        }

        String prefix = "child_";


        // 构建待保存的子用户资产实体列表
        List<TypeSurplusEntity> entityList = userPropertyMap.entrySet().stream()
                .flatMap(entry -> {
                    Long userId = entry.getKey();

                    return userOrderMap.getOrDefault(userId, List.of()).stream()
                            .filter(item -> item.getCommodityTypeCode().startsWith(prefix))
                            .flatMap(orderDetail -> {
                                return entry.getValue().stream().map(item -> {
                                            CommodityTypeEntity temp = restTypeMap.get(orderDetail.getCommodityTypeCode());
                                            // 如果找不到对应的商品类型，则跳过该订单项
                                            if (EmptyUtil.isEmpty(temp)) {
                                                return null;
                                            }
                                            String code = orderDetail.getCommodityTypeCode().substring(prefix.length());
                                            CommodityTypeEntity commodityType = commodityTypeAllMap.get(code);

                                            // 如果找不到对应的商品类型，则跳过该订单项
                                            if (EmptyUtil.isEmpty(commodityType)) {
                                                return null;
                                            }

                                            TypeSurplusEntity typeSurplusEntity = new TypeSurplusEntity();
                                            typeSurplusEntity.setId(SnowflakeManager.nextValue());
                                            typeSurplusEntity.setCommodityTypeId(commodityType.getId());
                                            typeSurplusEntity.setCommodityTypeCode(commodityType.getCode());
                                            typeSurplusEntity.setPropertyId(item.getId());
                                            typeSurplusEntity.setUserId(item.getUserId());
                                            typeSurplusEntity.setOrderDetailId(orderDetail.getId());
                                            // 获取固定的使用资产数量（如主播录制数、监控位），这些数据在redis中维护
                                            typeSurplusEntity.setUseNumber(0L);
                                            typeSurplusEntity.setTotalNumber(orderDetail.getTotalNumber());
                                            typeSurplusEntity.setStartTime(orderDetail.getStartDate());
                                            typeSurplusEntity.setEndTime(orderDetail.getExpirationDate());
                                            typeSurplusEntity.setUseStatus(0);
                                            typeSurplusEntity.setTimeStatus(0);
                                            return typeSurplusEntity;
                                        })
                                        .filter(Objects::nonNull); // 过滤掉无效实体
                            });
                })
                .toList();

        // 批量保存构建好的资产实体列表
        if (EmptyUtil.isNotEmpty(entityList)) {
            typeSurplusService.saveBatch(entityList, 1000);
        }
    }


    @Override
    public void synchronousTypeSurplus(List<OrderDetailInfoVo> orderDetailsList) {

        // 保存已经存在的typeSurplus数据
        List<OrderDetailInfoVo> list1 = orderDetailsList.stream()
                .filter(item -> ObjectUtil.isEmpty(item.getPropertyId()) && item.getCommodityTypeReset() == 1 && (item.getStatus() == 1 || item.getStatus() == 4))
                .toList();
        if (ObjectUtil.isNotEmpty(list1)){
            List<Long> orderDetailsIds = list1.stream().map(OrderDetailVo::getId).toList();
            if (ObjectUtil.isNotEmpty(orderDetailsIds)){

                List<TypeSurplusEntity> list = typeSurplusService.list(new LambdaQueryWrapper<TypeSurplusEntity>()
                        .in(TypeSurplusEntity::getOrderDetailId, orderDetailsIds)
                );
                for (TypeSurplusEntity item : list) {
                    OrderDetailInfoVo detailInfoVo = list1.stream().filter(val -> val.getId().equals(item.getOrderDetailId())).findAny().orElse(null);
                    if (ObjectUtil.isNotEmpty(detailInfoVo)){
                        item.setTotalNumber(detailInfoVo.getTotalNumber());
                    }
                }
                // 修改总数量
                list = list.stream().map(item -> {
                    TypeSurplusEntity e = new TypeSurplusEntity();
                    e.setId(item.getId());
                    e.setTotalNumber(item.getTotalNumber());
                    return e;
                }).toList();
                if (ObjectUtil.isNotEmpty(list))typeSurplusService.updateBatchById(list);
            }
        }

        List<OrderDetailInfoVo> list2 = orderDetailsList.stream()
                .filter(item -> ObjectUtil.isNotEmpty(item.getPropertyId()) && item.getCommodityTypeReset() == 1 && (item.getStatus() == 1 || item.getStatus() == 4))
                .toList();
        if (ObjectUtil.isNotEmpty(list2)){
            List<Long> userIds = list2.stream().map(OrderDetailInfoVo::getUserId).distinct().toList();
            List<CommodityTypeEntity> commodityTypeList = commodityTypeService.listAllByCache();
            ArrayList<TypeSurplusEntity> entityList = new ArrayList<>();
            for (OrderDetailInfoVo orderDetail : list2) {
                // 添加主账号的资产
                TypeSurplusEntity surplus = new TypeSurplusEntity();
                surplus.setId(SnowflakeManager.nextValue());
                surplus.setCommodityTypeId(orderDetail.getCommodityTypeId());
                surplus.setCommodityTypeCode(orderDetail.getCommodityTypeCode());
                surplus.setPropertyId(orderDetail.getPropertyId());
                surplus.setUserId(orderDetail.getUserId());
                surplus.setOrderDetailId(orderDetail.getId());
                surplus.setUseNumber(0L);
                surplus.setTotalNumber(orderDetail.getTotalNumber());
                surplus.setStartTime(orderDetail.getResetDate());
                surplus.setEndTime(orderDetail.getNextReset() == null ? orderDetail.getExpirationDate() : orderDetail.getNextReset());
                if (orderDetail.getStatus() == 1){
                    surplus.setUseStatus(0);
                }else if (orderDetail.getStatus() == 4){
                    surplus.setUseStatus(4);
                }
                surplus.setTimeStatus(0);
                entityList.add(surplus);


                // 保存子账号资产
                CommodityTypeEntity commodityType = commodityTypeList.stream()
                        .filter(item -> item.getCode().equals(orderDetail.getCommodityTypeCode()))
                        .findAny()
                        .orElse(null);
                if (commodityType.getSubAccountHave() == 1 && commodityType.getIsReset() != null && commodityType.getIsReset() == 1){
                    // 查询子用户的资产id
                    List<UserPropertyEntity> userPropertyList = userPropertyService.list(new LambdaQueryWrapper<UserPropertyEntity>()
                            .eq(UserPropertyEntity::getParentId, orderDetail.getUserId())
                            .eq(UserPropertyEntity::getType, 1)
                    );
                    if (ObjectUtil.isNotEmpty(userPropertyList)){
                        userPropertyList.forEach(userProperty -> {
                            TypeSurplusEntity typeSurplusEntity = new TypeSurplusEntity();
                            typeSurplusEntity.setId(SnowflakeManager.nextValue());
                            typeSurplusEntity.setCommodityTypeId(orderDetail.getCommodityTypeId());
                            typeSurplusEntity.setCommodityTypeCode(orderDetail.getCommodityTypeCode());
                            typeSurplusEntity.setPropertyId(userProperty.getId());
                            typeSurplusEntity.setUserId(userProperty.getUserId());
                            typeSurplusEntity.setOrderDetailId(orderDetail.getId());
                            typeSurplusEntity.setUseNumber(0L);
                            typeSurplusEntity.setTotalNumber(orderDetail.getTotalNumber());
                            typeSurplusEntity.setStartTime(orderDetail.getStartDate());
                            typeSurplusEntity.setEndTime(orderDetail.getExpirationDate());
                            typeSurplusEntity.setUseStatus(0);
                            typeSurplusEntity.setTimeStatus(0);
                            entityList.add(typeSurplusEntity);
                        });
                    }
                }
            }
            // 保存到数据库中
            if (ObjectUtil.isNotEmpty(entityList)) typeSurplusService.saveBatch(entityList);
        }

    }

    @Override
    public List<TypeSurplusInfoVo> currentSurplus(Long propertyId, Long commodityTypeId) {
        List<TypeSurplusEntity> typeSurplusEntities = typeSurplusService.list(new LambdaQueryWrapper<TypeSurplusEntity>()
                .eq(TypeSurplusEntity::getPropertyId, propertyId)
                .eq(TypeSurplusEntity::getCommodityTypeId, commodityTypeId)
                .in(TypeSurplusEntity::getUseStatus, 0, 1)
                .eq(TypeSurplusEntity::getTimeStatus, 0)
//                .le(TypeSurplusEntity::getStartTime, new Date())
//                .ge(TypeSurplusEntity::getEndTime, new Date())
                .orderByAsc(TypeSurplusEntity::getEndTime)
        );
        if (ObjectUtil.isNotEmpty(typeSurplusEntities)) {
            List<TypeSurplusInfoVo> result = BeanUtil.copyToList(typeSurplusEntities, TypeSurplusInfoVo.class);
            // 查询订单
            List<OrderDetailEntity> orderDetailList = orderDetailService.listByIds(typeSurplusEntities.stream().map(TypeSurplusEntity::getOrderDetailId).toList());
            if (ObjectUtil.isNotEmpty(orderDetailList)){
                List<OrderEntity> orderList = orderService.listByIds(orderDetailList.stream().map(OrderDetailEntity::getOrderId).toList());
                if (ObjectUtil.isNotEmpty(orderList)){
                    for (TypeSurplusInfoVo surplusInfoVo : result) {
                        orderDetailList.stream().filter(item -> item.getId().equals(surplusInfoVo.getOrderDetailId())).findFirst().ifPresent(item -> {
                            orderList.stream().filter(val -> val.getId().equals(item.getOrderId())).findFirst().ifPresent(val -> {
                                surplusInfoVo.setCommodityType(val.getCommodityType());
                                surplusInfoVo.setCommodityTypeLevel(val.getLevel());
                            });
                        });
                    }
                }
            }
            return result;
        }
        return List.of();
    }

    @Override
    public void updateBatch(List<TypeSurplusBo> typeSurplusBos) {
        if (ObjectUtil.isEmpty(typeSurplusBos)) return;
        typeSurplusService.updateBatchById(BeanUtil.copyToList(typeSurplusBos, TypeSurplusEntity.class));
    }



    /**
     * 根据详情结果id，查需重置的（生效中的、在使用的、用完的）资产的用量和总理
     * 并且根据订单详情id设置对应的资产用量，剩余，总量
     * @param details
     */
    @Override
    public void selectByDetailIds(List<ClintGetDataVo> details) {
        if (!details.isEmpty()) {
            //需重置的详情
            List<Long> ids = details.stream().filter(x -> x.getCommodityTypeReset() == 1).map(ClintGetDataVo::getId).toList();
            //获取用量
            List<TypeSurplusEntity> surplusEntityList = ObjectUtil.isEmpty(ids) ? new ArrayList<>() : typeSurplusService.lambdaQuery()
                    //订单详情id
                    .in(TypeSurplusEntity::getOrderDetailId, ids)
                    //在使用的、用完的资产
                    .in(TypeSurplusEntity::getUseStatus, List.of(0, 1))
                    //生效中的资产
                    .eq(TypeSurplusEntity::getTimeStatus, 0)
                    .list();

            //根据订单详情id设置对应的资产用量，剩余，总量
            details.forEach(x -> {
                if (x.getUseNumber()==null){
                    x.setUseNumber(BigDecimal.ZERO);
                }
                if(x.getTotalNumber()==null){
                    x.setTotalNumber(BigDecimal.ZERO);
                }
                if (!surplusEntityList.isEmpty()) {
                    //商品类型资产转为map进行匹配(key:orderDetailId ,value:TypeSurplusEntity)
                    Map<Long, TypeSurplusEntity> surplusEntityMap = surplusEntityList.stream().collect(Collectors
                            .toMap(TypeSurplusEntity::getOrderDetailId, Function.identity(), (o, n) -> n));
                    //商品类型资产 匹配
                    if (!surplusEntityMap.isEmpty()) {
                        TypeSurplusEntity typeSurplusEntity = surplusEntityMap.get(x.getId());
                        if (typeSurplusEntity != null) {
                            x.setUseNumber(BigDecimal.valueOf(typeSurplusEntity.getUseNumber()));
                            x.setTotalNumber(BigDecimal.valueOf(typeSurplusEntity.getTotalNumber()));
                            x.setStartTime(typeSurplusEntity.getStartTime());
                        }
                    }
                }
                x.setEndTime(CommonUtils.getResourceUpdateTime(x.getEndTime()));
                BigDecimal remUseNumber = x.getTotalNumber().subtract(x.getUseNumber());
                x.setRemUseNumber(remUseNumber.compareTo(BigDecimal.ZERO)<0?BigDecimal.ZERO:remUseNumber);
            });
        }
    }


    /**
     * 获取当前用户剩余数量
     *
     * @param propertyId      资产ID
     * @param commodityTypeId 套餐资产类型ID
     *
     * @return 总剩余数量
     */
    @Override
    public Long currentSurplusCount(Long propertyId, Long commodityTypeId) {
        return typeSurplusService.currentSurplusCount(propertyId, commodityTypeId);
    }
}

