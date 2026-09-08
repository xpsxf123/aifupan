package com.jiuyu.replay.order.bll;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.util.FunctionUtil;
import com.jiuyu.replay.common.constant.CommonProperties;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.constant.TencentCosProperties;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.dto.activity.InviteUserRewardDetailDto;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderListVo;
import com.jiuyu.replay.generic.vo.order.OrderVo;
import com.jiuyu.replay.order.bean.CreateOrder;
import com.jiuyu.replay.order.bean.factory.OrderFactoryUtils;
import com.jiuyu.replay.order.bo.*;
import com.jiuyu.replay.order.constant.OrderProperties;
import com.jiuyu.replay.order.entity.OrderEntity;
import com.jiuyu.replay.order.producer.*;
import com.jiuyu.replay.order.service.CrmOrderEventService;
import com.jiuyu.replay.order.vo.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * 订单
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Component
@AllArgsConstructor
@Slf4j
public class OrderBll {

    private final OrderProducer orderProducer;

    private final OrderDetailProducer orderDetailProducer;

    private final OrderPayProducer orderPayProducer;

    private final CommodityProducer commodityProducer;

    private final IncrementProducer incrementProducer;

    private final CommodityTypeProducer commodityTypeProducer;

    private final CommodityPriceProducer commodityPriceProducer;

    private final RedisTemplate<String, Object> redisTemplate;

    private final OrderProperties orderProperties;

    private final TypeSurplusProducer typeSurplusProducer;

    private final UserPropertyProducer userPropertyProducer;

    private final PackageProducer packageProducer;

    private final CommonProperties commonProperties;

    private final TencentCosProperties tencentCosProperties;

    private final UserFeign userFeign;

    private final OrderExtendProducer orderExtendProducer;

    private final SystemKvProducer systemKvProducer;

    private final CrmOrderEventService crmOrderEventService;


    /**
     * 订单列表
     *
     * @param orderListBo 订单列表查询参数
     * @return
     */
    public R<PageUtils<OrderListVo>> queryPage(OrderListBo orderListBo) {

        return R.ok("获取成功", orderProducer.queryPage(orderListBo));
    }

    /**
     * 订单信息
     *
     * @param id 订单id
     * @return
     */
    public R<OrderInfoVo> info(Long id) {
        OrderInfoVo orderInfoVo = orderProducer.info(id);
        return R.ok("获取成功", orderInfoVo);
    }

    /**
     * 获取订单信息-单表
     * @param id
     * @return
     */
    public R<OrderInfoVo> getById(Long id){
        return R.ok("获取成功", orderProducer.getById(id));
    }

    /**
     * 新增订单
     *
     * @param orderBo 订单对象
     * @return
     */
    public R<String> save(OrderBo orderBo) {

        OrderInfoVo orderInfoVo = orderProducer.save(orderBo);
        return R.ok("添加成功");
    }

    /**
     * 修改订单
     *
     * @param orderBo 订单对象
     * @return
     */
    public R<String> update(OrderBo orderBo) {

        orderProducer.update(orderBo);
        return R.ok("修改成功");
    }

    /**
     * 删除订单
     *
     * @param id 订单id
     * @return
     */
    public R<String> delete(java.lang.Long id) {

        orderProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 添加活动订单
     * @param dtoList
     */
    @Transactional(rollbackFor = Exception.class)
    public List<InviteUserRewardDetailDto> addActivityOrder(List<InviteUserRewardDetailDto> dtoList){

        if (ObjectUtil.isNotEmpty(dtoList)){
            dtoList.forEach(item  -> {
                UserDto userDto = userFeign.userById(item.getUserId());
                if (ObjectUtil.isEmpty(userDto)) return;
                if (item.getRewardType() == 0){

                    CreateOrderBo createOrderBo = new CreateOrderBo();
                    createOrderBo.setCommodityId(item.getPackageId());
                    createOrderBo.setCommodityPriceId(item.getPackagePriceId());
                    createOrderBo.setCommodityType(1);

                    OrderInfoVo orderInfoVo = ResultUtil.getResult(this.currentOrderByUserId(item.getUserId()));
//                    if (ObjectUtil.isEmpty(orderInfoVo)) return;
                    RRException.isNotEmpty(orderInfoVo, "用户没有订单，不能升级");
                    Integer level = orderInfoVo.getLevel();
//                    if (level == null) return;
                    if (level == null) RRException.create("没有检查到当前用户存在套餐，不能升级");
                    PackageInfoVo info = packageProducer.info(createOrderBo.getCommodityId());
//                    if (info == null) return;
                    if (info == null) RRException.create("商品不存在");
                    if (info.getLevel() <= 0){
//                        if (level >= info.getLevel()) return;
                        if (level >= info.getLevel()) {
                            RRException.create("版本升级不能向下升级");
                        }
                        createOrderBo.setOrderType(0);
                    }else {
                        if (level > 0) {
                            createOrderBo.setOrderType(1);
                        } else {
                            createOrderBo.setOrderType(2);
                        }
                    }
                    CommodityPriceInfoVo priceInfoVo = commodityPriceProducer.info(item.getPackagePriceId());
//                    if (ObjectUtil.isEmpty(priceInfoVo)) return;
                    RRException.isNotEmpty(priceInfoVo, "商品价格不存在");
                    createOrderBo.setDiscountRate(priceInfoVo.getRealPrice());
                    createOrderBo.setSource(3);
                    createOrderBo.setTitle(item.getActivityName());
                    createOrderBo.setUserId(item.getUserId());
                    createOrderBo.setUserName(userDto.getNickName());
                    createOrderBo.setDiscountRate(0);
                    createOrderBo.setSource(3);
                    createOrderBo.setBeforeUpgrading(null);
                    createOrderBo.setTrialOrder(1);
                    // 下单
                    CreateOrderVo order = this.createOrder(createOrderBo);
                    // 手动的单，直接订单完成接口
                    if (ObjectUtil.isNotEmpty(order)) {
                        this.successOrder(order.getOrderId(), orderInfoVo.getId().equals(createOrderBo.getBeforeUpgrading()));
                    }
                    item.setOrderId(order.getOrderId());
                }else if (item.getRewardType() == 1){

                    CreateOrderBo createOrderBo = new CreateOrderBo();
                    createOrderBo.setCommodityId(item.getId());
                    createOrderBo.setTitle(item.getActivityName());
                    ActivityCommodityBo activityCommodityBo = new ActivityCommodityBo();
                    activityCommodityBo.setId(item.getCommodityTypeId());
                    activityCommodityBo.setCommodityNumber(item.getCommodityNumber());
                    activityCommodityBo.setValidityNum(item.getValidityNum());
                    activityCommodityBo.setValidityUnit(item.getValidityUnit());
                    createOrderBo.setActivityCommodityBo(activityCommodityBo);
                    createOrderBo.setUserId(item.getUserId());
                    createOrderBo.setUserName(userDto.getNickName());
                    createOrderBo.setOrderType(7);
                    createOrderBo.setCommodityType(3);
                    createOrderBo.setDiscountRate(0);
                    createOrderBo.setSource(3);
                    createOrderBo.setBeforeUpgrading(null);
                    createOrderBo.setTrialOrder(1);
                    // 下单
                    CreateOrderVo order = this.createOrder(createOrderBo);
                    // 手动的单，直接订单完成接口
                    if (ObjectUtil.isNotEmpty(order)) {
                        this.successOrder(order.getOrderId(), false);
                    }
                    item.setOrderId(order.getOrderId());
                }
            });

        }
        return dtoList;
    }

    /**
     * 完成订单
     *
     * @param orderId
     * @param isUse
     */
    @Transactional(rollbackFor = Exception.class)
    public void successOrder(Long orderId, boolean isUse) {
        if (orderId == null) RRException.create("订单id为空");
        OrderInfoVo order = orderProducer.info(orderId);
        // 停用之前的订单或者是冻结之前的订单
        // 订单类型 0免费订单，1升级订单，2免费版换收费版，6编辑订单
        if (order.getOrderType() == 0 || order.getOrderType() == 1 || order.getOrderType() == 2 || order.getOrderType() == 6) {
            // 停用之前的套餐
            if (order.getCommodityType() == 1) {
                // 获取之前的订单版本
                List<OrderInfoVo> oldOrderList = orderProducer.getOldVersionOrderList(order.getUserId(), order.getId());
                if (CollectionUtil.isNotEmpty(oldOrderList)){
                    // 如果是升级订单，就把之前那的关掉，如果是免费版升级到收费版就把免费的订单冻结
                    if (order.getOrderType() == 1 || order.getOrderType() == 6) {
                        for (OrderInfoVo orderItem : oldOrderList) {
                            // 判断升级订单还是手动编辑
                            orderProducer.orderStop(orderItem.getId(), order.getId(), isUse, order.getOrderType() == 6 ? 8 : 5);
                        }
                    } else {
                        if (order.getOrderType() == 2) {
                            // 处理纯录制版本的订单
                            // 从 KV 配置获取纯录制版对应的 level 值
                            Integer pureRecordingLevel = systemKvProducer.getValueByKey("pure_recording_version_level", (Integer) null);
                            if (pureRecordingLevel != null) {
                                // 有纯录制版的订单在冻结，并且当前是免费版升级，就把纯录制停掉
                                List<OrderInfoVo> orderInfoVos = orderProducer.listFrozenOrderByUserIdAndLevel(order.getUserId(), pureRecordingLevel);
                                if (ObjectUtil.isNotEmpty(orderInfoVos)) {
                                    for (OrderInfoVo orderItem : orderInfoVos) {
                                        // 判断升级订单还是手动编辑
                                        orderProducer.orderStop(orderItem.getId(), order.getId(), isUse, order.getOrderType() == 6 ? 8 : 5);
                                    }
                                }
                            }
                        }
                        orderProducer.freezeOrderByUserId(order.getUserId(), oldOrderList.stream().map(OrderVo::getId).toList());
                    }
                }
            }
        }
        // 如果是升级订单，检查存在续费的订单吗？如果有就修改开始时间和结束时间-不用了，改为把之前的订单全部关闭掉
//        if (order.getOrderType() == 1 || order.getOrderType() == 6){
//            orderProducer.checkRenewalOrder(order);
//        }
        // 订单状态修改完成
        OrderInfoVo orderInfoVo = orderProducer.payOrderHandle(order);
        CreateOrder bean = OrderFactoryUtils.getCreateOrder(order.getCommodityType());
        // 是否使用订单
        if (bean.isUseByOrder(order)) {
            useOrder(orderInfoVo);
        }
        publishCrmOrderChangedEvent(orderInfoVo);
    }

    public void successOrder2(Long orderId, Date start, Date end) {
        if (orderId == null) RRException.create("订单id为空");
        OrderInfoVo order = orderProducer.info(orderId);
        // 订单状态修改完成
        OrderInfoVo orderInfoVo = orderProducer.payOrderHandle(order, start, end);
        CreateOrder bean = OrderFactoryUtils.getCreateOrder(order.getCommodityType());
        // 是否使用订单
        if (bean.isUseByOrder(order)) {
            useOrder(orderInfoVo);
        }
    }

    private void publishCrmOrderChangedEvent(OrderInfoVo orderInfoVo) {
        if (orderInfoVo == null || orderInfoVo.getId() == null || orderInfoVo.getUserId() == null) {
            return;
        }
        CrmOrderChangedBo eventBo = new CrmOrderChangedBo();
        eventBo.setEventType(resolveOrderEventType(orderInfoVo));
        eventBo.setOccurredAt(new Date());
        eventBo.setUserId(orderInfoVo.getUserId());
        eventBo.setOrderId(orderInfoVo.getId());
        crmOrderEventService.publishOrderChangedEvent(eventBo);
    }

    private String resolveOrderEventType(OrderInfoVo orderInfoVo) {
        if (ObjectUtil.equal(orderInfoVo.getOrderType(), 1)) {
            return "UPGRADE";
        }
        if (ObjectUtil.equal(orderInfoVo.getOrderType(), 3)) {
            return "RENEWAL";
        }
        if (ObjectUtil.equal(orderInfoVo.getOrderType(), 4)
                || ObjectUtil.equal(orderInfoVo.getCommodityType(), 0)) {
            return "INCREMENT";
        }
        return "PAY_SUCCESS";
    }

    /**
     * 检查续费订单，重新计算续费订单的开始时间和结束时间
     * @param order
     */
    public void checkRenewalOrder(OrderInfoVo order){
        orderProducer.checkRenewalOrder(order);
    }

    public void useOrder(OrderInfoVo orderInfoVo) {
        // 更新订单状态
        OrderBo orderBo = new OrderBo();
        orderBo.setId(orderInfoVo.getId());
        orderBo.setStatus(2);
        orderProducer.update(orderBo);
        // 更新订单详情状态
        orderDetailProducer.updateStatus(orderInfoVo.getId(), 1);

        // 获取主用户资产对象
        UserPropertyInfoVo userPropertyInfoVo = userPropertyProducer.getOne(orderInfoVo.getUserId());
        if (ObjectUtil.isNull(userPropertyInfoVo)) RRException.create("用户资产不存在");

        // 用户下单后为用户添加资产--只用于添加自己的资产，不添加子用户的
        typeSurplusProducer.payAddTypeSurplus(orderInfoVo, userPropertyInfoVo);

        // 用户下单后为子用户添加资产--只添加子用户的
        typeSurplusProducer.payAddChildTypeSurplus(orderInfoVo);

        // 统计资产
        // 统计主账号的总资产
        userPropertyProducer.statisticsProperty(userPropertyInfoVo);

        // 统计子账号的总资产
        userPropertyProducer.statisticsChildProperty(userPropertyInfoVo);

        // 是套餐就更新用户资产缓存
        if (orderInfoVo.getCommodityType() == 1) {
            orderProducer.setUserPackageCache(orderInfoVo);
        }
    }


    /**
     * 批量处理订单使用逻辑
     *
     * @param userOrderMap 用户ID与订单信息的映射关系，Key为用户ID，Value为订单信息
     */
    private void batchUseOrder(Map<Long, OrderInfoVo> userOrderMap) {
        // 检查输入参数是否为空
        if (EmptyUtil.isEmpty(userOrderMap)) {
            return;
        }

        // 根据用户ID列表获取用户资产信息
        List<UserPropertyInfoVo> userPropertyList = userPropertyProducer.getUserPropertyByUserIds(new ArrayList<>(userOrderMap.keySet()));
        if (EmptyUtil.isEmpty(userPropertyList)) {
            log.error("[订单处理] batch use order query property empty... userIds:{}", userOrderMap.keySet());
            return;
            //throw new RRException("用户资产不存在");
        }

        // 提取所有订单ID，用于批量更新订单状态
        List<Long> orderIds = userOrderMap.values().stream().map(OrderInfoVo::getId).toList();

        // 批量更新订单状态为已支付(状态码2)
        orderProducer.batchUpdateOrderStatus(orderIds, 2);

        // 批量更新订单详情状态为生效中(状态码1)
        orderDetailProducer.batchUpdateStatusInOrderId(orderIds, 1);

        // 构建订单与用户资产的映射关系
        Map<OrderInfoVo, UserPropertyInfoVo> userPropertyMap = userPropertyList.stream()
            .collect(Collectors.toMap(
                property -> userOrderMap.get(property.getUserId()),
                Function.identity(),
                FunctionUtil::mergeFirst));

        // 为用户批量添加资产（仅为主用户自身资产，不包括子用户资产）
        typeSurplusProducer.batchPayAddTypeSurplus(userPropertyMap);

        // 补救漏建的主账号资产（检查应建但未建的明细并补建）
//        typeSurplusProducer.remedyMissedTypeSurplus(userPropertyMap);

        // 为子用户批量添加资产（仅限子用户资产）
        typeSurplusProducer.batchPayAddChildTypeSurplus(userOrderMap.values());

        // 统计资产信息

        // 批量统计主账号的总资产
        userPropertyProducer.batchStatisticsProperty(userPropertyList);

        // 批量统计子账号的总资产
        userPropertyProducer.batchStatisticsChildProperty(userPropertyList);
    }


    /**
     * 设置用户套餐缓存
     *
     * @param orderInfoVo
     */
    public void setUserPackageCache(OrderInfoVo orderInfoVo) {
        orderProducer.setUserPackageCache(orderInfoVo);
    }

    /**
     * 设置用户套餐缓存
     *
     * @param orders
     */
    public void setUserPackageCache(List<OrderInfoVo> orders) {
        if (CollectionUtil.isNotEmpty(orders)) {
            orders.forEach(orderProducer::setUserPackageCache);
        }
    }

    public R<List<OrderInfoVo>> getNotExpirationOrders(List<Long> userIds) {
        List<OrderInfoVo> result = orderProducer.getNotExpirationOrders(userIds);
        return R.ok(result);
    }

    /**
     * 获取用户订单
     *
     * @param userId
     * @return
     */
    public R<List<OrderInfoVo>> getOrderByUserId(Long userId) {
        List<OrderInfoVo> result = orderProducer.getOrderByUserId(userId);
        return R.ok(result);
    }

    public CreateOrderVo createOrder(CreateOrderBo createOrderBo) {

        CreateOrder bean = OrderFactoryUtils.getCreateOrder(createOrderBo.getCommodityType());

        CreateOrderVo result = new CreateOrderVo();
        // 1.创建订单
        bean.checkOrder(createOrderBo);
        OrderBo orderBo = bean.saveOrder(createOrderBo);
        createOrderBo.setTrialOrder(orderBo.getTrialOrder());
        // 1.1 保存订单
        orderProducer.save(orderBo);
        // 1.2 保存订单详情
        orderDetailProducer.saveBatch(orderBo.getOrderDetailList());
        // 1.3 添加支付图片
        OrderExtendBo orderExtendBo = new OrderExtendBo();
        orderExtendBo.setOrderId(orderBo.getId());
        orderExtendBo.setPayPictures(createOrderBo.getPayPictures());
        orderExtendProducer.save(orderExtendBo);
        result.setOrderId(orderBo.getId());
        if (createOrderBo.getSource() == 0) {
            // 2.生成支付订单
            // 2.1调用支付接口
            OrderPayBo payBo = new OrderPayBo();
            if (createOrderBo.getSource() == 0) payBo.setId(SnowflakeManager.nextValue());
            payBo.setOrderId(orderBo.getId());
            payBo.setPayStatus(0);
            payBo.setPayType(createOrderBo.getPayType());
            payBo.setPayMoney(orderBo.getTotalPrice());
            orderPayProducer.save(payBo);
            result.setPayMoney(orderBo.getTotalPrice());
            result.setOrderPayId(payBo.getId());
            result.setTitle(orderBo.getTitle());
            result.setPayType(payBo.getPayType());
        }
        return result;
    }


    public R<String> checkSetUpASubAccountOrder(Long id) {

        OrderListBo bo = new OrderListBo();
        bo.setUserId(id);
        bo.setStatusList(Arrays.asList(1, 2));
        bo.setLimit(-1);
        R<PageUtils<OrderListVo>> pageUtilsR = this.queryPage(bo);
        if (ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())) {
            List<OrderListVo> list = pageUtilsR.getData().getList();
            // 只看版本
            list = list.stream().filter(item -> ObjectUtil.equals(item.getCommodityType(), 1)).toList();
            if (list.size() > 1) RRException.create("该账号存在多个有效订单，无法设置为子账号");
            list.forEach(item -> {
                if (item.getCommodityType() != 1) {
                    RRException.create("该账号存在别的有效订单，无法设置为子账号");
                } else {
                    if (item.getOrderType() != 0) {
                        RRException.create("该账号存在别的有效订单，无法设置为子账号");
                    }
                }

            });
        }
        return R.ok("成功");
    }

    /**
     * 根据套餐id获取用户id
     * @param packageId
     * @return
     */
    public List<Long> getUserIdByPackageId(Long packageId) {
        List<Long> userIds = orderProducer.getUserIdByPackageId(packageId);
        return userIds;
    }

    /**
     * 判断用户是否可以使用激活码
     * @param userId
     * @return
     */
    public R<String> isUseActivationCode(Long userId) {
        OrderInfoVo orderInfoVo = orderProducer.currentOrderByUserId(userId);
        Integer level = orderInfoVo.getLevel();
        if (level == null || level >= 0){
            RRException.create("您当前的版本不符合，只允许激活版用户使用");
        }
        // 判断子账号不能使用邀请码
        Long parentId = ResultUtil.getResult(userFeign.getUserParentId(userId));
        if (parentId != null){
            RRException.create("子账号不能使用邀请码");
        }
        return R.ok("可以使用");
    }

    /**
     * 获取用户订单等级
     * @param userId
     * @return
     */
    public R<Integer> getUserOrderLevel(Long userId){
        Long parentId = ResultUtil.getResult(userFeign.getUserParentId(userId));
        if (parentId != null){
            userId = parentId;
        }
        OrderInfoVo orderInfoVo = orderProducer.currentOrderByUserId(userId);
        Integer level = orderInfoVo.getLevel();
        if (level == null) level = -1;
        return R.ok(level);
    }

    /**
     * 获取当前订单
     * @param userId
     * @return
     */
    public R<OrderInfoVo> currentOrderByUserId(Long userId) {
        OrderInfoVo order = orderProducer.currentOrderByUserId(userId);
        return R.ok(order);
    }

    /**
     * 获取当前订单
     *
     * @param userId 用户id
     * @return 订单
     */
    public OrderInfoVo orderByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        UserDto userDto = userFeign.userById(userId);
        if (ObjectUtil.isEmpty(userDto)) {
            return null;
        }
        return orderProducer.currentOrderByUserId(userDto.getParentId() != null && userDto.getParentId() > 0 ? userDto.getParentId() : userId);
    }

    /**
     * 获取当前订单
     * @param userIds
     * @return
     */
    public List<OrderInfoVo> currentOrderByUserIds(List<Long> userIds) {
        return orderProducer.currentOrderByUserIds(userIds);
    }

    /**
     * 获取当前最后的订单
     *
     * @param userIds 用户ids
     * @return 订单
     */
    public Map<Long, OrderFinallyVo> currentOrderFinallyByUserIds(List<Long> userIds) {
        if (ObjectUtil.isEmpty(userIds)) {
            return Map.of();
        }

        List<UserDto> userDtos = userFeign.listByIds(userIds.stream().distinct().toList());
        Map<Long, Long> userMap = new HashMap<>();
        List<Long> userIdsNew = userDtos.stream().map(item -> {
            if (ObjectUtil.isNotEmpty(item.getParentId()) && item.getParentId() > 0) {
                userMap.put(item.getId(), item.getParentId());
                return item.getParentId();
            }
            userMap.put(item.getId(), item.getId());
            return item.getId();
        }).distinct().toList();
        List<OrderFinallyVo> orderFinallyVos = orderProducer.currentOrderFinallyByUserIds(userIdsNew);
        if (ObjectUtil.isEmpty(orderFinallyVos)) {
            return Map.of();
        }
        Map<Long, OrderFinallyVo> orderMap = orderFinallyVos.stream()
                .collect(Collectors.toMap(OrderFinallyVo::getUserId, v -> v, (v1, v2) -> v1));
        Map<Long, OrderFinallyVo> res = new HashMap<>();

        userIds.forEach(item -> res.put(item, userMap.get(item) == null ? null : orderMap.getOrDefault(userMap.get(item), null)));

        return res;
    }

    /**
     * 获取当前过期的订单
     * @return
     */
    public R<List<OrderInfoVo>> listByExpiredOrder(){
        return R.ok(orderProducer.listByExpiredOrder());
    }


    /**
     * 获取当前过期的订单
     * @param expiredTime 过期时间 允许为空
     * @param consumer 消费者 必填
     * @param termination 停止条件
     */
    public void loadExpiredOrder(LocalDateTime expiredTime, Consumer<List<OrderInfoVo>> consumer, Supplier<Boolean> termination) {
        orderProducer.loadExpiredOrder(expiredTime, consumer, termination);
    }

    /**
     * 订单停用
     * @param orderStopId   停用的订单id
     * @param afterUpgrading  升级前的订单id
     * @param isDeleteCache  是否删除缓存
     * @return
     */
    public R<String> orderStop(Long orderStopId, Long afterUpgrading, boolean isDeleteCache){
        orderProducer.orderStop(orderStopId, afterUpgrading, isDeleteCache);
        return R.ok();
    }

    /**
     * 批量订单停用
     * @param orderIdMap   停用的订单id key = 本次停用的订单ID value = 升级前的订单ID
     * @param isDeleteCache  是否删除缓存
     */
    public void batchStopOrder(Map<Long, Long> orderIdMap, boolean isDeleteCache) {
        orderProducer.batchStopOrder(orderIdMap, isDeleteCache);
    }

    /**
     * 订单停用
     * @param orderStopId   停用的订单id
     * @param afterUpgrading  升级前的订单id
     * @param isDeleteCache  是否删除缓存
     * @return
     */
    public R<String> orderStop(Long orderStopId, Long afterUpgrading, boolean isDeleteCache, Integer newStatus){
        orderProducer.orderStop(orderStopId, afterUpgrading, isDeleteCache, newStatus);
        return R.ok();
    }

    /**
     * 解冻用户订单
     * @param userId
     */
    public void unfreezeOrderByUserId(Long userId){
        log.info("解冻订单");
        orderProducer.unfreezeOrderByUserId(userId);
    }

    /**
     * 过期订单处理
     * @param order
     * @return
     */
    @Transactional
    public R<String> expiredOrderProcessing(OrderInfoVo order){
        log.info("订单过期处理，订单id={}", order.getId());
        orderProducer.orderStop(order.getId(), null, order.getCommodityType() == 1 && order.getStatus() == 2);
        // 记录用户是否需要更新资产
        boolean flag = true;
        if (order.getCommodityType() == 1 && order.getStatus() == 2){
            log.info("检查用户={}{}是否存在未开始的订单", order.getUserId(), order.getUserName());
            List<OrderInfoVo> notOrderList = orderProducer.listByNotStartOrder(order.getUserId());
            log.info("未开始的订单={}", JSON.toJSONString(notOrderList));
            if (ObjectUtil.isNotEmpty(notOrderList)){
                // 使用未开始的订单
                this.useOrder(notOrderList.get(0));
            }else{
                log.info("当前用户没有未开始的订单，解冻用户订单，userid={}", order.getUserId());
                orderProducer.unfreezeOrderByUserId(order.getUserId(), false);

                // 检查单前是否存在版本
                log.info("检查用户订单, userid={}", order.getUserId());
                R<List<OrderInfoVo>> orderByUserId = this.getOrderByUserId(order.getUserId());
                log.info("获取用户={}{}的当前存在订单={}", order.getUserId(), order.getUserName(), JSON.toJSONString(orderByUserId));
                if (orderByUserId.getCode() == 0) {
                    if (ObjectUtil.isNotEmpty(orderByUserId.getData())) {
                        List<OrderInfoVo> orderList = orderByUserId.getData();
                        // 判断是否有正在使用的版本
                        if (orderList.stream().noneMatch(o -> o.getCommodityType() == 1 && o.getStatus() == 2)) {
                            // 没有，就下免费的版本
                            CreateOrderBo bo = new CreateOrderBo();
                            bo.setUserId(order.getUserId());
                            bo.setUserName(order.getUserName());
                            this.createNewOrder(bo);
                            flag = false;
                        }else{
                            // 修改用户单前使用的订单缓存
                            orderList.stream().filter(o -> o.getCommodityType() == 1 && o.getStatus() == 2)
                                    .findFirst().ifPresent(val -> {
                                        log.info("修改用户单前使用的订单缓存，order={}", JSON.toJSONString(val));
                                        orderProducer.setUserPackageCache(val);
                                    });
                        }
                    } else {
                        // 没有，就下免费的版本.
                        CreateOrderBo bo = new CreateOrderBo();
                        bo.setUserId(order.getUserId());
                        bo.setUserName(order.getUserName());
                        this.createNewOrder(bo);
                        flag = false;
                    }

                }
            }
        }
        // 只有订单是生效的才统计资产
        if (flag && order.getStatus() == 2){
            log.info("用户={}{}，开始统计资产", order.getUserId(), order.getUserName());
            UserPropertyInfoVo userProperty = userPropertyProducer.getOne(order.getUserId());

            // 如果这个订单是冻结的订单就只是统计数据库的资产，不修改用户的资产使用字段
            userPropertyProducer.statisticsProperty(userProperty);

            // 只有在用的订单才统计子账号的资产
            log.info("用户={}{}，开始统计子账号的总资产", order.getUserId(), order.getUserName());
            userPropertyProducer.statisticsChildProperty(userProperty);
        }
        log.info("订单处理完成id={}，订单={}", order.getId(), JSONUtil.toJsonStr(order));
        return R.ok("完成");
    }


    /**
     * 批量处理过期订单
     * <p>该方法主要处理已过期的订单，包括停用过期订单、激活未开始订单、为无订单用户创建免费版本等操作</p>
     *
     * @param orders 过期订单列表
     *
     * @return 处理过的用户ID列表
     */
    @Transactional(rollbackFor = Throwable.class)
    public List<Long> batchExpiredOrderProcessing(List<OrderInfoVo> orders) {
        if (EmptyUtil.isEmpty(orders)) {
            return List.of();
        }

        // 停用过期订单
        orderProducer.batchStopOrder(orders.stream().collect(Collectors.toMap(OrderInfoVo::getId, order -> -1L)), false);
        // 筛选出当前生效中的订单
        List<OrderInfoVo> takeEffectOrderList = orders.stream().filter(order -> order.getStatus() == 2).toList();

        if (EmptyUtil.isEmpty(takeEffectOrderList)) {
            return List.of();
        }
        // 用户资产统计标记映射
        Map<Long, Boolean> userStatisticsFlagMap = new HashMap<>(orders.size(), 1F);

        // 筛选出商品类型为正常版本(月底资源重置)的用户列表
        List<Long> restOrderUserList = takeEffectOrderList.stream().filter(order -> order.getCommodityType() == 1).map(OrderInfoVo::getUserId).toList();

        if (EmptyUtil.isNotEmpty(restOrderUserList)) {
            Map<Long, Integer> userTypeMap = userFeign.getUserTypeMap(restOrderUserList);
            // 获取用户未开始的订单
            Map<Long, OrderInfoVo> userNotStartOrderMap = orderProducer.listUserNotStart(restOrderUserList);
            if (EmptyUtil.isNotEmpty(userNotStartOrderMap)) {
                // 使用未开始的订单
                this.batchUseOrder(userNotStartOrderMap);
            }
            // 没有待开始订单的用户
            List<Long> notOrderUserIds = restOrderUserList.stream().filter(userId -> userNotStartOrderMap == null || !userNotStartOrderMap.containsKey(userId)).toList();
            if (EmptyUtil.isNotEmpty(notOrderUserIds)) {
                // 构建用户ID到订单的映射
                Map<Long, OrderInfoVo> requestUserOrderMap = orders.stream().filter(order -> order.getCommodityType() == 1 && order.getStatus() == 2)
                    .collect(Collectors.toMap(OrderInfoVo::getUserId, Function.identity(), FunctionUtil::mergeFirst));

                // 解冻这些用户的订单
                orderProducer.batchUserUnfreezeOrders(notOrderUserIds, true);
                // 获取这些用户的所有订单
                Map<Long, List<OrderInfoVo>> userOrderMap = orderProducer.listUserOrder(notOrderUserIds);
                notOrderUserIds.forEach(userId -> {
                    List<OrderInfoVo> orderList = userOrderMap.get(userId);
                    OrderInfoVo order = requestUserOrderMap.get(userId);
                    if (EmptyUtil.isNotEmpty(orderList)) {
                        // 判断是否有正在使用的版本
                        if (orderList.stream().noneMatch(o -> o.getCommodityType() == 1 && o.getStatus() == 2)) {
                            // 没有正在使用的版本，创建激活版本订单
                            CreateOrderBo bo = new CreateOrderBo();
                            bo.setUserId(order.getUserId());
                            bo.setUserName(order.getUserName());
                            this.createNewInitOrder(bo);
                            userStatisticsFlagMap.put(userId, false);
                        } else {
                            // 修改用户当前使用的订单缓存
                            orderList.stream().filter(o -> o.getCommodityType() == 1 && o.getStatus() == 2)
                                .findFirst().ifPresent(val -> {
                                    log.info("修改用户单前使用的订单缓存，order={}", JSON.toJSONString(val));
                                    orderProducer.setUserPackageCache(val);
                                });
                        }
                    } else {
                        if (order == null && !Objects.equals(userTypeMap.get(userId), 0)) {
                            log.error("用户={}，没有订单，创建免费版本订单", userId);
                            return;
                        }
                        // 没有订单，创建激活版本订单
                        CreateOrderBo bo = new CreateOrderBo();
                        bo.setUserId(order == null ? userId : order.getUserId());
                        bo.setUserName(order == null ? "" : order.getUserName());
                        this.createNewInitOrder(bo);
                        userStatisticsFlagMap.put(userId, false);
                    }
                });
            }
        }

        // 筛选出需要统计资产的用户（生效且未被标记为不需要统计的用户）
        List<Long> needStatisticsUserIds = orders.stream().filter(entry -> entry.getStatus() == 2 && userStatisticsFlagMap.getOrDefault(entry.getUserId(), true)).map(OrderInfoVo::getUserId).distinct().toList();
        if (EmptyUtil.isEmpty(needStatisticsUserIds)) {
            return restOrderUserList;
        }

        // 获取这些用户的资产信息
        List<UserPropertyInfoVo> userPropertyList = userPropertyProducer.getUserPropertyByUserIds(needStatisticsUserIds);
        if (EmptyUtil.isEmpty(userPropertyList)) {
            return restOrderUserList;
        }

        // 如果这个订单是冻结的订单就只是统计数据库的资产，不修改用户的资产使用字段
        userPropertyProducer.batchStatisticsProperty(userPropertyList);

        // 只有在用的订单才统计子账号的资产
        userPropertyProducer.batchStatisticsChildProperty(userPropertyList);

        return restOrderUserList;

    }


    public R<String> createNewOrder(CreateOrderBo createOrderBo){
        log.info("用户={},userName={}没有正在使用的版本，开始下免费版本", createOrderBo.getUserId(), createOrderBo.getUserName());
        // 检查
        PackageInfoVo packageData = packageProducer.getGratisPackage();
        if (packageData == null) return R.error(3001, "查询初始版本失败");
        createOrderBo.setBeforeUpgrading(null);
        createOrderBo.setCommodityId(packageData.getId());
        createOrderBo.setCommodityPriceId(packageData.getCommodityPriceList().get(0).getId());
        createOrderBo.setDiscountRate(0);
        createOrderBo.setSource(1);
        createOrderBo.setOrderType(0);
        createOrderBo.setCommodityType(1);
        // 下单
        CreateOrderVo order = this.createOrder(createOrderBo);

        // 手动的单，直接订单完成接口
        if (ObjectUtil.isNotEmpty(order)) {
            this.successOrder(order.getOrderId(), true);
        }
        return R.ok("下单成功");
    }

    public R<String> createNewInitOrder(CreateOrderBo createOrderBo){
        log.info("用户={},userName={}没有正在使用的版本，开始下免费版本", createOrderBo.getUserId(), createOrderBo.getUserName());
        // 检查
        PackageInfoVo packageData = packageProducer.getPackageInit();
        if (packageData == null) return R.error(3001, "查询初始版本失败");
        createOrderBo.setBeforeUpgrading(null);
        createOrderBo.setCommodityId(packageData.getId());
        createOrderBo.setCommodityPriceId(packageData.getCommodityPriceList().get(0).getId());
        createOrderBo.setDiscountRate(0);
        createOrderBo.setSource(1);
        createOrderBo.setOrderType(0);
        createOrderBo.setCommodityType(1);
        // 下单
        CreateOrderVo order = this.createOrder(createOrderBo);

        // 手动的单，直接订单完成接口
        if (ObjectUtil.isNotEmpty(order)) {
            this.successOrder(order.getOrderId(), true);
        }
        return R.ok("下单成功");
    }

    /**
     * 当前订单开始使用
     * @param userId
     * @return      是否有使用的订单
     */
    public boolean startCurrentOrder(Long userId) {
        List<OrderInfoVo> notOrderList = orderProducer.listByNotStartOrder(userId);
        System.out.println("未开始的订单={}" + JSON.toJSONString(notOrderList));
        if (ObjectUtil.isNotEmpty(notOrderList)){
            System.out.println("使用未开始的订单");
            // 使用未开始的订单
            this.useOrder(notOrderList.get(0));
            return true;
        }else{
            return false;
        }
    }

    /**
     * 获取当前用户的未开始订单
     * @param userId
     * @return
     */
    public R<OrderInfoVo> currentUserStayOrder(Long userId) {
        return R.ok(orderProducer.currentUserStayOrder(userId));
    }

    /**
     * 关闭订单
     * @param orderId
     * @return
     */
    public R<String> closeOrder(Long orderId, Boolean isClosePay) {
        orderProducer.closeOrder(orderId, isClosePay);
        return R.ok("完成");
    }

    public R<UserVersionOrderVo> userVersionOrder(Long userId) {
        UserDto userDto = userFeign.userById(userId);
        if (userDto == null || !ObjectUtil.equals(userDto.getUserType(), UserEnums.userType.CLIENT_USER.getCode())) {
            return R.ok();
        }
        R<List<OrderInfoVo>> orderOldList = this.getOrderByUserId(userId);
        if (orderOldList.getCode() == 0 && ObjectUtil.isNotEmpty(orderOldList.getData())) {
            List<OrderInfoVo> orderList = orderOldList.getData();
            UserVersionOrderVo result = new UserVersionOrderVo();
            result.setCommodityName(orderList.get(0).getCommodityName());
            result.setStartDate(orderList.get(0).getStartDate());
            result.setEndDate(orderList.get(orderList.size() - 1).getEndDate());
            result.setLevel(orderList.get(0).getLevel());
            result.setOriginalPrice(0);
            result.setDiscountRate(0);
            result.setOriginalPrice(0);
            result.setRealPrice(0);
            for (OrderInfoVo orderInfoVo : orderList) {
                result.setDiscountRate(orderInfoVo.getDiscountRate() + result.getDiscountRate());
                result.setOriginalPrice(orderInfoVo.getOriginalPrice() + result.getOriginalPrice());
                result.setRealPrice(orderInfoVo.getRealPrice() + result.getRealPrice());
            }

            result.setTotalDay((int) DateUtil.betweenDay(result.getStartDate(), result.getEndDate(), true));
            result.setSurplusDay((int) DateUtil.betweenDay(result.getEndDate(), new Date(), false));

            BigDecimal multiply = ObjectUtil.equal(result.getTotalDay(), 0) ? BigDecimal.valueOf(0) : BigDecimal.valueOf(result.getRealPrice()).divide(BigDecimal.valueOf(result.getTotalDay()), RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(result.getSurplusDay())).setScale(0, RoundingMode.HALF_UP);
            result.setSurplusAmount(multiply.intValue());
            PackageInfoVo info = packageProducer.info(orderList.get(0).getCommodityId());
            if (ObjectUtil.isNotEmpty(info)) {
                result.setPackageLogoUrl(tencentCosProperties.getPublicBucket().getAccessUrl() + "/img/" + info.getWebsiteLogoImages());
            }
            return R.ok(result);
        }
        return R.ok();
    }

    /**
     * 根据付费到期时间查询,也可返回用户对应的付费到期时间
     * @param expireTime 付费到期时间
     * @return
     */
    public OrderTimeVo getUserIdByExpireTime(Long expireTime) {
        return orderProducer.getUserIdByExpireTime(expireTime);
    }

    public R<List<OrderInfoVo>> getYearOrder() {
        return  R.ok(orderProducer.getYearOrder());
    }

    public R<List<OrderInfoVo>> getYearOrder2(List<Long> ids) {
        return  R.ok(orderProducer.getYearOrder2(ids));
    }

    /**
     * 获取用户所有订单
     * @param userId
     * @return
     */
    public R<List<OrderInfoVo>> listAllByUserId(Long userId) {
        return R.ok(orderProducer.listAllByUserId(userId));
    }

    /**
     * 修改订单是否佣金
     * @param orderId
     * @param isCommission
     * @return
     */
    public R<String> updateIsCommission(Long orderId, int isCommission) {
        OrderBo orderBo = new OrderBo();
        orderBo.setId(orderId);
        orderBo.setIsCommission(isCommission);
        orderProducer.update(orderBo);
        return R.ok("");
    }

    public List<Long> getOrdersUserIdNoPre(Long userId) {
        return orderProducer.getOrdersUserIdNoPre(userId);
    }

    public List<OrderListVo> selectByQuery(OrderBo orderBo) {
        return  orderProducer.selectByQuery(orderBo);
    }

    public List<OrderListVo> selectByUserIds(List<Long> longList) {
        return  orderProducer.selectByUserIds(longList);
    }

    /**
     * 获取用户的版本过期时间
     *
     * @param userIds 用户ids
     * @return 用户版本过期时间
     */
    public Map<Long, Date> userOrderExpireTimeByUserIds(List<Long> userIds) {
        return orderProducer.userOrderExpireTimeByUserIds(userIds);
    }

    /**
     * 过滤已存在的订单id
     *
     * @param orderIds 订单ids
     * @return 过滤后的订单ids
     */
    public List<Long> filterExistsIds(List<Long> orderIds) {
        if (EmptyUtil.isEmpty(orderIds)) {
            return Collections.emptyList();
        }
        return orderProducer.filterExistsIds(orderIds);
    }


    /**
     * 获取订单是否免费
     *
     * @param orderIds 订单ids
     * @return 订单是否免费 如果订单不存在则不返回这个key
     */
    public Map<Long, Boolean> getOrderFreeMap(List<Long> orderIds) {
        if (EmptyUtil.isEmpty(orderIds)) {
            return Map.of();
        }
        return orderProducer.getOrderFreeMap(orderIds);
    }

    /**
     * 获取订单基础信息
     *
     * @param orderIds 订单ids
     *
     * @return {@link List }<{@link Long }>
     */
    public Map<Long, OrderVo> getOrderBaseInfoMap(List<Long> orderIds) {
        return orderProducer.getOrderBaseInfoMap(orderIds, OrderEntity::getOrderType, OrderEntity::getUserId, OrderEntity::getCommodityId, OrderEntity::getLevel);
    }

    /**
     * 纯录制版/复盘按切换
     *
     * @param clientVersionConfigBo 参数
     */
    public void setClientVersionConfig(ClientVersionConfigBo clientVersionConfigBo) {
        Long userId = clientVersionConfigBo.getUserId();

        // 获取用户当前生效的订单
        List<OrderInfoVo> orderList = orderProducer.getOrderByUserId(userId);
        OrderInfoVo order = orderList.stream().filter(item -> ObjectUtil.equals(2, item.getStatus())).findFirst().orElse(null);

        if (order == null) {
            throw new BusinessException("未获取当前版本，切换失败");
        }

        // 从 KV 配置获取纯录制版对应的 level 值
        Integer pureRecordingLevel = systemKvProducer.getValueByKey("pure_recording_version_level", (Integer) null);
        if (pureRecordingLevel == null) {
            throw new BusinessException("系统配置异常：纯录制版等级未配置");
        }

        boolean frozenCurrent = false;

        if (OrderEnums.clientVersion.RECORD.getCode().equals(clientVersionConfigBo.getClientVersion())) {
            // 切换成纯录制版

            // 幂等：当前已经是纯录制版等级，直接返回
            if (pureRecordingLevel.equals(order.getLevel())) {
                return;
            }

            // 仅免费版（level <= 0）允许切换成纯录制版
            if (order.getLevel() != null && order.getLevel() > 0) {
                throw new BusinessException("当前版本不支持切换成纯录制版");
            }

            // 查询冻结订单中是否存在纯录制版（status=7 and level=pureRecordingLevel），按开始时间升序
            List<OrderInfoVo> frozenPureRecordingOrders = orderProducer.listFrozenOrderByUserIdAndLevel(userId, pureRecordingLevel);
            // 过滤掉时间过期的
            DateTime now = new DateTime();
            frozenPureRecordingOrders = frozenPureRecordingOrders.stream()
                    .filter(item -> now.isAfter(new DateTime(item.getStartDate())) && now.isBefore(new DateTime(item.getEndDate())))
                    .toList();
            if (ObjectUtil.isNotEmpty(frozenPureRecordingOrders)) {
                frozenCurrent = true;
            }
        } else if (OrderEnums.clientVersion.REPLAY.getCode().equals(clientVersionConfigBo.getClientVersion())) {
            // 切换成复盘版

            // 幂等：当前不是纯录制版，无需切换，直接返回
            if (pureRecordingLevel.equals(order.getLevel())) {
                frozenCurrent = true;
            }
        } else {
            throw new BusinessException("版本类型未知");
        }

        // 切换版本
        if (frozenCurrent) {
            // 冻结当前的订单
            orderProducer.freezeOrderByUserId(userId, orderList.stream().map(OrderVo::getId).toList());

            // 解冻订单
            orderProducer.unfreezeOrderByUserId(userId, true, orderList.stream().map(OrderVo::getId).toList());

            // 获取主用户资产对象
            UserPropertyInfoVo userPropertyInfoVo = userPropertyProducer.getOne(clientVersionConfigBo.getUserId());
            if (ObjectUtil.isNull(userPropertyInfoVo)) RRException.create("用户资产不存在");

            OrderInfoVo newOrder = orderProducer.currentOrderDetailsByUserId(clientVersionConfigBo.getUserId());

            // 用户下单后为用户添加资产--只用于添加自己的资产，不添加子用户的
            typeSurplusProducer.payAddTypeSurplus1(newOrder, userPropertyInfoVo);

            // 用户下单后为子用户添加资产--只添加子用户的
            typeSurplusProducer.payAddChildTypeSurplus(newOrder);

            // 统计用户资源
            UserPropertyInfoVo userProperty = userPropertyProducer.getOne(userId);

            // 如果这个订单是冻结的订单就只是统计数据库的资产，不修改用户的资产使用字段
            userPropertyProducer.statisticsProperty(userProperty);

            // 只有在用的订单才统计子账号的资产
            userPropertyProducer.statisticsChildProperty(userProperty);
        }
        // 设置版本
        userFeign.saveClientVersion(userId, clientVersionConfigBo.getClientVersion());
    }

    /**
     * 获取用户的客户端版本-根据版本判断
     *
     * @param userId 用户id
     * @return 用户版本
     */
    public UserClientVersionConfigVo userClientVersionConfig(Long userId) {
        UserClientVersionConfigVo result = new UserClientVersionConfigVo();
        result.setClientVersion(OrderEnums.clientVersion.REPLAY.getCode());
        result.setIsVersionSelect(OrderEnums.versionSelect.NOT_SELECT.getCode());

        // 获取当前的版本
        String clientVersion = userFeign.getClientVersion(userId);
        if (OrderEnums.clientVersion.RECORD.getCode().equals(clientVersion)) {
            result.setIsVersionSelect(OrderEnums.versionSelect.SELECT.getCode());
        } else {
            // 获取当前的版本
            OrderInfoVo order = orderProducer.currentOrderByUserId(userId);
            if (order == null) {
                return result;
            }

            // 获取纯录制版的等级
            Integer pureRecordingLevel = systemKvProducer.getValueByKey("pure_recording_version_level", (Integer) null);

            // 不是免费版、激活版、纯录制版的就不弹窗
            if (order.getLevel() > 0 && !ObjUtil.equal(order.getLevel(), pureRecordingLevel)) {
                result.setIsVersionSelect(OrderEnums.versionSelect.NOT_SELECT.getCode());
            } else {
                result.setIsVersionSelect(OrderEnums.versionSelect.SELECT.getCode());
            }
        }
        result.setClientVersion(clientVersion);
        return result;
    }

    /**
     * 下单前检查子账号数量是否超出
     *
     * @param userId      用户id
     * @param orderSubNum 新版本的子账号数量
     * @param subNum      子账号数量
     */
    public void checkSubAccountNum(Long userId, int orderSubNum, int subNum) {

        // 获取增量包的子账号数量
        int incrementalSubNum = orderProducer.getIncrementalSubNum(userId, OrderEnums.commodityTypeCode.SUB_ACCOUNT_COUNT.getCode());

        // 判断子账号是否超出
        if (incrementalSubNum + orderSubNum < subNum) {
            RRException.create(StrUtil.format("当前子账号数量超出限制，请解绑多余账号后再升级（超出 {} 个）", subNum - (incrementalSubNum + orderSubNum)));
        }

    }

    /**
     * 获取用户冻结订单列表（等级大于等于指定等级且等级 > 0）
     *
     * @param userId   用户id
     * @param minLevel 最小等级（包含）
     * @return 冻结订单列表
     */
    public List<OrderInfoVo> listFrozenOrderByUserIdAndMinLevel(Long userId, Integer minLevel) {
        return orderProducer.listFrozenOrderByUserIdAndMinLevel(userId, minLevel);
    }
}
