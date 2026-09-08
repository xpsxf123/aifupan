package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.util.DateOps;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.util.FunctionUtil;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.utils.*;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.*;
import com.jiuyu.replay.order.bo.OrderBo;
import com.jiuyu.replay.order.bo.OrderListBo;
import com.jiuyu.replay.order.entity.*;
import com.jiuyu.replay.order.producer.OrderProducer;
import com.jiuyu.replay.order.repository.dao.OrderDao;
import com.jiuyu.replay.order.repository.dao.OrderPayDao;
import com.jiuyu.replay.order.repository.service.*;
import com.jiuyu.replay.order.repository.service.impl.OrderPayServiceImpl;
import com.jiuyu.replay.order.vo.OrderFinallyVo;
import com.jiuyu.replay.order.vo.OrderTimeVo;
import com.jiuyu.replay.order.vo.PackageInfoVo;
import com.jiuyu.replay.order.vo.TypeConsumptionVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * 订单
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Slf4j
@Service
@AllArgsConstructor
public class OrderProducerImpl implements OrderProducer {

    private final OrderService orderService;

    private final OrderDetailService orderDetailService;

    private final TypeSurplusService typeSurplusService;

    private final OrderPayServiceImpl orderPayService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final CommodityTypeService commodityTypeService;

    private final UserPropertyDetailsService userPropertyDetailsService;

    private final UserPropertyService userPropertyService;

    private final List<String> oneDayPropertyCode = List.of("textExtractionNum", "shortVideoNum", "searchInfluencerNum", "child_searchInfluencerNum", "searchHotVideoNum", "child_searchHotVideoNum");
    private final OrderDao orderDao;
    private final OrderPayDao orderPayDao;


    @Override
    public PageUtils<OrderListVo> queryPage(OrderListBo orderListBo) {
        LambdaQueryWrapper<OrderEntity> wrapper = new LambdaQueryWrapper<OrderEntity>()
                .like(ObjectUtil.isNotEmpty(orderListBo.getTitle()), OrderEntity::getTitle, orderListBo.getTitle())
                .eq(ObjectUtil.isNotEmpty(orderListBo.getUserId()), OrderEntity::getUserId, orderListBo.getUserId())
                .like(ObjectUtil.isNotEmpty(orderListBo.getUserName()), OrderEntity::getUserName, orderListBo.getUserName())
                .eq(ObjectUtil.isNotEmpty(orderListBo.getCommodityId()), OrderEntity::getCommodityId, orderListBo.getCommodityId())
                .like(ObjectUtil.isNotEmpty(orderListBo.getId()), OrderEntity::getId, orderListBo.getId())
                .eq(ObjectUtil.isNotEmpty(orderListBo.getStatus()), OrderEntity::getStatus, orderListBo.getStatus())
                .eq(ObjectUtil.isNotEmpty(orderListBo.getOrderType()), OrderEntity::getOrderType, orderListBo.getOrderType())
                .eq(ObjectUtil.isNotEmpty(orderListBo.getSource()), OrderEntity::getSource, orderListBo.getSource())
                .eq(ObjectUtil.isNotEmpty(orderListBo.getCreateId()), OrderEntity::getCreateId, orderListBo.getCreateId())
                .eq(ObjectUtil.isNotEmpty(orderListBo.getTrialOrder()), OrderEntity::getTrialOrder, orderListBo.getTrialOrder())
                .between(ObjectUtil.isNotEmpty(orderListBo.getStartDate()) && ObjectUtil.isNotEmpty(orderListBo.getEndDate()), OrderEntity::getCreateDate, orderListBo.getStartDate(), orderListBo.getEndDate())
                .in(ObjectUtil.isNotEmpty(orderListBo.getStatusList()), OrderEntity::getStatus, orderListBo.getStatusList());

        IPage<OrderEntity> iPage = orderService.page(new Query<OrderEntity>().getPage(orderListBo.getPage(), orderListBo.getLimit()), wrapper);

        PageUtils<OrderListVo> pageUtils = new PageUtils<>(orderListBo.getPage(), orderListBo.getLimit(), iPage);

        List<OrderEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
            List<OrderListVo> vos = records.stream().map(item -> {
                OrderListVo orderVo = new OrderListVo();
                BeanUtils.copyProperties(item, orderVo);
                return orderVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public OrderInfoVo info(Long id) {

        OrderEntity orderEntity = orderService.getById(id);
        if (orderEntity != null) {
            OrderInfoVo orderInfoVo = new OrderInfoVo();
            BeanUtils.copyProperties(orderEntity, orderInfoVo);
            List<OrderDetailEntity> list = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                    .eq(OrderDetailEntity::getOrderId, id)
            );
            if (ObjectUtil.isNotEmpty(list)) {
                orderInfoVo.setOrderDetailList(BeanUtil.copyToList(list, OrderDetailInfoVo.class));
            }

            OrderPayEntity orderPay = orderPayService.getOne(new LambdaQueryWrapper<OrderPayEntity>()
                    .eq(OrderPayEntity::getOrderId, id)
                    .last("limit 1")
            );
            if (ObjectUtil.isNotEmpty(orderPay)) {
                orderInfoVo.setOrderPay(BeanUtil.copyProperties(orderPay, OrderPayInfoVo.class));
            }
            return orderInfoVo;
        }

        return null;
    }

    /**
     * 新增订单
     *
     * @param orderBo 订单对象
     * @return
     */
    public OrderInfoVo save(OrderBo orderBo) {

        OrderEntity orderEntity = new OrderEntity();
        BeanUtils.copyProperties(orderBo, orderEntity);
        if (ObjectUtil.isEmpty(orderBo.getId())) orderEntity.setId(SnowflakeManager.nextValue());
        orderEntity.setCreateDate(new Date());
        orderEntity.setUpdateDate(new Date());
        // 固化下单主账号当时的租户id：跨域查 tb_user.active_tenant_id（仅主账号下单，恒为自身租户）
        // 之后清零/reset 及按租户统计直接读 order.tenantId，不再跨域
        if (orderEntity.getTenantId() == null && orderEntity.getUserId() != null) {
            orderEntity.setTenantId(orderDao.getActiveTenantIdByUserId(orderEntity.getUserId()));
        }

        orderService.save(orderEntity);

        OrderInfoVo orderInfoVo = new OrderInfoVo();
        BeanUtils.copyProperties(orderEntity, OrderInfoVo.class);

        return orderInfoVo;
    }

    /**
     * 修改订单
     *
     * @param orderBo 订单对象
     * @return
     */
    public void update(OrderBo orderBo) {

        OrderEntity orderEntity = new OrderEntity();
        BeanUtils.copyProperties(orderBo, orderEntity);
        orderEntity.setUpdateDate(new Date());

        orderService.updateById(orderEntity);
    }

    /**
     * 删除订单
     *
     * @param id 订单id
     * @return
     */
    public void deleteById(java.lang.Long id) {

        orderService.removeById(id);
    }

    @Override
    public OrderInfoVo payOrderHandle(OrderInfoVo order) {
        Date now = new Date();
        // 修改订单状态
        order.setStatus(1);
        if (order.getSource() == 0) order.setPayDate(now);
        if (order.getOrderType() == 0 || order.getOrderType() == 2 || order.getOrderType() == 4 || order.getOrderType() == 5 || order.getOrderType() == 7) {
            // 正常订单和免费换收费
            order.setStartDate(now);
            order.setEndDate(ExpirationUtils.getExpirationEndDate(now, order.getExpiration(), order.getExpirationUnit()));
        } else if (order.getOrderType() == 1 || order.getOrderType() == 6) {
            // 套餐升级
            // 获取升级前的订单
            OrderEntity beforeOrder = orderService.getById(order.getBeforeUpgrading());
            if (order.getOrderType() == 1){
                order.setStartDate(now);
                order.setEndDate(ExpirationUtils.getExpirationEndDate(order.getStartDate(), order.getExpiration(), order.getExpirationUnit()));
            }else if (order.getOrderType() == 6){
                // 当前时间
                Date startTime = beforeOrder.getStartDate();
                Date endTime = beforeOrder.getEndDate();
                if (now.after(startTime) && now.before(endTime)){
                    // 之前的订单是在当前时间段内，说明是在使用的
                    order.setStartDate(now);
                }else{
                    // 之前的订单不在当前时间段内，说明是还未开始的
                    order.setStartDate(beforeOrder.getStartDate());
                }
                order.setEndDate(ExpirationUtils.getExpirationEndDate(order.getStartDate(), order.getExpiration(), order.getExpirationUnit()));
            }
        } else if (order.getOrderType() == 3) {
            // 续费订单
            // 续费的开始时间要在上一个订单的结束日期之后
            // 查询上一个订单的结束时间
            OrderEntity one = orderService.getOne(new LambdaQueryWrapper<OrderEntity>()
                    .eq(OrderEntity::getUserId, order.getUserId())
                    .in(OrderEntity::getStatus, 1, 2)
                    .eq(OrderEntity::getCommodityType, 1)
                    .orderByDesc(OrderEntity::getEndDate)
                    .last("limit 1")
            );

            // 判断是否一个版本等级
            if (!Objects.equals(one.getLevel(), order.getLevel())) {
                one = orderService.getOne(new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getUserId, order.getUserId())
                        .in(OrderEntity::getStatus, 7)
                        .eq(OrderEntity::getCommodityType, 1)
                        .orderByDesc(OrderEntity::getLevel, OrderEntity::getEndDate)
                        .last("limit 1")
                );
                if (one == null) {
                    RRException.create("没有找到要续费的版本");
                } else {
                    one.setEndDate(one.getEndDate().after(new Date()) ? one.getEndDate() : new Date());
                    order.setStatus(7);
                }
            }
            order.setStartDate(one.getEndDate());
            order.setEndDate(ExpirationUtils.getExpirationEndDate(order.getStartDate(), order.getExpiration(), order.getExpirationUnit()));
        }

        // 修改订单
        order.setRealEndDate(order.getEndDate());
        if (!orderService.updateById(BeanUtil.copyProperties(order, OrderEntity.class))) {
            RRException.create("订单保存失败，请重试");
        }

        // 修改订单详情
        List<OrderDetailInfoVo> orderDetailEntities = this.payUpdateDetail(order);

        OrderInfoVo orderInfoVo = BeanUtil.copyProperties(order, OrderInfoVo.class);
        orderInfoVo.setOrderDetailList(orderDetailEntities);
        return orderInfoVo;
    }

    @Override
    public OrderInfoVo payOrderHandle(OrderInfoVo order, Date start, Date end) {
        Date now = new Date();
        // 修改订单状态
        order.setStatus(1);
        if (order.getSource() == 0) order.setPayDate(now);
            // 正常订单和免费换收费
        order.setStartDate(start);
        order.setEndDate(end);

        // 修改订单
        orderService.updateById(BeanUtil.copyProperties(order, OrderEntity.class));

        // 修改订单详情
        List<OrderDetailInfoVo> orderDetailEntities = this.payUpdateDetail(order);

        OrderInfoVo orderInfoVo = BeanUtil.copyProperties(order, OrderInfoVo.class);
        orderInfoVo.setOrderDetailList(orderDetailEntities);
        return orderInfoVo;
    }

    @Override
    public List<OrderDetailInfoVo> payUpdateDetail(OrderInfoVo order) {
        if (order == null || order.getId() == null) {
            RRException.create("订单信息不能为空");
        }
        List<OrderDetailEntity> list = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                .eq(OrderDetailEntity::getOrderId, order.getId())
        );

        ArrayList<OrderDetailInfoVo> result = new ArrayList<>();
        list.forEach(item -> {
            item.setStartDate(order.getStartDate());
            item.setExpirationDate(order.getEndDate());
            item.setStatus(ObjectUtil.equal(order.getStatus(), 7) ? 7 : 0);
            item.setResetDate(item.getStartDate());
            if (order.getCommodityType() == 1 &&
                    item.getCommodityTypeReset() == 1 && ObjectUtil.isEmpty(item.getNextReset())) {
                item.setNextReset(ExpirationUtils.getExpirationEndDate(item.getStartDate(), item.getResetNum(), item.getResetUnit()));
            }
            OrderDetailInfoVo vo = BeanUtil.copyProperties(item, OrderDetailInfoVo.class);
            result.add(vo);
        });
        if (ObjectUtil.isNotEmpty(list)) {
            boolean updateFlag = orderDetailService.updateBatchById(list);
            if (!updateFlag) {
                RRException.create("订单保存失败，请重试");
            }
        }

        return result;
    }

    @Override
    public void orderStop(Long orderStopId, Long afterUpgrading, boolean isDeleteCache) {
        this.orderStop(orderStopId, afterUpgrading, isDeleteCache, 5);
    }

    /**
     * 批量订单停用
     *
     * @param orderIdMap    停用的订单id key = 本次停用的订单ID value = 升级前的订单ID
     * @param isDeleteCache 是否删除缓存
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void batchStopOrder(Map<Long, Long> orderIdMap, boolean isDeleteCache) {
        if (EmptyUtil.isEmpty(orderIdMap)) {
            return;
        }
        Date now = new Date();
        List<OrderEntity> orderList = orderIdMap.entrySet().stream().map(entry -> {
            // 订单停用
            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setId(entry.getKey());
            orderEntity.setStatus(5);
            // 设置更新时间和结束时间
            orderEntity.setUpdateDate(now);
            orderEntity.setRealEndDate(now);
            orderEntity.setAfterUpgrading(entry.getValue() != null && entry.getValue() > 0 ? entry.getValue() : null);
            return orderEntity;
        }).toList();
        orderService.updateBatchById(orderList);

        List<Long> orderDetailIds = orderDetailService.lambdaQuery().in(OrderDetailEntity::getOrderId, orderIdMap.keySet())
            .select(OrderDetailEntity::getId)
            .list().stream().map(OrderDetailEntity::getId).toList();
        if (EmptyUtil.isNotEmpty(orderDetailIds)) {
            orderDetailService.lambdaUpdate()
                .in(OrderDetailEntity::getId, orderDetailIds)
                .set(OrderDetailEntity::getStatus, 3)
                .set(OrderDetailEntity::getUpdateDate, now)
                .update();
            typeSurplusService.lambdaUpdate().in(TypeSurplusEntity::getOrderDetailId, orderDetailIds)
                .set(TypeSurplusEntity::getUseStatus, 2)
                .set(TypeSurplusEntity::getTimeStatus, 2)
                .update();
        }
    }

    @Override
    public void orderStop(Long orderStopId, Long afterUpgrading, boolean isDeleteCache, Integer newStatus) {
        if (ObjectUtil.isNotEmpty(orderStopId)) {
            Date now = new Date();
            // 订单停用
            OrderEntity orderEntity = new OrderEntity();
            // 创建实体对象并设置需要更新的字段
            // 设置主键ID
            orderEntity.setId(orderStopId);
            // 设置状态
            orderEntity.setStatus(newStatus);
            // 有条件地设置 afterUpgrading 字段
            if (ObjectUtil.isNotEmpty(afterUpgrading)) {
                orderEntity.setAfterUpgrading(afterUpgrading);
            }

            // 设置更新时间和结束时间
            orderEntity.setUpdateDate(now);
            orderEntity.setRealEndDate(now);

            // 如果状态为5，再次设置 afterUpgrading（根据你的业务逻辑）
            if (newStatus == 5) {
                orderEntity.setAfterUpgrading(afterUpgrading);
            }
            // 执行更新（会触发 MetaObjectHandler 的自动填充）
            orderService.updateById(orderEntity);

//            orderService.update(new LambdaUpdateWrapper<OrderEntity>()
//                    .set(OrderEntity::getStatus, newStatus)
//                    .set(ObjectUtil.isNotEmpty(afterUpgrading), OrderEntity::getAfterUpgrading, afterUpgrading)
//                    .set(OrderEntity::getUpdateDate, now)
//                    .set(OrderEntity::getRealEndDate, now)
//                    .set(newStatus == 5, OrderEntity::getAfterUpgrading, afterUpgrading)
//                    .eq(OrderEntity::getId, orderStopId)
//            );

            // 订单详情停用
            orderDetailService.update(new LambdaUpdateWrapper<OrderDetailEntity>()
                    .set(OrderDetailEntity::getStatus, 3)
                    .set(OrderDetailEntity::getUpdateDate, now)
                    .eq(OrderDetailEntity::getOrderId, orderStopId)
            );
            List<OrderDetailEntity> detailEntityList = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                    .eq(OrderDetailEntity::getOrderId, orderStopId)
            );
            // 订单中对应用户的资产停用
            if (ObjectUtil.isNotEmpty(detailEntityList)) {
                typeSurplusService.update(new LambdaUpdateWrapper<TypeSurplusEntity>()
                        .in(TypeSurplusEntity::getOrderDetailId, detailEntityList.stream().map(OrderDetailEntity::getId).toList())
                        .set(TypeSurplusEntity::getUseStatus, 2)
                        .set(TypeSurplusEntity::getTimeStatus, 2)
                );
            }

        }
        // 确定是否要删除缓存
        if (isDeleteCache) {
            OrderEntity order = orderService.getById(orderStopId);
            deleteUserPackageCache(order.getUserId());
        }
    }

    /**
     * 获取用户套餐
     *
     * @param userId
     * @return
     */
    @Override
    public OrderInfoVo currentOrderByUserId(Long userId) {
        OrderEntity one = orderService.getOne(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getStatus, 2)
                .eq(OrderEntity::getCommodityType, 1)
                .orderByDesc(OrderEntity::getLevel)
                .last("limit 1")
        );
        if (ObjectUtil.isEmpty(one)) return null;
        return BeanUtil.copyProperties(one, OrderInfoVo.class);
    }

    @Override
    public OrderInfoVo currentOrderDetailsByUserId(Long userId) {

        OrderInfoVo order = currentOrderByUserId(userId);

        if (ObjectUtil.isNotEmpty(order)) {
            List<OrderDetailEntity> orderDetails = orderDetailService.lambdaQuery()
                    .in(OrderDetailEntity::getOrderId, order.getId())
                    .list();
            order.setOrderDetailList(BeanUtil.copyToList(orderDetails, OrderDetailInfoVo.class));
        }

        return order;
    }

    @Override
    public List<OrderInfoVo> currentOrderByUserIds(List<Long> userIds) {
        if (ObjectUtil.isEmpty(userIds)) {
            return new ArrayList<>();
        }
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .in(OrderEntity::getUserId, userIds)
                .eq(OrderEntity::getStatus, 2)
                .eq(OrderEntity::getCommodityType, 1)
                .orderByDesc(OrderEntity::getLevel)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, OrderInfoVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public void setUserPackageCache(OrderInfoVo order) {
        if (order.getCommodityId() != null) {
//            redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.userPackageLevelCacheKey, order.getUserId()), order.getLevel());
//            redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.userPackageOrderCacheKey, order.getUserId()), order);
        }
    }

    @Override
    public void deleteUserPackageCache(Long userId) {
//        redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.userPackageLevelCacheKey, userId));
//        redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.userPackageOrderCacheKey, userId));
    }

    @Override
    public List<OrderInfoVo> getNotExpirationOrders(List<Long> userIds) {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getStatus, 2)
                .eq(OrderEntity::getCommodityType, 1)
                .in(ObjectUtil.isNotEmpty(userIds), OrderEntity::getUserId, userIds)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, OrderInfoVo.class);
        }
        return List.of();
    }

    @Override
    public List<OrderInfoVo> getOrderByUserId(Long userId) {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .in(OrderEntity::getStatus, 1, 2)
                .eq(OrderEntity::getCommodityType, 1)
                .orderByAsc(OrderEntity::getStartDate)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, OrderInfoVo.class);
        }
        return List.of();
    }


    /**
     * 批量获取用户订单
     *
     * @param userIds 用户ID
     *
     * @return {@link Map }<{@link Long }, {@link List }<{@link OrderInfoVo }>>
     */
    @Override
    public Map<Long, List<OrderInfoVo>> listUserOrder(List<Long> userIds) {
        if (EmptyUtil.isEmpty(userIds)) {
            return Map.of();
        }
        List<OrderEntity> list = orderService.lambdaQuery().in(OrderEntity::getUserId, userIds)
            .in(OrderEntity::getStatus, 1, 2)
            .eq(OrderEntity::getCommodityType, 1).list().stream().sorted(Comparator.comparing(OrderEntity::getStartDate)).toList();
        if (EmptyUtil.isEmpty(list)) {
            return Map.of();
        }
        return BeanUtil.copyToList(list, OrderInfoVo.class).stream().collect(Collectors.groupingBy(OrderInfoVo::getUserId));
    }

    @Override
    public Map<Long, List<OrderInfoVo>> listAllOrdersByUserIds(List<Long> userIds) {
        if (EmptyUtil.isEmpty(userIds)) {
            return Map.of();
        }
        List<OrderEntity> list = orderService.lambdaQuery()
                .in(OrderEntity::getUserId, userIds)
                .eq(OrderEntity::getIsDeleted, 0)
                .orderByAsc(OrderEntity::getStartDate)
                .list();
        if (EmptyUtil.isEmpty(list)) {
            return Map.of();
        }
        return BeanUtil.copyToList(list, OrderInfoVo.class).stream()
                .collect(Collectors.groupingBy(OrderInfoVo::getUserId));
    }

    @Override
    public void freezeOrderByUserId(Long userId, List<Long> orderIds) {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .in(ObjectUtil.isNotEmpty(orderIds), OrderEntity::getId, orderIds)
                .in(OrderEntity::getStatus, 1, 2)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            // 清除redis缓存
            list.forEach(item -> {
                if (item.getCommodityType() == 1) {
                    deleteUserPackageCache(item.getUserId());
                }
            });
            // 冻结订单
            List<Long> orderListIds = list.stream().map(OrderEntity::getId).toList();
//            orderService.update(new LambdaUpdateWrapper<OrderEntity>()
//                    .set(OrderEntity::getStatus, 7)
//                    .in(OrderEntity::getId, orderListIds)
//            );
            // 构建需要更新的订单实体列表
            List<OrderEntity> orderList = list.stream()
                    .map(order -> {
                        OrderEntity updatedOrder = new OrderEntity();
                        updatedOrder.setId(order.getId());       // 设置主键ID
                        updatedOrder.setStatus(7);             // 设置状态为7（冻结）
                        return updatedOrder;
                    })
                    .collect(Collectors.toList());

            // 执行批量更新（会触发 MetaObjectHandler 的自动填充）
            orderService.updateBatchById(orderList);

            // 冻结订单详情
            orderDetailService.update(new LambdaUpdateWrapper<OrderDetailEntity>()
                    .in(OrderDetailEntity::getOrderId, orderListIds)
                    .set(OrderDetailEntity::getStatus, 4)
            );

            // 冻结订单中对应用户的资产
            List<OrderDetailEntity> orderDetails = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                    .in(OrderDetailEntity::getOrderId, orderListIds)
            );
            if (ObjectUtil.isNotEmpty(orderDetails)) {
                List<Long> orderDetailsIds = orderDetails.stream().map(OrderDetailEntity::getId).distinct().toList();
                typeSurplusService.update(new LambdaUpdateWrapper<TypeSurplusEntity>()
                        .in(TypeSurplusEntity::getOrderDetailId, orderDetailsIds)
                        .setSql("use_status = if(use_status = {0}, {1}, use_status)", OrderEnums.UseStatus.USING.getCode(), OrderEnums.UseStatus.FREEZE.getCode())
                        .setSql("time_status = if(time_status = {0}, {1}, time_status)", OrderEnums.TimeStatus.EFFECTIVE.getCode(), OrderEnums.TimeStatus.FREEZE.getCode())
                );
            }

        }
    }

    @Override
    public void unfreezeOrderByUserId(Long userId) {
        this.unfreezeOrderByUserId(userId, true);
    }

    @Override
    public void unfreezeOrderByUserId(Long userId, boolean isThawActivated) {
        unfreezeOrderByUserId(userId, isThawActivated, null);
    }

    @Override
    public void unfreezeOrderByUserId(Long userId, boolean isThawActivated, List<Long> notOrderList) {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getStatus, 7)
                .ge(!isThawActivated, OrderEntity::getLevel, 0)
                .notIn(ObjectUtil.isNotEmpty(notOrderList), OrderEntity::getId, notOrderList)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            DateTime now = new DateTime();

            // 生效的订单
            List<OrderEntity> startOrder = list.stream().filter(item -> {
                DateTime startTime = new DateTime(item.getStartDate());
                DateTime endTime = new DateTime(item.getEndDate());
                return now.isAfter(startTime) && now.isBefore(endTime);
            }).toList();

            if (ObjectUtil.isNotEmpty(startOrder)) {
                List<Long> notPackageOrderIds = startOrder.stream().filter(item -> item.getCommodityType() != 1).map(OrderEntity::getId).toList();
                List<Long> packageOrderIds = startOrder.stream().filter(item -> item.getCommodityType() == 1)
                        .sorted((a, b) -> ObjectUtil.defaultIfNull(b.getLevel(), 0) - ObjectUtil.defaultIfNull(a.getLevel(), 0))
                        .map(OrderEntity::getId).toList();
                // 订单符合当前时间的可能有多个版本需要处理，因此只解冻等级最大的一个订单
                // 冻结订单
                List<Long> yesIds = new ArrayList<>(notPackageOrderIds);
                if (ObjectUtil.isNotEmpty(packageOrderIds)) {
                    yesIds.add(packageOrderIds.get(0));
                }
//                orderService.update(new LambdaUpdateWrapper<OrderEntity>()
//                        .set(OrderEntity::getStatus, 2)
//                        .in(OrderEntity::getId, yesIds)
//                );
                // 构建需要更新的订单实体列表
                List<OrderEntity> orderList = yesIds.stream()
                        .map(id -> {
                            OrderEntity order = new OrderEntity();
                            order.setId(id);          // 设置主键ID
                            order.setStatus(2);       // 设置状态为2
                            return order;
                        })
                        .collect(Collectors.toList());
                // 执行批量更新（会触发 MetaObjectHandler 的自动填充）
                orderService.updateBatchById(orderList);

                orderDetailService.update(new LambdaUpdateWrapper<OrderDetailEntity>()
                        .in(OrderDetailEntity::getOrderId, yesIds)
                        .set(OrderDetailEntity::getStatus, 1)
                );
                // 冻结订单中对应用户的资产
                List<OrderDetailEntity> orderDetails = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                        .in(OrderDetailEntity::getOrderId, yesIds)
                );
                if (ObjectUtil.isNotEmpty(orderDetails)) {
                    List<Long> orderDetailsIds = orderDetails.stream().map(OrderDetailEntity::getId).distinct().toList();
                    // 把冻结的资产进行解冻，只要是结束时间大于当前的时间，就解冻
                    typeSurplusService.update(new LambdaUpdateWrapper<TypeSurplusEntity>()
                            .in(TypeSurplusEntity::getOrderDetailId, orderDetailsIds)
//                            .le(TypeSurplusEntity::getStartTime, now)
                            .ge(TypeSurplusEntity::getEndTime, now)
                            .setSql("use_status = if(use_status = {0}, {1}, use_status)", OrderEnums.UseStatus.FREEZE.getCode(), OrderEnums.UseStatus.USING.getCode())
                            .setSql("time_status = if(time_status = {0}, {1}, time_status)", OrderEnums.TimeStatus.FREEZE.getCode(), OrderEnums.TimeStatus.EFFECTIVE.getCode())
                    );
                    // 把已经过期的资产进行过期
                    typeSurplusService.update(new  LambdaUpdateWrapper<TypeSurplusEntity>()
                            .in(TypeSurplusEntity::getOrderDetailId, orderDetailsIds)
                            .le(TypeSurplusEntity::getEndTime, now)
                            .setSql("use_status = if(use_status in ({0}, {1}), {2}, use_status)",
                                    OrderEnums.UseStatus.USING.getCode(),
                                    OrderEnums.UseStatus.FREEZE.getCode(),
                                    OrderEnums.UseStatus.EXPIRED.getCode())
                            .setSql("time_status = if(time_status in ({0}, {1}), {2}, time_status)",
                                    OrderEnums.TimeStatus.EFFECTIVE.getCode(),
                                    OrderEnums.TimeStatus.FREEZE.getCode(),
                                    OrderEnums.TimeStatus.EXPIRED.getCode())
                    );
                }

            }

            // 未开始的订单
            List<OrderEntity> noStartOrder = list.stream().filter(item -> now.isBefore(new DateTime(item.getStartDate()))).toList();
            if (ObjectUtil.isNotEmpty(noStartOrder)) {
                // 更新订单状态
//                orderService.update(new LambdaUpdateWrapper<OrderEntity>()
//                        .set(OrderEntity::getStatus, 1)
//                        .in(OrderEntity::getId, noStartOrder.stream().map(OrderEntity::getId).toList())
//                );
                // 构建需要更新的订单实体列表
                List<OrderEntity> orderList = noStartOrder.stream()
                        .map(order -> {
                            OrderEntity updatedOrder = new OrderEntity();
                            updatedOrder.setId(order.getId());      // 设置主键ID
                            updatedOrder.setStatus(1);            // 设置状态为1
                            return updatedOrder;
                        })
                        .collect(Collectors.toList());

                // 执行批量更新（会触发 MetaObjectHandler 的自动填充）
                orderService.updateBatchById(orderList);



                // 更新订单详情
                orderDetailService.update(new LambdaUpdateWrapper<OrderDetailEntity>()
                        .in(OrderDetailEntity::getOrderId, noStartOrder.stream().map(OrderEntity::getId).toList())
                        .set(OrderDetailEntity::getStatus, 0)
                );
            }

            // 查询当前用户是否用版本-为了添加redis缓存
            OrderEntity one = orderService.getOne(new LambdaQueryWrapper<OrderEntity>()
                    .eq(OrderEntity::getStatus, 2)
                    .eq(OrderEntity::getCommodityType, 1)
                    .eq(OrderEntity::getUserId, userId)
                    .last("limit 1")
            );
            // 如果有版本就添加缓存
            if (ObjectUtil.isNotEmpty(one)) {
                setUserPackageCache(BeanUtil.copyProperties(one, OrderInfoVo.class));

                List<OrderDetailInfoVo> monthExpiredOrders = this.getMonthExpiredOrders(one.getId());
                if (ObjectUtil.isNotEmpty(monthExpiredOrders)) {
                    this.setExpiration(monthExpiredOrders);
                    this.setNextExpirationTime(monthExpiredOrders);
                }
            }
        }
    }

    /**
     * 批量解冻用户的订单
     *
     * @param userIds         用户id集合
     * @param isThawActivated 是否解冻激活
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void batchUserUnfreezeOrders(Collection<Long> userIds, boolean isThawActivated) {
        if (EmptyUtil.isEmpty(userIds)) {
            return;
        }
        // 定义状态码：1-生效订单，2-未生效订单，0-忽略订单
        int takeEffectCode = 1;
        int notTakeEffectCode = 2;
        int ignoreCode = 0;
        LocalDateTime now = LocalDateTime.now();
        // 查询用户的所有冻结状态订单(status=7)，并按时间状态分组
        Map<Integer, Map<Long, List<OrderEntity>>> userOrderMap = orderService.lambdaQuery().in(OrderEntity::getUserId, userIds)
            .eq(OrderEntity::getStatus, 7)
            .ge(!isThawActivated, OrderEntity::getLevel, 0).list()
            .stream().collect(Collectors.groupingBy(item -> {
                LocalDateTime startDate = DateOps.of(item.getStartDate().getTime()).getLocalDateTime();
                LocalDateTime endDate = DateOps.of(item.getEndDate().getTime()).getLocalDateTime();
                // 生效的订单
                if (now.isAfter(startDate) && now.isBefore(endDate)) {
                    return takeEffectCode;
                }
                // 未开始的订单
                if (now.isBefore(startDate)) {
                    return notTakeEffectCode;
                }
                return ignoreCode;
            }, Collectors.groupingBy(OrderEntity::getUserId)));
        if (EmptyUtil.isEmpty(userOrderMap)) {
            return;
        }

        // 定义处理生效订单的消费者函数
        Consumer<List<Long>> freezeConsumer = yesIds -> {
            if (EmptyUtil.isEmpty(yesIds)) {
                return;
            }
            // 构建需要更新的订单实体列表
            List<OrderEntity> orderList = yesIds.stream()
                .map(id -> {
                    OrderEntity order = new OrderEntity();
                    order.setId(id);          // 设置主键ID
                    order.setStatus(2);       // 设置状态为2（生效中）
                    return order;
                })
                .collect(Collectors.toList());
            // 执行批量更新（会触发 MetaObjectHandler 的自动填充）
            orderService.updateBatchById(orderList);

            // 更新订单详情状态为生效中
            orderDetailService.update(new LambdaUpdateWrapper<OrderDetailEntity>()
                .in(OrderDetailEntity::getOrderId, yesIds)
                .set(OrderDetailEntity::getStatus, 1)
            );
            // 冻结订单中对应用户的资产
            List<OrderDetailEntity> orderDetails = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                .in(OrderDetailEntity::getOrderId, yesIds)
            );
            if (EmptyUtil.isEmpty(orderDetails)) {
                return;
            }
            List<Long> orderDetailsIds = orderDetails.stream().map(OrderDetailEntity::getId).distinct().toList();
            // 把冻结的资产进行解冻，将冻结状态改为使用中状态
            typeSurplusService.update(new LambdaUpdateWrapper<TypeSurplusEntity>()
                .in(TypeSurplusEntity::getOrderDetailId, orderDetailsIds)
                .le(TypeSurplusEntity::getStartTime, now)
                .ge(TypeSurplusEntity::getEndTime, now)
                .setSql("use_status = if(use_status = {0}, {1}, use_status)", OrderEnums.UseStatus.FREEZE.getCode(), OrderEnums.UseStatus.USING.getCode())
                .setSql("time_status = if(time_status = {0}, {1}, time_status)", OrderEnums.TimeStatus.FREEZE.getCode(), OrderEnums.TimeStatus.EFFECTIVE.getCode())
            );
            // 把已经过期的资产进行过期处理
            typeSurplusService.update(new LambdaUpdateWrapper<TypeSurplusEntity>()
                .in(TypeSurplusEntity::getOrderDetailId, orderDetailsIds)
                .le(TypeSurplusEntity::getEndTime, now)
                .setSql("use_status = if(use_status in ({0}, {1}), {2}, use_status)",
                    OrderEnums.UseStatus.USING.getCode(),
                    OrderEnums.UseStatus.FREEZE.getCode(),
                    OrderEnums.UseStatus.EXPIRED.getCode())
                .setSql("time_status = if(time_status in ({0}, {1}), {2}, time_status)",
                    OrderEnums.TimeStatus.EFFECTIVE.getCode(),
                    OrderEnums.TimeStatus.FREEZE.getCode(),
                    OrderEnums.TimeStatus.EXPIRED.getCode())
            );
        };


        // 处理生效中的订单
        Map<Long, List<OrderEntity>> takeEffectMap = userOrderMap.get(takeEffectCode);
        if (EmptyUtil.isNotEmpty(takeEffectMap)) {
            // 获取非套餐类型的订单ID
            List<Long> notPackageOrderIds = takeEffectMap.values().stream().flatMap(startOrder -> startOrder.stream().filter(item -> item.getCommodityType() != 1).map(OrderEntity::getId)).toList();
            // 订单符合当前时间的可能有多个版本需要处理，因此只解冻每个用户等级最大的一个订单
            List<Long> packageOrderIds = takeEffectMap.entrySet().stream().flatMap(entry -> {
                return entry.getValue().stream().filter(item -> item.getCommodityType() == 1)
                    .sorted((a, b) -> ObjectUtil.defaultIfNull(b.getLevel(), 0) - ObjectUtil.defaultIfNull(a.getLevel(), 0))
                    .map(OrderEntity::getId).findFirst().stream();
            }).toList();

            // 需要冻结的订单ID
            List<Long> yesIds = Stream.of(notPackageOrderIds, packageOrderIds).flatMap(Collection::stream).toList();
            freezeConsumer.accept(yesIds);
        }

        // 处理未开始的订单
        Map<Long, List<OrderEntity>> noStartOrderMap = userOrderMap.get(notTakeEffectCode);
        // 未开始的订单
        if (EmptyUtil.isNotEmpty(noStartOrderMap)) {
            // 构建需要更新的订单实体列表
            List<OrderEntity> orderList = noStartOrderMap.values().stream()
                .flatMap(noStartOrder -> {
                    return noStartOrder.stream().map(order -> {
                        OrderEntity updatedOrder = new OrderEntity();
                        updatedOrder.setId(order.getId());      // 设置主键ID
                        updatedOrder.setStatus(1);            // 设置状态为1（未开始）
                        return updatedOrder;
                    });
                }).toList();
            // 执行批量更新（会触发 MetaObjectHandler 的自动填充）
            orderService.updateBatchById(orderList);

            // 更新订单详情状态为未开始
            orderDetailService.update(new LambdaUpdateWrapper<OrderDetailEntity>()
                .in(OrderDetailEntity::getOrderId, orderList.stream().map(OrderEntity::getId).toList())
                .set(OrderDetailEntity::getStatus, 0)
            );
        }

        // 查询当前用户可用的版本订单-为了添加redis缓存
        Map<Long, Long> userAvailableOrderMap = orderService.lambdaQuery().in(OrderEntity::getUserId, userIds)
            .select(OrderEntity::getUserId, OrderEntity::getId)
            .eq(OrderEntity::getStatus, 2)
            .eq(OrderEntity::getCommodityType, 1).list()
            .stream().collect(Collectors.toMap(OrderEntity::getUserId, OrderEntity::getId, FunctionUtil::mergeFirst));
        if (EmptyUtil.isEmpty(userAvailableOrderMap)) {
            return;
        }
        // 获取月度过期订单详情
        List<OrderDetailInfoVo> monthExpiredOrders = orderDetailService.lambdaQuery().in(OrderDetailEntity::getOrderId, userAvailableOrderMap.values())
            .eq(OrderDetailEntity::getStatus, 1)
            .ne(OrderDetailEntity::getIsDeleted, 1)
            .le(OrderDetailEntity::getNextReset, new Date()).list().stream()
            .map(detail -> BeanUtil.copyProperties(detail, OrderDetailInfoVo.class)).toList();
        if (EmptyUtil.isNotEmpty(monthExpiredOrders)) {
            this.setExpiration(monthExpiredOrders);
            this.setNextExpirationTime(monthExpiredOrders);
        }
    }


    @Override
    public List<OrderDetailInfoVo> getMonthExpiredOrders(Long orderId) {
        List<OrderDetailEntity> list = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                .eq(ObjectUtil.isNotEmpty(orderId), OrderDetailEntity::getOrderId, orderId)
                .in(OrderDetailEntity::getStatus, 1)
                .ne(OrderDetailEntity::getIsDeleted, 1)
                .le(OrderDetailEntity::getNextReset, new Date())
        );
        return BeanUtil.copyToList(list, OrderDetailInfoVo.class);
    }

    @Override
    public void setExpiration(List<OrderDetailInfoVo> monthExpiredOrders) {
// 获取订单详情对应的商品类型资产剩余表
        List<TypeSurplusEntity> typeSurplusEntities = typeSurplusService.list(new LambdaQueryWrapper<TypeSurplusEntity>()
                .in(TypeSurplusEntity::getOrderDetailId, monthExpiredOrders.stream().map(OrderDetailInfoVo::getId).toList())
//                .in(TypeSurplusEntity::getUseStatus, 0, 4) // 查询状态为 0：在用，4：冻结
                .in(TypeSurplusEntity::getTimeStatus, 0, 3) // 查询状态为 0：在用，3：冻结
        );
        if (ObjectUtil.isNotEmpty(typeSurplusEntities)) {
            // 查询对应的订单-获取订单对应的map
            List<OrderEntity> orderList = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                    .in(OrderEntity::getId, monthExpiredOrders.stream().map(OrderDetailInfoVo::getOrderId).toList())
            );
            Map<Long, OrderEntity> orderMaps = orderList.stream()
                    .collect(Collectors.toMap(OrderEntity::getId, orderEntity -> orderEntity, (v1, v2) -> v1));

            // 获取订单详情对应的map
            Map<Long, OrderDetailInfoVo> detailMaps = monthExpiredOrders.stream()
                    .collect(Collectors.toMap(OrderDetailInfoVo::getId, orderDetailInfoVo -> orderDetailInfoVo, (v1, v2) -> v1));

            Date now = new Date();
            List<UserPropertyDetailsEntity> detailsList = new ArrayList<>();
            for (TypeSurplusEntity typeSurplus : typeSurplusEntities) {
                OrderDetailInfoVo orderDetail = detailMaps.get(typeSurplus.getOrderDetailId());
                if (ObjectUtil.isEmpty(orderDetail)) RRException.create("获取订单详情出错-" + typeSurplus.getOrderDetailId());
                OrderEntity order = orderMaps.get(orderDetail.getOrderId());
                if (ObjectUtil.isEmpty(order)) RRException.create("获取订单出错-" + orderDetail.getOrderId());
                if (ObjectUtil.equals(typeSurplus.getUseStatus(), OrderEnums.UseStatus.USING.getCode())) {
                    typeSurplus.setUseStatus(OrderEnums.UseStatus.EXPIRED.getCode());
                }
                typeSurplus.setTimeStatus(OrderEnums.TimeStatus.EXPIRED.getCode());
                // 如果已经使用完，则跳过
                if (typeSurplus.getUseNumber() >= typeSurplus.getTotalNumber()) continue;
                // 设置资产流水记录
                UserPropertyDetailsEntity e = new UserPropertyDetailsEntity();
                e.setId(SnowflakeManager.nextValue());
                e.setUserId(order.getUserId());
                e.setUserName(order.getUserName());
                // 租户id直接取订单固化值（资产归属租户，不随人换租户跑）
                e.setTenantId(order.getTenantId());
                // 在月底清零中是没有子账号的，因为子账号使用的资源是父账号的，所以这里不设置
                e.setParentUserId(0L);
                e.setOrderDetailId(orderDetail.getId());
                e.setPropertyId(typeSurplus.getPropertyId());
                e.setTypeSurplusId(typeSurplus.getId());
                e.setCommodityTypeId(typeSurplus.getCommodityTypeId());
                e.setCommodityTypeCode(typeSurplus.getCommodityTypeCode());
                // 如果是一天就重置的资产，到资产清零就不存库了
                if (oneDayPropertyCode.contains(typeSurplus.getCommodityTypeCode())) {
                    continue;
                }
                // 有缓存
                CommodityTypeEntity commodityType = commodityTypeService.getById(typeSurplus.getCommodityTypeId());
                e.setCommodityTypeName(commodityType.getName());
                e.setCommodityTypeUnit(commodityType.getUnit());
                // 减
                e.setSigns(0);
                e.setQuantity(typeSurplus.getTotalNumber() - typeSurplus.getUseNumber());
                e.setRemarks(StrUtil.format("到资产清零时间，[{}]清零", commodityType.getName()));
                e.setCreateDate(now);
                e.setAssetCreationType(AiEnums.assetCreationType.SYSTEM_CREATION.getCode());
                detailsList.add(e);
            }

            if (ObjectUtil.isNotEmpty(detailsList)) {
                userPropertyDetailsService.saveBatch(detailsList);
            }

            typeSurplusService.updateBatchById(typeSurplusEntities);
        }
    }

    @Override
    public List<UserPropertyEntity> setNextExpirationTime(List<OrderDetailInfoVo> monthExpiredOrders) {
// 查询对应的订单-获取订单对应的map
        List<OrderEntity> orderList = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .in(OrderEntity::getId, monthExpiredOrders.stream().map(OrderDetailInfoVo::getOrderId).toList())
        );
        Map<Long, OrderEntity> orderMaps = orderList.stream()
                .collect(Collectors.toMap(OrderEntity::getId, orderEntity -> orderEntity, (v1, v2) -> v1));

        // 获取对应用户的资产对应的map
        List<UserPropertyEntity> userPropertyDetails = userPropertyService.list(new LambdaQueryWrapper<UserPropertyEntity>()
                .in(UserPropertyEntity::getUserId, orderList.stream().map(OrderEntity::getUserId).toList())
                .eq(UserPropertyEntity::getType, 0)
                .eq(UserPropertyEntity::getIsUse, 1)
        );
        // 获取主账号对应所以子账号的资产list
        Map<Long, List<UserPropertyEntity>> childUserPropertyMap = new HashMap<>();
        if (EmptyUtil.isNotEmpty(userPropertyDetails)) {
            List<Long> ids = userPropertyDetails.stream().map(UserPropertyEntity::getId).toList();
            childUserPropertyMap = new BatchQuery<>((limit, idx) -> userPropertyService.lambdaQuery()
                    .in(UserPropertyEntity::getParentId, ids)
                    .le(idx != null, UserPropertyEntity::getId, idx)
                    .eq(UserPropertyEntity::getIsUse, 1)
                    .last("limit " + limit)
                    .orderByDesc(UserPropertyEntity::getId)
                    .list(), UserPropertyEntity::getId)
                    .batch(1000)
                    .get()
                    .stream()
                    .collect(Collectors.groupingBy(UserPropertyEntity::getParentUserId));
            ;
        }
        Map<Long, UserPropertyEntity> userPropertyMap = userPropertyDetails.stream()
                .collect(Collectors.toMap(UserPropertyEntity::getUserId, orderEntity -> orderEntity, (v1, v2) -> v1));


        List<OrderDetailInfoVo> list = monthExpiredOrders.stream()
                .filter(detail -> detail.getStatus() == 1 && detail.getCommodityTypeReset() == 1)
                .toList();

        Map<String, CommodityTypeEntity> commodityTypeMap = commodityTypeService.listAllByCache().stream()
                .collect(Collectors.toMap(CommodityTypeEntity::getCode, Function.identity(), (v1, v2) -> v1));

        if (ObjectUtil.isNotEmpty(list)) {
            Date now = new Date();
            ArrayList<TypeSurplusEntity> entityList = new ArrayList<>();
            List<UserPropertyDetailsEntity> detailsList = new ArrayList<>();
            for (OrderDetailInfoVo detail : list) {
                // 判断是否要重置和有下次重置时间
                if (!ObjectUtil.equals(detail.getCommodityTypeReset(), 1) || ObjectUtil.isEmpty(detail.getNextReset()) || ObjectUtil.compare(detail.getResetNum(), 0) < 0) {
                    continue;
                }
                // 获取下一次清零时间
                Date expirationEndDate = getNextReset(detail, detail.getNextReset());
                detail.setResetDate(detail.getNextReset());
                detail.setNextReset(expirationEndDate);
                detail.setUpdateDate(now);
                CommodityTypeEntity commodityType = commodityTypeMap.get(detail.getCommodityTypeCode());
                if (ObjectUtil.isEmpty(commodityType)) continue;
                OrderEntity order = orderMaps.get(detail.getOrderId());
                if (ObjectUtil.isEmpty(order)) continue;
                UserPropertyEntity userProperty = userPropertyMap.get(order.getUserId());
                if (ObjectUtil.isEmpty(userProperty)) continue;

                if (!commodityType.getCode().startsWith("child_")) {
                    // 主账号： 添加商品类型资产剩余表和添加资产流水记录
                    setValue(detail, commodityType, userProperty, order, entityList, detailsList);
                } else if (commodityType.getCode().startsWith("child_") && ObjectUtil.equals(commodityType.getSubAccountHave(), 1)) {
                    // 子账号： 添加商品类型资产剩余表和添加资产流水记录
                    List<UserPropertyEntity> childUserList = childUserPropertyMap.get(userProperty.getUserId());
                    if (ObjectUtil.isNotEmpty(childUserList)) {
                        for (UserPropertyEntity item : childUserList) {
                            // 获取父资产类型
                            CommodityTypeEntity temp = commodityTypeMap.get(detail.getCommodityTypeCode().substring("child_".length()));
                            if (ObjectUtil.isEmpty(temp)) {
                                continue;
                            }
                            // 子账号： 添加商品类型资产剩余表和添加资产流水记录
                            setValue(detail, temp, item, order, entityList, detailsList);
                        }
                    }
                }

//                // 添加商品类型资产剩余表
//                TypeSurplusEntity typeSurplus = new TypeSurplusEntity();
//                typeSurplus.setId(SnowflakeManager.nextValue());
//                typeSurplus.setCommodityTypeId(detail.getCommodityTypeId());
//                typeSurplus.setCommodityTypeCode(detail.getCommodityTypeCode());
//                typeSurplus.setPropertyId(userProperty.getId());
//                typeSurplus.setUserId(order.getUserId());
//                typeSurplus.setOrderDetailId(detail.getId());
//                typeSurplus.setUseNumber(0L);
//                typeSurplus.setTotalNumber(detail.getTotalNumber());
//                typeSurplus.setStartTime(detail.getResetDate());
//                typeSurplus.setEndTime(detail.getNextReset());
//                typeSurplus.setUseStatus(0);
//                typeSurplus.setTimeStatus(0);
//                entityList.add(typeSurplus);
//
//                // 添加资产流水记录
//                UserPropertyDetailsEntity e = new UserPropertyDetailsEntity();
//                e.setId(SnowflakeManager.nextValue());
//                e.setUserId(order.getUserId());
//                e.setUserName(order.getUserName());
//                // 在月底清零中是没有子账号的，因为子账号使用的资源是父账号的，所以这里不设置
//                e.setParentUserId(0L);
//                e.setOrderDetailId(detail.getId());
//                e.setPropertyId(userProperty.getId());
//                e.setTypeSurplusId(typeSurplus.getId());
//                e.setCommodityTypeId(detail.getCommodityTypeId());
//                e.setCommodityTypeCode(detail.getCommodityTypeCode());
//                e.setCommodityTypeName(detail.getCommodityTypeName());
//                e.setCommodityTypeUnit(detail.getCommodityTypeUnit());
//                // 加
//                e.setSigns(1);
//                e.setQuantity(typeSurplus.getTotalNumber());
//                e.setRemarks(StrUtil.format("{}到达重置资源时间", order.getCommodityName(), commodityType.getName()));
//                e.setCreateDate(now);
//                detailsList.add(e);
            }
            // 批量保存
            if (ObjectUtil.isNotEmpty(entityList)) {
                typeSurplusService.saveBatch(entityList);
            }
            if (ObjectUtil.isNotEmpty(detailsList)) {
                userPropertyDetailsService.saveBatch(detailsList);
            }

            orderDetailService.updateBatchById(BeanUtil.copyToList(list, OrderDetailEntity.class));

        }

        return userPropertyDetails;
    }

    private void setValue(OrderDetailInfoVo detail, CommodityTypeEntity commodityType, UserPropertyEntity userProperty, OrderEntity order, ArrayList<TypeSurplusEntity> entityList, List<UserPropertyDetailsEntity> detailsList) {
        // 添加商品类型资产剩余表
        TypeSurplusEntity typeSurplus = new TypeSurplusEntity();
        typeSurplus.setId(SnowflakeManager.nextValue());
        typeSurplus.setCommodityTypeId(commodityType.getId());
        typeSurplus.setCommodityTypeCode(commodityType.getCode());
        typeSurplus.setPropertyId(userProperty.getId());
        typeSurplus.setUserId(order.getUserId());
        typeSurplus.setOrderDetailId(detail.getId());
        typeSurplus.setUseNumber(0L);
        typeSurplus.setTotalNumber(detail.getTotalNumber());
        typeSurplus.setStartTime(detail.getResetDate());
        typeSurplus.setEndTime(detail.getNextReset());
        typeSurplus.setUseStatus(0);
        typeSurplus.setTimeStatus(0);
        entityList.add(typeSurplus);

        // 添加资产流水记录-只有不是一天就重置的资源才添加流水记录
        if (!oneDayPropertyCode.contains(commodityType.getCode())) {
            UserPropertyDetailsEntity e = new UserPropertyDetailsEntity();
            e.setId(SnowflakeManager.nextValue());
            e.setUserId(order.getUserId());
            e.setUserName(order.getUserName());
            // 租户id直接取订单固化值（资产归属租户，不随人换租户跑）
            e.setTenantId(order.getTenantId());
            // 在月底清零中是没有子账号的，因为子账号使用的资源是父账号的，所以这里不设置
            e.setParentUserId(0L);
            e.setOrderDetailId(detail.getId());
            e.setPropertyId(userProperty.getId());
            e.setTypeSurplusId(typeSurplus.getId());
            e.setCommodityTypeId(commodityType.getId());
            e.setCommodityTypeCode(commodityType.getCode());
            e.setCommodityTypeName(commodityType.getName());
            e.setCommodityTypeUnit(commodityType.getUnit());
            // 加
            e.setSigns(1);
            e.setQuantity(typeSurplus.getTotalNumber());
            e.setRemarks(StrUtil.format("{}到达重置资源时间", order.getCommodityName()));
            e.setCreateDate(new Date());
            e.setAssetCreationType(AiEnums.assetCreationType.SYSTEM_CREATION.getCode());
            detailsList.add(e);
        }
    }

    private Date getNextReset(OrderDetailInfoVo detail, Date date) {
        Date next = date;
        Date now = new Date();

        for (int i = 0; i < 1000; i++) {
            detail.setNextReset(next);
            Date candidate = ExpirationUtils.getExpirationEndDate(next, detail.getResetNum(), detail.getResetUnit());

            // 防止配置错误导致时间不推进
            if (candidate.getTime() <= next.getTime()) {
                logErrorAndThrow(detail, "重置时间未推进，请检查 resetNum 或 resetUnit 配置！");
            }

            next = candidate;

            // 如果下一次重置时间在未来，结束循环
            if (!next.before(now)) {
                return next;
            }
        }

        // 超过最大循环次数仍未得到结果
        logErrorAndThrow(detail, "重置时间计算次数过多，可能出现死循环！");
        return null; // 实际不会执行到这里
    }

    private void printlnErrorLog(OrderDetailInfoVo detail) {
        log.error("重置时间未推进，检查resetNum或resetUnit配置！orderDetailsId = {}, resetNum = {}, resetUnit = {}",
                detail.getId(), detail.getResetNum(), detail.getResetUnit());
    }

    /**
     * 打印错误并抛出异常
     */
    private void logErrorAndThrow(OrderDetailInfoVo detail, String message) {
        printlnErrorLog(detail);
        throw new BusinessException(StatusCode.OPERATION_EX.getCode(), message);
    }

    @Override
    public List<Long> getUserIdByPackageId(Long packageId) {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getStatus, 2)
                .eq(OrderEntity::getCommodityType, 1)
                .eq(OrderEntity::getCommodityId, packageId)
                .select(OrderEntity::getUserId)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            return list.stream().map(OrderEntity::getUserId).collect(Collectors.toList());
        }
        return List.of();
    }


    @Override
    public void checkRenewalOrder(OrderInfoVo order) {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, order.getUserId())
                .eq(OrderEntity::getStatus, 1)
                .orderByAsc(OrderEntity::getStartDate)
        );
        if (ObjectUtil.isNotEmpty(list)) {

            // 查询订单详情
            List<OrderDetailEntity> orderDetailEntityList = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                    .in(OrderDetailEntity::getOrderId, list.stream().map(OrderEntity::getId).toList())
            );

            Date temp = order.getEndDate();
            for (OrderEntity item : list) {
                item.setStartDate(temp);
                item.setEndDate(ExpirationUtils.getExpirationEndDate(item.getStartDate(), item.getExpiration(), item.getExpirationUnit()));
                temp = item.getEndDate();
                // 修改订单的开始结束时间
//                orderService.update(new LambdaUpdateWrapper<OrderEntity>()
//                        .set(OrderEntity::getStartDate, item.getStartDate())
//                        .set(OrderEntity::getEndDate, item.getEndDate())
//                        .eq(OrderEntity::getId, item.getId())
//                );
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setId(item.getId());
                orderEntity.setStartDate(item.getStartDate());
                orderEntity.setEndDate(item.getEndDate());
                orderService.updateById(orderEntity);

                List<OrderDetailEntity> detailEntityList = orderDetailEntityList.stream()
                        .filter(detail -> detail.getOrderId().equals(item.getId()))
                        .toList();
                if (ObjectUtil.isNotEmpty(detailEntityList)) {
                    detailEntityList.forEach(detail -> {
                        detail.setStartDate(item.getStartDate());
                        detail.setExpirationDate(item.getEndDate());
                        // 修改订单详情的开始结束时间
                        orderDetailService.update(new LambdaUpdateWrapper<OrderDetailEntity>()
                                .set(OrderDetailEntity::getStartDate, detail.getStartDate())
                                .set(OrderDetailEntity::getExpirationDate, detail.getExpirationDate())
                                .eq(OrderDetailEntity::getId, detail.getId())
                        );
                    });
                }
            }
        }
    }


    @Override
    public List<OrderInfoVo> listByExpiredOrder() {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .in(OrderEntity::getStatus, 1, 2, 7)
                .le(OrderEntity::getEndDate, new Date())
        );
        if (ObjectUtil.isEmpty(list)) return List.of();
        List<OrderInfoVo> result = BeanUtil.copyToList(list, OrderInfoVo.class);
        List<Long> ids = result.stream().map(OrderVo::getId).toList();
        List<OrderDetailEntity> orderDetailsList = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                .in(OrderDetailEntity::getOrderId, ids)
        );
        if (ObjectUtil.isNotEmpty(orderDetailsList)) {
            List<OrderDetailInfoVo> orderDetailInfoVos = BeanUtil.copyToList(orderDetailsList, OrderDetailInfoVo.class);
            Map<Long, List<OrderDetailInfoVo>> listMap = orderDetailInfoVos.stream().collect(Collectors.groupingBy(OrderDetailInfoVo::getOrderId));
            DataUtils.setFieldMap(result, "id", "orderDetailList", listMap);
        }
        return result;
    }


    /**
     * 获取当前过期的订单
     *
     * @param expiredTime 过期时间 允许为空
     * @param consumer    消费者 必填
     * @param termination  终止条件 必填
     */
    @Override
    public void loadExpiredOrder(LocalDateTime expiredTime, Consumer<List<OrderInfoVo>> consumer, Supplier<Boolean> termination) {
        LocalDateTime now = expiredTime == null ? LocalDateTime.now() : expiredTime;
        BatchQuery<Long, OrderEntity> batchQuery = new BatchQuery<>(( limit, idx) -> {
            log.info("[订单主表] batch load detail idx: {}, limit: {}", idx, limit);
            return orderService.lambdaQuery().in(OrderEntity::getStatus, 1, 2, 7)
                .lt(idx != null, OrderEntity::getId, idx)
                .le(OrderEntity::getEndDate, now).orderByDesc(OrderEntity::getId).last("limit " + limit).list();
        }, OrderEntity::getId);
        // 一次查询1000条
        batchQuery.batch(1000);
        batchQuery.isTermination((count, rows) -> termination.get());
        // 单次查询结果的消费者
        batchQuery.consumer(rows -> {
            List<OrderInfoVo> result = BeanUtil.copyToList(rows, OrderInfoVo.class);
            List<Long> ids = result.stream().map(OrderVo::getId).toList();
            List<OrderDetailEntity> orderDetailsList = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                .in(OrderDetailEntity::getOrderId, ids)
            );
            if (ObjectUtil.isNotEmpty(orderDetailsList)) {
                List<OrderDetailInfoVo> orderDetailInfoVos = BeanUtil.copyToList(orderDetailsList, OrderDetailInfoVo.class);
                Map<Long, List<OrderDetailInfoVo>> listMap = orderDetailInfoVos.stream().collect(Collectors.groupingBy(OrderDetailInfoVo::getOrderId));
                DataUtils.setFieldMap(result, "id", "orderDetailList", listMap);
            }
            consumer.accept(result);
        });
        // 最大10w条
        batchQuery.run(null, 100000);
    }


    /**
     * 加载过期订单详细信息
     *
     * @param expiredTime 过期时间
     * @param consumer    消费者
     * @param termination  终止条件
     */
    @Override
    public void loadExpiredOrderDetail(LocalDateTime expiredTime, Consumer<List<OrderDetailInfoVo>> consumer, Supplier<Boolean> termination) {
        LocalDateTime now = expiredTime == null ? LocalDateTime.now() : expiredTime;
        new BatchQuery<>((limit, idx) -> {
            log.info("[订单详情] batch load detail idx: {}, limit: {}", idx, limit);
            return orderDetailService.lambdaQuery()
                .lt(idx != null, OrderDetailEntity::getId, idx)
                .eq(OrderDetailEntity::getStatus, 1)
                .le(OrderDetailEntity::getNextReset, now)
                .orderByDesc(OrderDetailEntity::getId)
                .last("limit " + limit)
                .list();
        }, OrderDetailEntity::getId).batch(1000).isTermination((count, rows) -> termination.get()).consumer(rows -> {
             consumer.accept(BeanUtil.copyToList(rows, OrderDetailInfoVo.class));
        }).run(null, 100000);

    }


    /**
     * 加载所有过期订单详细信息
     *
     * @param expiredTime 过期时间
     *
     * @return 订单详细信息
     */
    @Override
    public List<OrderDetailInfoVo> loadExpiredAllOrderDetail(LocalDateTime expiredTime) {
        LocalDateTime now = expiredTime == null ? LocalDateTime.now() : expiredTime;
        return new BatchQuery<>((limit, idx) -> {
            log.info("[订单详情] batch load detail idx: {}, limit: {}", idx, limit);
            return BeanUtil.copyToList(orderDetailService.lambdaQuery()
                .lt(idx != null, OrderDetailEntity::getId, idx)
                .eq(OrderDetailEntity::getStatus, 1)
                .le(OrderDetailEntity::getNextReset, now)
                .ne(OrderDetailEntity::getIsDeleted,1)
                .orderByDesc(OrderDetailEntity::getId)
                .last("limit " + limit)
                .list(), OrderDetailInfoVo.class);
        }, OrderDetailInfoVo::getId).batch(3000).get();
    }

    @Override
    public List<OrderInfoVo> listByNotStartOrder(Long userId) {

        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getCommodityType, 1)
                .eq(OrderEntity::getStatus, 1)
                .le(OrderEntity::getStartDate, DateUtil.now())
                .orderByAsc(OrderEntity::getStartDate)
        );
        List<OrderInfoVo> result = BeanUtil.copyToList(list, OrderInfoVo.class);
        if (ObjectUtil.isNotEmpty(result)){
            List<Long> ids = result.stream().map(OrderVo::getId).toList();
            List<OrderDetailEntity> orderDetailsList = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                    .in(OrderDetailEntity::getOrderId, ids)
            );
            if (ObjectUtil.isNotEmpty(orderDetailsList)){
                List<OrderDetailInfoVo> orderDetailInfoVos = BeanUtil.copyToList(orderDetailsList, OrderDetailInfoVo.class);
                Map<Long, List<OrderDetailInfoVo>> listMap = orderDetailInfoVos.stream().collect(Collectors.groupingBy(OrderDetailInfoVo::getOrderId));
                DataUtils.setFieldMap(result, "id", "orderDetailList", listMap);
            }
        }
        return result;
    }


    /**
     * 获取用户未开始订单
     *
     * @param userIds 用户ids
     *
     * @return 订单
     */
    @Override
    public Map<Long, OrderInfoVo> listUserNotStart(List<Long> userIds) {
        if (EmptyUtil.isEmpty(userIds)) {
            return Map.of();
        }
        List<OrderEntity> orderList = orderService.lambdaQuery().in(OrderEntity::getUserId, userIds)
            .eq(OrderEntity::getCommodityType, 1)
            .eq(OrderEntity::getStatus, 1)
            .le(OrderEntity::getStartDate, DateOps.of(LocalDateTime.now().withHour(23).withMinute(59).withSecond(59)).getDate()).list();
        if (EmptyUtil.isEmpty(orderList)) {
            return Map.of();
        }
        // 取单个用户最早未开始的
        Collection<OrderEntity> userFirstOrderList = orderList.stream().sorted(Comparator.comparing(OrderEntity::getStartDate)).collect(Collectors.toMap(OrderEntity::getUserId, Function.identity(), (v1, v2) -> v1)).values();
        List<OrderInfoVo> result = BeanUtil.copyToList(userFirstOrderList, OrderInfoVo.class);
        orderList = null;
        userFirstOrderList = null;
        if (EmptyUtil.isEmpty(result)) {
            return Map.of();
        }
        List<Long> orderIds = result.stream().map(OrderInfoVo::getId).toList();
        Map<Long, OrderInfoVo> resultMap = result.stream().collect(Collectors.toMap(OrderInfoVo::getUserId, Function.identity(), (v1, v2) -> v1));
        result = null;
        List<OrderDetailEntity> detailList = orderDetailService.lambdaQuery().in(OrderDetailEntity::getOrderId, orderIds).list();
        if (EmptyUtil.isEmpty(detailList)) {
            return resultMap;
        }
        Map<Long, List<OrderDetailInfoVo>> detailMap = BeanUtil.copyToList(detailList, OrderDetailInfoVo.class).stream().collect(Collectors.groupingBy(OrderDetailInfoVo::getOrderId));
        resultMap.values().forEach(order -> {
            order.setOrderDetailList(detailMap.get(order.getId()));
        });
        return resultMap;
    }


    /**
     * 批量更新订单状态
     *
     * @param orderIds 订单ids
     * @param status   状态
     */
    @Override
    public void batchUpdateOrderStatus(List<Long> orderIds, int status) {
        if (EmptyUtil.isEmpty(orderIds)) {
            return;
        }
        orderService.lambdaUpdate()
            .in(OrderEntity::getId, orderIds)
            .set(OrderEntity::getStatus, status)
            .ne(OrderEntity::getStatus, status)
            .update();
    }

    /**
     * 过滤已存在的订单id
     *
     * @param orderIds 订单ids
     *
     * @return 订单ids
     */
    @Override
    public List<Long> filterExistsIds(List<Long> orderIds) {
        if (EmptyUtil.isEmpty(orderIds)) {
            return List.of();
        }
        List<Long> existsIds = orderService.lambdaQuery().select(OrderEntity::getId)
            .in(OrderEntity::getId, orderIds)
            .ne(OrderEntity::getIsDeleted, 1).list().stream().map(OrderEntity::getId).toList();
        if (EmptyUtil.isEmpty(existsIds)) {
            return List.of();
        }
        return existsIds;
    }

    /**
     * 获取订单用户id
     *
     * @param orderIds 订单ids
     *
     * @return key:订单id value:用户id
     */
    @Override
    public Map<Long, Long> getOrderUserMap(List<Long> orderIds) {
        if (EmptyUtil.isEmpty(orderIds)) {
            return Map.of();
        }
        return orderService.lambdaQuery().select(OrderEntity::getId, OrderEntity::getUserId)
            .in(OrderEntity::getId, orderIds)
            .ne(OrderEntity::getIsDeleted, 1).list().stream().collect(Collectors.toMap(OrderEntity::getId, OrderEntity::getUserId));
    }



    /**
     * 获取订单是否免费
     *
     * @param orderIds 订单ids
     *
     * @return 订单是否免费 如果订单不存在则不返回这个key
     */
    @Override
    public Map<Long, Boolean> getOrderFreeMap(List<Long> orderIds) {
        if (EmptyUtil.isEmpty(orderIds)) {
            return Map.of();
        }
        return orderService.lambdaQuery().in(OrderEntity::getId, orderIds)
            .select(OrderEntity::getId, OrderEntity::getOrderType)
            .list().stream().collect(Collectors.toMap(OrderEntity::getId, order -> order.getOrderType() == 0));
    }

    /**
     * 获取订单基础信息
     *
     * @param orderIds      订单ids
     * @param selectColumns 返回字段
     *
     * @return 订单基础信息 如果订单不存在则不返回这个key
     */
    @Override
    public Map<Long, OrderVo> getOrderBaseInfoMap(List<Long> orderIds, SFunction<OrderEntity, ?>... selectColumns) {
        if (EmptyUtil.isEmpty(orderIds)) {
            return Map.of();
        }
        List<SFunction<OrderEntity, ?>> selectColumnsList = new ArrayList<>();
        selectColumnsList.add(OrderEntity::getId);
        if (selectColumns != null) {
            selectColumnsList.addAll(Arrays.asList(selectColumns));
        }
        return orderService.lambdaQuery().select(selectColumnsList)
            .in(OrderEntity::getId, orderIds)
            .ne(OrderEntity::getIsDeleted, 1)
            .list().stream()
            .collect(Collectors.toMap(OrderEntity::getId, item -> BeanUtil.copyProperties(item, OrderVo.class)));
    }

    @Override
    public List<OrderInfoVo> listExistByUserId(Long userId) {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
        );
        if (ObjectUtil.isNotEmpty(list)) return BeanUtil.copyToList(list, OrderInfoVo.class);
        return List.of();
    }

    @Override
    public OrderInfoVo getById(Long id) {
        OrderEntity byId = orderService.getById(id);
        if (byId != null) return BeanUtil.copyProperties(byId, OrderInfoVo.class);
        return null;
    }

    @Override
    public List<OrderInfoVo> getOldVersionOrderList(Long userId, Long newOrderId) {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getCommodityType, 1)
                .in(OrderEntity::getStatus, 1, 2)
                .ne(OrderEntity::getId, newOrderId)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, OrderInfoVo.class);
        }
        return List.of();
    }

    @Override
    public OrderInfoVo currentUserStayOrder(Long userId) {
        OrderEntity orderEntity = orderService.getOne(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getStatus, 0)
                .last("limit 1")
        );
        if (orderEntity != null) {
            OrderInfoVo orderInfoVo = new OrderInfoVo();
            BeanUtils.copyProperties(orderEntity, orderInfoVo);
            List<OrderDetailEntity> list = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                    .eq(OrderDetailEntity::getOrderId, orderEntity.getId())
            );
            if (ObjectUtil.isNotEmpty(list)) {
                orderInfoVo.setOrderDetailList(BeanUtil.copyToList(list, OrderDetailInfoVo.class));
            }

            OrderPayEntity orderPay = orderPayService.getOne(new LambdaQueryWrapper<OrderPayEntity>()
                    .eq(OrderPayEntity::getOrderId, orderEntity.getId())
                    .last("limit 1")
            );
            if (ObjectUtil.isNotEmpty(orderPay)) {
                OrderPayInfoVo orderPay1 = BeanUtil.copyProperties(orderPay, OrderPayInfoVo.class);
                orderPay1.setUrlCode(orderPay1.getPayCode());
                orderInfoVo.setOrderPay(orderPay1);
            }
            orderInfoVo.setPlanEndDate(ExpirationUtils.getExpirationEndDate(new Date(), orderInfoVo.getExpiration(), orderInfoVo.getExpirationUnit()));
            return orderInfoVo;
        }
        return null;
    }

    @Override
    public OrderInfoVo currentUserOrderDetails(Long userId) {
        OrderInfoVo orderInfoVo = null;
        OrderEntity orderEntity = orderService.getOne(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getStatus, 2)
                .eq(OrderEntity::getCommodityType, 1)
                .last("limit 1")
        );
        if (orderEntity != null) {
            orderInfoVo = BeanUtil.copyProperties(orderEntity, OrderInfoVo.class);

            List<OrderDetailEntity> list = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                    .eq(OrderDetailEntity::getOrderId, orderEntity.getId())
            );

            orderInfoVo.setOrderDetailList(BeanUtil.copyToList(list, OrderDetailInfoVo.class));

        }
        return orderInfoVo;
    }

    @Override
    public void closeOrder(Long orderId, Boolean isClosePay) {
//        orderService.update(new LambdaUpdateWrapper<OrderEntity>()
//                .eq(OrderEntity::getId, orderId)
//                .set(OrderEntity::getStatus, 6)
//        );

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(orderId);
        orderEntity.setStatus(6);
        orderService.updateById(orderEntity);

        if (isClosePay){
            orderPayService.update(new LambdaUpdateWrapper<OrderPayEntity>()
                    .eq(OrderPayEntity::getOrderId, orderId)
                    .set(OrderPayEntity::getPayStatus, 4)
            );
        }
    }

    /**
     * 根据付费到期时间查询,也可返回用户对应的付费到期时间
     * @param expireTime 付费到期时间
     * @return
     */
    @Override
    public OrderTimeVo getUserIdByExpireTime(Long expireTime) {
        OrderTimeVo orderTimeVo = new OrderTimeVo();
        List<OrderEntity> levelOrder = orderService.list(new LambdaQueryWrapper<OrderEntity>().gt(OrderEntity::getLevel,0).ge(OrderEntity::getStatus,1).le(OrderEntity::getStatus,2));
        // 根据userID查询当前用户有多少个订单
        Map<Long,List<OrderEntity>> orderEntityMap = levelOrder.stream().collect(Collectors.groupingBy(OrderEntity::getUserId));
        // 去重,只留一个用户匹配Map集合
        List<OrderEntity> onlyOneUserIdS = levelOrder.stream().collect(Collectors.collectingAndThen(Collectors.toMap(
                OrderEntity::getUserId,
                data -> data,
                (a,b) -> b),
                map -> new ArrayList<>(map.values())
        ));

        List<Long> userIds = new ArrayList<>();
        Map<Long,Long> userOrderTimeMap = new HashMap<>();
        for (OrderEntity orderEntity : onlyOneUserIdS) {
            // 根据相同的userId取出当前用户的所有有效订单
            List<OrderEntity> userOrders = orderEntityMap.getOrDefault(orderEntity.getUserId(), Collections.emptyList());
            if (userOrders != null && userOrders.size() > 0){
                Date currentDate = new Date();
                // 给所有有效订单升序排序,确保能拿到第一条订单生效时间
                List<OrderEntity> sortOrder = userOrders.stream().sorted(Comparator.comparing(OrderEntity::getCreateDate)).collect(Collectors.toList());
                // 第一条订单开始时间
                long finalStartTime = sortOrder.get(0).getStartDate().getTime();
                // 最后一条订单结束时间减去第一条订单开始时间 = 所有订单的有效时间
                long finalLastTime = sortOrder.get(sortOrder.size() - 1).getEndDate().getTime() - finalStartTime;
                // 算出当前用户已使用了多久版本时间(当前时间减去第一条订单开始时间)
                long usedDays = TimeUnit.MILLISECONDS.toDays(currentDate.getTime() - finalStartTime);

                // 所有订单加起来的时间
                long diffDays = 1;
                if (finalLastTime > 0){
                    diffDays = TimeUnit.MILLISECONDS.toDays(finalLastTime);
                }
                // 不满一天则算一天
                if (finalLastTime % TimeUnit.DAYS.toMillis(1) > 0){
                    diffDays++;
                }
                diffDays -=usedDays;
                    if (diffDays < expireTime){
                        userIds.add(orderEntity.getUserId());
                    }
                userOrderTimeMap.put(orderEntity.getUserId(),diffDays);
            }
        }
        orderTimeVo.setUserIdsByExpireTime(userIds);
        orderTimeVo.setUserOrderTime(userOrderTimeMap);

        return orderTimeVo;
    }

    @Override
    public List<OrderDetailInfoVo> synchronousPackage(PackageInfoVo info) {

        RRException.isNotEmpty(info, "套餐信息不能为空");
        RRException.isNotEmpty(info.getTypeConsumptionList(), "版本资产不能为空");


        List<OrderEntity> list1 = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getCommodityId, info.getId())
                .eq(OrderEntity::getCommodityType, 1)
                .in(OrderEntity::getStatus, 1, 2, 7)
                .in(OrderEntity::getOrderType, 0, 1, 2, 3)
        );

        List<OrderDetailEntity> detailsList1 = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                .in(OrderDetailEntity::getOrderId, list1.stream().map(OrderEntity::getId).toList())
        );

        // 获取订单详情
        List<OrderInfoVo> orderList = BeanUtil.copyToList(list1, OrderInfoVo.class);
        List<OrderDetailInfoVo> detailsList = BeanUtil.copyToList(detailsList1, OrderDetailInfoVo.class);
        DataUtils.setFieldMap(orderList, "id", "orderDetailList", detailsList.stream().collect(Collectors.groupingBy(OrderDetailInfoVo::getOrderId)));

        Date now = new Date();

        Map<Long, CommodityTypeEntity> commodityTypeMap = commodityTypeService.listAllByCache().stream().collect(Collectors.toMap(CommodityTypeEntity::getId, Function.identity()));

        List<OrderDetailInfoVo> saveOrUpdateList = new ArrayList<>();
        // 分析订单中的资产情况
        for (OrderInfoVo order : orderList) {
            // 获取订单中的资产情况
            List<OrderDetailInfoVo> orderDetailList = order.getOrderDetailList();
            List<TypeConsumptionVo> typeConsumptionList = info.getTypeConsumptionList();


            for (TypeConsumptionVo typeConsumptionVo : typeConsumptionList) {
                OrderDetailInfoVo orderDetail = orderDetailList.stream()
                        .filter(item -> item.getCommodityTypeId().equals(typeConsumptionVo.getCommodityTypeId()))
                        .findAny()
                        .orElse(null);
                if (ObjectUtil.isNotEmpty(orderDetail)){
                    // TODO 现在暂时不更新资产，后续再做
                    if (true) continue;
                    // 判断订单中的资产和版本资产是否一致，不一致则更新
                    if (!ObjectUtil.equals(orderDetail.getTotalNumber(), typeConsumptionVo.getNumber())){
                        orderDetail.setTotalNumber(typeConsumptionVo.getNumber());
                        orderDetail.setUserId(order.getUserId());
                        saveOrUpdateList.add(orderDetail);
                    }
                }else{
                    OrderDetailInfoVo orderDetailBo = new OrderDetailInfoVo();
                    // 设置订单详情ID为Snowflake算法生成的下一个值
                    orderDetailBo.setId(SnowflakeManager.nextValue());
                    // 设置资产ID为-1，表示未关tb_type_surplus表
                    if (order.getStatus() != 1) orderDetailBo.setPropertyId(-1L);
                    // 设置订单ID为刚刚创建的订单ID
                    orderDetailBo.setOrderId(order.getId());
                    // 设置商品ID为当前消费类型的ID
                    orderDetailBo.setCommodityId(typeConsumptionVo.getCommodityTypeId());
                    // 设置商品名称为当前消费类型的名称
                    orderDetailBo.setCommodityName(typeConsumptionVo.getCommodityTypeName());
                    // 设置商品类型ID为当前消费类型的ID
                    orderDetailBo.setCommodityTypeId(typeConsumptionVo.getCommodityTypeId());
                    // 设置商品类型代码为当前消费类型的代码
                    orderDetailBo.setCommodityTypeCode(typeConsumptionVo.getCommodityTypeCode());
                    // 设置商品类型名称为当前消费类型的名称
                    orderDetailBo.setCommodityTypeName(typeConsumptionVo.getCommodityTypeName());
                    // 设置商品类型单位为当前消费类型的单位
                    orderDetailBo.setCommodityTypeUnit(typeConsumptionVo.getCommodityTypeUnit());
                    // 设置商品类型是否重置为当前消费类型的重置状态
                    orderDetailBo.setCommodityTypeReset(typeConsumptionVo.getCommodityTypeReset());
                    // 设置总数量为当前消费类型的数量
                    orderDetailBo.setTotalNumber(typeConsumptionVo.getNumber());
                    // 设置开始使用时间为空
                    orderDetailBo.setStartDate(order.getStartDate());
                    CommodityTypeEntity commodityType = commodityTypeMap.get(typeConsumptionVo.getCommodityTypeId());
                    RRException.isNotEmpty(commodityType, "商品类型不存在");
                    // 设置重置数量为重置数量
                    orderDetailBo.setResetNum(commodityType.getResetNum());
                    // 设置重置单位为重置单位
                    orderDetailBo.setResetUnit(commodityType.getResetUnit());
                    // 设置下次重置日期为空
                    orderDetailBo.setResetDate(getStartTime(now, order.getStartDate(), orderDetailBo.getResetNum(), orderDetailBo.getResetUnit()));
                    // 设置下次重置日期为空
                    orderDetailBo.setNextReset(typeConsumptionVo.getCommodityTypeReset() == 1 ? ExpirationUtils.getExpirationEndDate(orderDetailBo.getResetDate(), orderDetailBo.getResetNum(), orderDetailBo.getResetUnit()) : null);
                    // 设置到期日期为空
                    orderDetailBo.setExpirationDate(order.getEndDate());
                    // 设置状态为0未开始
                    if (order.getStatus() == 1){
                        orderDetailBo.setStatus(0);
                    }else if (order.getStatus() == 2){
                        orderDetailBo.setStatus(1);
                    }else if (order.getStatus() == 7){
                        orderDetailBo.setStatus(4);
                    }
                    // 设置创建日期为当前日期
                    orderDetailBo.setCreateDate(now);
                    // 设置更新日期为当前日期
                    orderDetailBo.setUpdateDate(now);
                    // 设置是否删除为0，表示未删除
                    orderDetailBo.setIsDeleted(0);
                    // 将当前订单详情添加到订单详情列表中
                    orderDetailBo.setUserId(order.getUserId());
                    saveOrUpdateList.add(orderDetailBo);
                }


            }

        }

        // 更新或者添加订单详情
        if (ObjectUtil.isNotEmpty(saveOrUpdateList)){
            // 批量保存或更新订单详情到数据库中
            List<OrderDetailEntity> orderDetailEntities = BeanUtil.copyToList(saveOrUpdateList, OrderDetailEntity.class);
            orderDetailService.saveBatch(orderDetailEntities);
            return saveOrUpdateList;
        }
        return new ArrayList<>();
    }

    @Override
    public List<OrderInfoVo> getYearOrder() {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getCommodityType, 1)
                .eq(OrderEntity::getStatus, 2)
                .eq(OrderEntity::getExpiration, 1)
                .eq(OrderEntity::getExpirationUnit, 5)
                .gt(OrderEntity::getLevel, 0)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, OrderInfoVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public List<OrderInfoVo> getYearOrder2(List<Long> ids) {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getCommodityType, 0)
                .eq(OrderEntity::getStatus, 2)
                .in(OrderEntity::getCommodityId, ids)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, OrderInfoVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public List<OrderInfoVo> listAllByUserId(Long userId) {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .orderByAsc(OrderEntity::getStartDate)
        );

        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, OrderInfoVo.class);
        }

        return new ArrayList<>();
    }

    private Date getStartTime(Date now, Date date, int expiration, int expirationUnit){
        Date current = date;
        if (expiration <= 0) {
            return current;
        }
        while (true){
            Date expirationEndDate = ExpirationUtils.getExpirationEndDate(current, expiration, expirationUnit);
            if (expirationEndDate.getTime() > now.getTime()){
                return current;
            }else{
                current = expirationEndDate;
            }
        }
    }


    @Override
    public List<Long> getOrdersUserIdNoPre(Long userId) {
        List<OrderEntity> orderEntityList = orderService.lambdaQuery()
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getStatus, 2)
                .list();
        if (!orderEntityList.isEmpty()){
            return orderEntityList.stream().map(OrderEntity::getId).toList();
        }
        return new ArrayList<Long>();
    }


    /**
     * 根据是否是付费订单，查询状态为生效中的版本订单列表
     * @param orderBo
     * @return
     */
    @Override
    public List<OrderListVo> selectByQuery(OrderBo orderBo) {
        List<OrderEntity> orderEntityList = orderService.lambdaQuery()
                .select(OrderEntity::getId, OrderEntity::getUserId, OrderEntity::getTrialOrder)
                .eq(OrderEntity::getTrialOrder, orderBo.getTrialOrder())
                .eq(OrderEntity::getStatus,orderBo.getStatus())
                .eq(OrderEntity::getCommodityType,orderBo.getCommodityType())
                .list();
        return BeanUtil.copyToList(orderEntityList, OrderListVo.class);
    }

    @Override
    public List<OrderInfoVo> listFrozenOrderByUserIdAndLevel(Long userId, Integer level) {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getStatus, 7)
                .eq(OrderEntity::getLevel, level)
                .orderByAsc(OrderEntity::getStartDate)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, OrderInfoVo.class);
        }
        return List.of();
    }

    @Override
    public List<OrderInfoVo> listFrozenOrderByUserIdAndMinLevel(Long userId, Integer minLevel) {
        List<OrderEntity> list = orderService.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getStatus, 7)
                .ge(OrderEntity::getLevel, minLevel)
                .gt(OrderEntity::getLevel, 0)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, OrderInfoVo.class);
        }
        return List.of();
    }


    /**
     * 通过用户id 查询订单列表
     * 过滤条件  用户ids  生效中  版本
     * @param longList
     * @return
     */
    @Override
    public List<OrderListVo> selectByUserIds(List<Long> longList) {
        if (!ObjectUtil.isEmpty(longList)){
            List<OrderEntity> orderEntityList = orderService.lambdaQuery()
                    .in(OrderEntity::getUserId, longList)
                    //生效中
                    .eq(OrderEntity::getStatus,2)
                    //版本
                    .eq(OrderEntity::getCommodityType,1)
                    //查询字段 id、userId、trialOrder
                    .select(OrderEntity::getId, OrderEntity::getUserId, OrderEntity::getTrialOrder).list();
            return BeanUtil.copyToList(orderEntityList, OrderListVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public Map<Long, Date> userOrderExpireTimeByUserIds(List<Long> userIds) {
        List<OrderEntity> list = orderService.list(new QueryWrapper<OrderEntity>()
                .select("max(end_date) as end_date", "user_id")
                .lambda()
                .in(OrderEntity::getUserId, userIds)
                .in(OrderEntity::getStatus, 1, 2)
                .eq(OrderEntity::getCommodityType, 1)
                .orderByAsc(OrderEntity::getEndDate)
                .groupBy(OrderEntity::getUserId)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            return list.stream().collect(Collectors.toMap(OrderEntity::getUserId, OrderEntity::getEndDate, (v1, v2) -> v1));
        }
        return new HashMap<>();
    }

    @Override
    public List<OrderFinallyVo> currentOrderFinallyByUserIds(List<Long> userIds) {
        if (ObjectUtil.isEmpty(userIds)) {
            return new ArrayList<>();
        }
        List<OrderEntity> list = orderService.list(new QueryWrapper<OrderEntity>()
                .select("user_id", "max(user_name) as user_name", "max(commodity_id) as commodity_id", "max(commodity_name) as commodity_name",
                        "max(level) as level", "min(start_date) as start_date", "max(end_date) as end_date", "MIN(trial_order) AS trial_order"
                )
                .lambda()
                .in(OrderEntity::getUserId, userIds)
                .eq(OrderEntity::getStatus, 2)
                .eq(OrderEntity::getCommodityType, 1)
                .orderByDesc(OrderEntity::getLevel)
                .groupBy(OrderEntity::getUserId)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, OrderFinallyVo.class);
        }
        return new ArrayList<>();
    }

    /**
     * 获取增量包的子账号数量
     *
     * @param userId            用户id
     * @param commodityTypeCode 类型
     * @return 子账号数量
     */
    @Override
    public int getIncrementalSubNum(Long userId, String commodityTypeCode) {
        return orderDao.getIncrementalSubNum(userId, commodityTypeCode);
    }

    /**
     * 是否付费用户
     *
     * @param orderUserId 订单用户id
     *
     * @return 是否有付费用户
     */
    @Override
    public boolean hasPaidUser(long orderUserId) {
        Integer hasPaidUser = orderDao.hasPaidUser(orderUserId);
        return hasPaidUser != null && hasPaidUser > 0;
    }

    @Override
    public List<com.jiuyu.replay.order.vo.datahub.DataHubOrderItemVo> listDataHubOrders(
            java.util.Collection<Long> tenantIds, Date updatedAfter,
            Date cursorUpdateDate, Long cursorId, int limit) {
        List<OrderEntity> list = orderService.lambdaQuery()
                .eq(OrderEntity::getIsDeleted, 0)
                .in(EmptyUtil.isNotEmpty(tenantIds), OrderEntity::getTenantId, tenantIds)
                .ge(updatedAfter != null, OrderEntity::getUpdateDate, updatedAfter)
                .and(cursorUpdateDate != null && cursorId != null, w -> w
                        .gt(OrderEntity::getUpdateDate, cursorUpdateDate)
                        .or(o -> o.eq(OrderEntity::getUpdateDate, cursorUpdateDate)
                                .gt(OrderEntity::getId, cursorId)))
                .orderByAsc(OrderEntity::getUpdateDate)
                .orderByAsc(OrderEntity::getId)
                .last("limit " + limit)
                .list();
        if (EmptyUtil.isEmpty(list)) {
            return List.of();
        }
        return BeanUtil.copyToList(list, com.jiuyu.replay.order.vo.datahub.DataHubOrderItemVo.class);
    }
}
