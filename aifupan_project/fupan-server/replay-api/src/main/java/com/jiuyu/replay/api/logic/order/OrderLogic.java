package com.jiuyu.replay.api.logic.order;

import com.alipay.api.AlipayApiException;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bo.*;
import com.jiuyu.replay.order.vo.CreateOrderVo;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderListVo;
import com.jiuyu.replay.order.vo.UserVersionOrderVo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;


/**
 * 订单
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
public interface OrderLogic {


    /**
     * 订单列表
     * @param orderListBo 订单列表查询参数
     * @return
     */
    R<PageUtils<OrderListVo>> queryPage(OrderListBo orderListBo);

    /**
     * 订单信息
     *
     * @param id 订单id
     * @return
     */
    R<OrderInfoVo> info(java.lang.Long id);

    /**
     * 新增订单
     * @param orderBo 订单对象
     * @return
     */
    R<String> save(OrderBo orderBo);

    /**
     * 修改订单
     * @param orderBo 订单对象
     * @return
     */
    R<String> update(OrderBo orderBo);

    /**
     * 删除订单
     * @param id 订单id
     * @return
     */
    R<String> delete(java.lang.Long id);

    /**
     * 创建客户端订单
     *
     * @param createClientOrder
     * @return
     */
    R<CreateOrderVo> createClientOrder(OnlinePayOrderBo createClientOrder);


    /**
     * 处理微信支付回调
     * @return
     */
    boolean wechatPayCallbackHandle(HttpServletRequest request) throws Exception;
    /**
     * 处理支付宝支付回调
     * @return
     */
    boolean alipayPayCallbackHandle(Long orderPayId, String transactionId, String jsonString);

    /**
     * 升级订单
     * @param createClientOrder
     */
    void pcUpgradeOrder(CreateClientOrder createClientOrder);

    /**
     * 续费订单
     * @param createClientOrder
     */
    void pcRenewalOrder(CreateClientOrder createClientOrder);

    /**
     * 增量包订单
     * @param createClientOrder
     */
    void pcIncrementsOrder(CreateClientOrder createClientOrder);

    /**
     * 新下的订单
     *
     * @param userId
     * @return
     */
    R<String> newPcCreateOrder(Long userId);

    /**
     * 新下的订单（之前是没有的）
     * @param userId
     * @return
     */
    R<String> newPcCreateInitOrder(Long userId);

    /**
     * 邀请码创建订单
     * @param userId
     * @param packageId
     * @param commodityPriceId
     * @return
     */
    R<String> invitationCodeCreateOrder(Long userId, Long packageId, Long commodityPriceId);

    /**
     * 查询用户的在用订单
     * @param userId
     * @return
     */
    R<List<OrderInfoVo>> getOrderByUserId(Long userId);

    /**
     * 检查用户的订单
     * @param subUserId
     */
    R<String> checkUserOrder(Long subUserId);

    /**
     * 创建订单
     * @param orderBo
     * @return
     */
    R<String> createOrder(CreateOrderBo orderBo);

    /**
     * 停止订单
     * @param orderId
     * @return
     */
    R<String> orderStop(Long orderId);

    /**
     * 订单编辑
     * @param createClientOrder
     * @return
     */
    R<String> orderEdit(CreateClientOrder createClientOrder);

    /**
     * 获取用户版本订单
     *
     * @param userId
     * @return
     */
    R<UserVersionOrderVo> userVersionOrder(Long userId);

    /**
     * 获取用户的待支付订单信息
     *
     * @param userId
     * @return
     */
    R<OrderInfoVo> currentUserStayOrder(Long userId);

    /**
     * 关闭订单
     * @param orderId
     * @return
     */
    R<String> closeOrder(Long orderId) throws AlipayApiException;

    /**
     * 从激活版升级到免费版
     * @param userId
     * @return
     */
    R<String> updateFreeVersion(Long userId);

    /**
     * 刷年订单用户的数据看板数据
     * @return
     */
    R<String> brushBoardOrder(Map<Integer, Long> map);
}

