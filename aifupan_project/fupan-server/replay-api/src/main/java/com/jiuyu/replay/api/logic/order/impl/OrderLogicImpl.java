package com.jiuyu.replay.api.logic.order.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.AlipayApiException;
import com.jiuyu.replay.agent.bll.AgentCommissionBll;
import com.jiuyu.replay.agent.bll.InviteUrlCodeBll;
import com.jiuyu.replay.agent.bo.CommissionAllocationBo;
import com.jiuyu.replay.agent.vo.AgentCommissionInfoVo;
import com.jiuyu.replay.api.constant.Constant;
import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.api.logic.order.OrderLogic;
import com.jiuyu.replay.common.bll.SystemKvBll;
import com.jiuyu.replay.common.constant.CommonProperties;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.feign.order.OrderFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.order.*;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import com.jiuyu.replay.order.bll.*;
import com.jiuyu.replay.order.bo.*;
import com.jiuyu.replay.order.constant.OrderProperties;
import com.jiuyu.replay.order.vo.CreateOrderVo;
import com.jiuyu.replay.order.vo.PackageInfoVo;
import com.jiuyu.replay.order.vo.UserVersionOrderVo;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserInfoVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.power.vo.UserVo;
import com.jiuyu.replay.third.bo.NativePayBo;
import com.jiuyu.replay.third.bo.WechatPayCallbackHandleBo;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Service
@AllArgsConstructor
@Slf4j
public class OrderLogicImpl implements OrderLogic {

    private final OrderBll orderBll;

//    private final PayBll payBll;

    private final OrderPayBll orderPayBll;

    private final RedisTemplate<String, Object> redisTemplate;

    private final OrderProperties orderProperties;

    private final PackageBll packageBll;

    private final UserBll userBll;

    private final UserPropertyBll userPropertyBll;

    private final CommonProperties commonProperties;

    private final AnchorUrlBll anchorUrlBll;
    private final OrderDetailBll orderDetailBll;
    private final InviteUrlCodeBll inviteUrlCodeBll;
    private final SystemKvBll systemKvBll;
    private final AgentCommissionBll agentCommissionBll;
    private final UserFeign userApi;
    private final OrderFeign orderFeign;
    private final OrderExtendBll orderExtendBll;


    @Override
    public R<PageUtils<OrderListVo>> queryPage(OrderListBo orderListBo) {

        R<PageUtils<OrderListVo>> pageUtilsR = orderBll.queryPage(orderListBo);

        if (ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())){

            List<OrderListVo> list = pageUtilsR.getData().getList();
            List<Long> orderIds = list.stream().map(OrderVo::getId).toList();
            Set<Long> optIds = new HashSet<>();
            list.forEach(x->{
                if (x.getCreateId()!=null){
                    optIds.add(x.getCreateId());
                }
                if (x.getUpdateId()!=null){
                    optIds.add(x.getUpdateId());
                }

            });
            R<List<AgentCommissionInfoVo>> agentCommissionR = agentCommissionBll.listByOrderIds(orderIds);

            Map<Long, UserDto> userMap = new HashMap<>(optIds.size());
            if (ObjectUtil.isNotEmpty(optIds)){
                List<UserDto> userDtos = userApi.listByIds(new ArrayList<>(optIds));
                userMap = userDtos.stream().collect(Collectors.toMap(UserDto::getId, Function.identity(), (a, b) -> b));
            }

            List<AgentCommissionInfoVo> data = agentCommissionR.getData();
            for (OrderListVo orderListVo : list) {
                orderListVo.setCreateName(userMap.get(orderListVo.getCreateId())==null?null:userMap.get(orderListVo.getCreateId()).getNickName());
                orderListVo.setUpdateName(userMap.get(orderListVo.getUpdateId())==null?null:userMap.get(orderListVo.getUpdateId()).getNickName());
                if (ObjectUtil.isNotEmpty(data)){
                    Map<Long, AgentCommissionInfoVo> collect = data.stream().collect(Collectors.toMap(AgentCommissionInfoVo::getOrderId, o -> o, (a, b) -> b));
                    AgentCommissionInfoVo agentCommissionInfoVo = collect.get(orderListVo.getId());
                    if (ObjectUtil.isNotEmpty(agentCommissionInfoVo)){
                        orderListVo.setCommission(agentCommissionInfoVo.getCommission());
                        orderListVo.setCommissionMoney(agentCommissionInfoVo.getCommissionMoney());
                        orderListVo.setCommissionType(agentCommissionInfoVo.getCommissionType());
                    }
                }
            }
        }

        return pageUtilsR;
    }


    /**
     * 订单详情
     * 填充代理佣金信息
     * 填充创建者名称修改者名称
     * @param id 订单id
     * @return
     */
    @Override
    public R<OrderInfoVo> info(Long id) {

        R<OrderInfoVo> info = orderBll.info(id);

        // 如果订单查询失败，直接返回结果
        if (ObjectUtil.isEmpty(info) || ObjectUtil.isEmpty(info.getData())) {
            return info;
        }

        // 设置订单扩展表
        OrderExtendInfoVo byOrderId = orderExtendBll.getByOrderId(id);
        if (ObjectUtil.isNotEmpty(byOrderId)) {
            info.getData().setOrderExtend(byOrderId);
        }

        // 查询并填充代理佣金信息
        R<AgentCommissionInfoVo> commissionResult = agentCommissionBll.getByOrderId(id);
        if (ObjectUtil.isNotEmpty(commissionResult.getData())) {
            AgentCommissionInfoVo commissionInfo = commissionResult.getData();
            info.getData().setCommission(commissionInfo.getCommission());
            info.getData().setCommissionMoney(commissionInfo.getCommissionMoney());
            info.getData().setCommissionType(commissionInfo.getCommissionType());
        }

        //填充订单详情的 创建者修改者
        if (ObjectUtil.isNotEmpty(info.getData())){
            OrderInfoVo orderInfoVo = info.getData();

            // 收集需要查询的用户ID
            Set<Long> optIds = new HashSet<>();
            if (orderInfoVo.getCreateId()!=null){
                optIds.add(orderInfoVo.getCreateId());
            }
            if(orderInfoVo.getUpdateId()!=null){
                optIds.add(orderInfoVo.getUpdateId());
            }
            // 收集结果不为[] 查询用户信息
            if (ObjectUtil.isNotEmpty(optIds)){
                R<List<UserListVo>> listByIds = userBll.listByIds(new ArrayList<>(optIds));
                //查询结果不为null  不为 []
                if (ObjectUtil.isNotEmpty(listByIds)&& ObjectUtil.isNotEmpty(listByIds.getData())){
                    //结果映射
                    Map<Long, String> nameMap = listByIds.getData().stream().collect(Collectors
                            .toMap(UserListVo::getId, UserListVo::getNickName , (old, newValue) -> newValue));
                    //填充创建者名称 修改者名称
                    orderInfoVo.setCreateName(nameMap.get(orderInfoVo.getCreateId()));
                    orderInfoVo.setUpdateName(nameMap.get(orderInfoVo.getUpdateId()));
                }
            }

        }

        return info;
    }

    @Override
    public R<String> save(OrderBo orderBo) {

        return orderBll.save(orderBo);
    }

    @Override
    public R<String> update(OrderBo orderBo) {

        return orderBll.update(orderBo);
    }

    @Override
    public R<String> delete(java.lang.Long id) {

        return orderBll.delete(id);
    }

    @Override
    @Transactional
    public R<CreateOrderVo> createClientOrder(OnlinePayOrderBo onlinePayOrderBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        // 校验购买人是否正常
        // 子账号不能买，管理员不能买
        // 有待支付的订单不能购买
        if (user.getUserType() != 0) RRException.create("当前用户类型不能在线购买版本");
        R<OrderInfoVo> orderInfoVoR = orderBll.currentUserStayOrder(user.getId());
        if (orderInfoVoR.getCode() == 0 && orderInfoVoR.getData() != null){
            return R.error(3000,"当前用户有未完成订单，请先先取消待支付订单");
        }
        OrderInfoVo currentOrder = orderFeign.currentOrderByUserId(user.getId());
        RRException.isNotEmpty(currentOrder, "获取用户版本等级失败");
        RRException.isNotEmpty(currentOrder.getLevel(), "获取用户版本等级失败");
        Integer level = currentOrder.getLevel();
        if (!ObjectUtil.equal(level, onlinePayOrderBo.getLevel())) RRException.create("当前用户版本不一致，请刷新页面重试");

        CreateOrderBo createOrderBo = BeanUtil.copyProperties(onlinePayOrderBo, CreateOrderBo.class);
        createOrderBo.setCommodityType(1);
        createOrderBo.setUserId(user.getId());
        createOrderBo.setUserName(user.getUsername());
        createOrderBo.setDiscountRate(0);
        createOrderBo.setSource(0);
        // 判断当前下单的订单类型
        if (createOrderBo.getCommodityType() == 0){
            createOrderBo.setOrderType(4);
        }else if (createOrderBo.getCommodityType() == 1){
            R<PackageInfoVo> info = packageBll.info(createOrderBo.getCommodityId());
            if (info.getCode() == 0 && ObjectUtil.isNotEmpty(info.getData())){
                PackageInfoVo data = info.getData();
                if (ObjectUtil.equal(data.getLevel(), level)){
                    createOrderBo.setOrderType(3);
                }else{
                    if (level > data.getLevel()){
                        RRException.create("版本不能向下购买");
                    }else if (level > 0){
                        createOrderBo.setOrderType(1);
                    }else {
                        createOrderBo.setOrderType(2);
                    }
                }
            }else {
                return R.error(3002,"版本不存在");
            }
        }
        if (createOrderBo.getOrderType() == 1 || createOrderBo.getOrderType() == 2){
            // 查询之前的版本能抵扣多少钱
            R<UserVersionOrderVo> userVersionOrderVoR = userVersionOrder(user.getId());
            if (userVersionOrderVoR.getCode() == 0 && ObjectUtil.isNotEmpty(userVersionOrderVoR.getData())){
                UserVersionOrderVo orderVo = userVersionOrderVoR.getData();
                createOrderBo.setDiscountRate(orderVo.getSurplusAmount());
            }
        }
        CreateOrderVo order = orderBll.createOrder(createOrderBo);
        if (order != null) {
            if (order.getOrderPayId() != null) {
                NativePayBo payBo = new NativePayBo();
                payBo.setOrderId(order.getOrderPayId());
                payBo.setPayType(createOrderBo.getPayType());
                payBo.setMoney(order.getPayMoney());
                payBo.setTitle(order.getTitle());
                if (createOrderBo.getPayType() == 0){
                    payBo.setCallbackAddress("replay/notify/wechatPayCallback");
                }else if (createOrderBo.getPayType() == 1){
                    payBo.setCallbackAddress("replay/notify/alipayPayCallback");
                }
//                R<String> payCode = payBll.getPayCode(payBo);
//                if (payCode.getCode() == 0) {
//                    order.setUrlCode(payCode.getData());
//                    OrderPayBo orderPayBo = new OrderPayBo();
//                    orderPayBo.setId(order.getOrderPayId());
//                    orderPayBo.setPayCode(payCode.getData());
//                    orderPayBll.update(orderPayBo);
//                    redisTemplate.opsForValue().set(orderProperties.getOrderTimeoutRedisKey() + payBo.getOrderId(), payBo.getOrderId(), Duration.ofMinutes(commonProperties.getOrderTimeoutMinutes()));
//                } else {
//                    return R.error(payCode.getCode(), payCode.getMsg());
//                }
            } else {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "获取失败，请稍后再试");
            }
        }
        return R.ok(order);
    }


    @Override
    @Transactional
    public boolean wechatPayCallbackHandle(HttpServletRequest request) throws Exception {

        WechatPayCallbackHandleBo wechatPayCallbackHandleBo = new WechatPayCallbackHandleBo();
        wechatPayCallbackHandleBo.setSerialNo(request.getHeader("Wechatpay-Serial"));
        wechatPayCallbackHandleBo.setNonceStr(request.getHeader("Wechatpay-Nonce"));
        wechatPayCallbackHandleBo.setTimestamp(request.getHeader("Wechatpay-Timestamp"));
        wechatPayCallbackHandleBo.setWechatSign(request.getHeader("Wechatpay-Signature"));
        wechatPayCallbackHandleBo.setBody(this.getRequestBody(request));

        try {
            // 解签
//            Transaction transaction = payBll.wechatPayCallbackHandle(wechatPayCallbackHandleBo);
//            if (transaction != null && ObjectUtil.equal(transaction.getTradeState(), Transaction.TradeStateEnum.SUCCESS)) {
//                // 订单号
//                Long orderId = Long.valueOf(transaction.getOutTradeNo());
//                // 微信支付号
//                String transactionId = transaction.getTransactionId();
//
//                // 支付成功，更新订单状态，以及对后续做处理
//                R<OrderInfoVo> orderInfoVoR = this.orderPayBll.paySuccessHandle(orderId, transactionId, JSONObject.toJSONString(transaction));
//                if (orderInfoVoR.getCode() == 0 && ObjectUtil.isNotEmpty(orderInfoVoR.getData())){
//                    orderBll.successOrder(orderInfoVoR.getData().getId(), true);
//                }
//            }
            return true;
        } catch (Exception e) {
            log.info("微信回调解签失败，参数：{}", wechatPayCallbackHandleBo);
        }

        return false;
    }

    @Override
    public boolean alipayPayCallbackHandle(Long orderPayId, String transactionId, String jsonString) {
        try {
            R<OrderInfoVo> orderInfoVoR = this.orderPayBll.paySuccessHandle(orderPayId, transactionId, JSONObject.toJSONString(jsonString));
            if (orderInfoVoR.getCode() == 0 && ObjectUtil.isNotEmpty(orderInfoVoR.getData())){
                orderBll.successOrder(orderInfoVoR.getData().getId(), true);
            }
            return true;
        } catch (Exception e) {
            log.info("微信回调解签失败，参数：{}", e.getMessage());
        }
        return false;
    }

    /**
     * 版本升级
     *
     * @param createClientOrder
     */
    @Override
    @CustomRedissonLock(key = "'pcUpgradeOrder_lock:' + (#args[0].userId != null ? #args[0].userId : T(com.jiuyu.replay.power.utils.GlobalObject).getLocalUser().id)")
    @Transactional
    public void pcUpgradeOrder(CreateClientOrder createClientOrder) {
        CreateOrderBo createOrderBo = BeanUtil.copyProperties(createClientOrder, CreateOrderBo.class);
        if (createOrderBo.getTrialOrder() == null) createOrderBo.setTrialOrder(0);
        if (createClientOrder.getUserId() != null) {
            R<UserInfoVo> info = userBll.info(createClientOrder.getUserId(), false);
            if (info.getCode() != 0 && ObjectUtil.isEmpty(info)) RRException.create("获取用户失败");
            UserInfoVo user = info.getData();
            if (!ObjectUtil.equals(user.getUserType(), UserEnums.userType.CLIENT_USER.getCode())) {
                RRException.create("用户类型不是主账号，开通失败");
            }
            createOrderBo.setUserId(user.getId());
            createOrderBo.setUserName(info.getData().getNickName());
        } else {
            UserCacheVo localUser = GlobalObject.getLocalUser();
            if (!ObjectUtil.equals(localUser.getUserType(), UserEnums.userType.CLIENT_USER.getCode())) {
                RRException.create("用户类型不是主账号，开通失败");
            }
            createOrderBo.setUserId(localUser.getId());
            createOrderBo.setUserName(localUser.getNickName());
        }
        OrderInfoVo orderInfoVo = orderFeign.currentOrderByUserId(createClientOrder.getUserId());
        RRException.isNotEmpty(orderInfoVo, "用户没有订单，不能升级");
        Integer level = orderInfoVo.getLevel();
        if (level == null) RRException.create("没有检查到当前用户存在套餐，不能升级");
        R<PackageInfoVo> info = packageBll.info(createOrderBo.getCommodityId());
        if (info == null || info.getCode() != 0 || info.getData() == null) RRException.create("商品不存在");
        if (info.getData().getLevel() <= 0){
            if (level >= info.getData().getLevel()){
                RRException.create("版本升级不能向下升级");
            }
            createOrderBo.setOrderType(0);
        }else {
            if (ObjectUtil.equal(level, info.getData().getLevel())) {
                RRException.create("版本等级相同，请走续费流程");
            }
            if (level > 0) {
                createOrderBo.setOrderType(1);
            } else {
                createOrderBo.setOrderType(2);
            }
        }

        // 检查子账号数据是否超出
        int subNum = userBll.getSubAccountNum(createClientOrder.getUserId());
        int orderSubNum = packageBll.getSubAccountNum(info.getData());
        orderBll.checkSubAccountNum(orderInfoVo.getUserId(), orderSubNum, subNum);
        createOrderBo.setDiscountRate(createClientOrder.getDiscountRate() == null ? 0 : createClientOrder.getDiscountRate());
        createOrderBo.setSource(1);
        createOrderBo.setPayPictures(createClientOrder.getPayPictures());
        // 下单
        CreateOrderVo order = orderBll.createOrder(createOrderBo);

        // 手动的单，直接订单完成接口
        if (ObjectUtil.isNotEmpty(order)) {
            orderBll.successOrder(order.getOrderId(), orderInfoVo.getId().equals(createOrderBo.getBeforeUpgrading()));

            // 分佣
            if (createOrderBo.getTrialOrder() == 0){
                selectCommissionMethod(createOrderBo.getUserId(), order.getOrderId());
            }

            // 记录当前的客户端版本
            userBll.saveClientVersionByLevel(createClientOrder.getUserId(), info.getData().getLevel());
        }
    }

    /**
     * 判断订单是否是可以分佣的订单
     * @param order
     * @return
     */
    private boolean hasCommissionOrder(OrderInfoVo order){
        // order不为null
        // 订单不是试用订单
        // 是版本
        // 等级大于0
        // 订单价格大于0
        // 状态是生效中或者待生效
        // 订单类型是升级订单、续费订单、免费升级订单
        return order != null &&
                order.getTrialOrder() == 0 &&
                order.getCommodityType() == 1 &&
                order.getLevel() > 0 &&
                order.getRealPrice() > 0 &&
                (order.getStatus() == 1 || order.getStatus() == 2) &&
                (order.getOrderType() == 1 || order.getOrderType() == 2 || order.getOrderType() == 3);
    }

    /**
     * 分佣-选择分佣，用户分佣还是代理商分佣
     * @param userId
     * @param orderId
     */
    public void selectCommissionMethod(Long userId, Long orderId){
        R<OrderInfoVo> orderR = orderBll.getById(orderId);
        if (orderR.getCode() == 0 && ObjectUtil.isNotEmpty(orderR.getData())) {
            OrderInfoVo order = orderR.getData();
            if (hasCommissionOrder(order)) {

                R<UserVo> userVoR = userBll.getById(userId);

                if (userVoR.getCode() == 0 && ObjectUtil.isNotEmpty(userVoR.getData())){

                    UserVo user = userVoR.getData();

                    if (ObjectUtil.isNotEmpty(user.getInviteUrlCode())){
                        com.jiuyu.replay.generic.vo.common.R<InviteUrlCodeInfoVo> inviteUrlCodeInfoVoR = inviteUrlCodeBll.getByInviteUrlCode(user.getInviteUrlCode());

                        if (inviteUrlCodeInfoVoR.getCode() == 0 && ObjectUtil.isNotEmpty(inviteUrlCodeInfoVoR.getData())){

                            InviteUrlCodeInfoVo data = inviteUrlCodeInfoVoR.getData();

                            if (data.getCodeType() == 2){
                                // TODO 用户分佣
                            }else{
                                // 代理商分佣
                                agentCommissionAllocation(orderR.getData(), user.getInviteUrlCode());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 订单分佣
     * 规则：
     * 1、是升级订单或者续费订单
     * 2、订单金额大于0
     * 3、新签分佣：用户第一次购买升级版本的开始时间到90天后，这90天算是新签用户
     * 4、续签分佣：过了新签时间后就算是续签
     * 5、断约用户：订单结束后的6个月内不算断约用户，6个月后算断约用户，断约用户在签约后重新分佣
     */
    public void agentCommissionAllocation(OrderInfoVo order, String inviteUrlCode){
        if (order == null) return;
        // 1、是版本 2、等级大于0 3、订单价格大于0 4、状态是生效中或者待生效 5、订单类型是升级订单、续费订单、免费升级订单
        if (hasCommissionOrder(order)) {
            // 判断是新签还是续签
            // 获取全部的订单
            R<List<OrderInfoVo>> orderList = orderBll.listAllByUserId(order.getUserId());

            if (orderList.getCode() == 0 && ObjectUtil.isNotEmpty(orderList.getData())){
                List<OrderInfoVo> orders = orderList.getData();

                // 获取历史订单，支持分佣的订单
                orders = orders.stream()
                        .filter(item -> !item.getId().equals(order.getId()) &&
                                        item.getTrialOrder() == 0 &&
                                        item.getCommodityType() == 1 &&
                                        item.getLevel() > 0 &&
                                        item.getRealPrice() > 0 &&
                                        (item.getStatus() != 0 && item.getStatus() != 6) &&
                                        (item.getOrderType() == 1 || item.getOrderType() == 2 || item.getOrderType() == 3)
                                )
                        .sorted(Comparator.comparing(OrderInfoVo::getStartDate))
                        .toList();

                // 获取新签的订单-基点
                Date now = new Date();
                R<SystemKvInfoVo> newRenewalIntervalR = systemKvBll.getByKey("new_renewal_interval");
                if (newRenewalIntervalR.getCode() != 0 || ObjectUtil.isEmpty(newRenewalIntervalR.getData())){
                    RRException.create("分佣失败，key:new_renewal_interval查询失败");
                    log.info("分佣失败，key:new_renewal_interval查询失败");
                    return;
                }
                Integer newRenewalInterval = NumberUtil.parseInt(newRenewalIntervalR.getData().getKvValue(), 90);
                // 断约用户天数，-1没有断约用户
                Integer breakAgreementNum = -1;
                R<SystemKvInfoVo> breakAgreementNumR = systemKvBll.getByKey("break_agreement_num");
                if (breakAgreementNumR.getCode() == 0 || ObjectUtil.isEmpty(breakAgreementNumR.getData())){
                    breakAgreementNum = NumberUtil.parseInt(breakAgreementNumR.getData().getKvValue(), 180);
                }

                // 获取新签分佣的开始时间
                Date startDate = now;
                for (int i = orders.size() - 1; i >= 0; i--) {
                    OrderInfoVo item = orders.get(i);

                    // 判断断约用户中的新签订单
                    long day = DateUtil.betweenDay(item.getRealEndDate(), now, true);
                    long day1 = DateUtil.betweenDay(item.getStartDate(), now, true);
                    if (breakAgreementNum != -1 &&
                            item.getRealEndDate().compareTo(now) < 0 &&
                            day >= breakAgreementNum &&
                            day1 >= newRenewalInterval + breakAgreementNum
                    ){
                        break;
                    }

                    // 如果只有一个订单，第一个那么就是新签的订单
                    if (i == 0){
                        startDate = item.getStartDate();
                        break;
                    }

                    // 判断断约用户中的续签订单
                    Date nextEndTime;
                    Date nextStartTime;
                    if (breakAgreementNum != -1){
                        nextEndTime = orders.get(i - 1).getRealEndDate();
                        nextStartTime = orders.get(i - 1).getStartDate();
                        long betweened = DateUtil.betweenDay(nextEndTime, item.getStartDate(), true);
                        long betweenedDay = DateUtil.betweenDay(nextStartTime, item.getRealEndDate(), true);
                        if (nextEndTime.compareTo(item.getStartDate()) < 0 &&
                                betweened >= breakAgreementNum &&
                                betweenedDay >= newRenewalInterval + breakAgreementNum
                        ){
                            startDate = item.getStartDate();
                            break;
                        }
                    }
                }

                // 判断当前的订单是新签和续签订单
                int commissionType = 0;
                if (startDate.compareTo(now) != 0 && DateUtil.betweenDay(startDate, now, false) >= newRenewalInterval){
                    commissionType = 1;
                }


                CommissionAllocationBo allocationBo = new CommissionAllocationBo();
                allocationBo.setInviteUrlCode(inviteUrlCode);
                allocationBo.setOrderId(order.getId());
                allocationBo.setOrderTotalMoney(order.getTotalPrice());
                allocationBo.setUserId(order.getUserId());
                allocationBo.setCommissionType(commissionType);
                allocationBo.setRemarks("");
                R<Boolean> booleanR = agentCommissionBll.commissionAllocation(allocationBo);
                if (booleanR.getCode() == 0 && booleanR.getData()){
                    orderBll.updateIsCommission(order.getId(), 1);
                }
            }

        }
    }

    /**
     * 版本续费
     *
     * @param createClientOrder
     */
    @Override
    @Transactional
    public void pcRenewalOrder(CreateClientOrder createClientOrder) {
        CreateOrderBo createOrderBo = BeanUtil.copyProperties(createClientOrder, CreateOrderBo.class);
        if (createOrderBo.getTrialOrder() == null) createOrderBo.setTrialOrder(0);
        if (createClientOrder.getUserId() != null) {
            R<UserInfoVo> info = userBll.info(createClientOrder.getUserId(), false);
            if (info.getCode() != 0 && ObjectUtil.isEmpty(info)) RRException.create("获取用户失败");
            UserInfoVo user = info.getData();
            if (!ObjectUtil.equals(user.getUserType(), UserEnums.userType.CLIENT_USER.getCode())) {
                RRException.create("用户类型不是主账号，开通失败");
            }
            createOrderBo.setUserId(user.getId());
            createOrderBo.setUserName(info.getData().getNickName());
        } else {
            UserCacheVo localUser = GlobalObject.getLocalUser();
            if (!ObjectUtil.equals(localUser.getUserType(), UserEnums.userType.CLIENT_USER.getCode())) {
                RRException.create("用户类型不是主账号，开通失败");
            }
            createOrderBo.setUserId(localUser.getId());
            createOrderBo.setUserName(localUser.getNickName());
        }
        createOrderBo.setOrderType(3);
        createOrderBo.setDiscountRate(createClientOrder.getDiscountRate() == null ? 0 : createClientOrder.getDiscountRate());
        createOrderBo.setSource(1);
        createOrderBo.setBeforeUpgrading(null);
        createOrderBo.setPayPictures(createClientOrder.getPayPictures());
        // 下单
        CreateOrderVo order = orderBll.createOrder(createOrderBo);

        // 手动的单，直接订单完成接口
        if (ObjectUtil.isNotEmpty(order)) {
            orderBll.successOrder(order.getOrderId(), false);
            // 分佣
            if (createOrderBo.getTrialOrder() == 0){
                selectCommissionMethod(createOrderBo.getUserId(), order.getOrderId());
            }
        }
    }

    /**
     * 购买增量包
     *
     * @param createClientOrder
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pcIncrementsOrder(CreateClientOrder createClientOrder) {
        CreateOrderBo createOrderBo = BeanUtil.copyProperties(createClientOrder, CreateOrderBo.class);
        UserCacheVo localUser = GlobalObject.getLocalUser();
        if (ObjectUtil.isEmpty(createOrderBo.getUserId())) {
            createOrderBo.setUserId(localUser.getId());
            createOrderBo.setUserName(localUser.getUsername());
        } else {
            R<UserInfoVo> info = userBll.info(createOrderBo.getUserId(), false);
            if (info.getCode() == 0 && info.getData() != null) {
                createOrderBo.setUserId(info.getData().getId());
                createOrderBo.setUserName(info.getData().getNickName());
            }
        }
        // 检查
        createOrderBo.setBeforeUpgrading(null);
        createOrderBo.setDiscountRate(0);
        createOrderBo.setSource(1);
        createOrderBo.setOrderType(4);
        createOrderBo.setCommodityType(0);
        createOrderBo.setPayPictures(createClientOrder.getPayPictures());
        // 下单
        CreateOrderVo order = orderBll.createOrder(createOrderBo);
        // 手动的单，直接订单完成接口
        if (ObjectUtil.isNotEmpty(order)) {
            orderBll.successOrder(order.getOrderId(), true);
        }
    }

    @Override
    public R<String> brushBoardOrder(Map<Integer, Long> map) {

        R<List<OrderInfoVo>> orderList1 = orderBll.getYearOrder2(map.values().stream().toList());
        Map<String, OrderInfoVo> userYesMap = orderList1.getData().stream()
                .collect(Collectors.toMap(item -> item.getUserId() + "_", Function.identity(), (a, b) -> b));

        R<List<OrderInfoVo>> orderList = orderBll.getYearOrder();

        if (ObjectUtil.isNotEmpty(orderList.getData())){
            for (OrderInfoVo orderInfoVo : orderList.getData()) {
                OrderInfoVo orderInfoVo1 = userYesMap.get(orderInfoVo.getUserId() + "_");
                if (orderInfoVo1 != null) continue;

                Long commodityId = map.get(orderInfoVo.getLevel());
                if (commodityId == null) continue;

                CreateClientOrder createClientOrder = new CreateClientOrder();
                createClientOrder.setCommodityId(commodityId);
                createClientOrder.setCommodityType(0);
                createClientOrder.setDiscountRate(0);
                createClientOrder.setUserId(orderInfoVo.getUserId());

                CreateOrderBo createOrderBo = BeanUtil.copyProperties(createClientOrder, CreateOrderBo.class);
                UserCacheVo localUser = GlobalObject.getLocalUser();
                if (ObjectUtil.isEmpty(createOrderBo.getUserId())) {
                    createOrderBo.setUserId(localUser.getId());
                    createOrderBo.setUserName(localUser.getUsername());
                } else {
                    R<UserInfoVo> info = userBll.info(createOrderBo.getUserId(), false);
                    if (info.getCode() == 0 && info.getData() != null) {
                        createOrderBo.setUserId(info.getData().getId());
                        createOrderBo.setUserName(info.getData().getNickName());
                    }
                }
                // 检查
                createOrderBo.setBeforeUpgrading(null);
                createOrderBo.setDiscountRate(0);
                createOrderBo.setSource(1);
                createOrderBo.setOrderType(4);
                createOrderBo.setCommodityType(0);
                // 下单
                CreateOrderVo order = orderBll.createOrder(createOrderBo);
                // 手动的单，直接订单完成接口
                if (ObjectUtil.isNotEmpty(order)) {
                    orderBll.successOrder2(order.getOrderId(), new Date(), orderInfoVo.getEndDate());
                }

            }
        }

        return R.ok("ok");
    }

    /**
     * 新下的订单
     *
     * @param userId
     * @return
     */
    @Override
    @Transactional
    public R<String> newPcCreateOrder(Long userId) {
        if (ObjectUtil.isEmpty(userId)) RRException.create("用户不能为空");
        CreateOrderBo createOrderBo = new CreateOrderBo();
        createOrderBo.setUserId(userId);
        UserCacheVo localUser = GlobalObject.getLocalUser();
        if (ObjectUtil.isEmpty(createOrderBo.getUserId())) {
            createOrderBo.setUserId(localUser.getId());
            createOrderBo.setUserName(localUser.getUsername());
        } else {
            R<UserInfoVo> info = userBll.info(createOrderBo.getUserId(), false);
            if (info.getCode() == 0 && info.getData() != null) {
                createOrderBo.setUserId(info.getData().getId());
                createOrderBo.setUserName(info.getData().getNickName());
            }
        }
        return orderBll.createNewOrder(createOrderBo);
    }

    /**
     * 新下的订单
     *
     * @param userId
     * @return
     */
    @Override
    @Transactional
    public R<String> newPcCreateInitOrder(Long userId) {
        if (ObjectUtil.isEmpty(userId)) RRException.create("用户不能为空");
        CreateOrderBo createOrderBo = new CreateOrderBo();
        createOrderBo.setUserId(userId);
        UserCacheVo localUser = GlobalObject.getLocalUser();
        if (ObjectUtil.isEmpty(createOrderBo.getUserId())) {
            createOrderBo.setUserId(localUser.getId());
            createOrderBo.setUserName(localUser.getUsername());
        } else {
            R<UserInfoVo> info = userBll.info(createOrderBo.getUserId(), false);
            if (info.getCode() == 0 && info.getData() != null) {
                createOrderBo.setUserId(info.getData().getId());
                createOrderBo.setUserName(info.getData().getNickName());
            }
        }
        return orderBll.createNewInitOrder(createOrderBo);
    }

    /**
     * 邀请码创建订单
     *
     * @param userId
     * @param packageId
     * @param commodityPriceId
     * @return
     */
    @Override
    public R<String> invitationCodeCreateOrder(Long userId, Long packageId, Long commodityPriceId) {
        if (ObjectUtil.isEmpty(userId)) RRException.create("用户不能为空");
        if (ObjectUtil.isEmpty(packageId)) RRException.create("版本不能为空");
        if (ObjectUtil.isEmpty(commodityPriceId)) RRException.create("价格不能为空");
        CreateOrderBo createOrderBo = new CreateOrderBo();
        createOrderBo.setUserId(userId);
        R<UserInfoVo> info = userBll.info(createOrderBo.getUserId(), false);
        if (info.getCode() == 0 && info.getData() != null) {
            createOrderBo.setUserId(info.getData().getId());
            createOrderBo.setUserName(info.getData().getNickName());
        }
        createOrderBo.setBeforeUpgrading(null);
        createOrderBo.setCommodityId(packageId);
        createOrderBo.setCommodityPriceId(commodityPriceId);
        createOrderBo.setDiscountRate(0);
        createOrderBo.setSource(2);
        createOrderBo.setOrderType(5);
        createOrderBo.setCommodityType(1);
        // 下单
        CreateOrderVo order = orderBll.createOrder(createOrderBo);
        // 完成订单-更新数据库
        // 手动的单，直接订单完成接口
        if (ObjectUtil.isNotEmpty(order)) {
            orderBll.successOrder(order.getOrderId(), true);
        }
        return R.ok("下单成功");
    }

    @Override
    public R<String> createOrder(CreateOrderBo orderBo) {
        // 下单
        CreateOrderVo order = orderBll.createOrder(orderBo);
        orderBll.successOrder(order.getOrderId(), true);
        return R.ok("下单成功");
    }

    @Override
    public R<List<OrderInfoVo>> getOrderByUserId(Long userId) {
        return orderBll.getOrderByUserId(userId);
    }

    @Override
    @Transactional
    public R<String> checkUserOrder(Long subUserId) {
        log.info("检查用户订单, userid={}", subUserId);
        Long parentId = ResultUtil.getResult(userApi.getUserParentId(subUserId));
        log.info("检查用户的父账号, parentId={}", parentId);
        if (ObjectUtil.isEmpty(parentId)) {
            R<List<OrderInfoVo>> orderByUserId = orderBll.getOrderByUserId(subUserId);
            if (orderByUserId.getCode() == 0) {
                if (ObjectUtil.isNotEmpty(orderByUserId.getData())) {
                    List<OrderInfoVo> orderList = orderByUserId.getData();
                    // 判断是否有正在使用的版本
                    if (orderList.stream().noneMatch(o -> o.getCommodityType() == 1 && o.getStatus() == 2)) {
                        // 没有，就下免费的版本
                        this.newPcCreateOrder(subUserId);
                    }
                } else {
                    // 没有，就下免费的版本
                    this.newPcCreateOrder(subUserId);
                }
            }
        }
        return R.ok("完成");
    }

    @Override
    @Transactional
    public R<String> orderStop(Long orderId) {
        R<OrderInfoVo> orderR = orderBll.getById(orderId);
        if (orderR.getCode() == 0 && orderR.getData() != null){
            OrderInfoVo order = orderR.getData();
            // 手动取消订单
            orderBll.orderStop(orderId, null, order.getCommodityType() == 1 && order.getStatus() == 2, 8);
            log.info("订单取消成功，取消的管理员账号为：{}", JSONUtil.toJsonStr(GlobalObject.getLocalUser()));
            if (order.getCommodityType() == 1 && order.getStatus() == 2){
                log.info("查看是否有续费的订单，如果有，就重新生成开始时间和结束时间");
                OrderInfoVo vo = new OrderInfoVo();
                vo.setBeforeUpgrading(orderId);
                vo.setEndDate(DateUtil.parse(DateUtil.now()));
                vo.setUserId(order.getUserId());
                orderBll.checkRenewalOrder(vo);

                // 清空用户弹幕监控和自动录制
                // 查询用户的子账号
                List<Long> userIds = new ArrayList<>();
                userIds.add(order.getUserId());
                R<List<Long>> subUserListR = userBll.getUserChild(List.of(order.getUserId()));
                if (subUserListR.getCode() == 0 && ObjectUtil.isNotEmpty(subUserListR.getData())) userIds.addAll(subUserListR.getData());
                userIds.forEach(userId -> {
                    // 清空用户弹幕监控和自动录制
                    userPropertyBll.updateByPropertyNum(userId, "anchorBarrageNum", 0L);
                });
                anchorUrlBll.closeAnchorBarrageNum(userIds);
//                anchorUrlBll.closeAutoUploadCloud(userIds);

                if (!orderBll.startCurrentOrder(order.getUserId())) {
                    System.out.println("订单取消成功，但是没有启动订单");
                    // 解冻订单
                    orderBll.unfreezeOrderByUserId(order.getUserId());
                    // 统计用户资源
                    userPropertyBll.statisticsUserProperty(order.getUserId());
                }
            }else{
                // 统计用户资源
                userPropertyBll.statisticsUserProperty(order.getUserId());
            }
        }

        return R.ok("订单取消成功");
    }

    @Override
    public R<String> orderEdit(CreateClientOrder createClientOrder) {
        CreateOrderBo createOrderBo = BeanUtil.copyProperties(createClientOrder, CreateOrderBo.class);
        if (createClientOrder.getUserId() != null) {
            R<UserInfoVo> info = userBll.info(createClientOrder.getUserId(), false);
            if (info.getCode() != 0 && ObjectUtil.isEmpty(info)) RRException.create("获取用户失败");
            createOrderBo.setUserId(info.getData().getId());
            createOrderBo.setUserName(info.getData().getNickName());
        } else {
            UserCacheVo localUser = GlobalObject.getLocalUser();
            createOrderBo.setUserId(localUser.getId());
            createOrderBo.setUserName(localUser.getNickName());
        }
        OrderInfoVo orderInfoVo = orderFeign.currentOrderByUserId(createClientOrder.getUserId());
        RRException.isNotEmpty(orderInfoVo, "没有检查到当前用户存在套餐，不能升级");
        Integer level = orderInfoVo.getLevel();
        if (level == null) RRException.create("没有检查到当前用户存在套餐，不能升级");
        R<PackageInfoVo> info = packageBll.info(createOrderBo.getCommodityId());
        if (info == null || info.getCode() != 0 || info.getData() == null) RRException.create("商品不存在");
        if (info.getData().getLevel() <= 0) RRException.create("不能修改为免费的版本");
        createOrderBo.setOrderType(6);
        createOrderBo.setDiscountRate(createClientOrder.getDiscountRate() == null ? 0 : createClientOrder.getDiscountRate());
        createOrderBo.setSource(1);
        // 下单
        CreateOrderVo order = orderBll.createOrder(createOrderBo);

        // 手动的单，直接订单完成接口
        if (ObjectUtil.isNotEmpty(order)) {
            orderBll.successOrder(order.getOrderId(), orderInfoVo.getId().equals(createOrderBo.getBeforeUpgrading()));
        }
        return R.ok("订单编辑成功");
    }

    @Override
    public R<UserVersionOrderVo> userVersionOrder(Long userId) {
        if (userId == null){
            UserCacheVo user = GlobalObject.getLocalUser();
            if (user != null){
                userId = user.getId();
            }
        }
        return orderBll.userVersionOrder(userId);
    }

    @Override
    public R<OrderInfoVo> currentUserStayOrder(Long userId) {
        if (userId == null){
            UserCacheVo user = GlobalObject.getLocalUser();
            userId = user.getId();
        }
        return orderBll.currentUserStayOrder(userId);
    }

    @Override
    @Transactional
    public R<String> closeOrder(Long orderId) throws AlipayApiException {
        R<OrderInfoVo> info = orderBll.info(orderId);
        // 查询当前的订单是否已经支付过了
        if (info.getCode() == 0 && ObjectUtil.isNotEmpty(info.getData())){
            OrderInfoVo order = info.getData();
            if (order.getStatus() == 0){
                OrderPayInfoVo orderPay = order.getOrderPay();
                if (ObjectUtil.isEmpty(orderPay)) RRException.create("订单查询失败");
                // 查询订单的支付状态
//                R<QueryOrderStatus> queryOrderStatusR = payBll.queryOrderStatus(orderPay.getId(), orderPay.getPayType());
//                if (queryOrderStatusR.getCode() == 0){
//                    QueryOrderStatus queryOrderStatus = queryOrderStatusR.getData();
//                    if (queryOrderStatus.getStatus() != 0){
//                        // 关闭订单
//                        R<Boolean> bR = payBll.closeOrder(orderPay.getId(), orderPay.getPayType());
//                        orderBll.closeOrder(orderId, bR.getData());
//                        return R.ok("订单关闭成功");
//                    }else{
//                        R<OrderInfoVo> orderInfoVoR = orderPayBll.paySuccessHandle(orderPay.getId(), null, null);
//                        if (orderInfoVoR.getCode() == 0 && ObjectUtil.isNotEmpty(orderInfoVoR.getData())){
//                            orderBll.successOrder(orderInfoVoR.getData().getId(), true);
//                        }
//                        return R.error(3001, "订单已经生效，不能关闭");
//                    }
//                }
            }else if (order.getStatus() == 6){
                log.info("订单已经关闭，在重复关闭");
            }else{
                RRException.create("订单已经生效，不能关闭");
            }
        }else{
            RRException.create("订单信息获取失败");
        }
        return null;
    }

    @Override
    @Transactional
    public R<String> updateFreeVersion(Long userId) {
        UserCacheVo user = GlobalObject.getLocalUser();
        if (userId != null && !Objects.equals(user.getId(), userId)){
            R<UserInfoVo> info = userBll.info(userId, false);
            if (info.getData().getUserType() != 0){
                return R.ok("非法用户类型，不能操作");
            }
        }else{
            if (user.getUserType() != 0){
                return R.ok("非法用户类型，不能操作");
            }
            userId = user.getId();
        }
        R<UserVersionOrderVo> userVersionOrderVoR = userVersionOrder(userId);
        if (userVersionOrderVoR.getCode() == 0 && ObjectUtil.isNotEmpty(userVersionOrderVoR.getData())){
            UserVersionOrderVo orderVo = userVersionOrderVoR.getData();
            if (orderVo.getLevel() != -1){
                return R.ok("当前版本不是激活版，操作无效");
            }
        }
        R<PackageInfoVo> gratisPackage = packageBll.getGratisPackage();
        if (gratisPackage.getCode() != 0 || ObjectUtil.isEmpty(gratisPackage.getData()))RRException.create("获取免费套餐失败");
        PackageInfoVo packageData = gratisPackage.getData();
        CreateClientOrder createClientOrder = new CreateClientOrder();
        createClientOrder.setUserId(userId);
        createClientOrder.setCommodityId(packageData.getId());
        createClientOrder.setCommodityPriceId(packageData.getCommodityPriceList().get(0).getId());
        createClientOrder.setCommodityType(1);
        pcUpgradeOrder(createClientOrder);
        return R.ok("操作成功");
    }

    /**
     * 将请求体转成字符串
     *
     * @param request request
     * @return 返回请求问
     * @throws IOException io
     */
    private String getRequestBody(HttpServletRequest request) throws IOException {
        ServletInputStream stream;
        BufferedReader reader = null;
        StringBuffer sb = new StringBuffer();
        try {
            stream = request.getInputStream();
            // 获取响应
            reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new IOException("读取返回支付接口数据流出现异常！");
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return sb.toString();
    }
}

