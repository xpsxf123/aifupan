package com.jiuyu.replay.order.producer;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.order.OrderDetailInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderListVo;
import com.jiuyu.replay.generic.vo.order.OrderVo;
import com.jiuyu.replay.order.bo.OrderBo;
import com.jiuyu.replay.order.bo.OrderListBo;
import com.jiuyu.replay.order.entity.OrderEntity;
import com.jiuyu.replay.order.entity.UserPropertyEntity;
import com.jiuyu.replay.order.vo.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * 订单
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
public interface OrderProducer {


    /**
     * 订单列表
     * @param orderListBo 订单列表查询参数
     * @return
     */
    PageUtils<OrderListVo> queryPage(OrderListBo orderListBo);

    /**
    * 订单信息
    * @param id 订单id
    * @return
    */
    OrderInfoVo info(Long id);

    /**
     * 新增订单
     * @param orderBo 订单对象
     * @return
     */
    OrderInfoVo save(OrderBo orderBo);

    /**
     * 修改订单
     * @param orderBo 订单对象
     * @return
     */
    void update(OrderBo orderBo);

    /**
     * 删除订单
     * @param id 订单id
     * @return
     */
    void deleteById(java.lang.Long id);

    /**
     * 支付成功处理
     * @param orderId
     */
    OrderInfoVo payOrderHandle(OrderInfoVo orderId);

    /**
     * 支付成功处理-为了刷数据而写
     * @param orderId
     * @return
     */
    OrderInfoVo payOrderHandle(OrderInfoVo orderId, Date start, Date end);

    /**
     * 支付成功处理详情
     * @param order
     */
    List<OrderDetailInfoVo> payUpdateDetail(OrderInfoVo order);

    /**
     * 订单过期处理
     *
     * @param orderStopId
     * @param afterUpgrading
     * @param isDeleteCache
     */
    void orderStop(Long orderStopId, Long afterUpgrading, boolean isDeleteCache);

    /**
     * 订单过期处理
     * @param orderStopId 单前的订单id
     * @param afterUpgrading 之前的订单id
     * @param isDeleteCache 是否删除缓存
     * @param newStatus 0：未支付 1：未开始，2：生效中 3：已过期 4：已退款 5：已结束(升级了，当前订单失效) 6：超时未支付关闭 7冻结 8手动取消订单
     */
    void orderStop(Long orderStopId, Long afterUpgrading, boolean isDeleteCache, Integer newStatus);

    /**
     * 获取用户套餐
     *
     * @param userId
     * @return
     */
    OrderInfoVo currentOrderByUserId(Long userId);

    /**
     * 获取用户套餐详情
     *
     * @param userId 用户id
     * @return 用户套餐详情
     */
    OrderInfoVo currentOrderDetailsByUserId(Long userId);

    /**
     * 获取当前用户套餐
     * @param userIds
     * @return
     */
    List<OrderInfoVo> currentOrderByUserIds(List<Long> userIds);

    /**
     * 设置用户套餐缓存
     * @param orderInfoVo
     */
    void setUserPackageCache(OrderInfoVo orderInfoVo);

    /**
     * 删除用户套餐缓存
     * @param userId
     */
    void deleteUserPackageCache(Long userId);

    /**
     * 获取过期订单
     *
     * @return
     */
    List<OrderInfoVo> getNotExpirationOrders(List<Long> userIds);

    /**
     * 查询用户的订单
     * @param userId
     * @return
     */
    List<OrderInfoVo> getOrderByUserId(Long userId);

    /**
     * 批量获取用户订单
     *
     * @param userIds 用户ID
     *
     * @return {@link Map }<{@link Long }, {@link List }<{@link OrderInfoVo }>>
     */
    Map<Long, List<OrderInfoVo>> listUserOrder(List<Long> userIds);

    /**
     * 批量查询用户下所有未删除的订单（不过滤状态和商品类型）
     *
     * @param userIds 用户ID列表
     *
     * @return key=userId，value=该用户订单列表（按 startDate 升序）
     */
    Map<Long, List<OrderInfoVo>> listAllOrdersByUserIds(List<Long> userIds);

    /**
     * 冻结用户的订单
     * @param userId
     */
    void freezeOrderByUserId(Long userId, List<Long> orderList);

    /**
     * 解冻用户的订单
     * @param userId 用户id
     */
    void unfreezeOrderByUserId(Long userId);

    /**
     * 解冻用户的订单
     *
     * @param userId          用户id
     * @param isThawActivated 清除缓存
     */
    void unfreezeOrderByUserId(Long userId, boolean isThawActivated);

    /**
     * 解冻用户的订单
     *
     * @param userId          用户id
     * @param isThawActivated 清除缓存
     * @param notOrderList    不解冻的订单
     */
    void unfreezeOrderByUserId(Long userId, boolean isThawActivated, List<Long> notOrderList);

    /**
     * 批量解冻用户的订单
     * @param userIds 用户id
     * @param isThawActivated 是否解冻激活
     */
    void batchUserUnfreezeOrders(Collection<Long> userIds, boolean isThawActivated);

    /**
     * 获取月到期的订单
     *
     * @param orderId
     * @return
     */
    List<OrderDetailInfoVo> getMonthExpiredOrders(Long orderId);

    /**
     * 设置过期
     *
     * @param monthExpiredOrders
     */
    void setExpiration(List<OrderDetailInfoVo> monthExpiredOrders);

    /**
     * 设置下次过期时间
     *
     * @param monthExpiredOrders
     * @return
     */
    List<UserPropertyEntity> setNextExpirationTime(List<OrderDetailInfoVo> monthExpiredOrders);

    /**
     * 根据套餐id查询用户id
     * @param packageId
     * @return
     */
    List<Long> getUserIdByPackageId(Long packageId);

    /**
     * 在订单升级后，检查是否有续费订单，有就修改一下续费订单的开始结束时间
     * @param order
     */
    void checkRenewalOrder(OrderInfoVo order);

    /**
     * 获取过期的订单
     * @return
     */
    List<OrderInfoVo> listByExpiredOrder();

    /**
     * 订单是否已经存在未过期的订单
     *
     * @param userId
     * @return
     */
    List<OrderInfoVo> listByNotStartOrder(Long userId);

    /**
     * 查询用户是否存在订单
     * @param userId
     * @return
     */
    List<OrderInfoVo> listExistByUserId(Long userId);

    /**
     * 获取订单信息-单表
     * @param id
     * @return
     */
    OrderInfoVo getById(Long id);

    /**
     * 获取升级前的订单
     *
     * @param userId
     * @param newOrderId
     * @return
     */
    List<OrderInfoVo> getOldVersionOrderList(Long userId, Long newOrderId);

    /**
     * 获取用户的当前订单
     * @param userId
     * @return
     */
    OrderInfoVo currentUserStayOrder(Long userId);

    /**
     * 获取用户的当前订单详情
     *
     * @param userId
     * @return
     */
    OrderInfoVo currentUserOrderDetails(Long userId);

    /**
     * 关闭订单
     *
     * @param orderId
     * @param isClosePay
     */
    void closeOrder(Long orderId, Boolean isClosePay);

    /**
     * 根据付费到期时间查询,也可返回用户对应的付费到期时间
     * @param expireTime 付费到期时间
     * @return
     */
    OrderTimeVo getUserIdByExpireTime(Long expireTime);

    /**
     * 同步版本
     * @param info
     * @return
     */
    List<OrderDetailInfoVo> synchronousPackage(PackageInfoVo info);

    List<OrderInfoVo> getYearOrder();

    List<OrderInfoVo> getYearOrder2(List<Long> ids);

    /**
     * 获取用户的所有订单
     * @param userId
     * @return
     */
    List<OrderInfoVo> listAllByUserId(Long userId);

    /**
     * 根据用户id获取生效中的订单id
     * @param userId
     * @return
     */
    List<Long> getOrdersUserIdNoPre(Long userId);

    List<OrderListVo> selectByQuery(OrderBo orderBo);

    /**
     * 查询用户指定等级的冻结订单，按开始时间升序排列
     *
     * @param userId 用户id
     * @param level  订单等级
     * @return 冻结订单列表
     */
    List<OrderInfoVo> listFrozenOrderByUserIdAndLevel(Long userId, Integer level);

    /**
     * 获取用户冻结订单列表（等级大于等于指定等级且等级 > 0）
     *
     * @param userId   用户id
     * @param minLevel 最小等级（包含）
     * @return 冻结订单列表
     */
    List<OrderInfoVo> listFrozenOrderByUserIdAndMinLevel(Long userId, Integer minLevel);

    List<OrderListVo> selectByUserIds(List<Long> longList);

    /**
     * 获取用户的版本过期时间
     *
     * @param userIds 用户ids
     * @return key:用户id value:过期时间
     */
    Map<Long, Date> userOrderExpireTimeByUserIds(List<Long> userIds);

    /**
     * 获取当前过期的订单
     * @param expiredTime 过期时间 允许为空
     * @param consumer 消费者 必填
     * @param termination 停止条件
     */
    void loadExpiredOrder(LocalDateTime expiredTime,  Consumer<List<OrderInfoVo>> consumer, Supplier<Boolean> termination);


    /**
     * 批量订单停用
     * @param orderIdMap   停用的订单id key = 本次停用的订单ID value = 升级前的订单ID
     * @param isDeleteCache  是否删除缓存
     */
    void batchStopOrder(Map<Long, Long> orderIdMap, boolean isDeleteCache);


    /**
     * 加载过期订单详细信息
     *
     * @param expiredTime 过期时间
     * @param consumer    消费者
     * @param termination  停止条件
     */
    void loadExpiredOrderDetail(LocalDateTime expiredTime, Consumer<List<OrderDetailInfoVo>> consumer, Supplier<Boolean> termination);


    /**
     * 加载所有过期订单详细信息
     *
     * @param expiredTime 过期时间
     * @return 订单详细信息
     */
    List<OrderDetailInfoVo> loadExpiredAllOrderDetail(LocalDateTime expiredTime);

    /**
     * 获取用户未开始订单
     * @param userIds 用户ids
     * @return 订单
     */
    Map<Long, OrderInfoVo> listUserNotStart(List<Long> userIds);

    /**
     * 批量更新订单状态
     * @param orderIds 订单ids
     * @param status 状态
     */
    void batchUpdateOrderStatus(List<Long> orderIds, int status);

    /**
     * 过滤已存在的订单id
     * @param orderIds 订单ids
     * @return 订单ids
     */
    List<Long> filterExistsIds(List<Long> orderIds);

    /**
     * 获取订单用户id
     * @param orderIds 订单ids
     * @return key:订单id value:用户id
     */
    Map<Long, Long> getOrderUserMap(List<Long> orderIds);

    /**

     * 获取订单是否免费
     *
     * @param orderIds 订单ids
     * @return 订单是否免费 如果订单不存在则不返回这个key
     */
    Map<Long, Boolean> getOrderFreeMap(List<Long> orderIds);


    /**
     * 获取订单基础信息
     *
     * @param orderIds 订单ids
     * @param selectColumns 返回字段
     * @return 订单基础信息 如果订单不存在则不返回这个key
     */
    Map<Long, OrderVo> getOrderBaseInfoMap(List<Long> orderIds, SFunction<OrderEntity, ?>... selectColumns);

    /**
     * 获取当前最后的订单
     *
     * @param userIds 用户ids
     * @return 订单
     */
    List<OrderFinallyVo> currentOrderFinallyByUserIds(List<Long> userIds);

    /**
     * 获取增量包的子账号数量
     *
     * @param userId            用户id
     * @param commodityTypeCode 类型
     * @return 子账号数量
     */
    int getIncrementalSubNum(Long userId, String commodityTypeCode);

    /**
     *  是否付费用户
     *
     * @param orderUserId 订单用户id
     * @return 是否有付费用户
     */
    boolean hasPaidUser(long orderUserId);

    /**
     * Data Hub：订单增量对账查询（按 update_date + id 键集游标，升序）
     *
     * @param tenantIds        租户id列表（可空 = 不限租户）
     * @param updatedAfter     增量起点（含，按 update_date，可空）
     * @param cursorUpdateDate 游标：上一页末行 update_date（可空 = 第一页）
     * @param cursorId         游标：上一页末行订单id（与 cursorUpdateDate 成对出现）
     * @param limit            本页条数上限
     *
     * @return {@link java.util.List }<{@link com.jiuyu.replay.order.vo.datahub.DataHubOrderItemVo }>
     */
    java.util.List<com.jiuyu.replay.order.vo.datahub.DataHubOrderItemVo> listDataHubOrders(
            java.util.Collection<Long> tenantIds, java.util.Date updatedAfter,
            java.util.Date cursorUpdateDate, Long cursorId, int limit);
}

