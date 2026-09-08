package com.jiuyu.replay.api.controller.order;

import com.alipay.api.AlipayApiException;
import com.jiuyu.replay.api.annotation.UserLock;
import com.jiuyu.replay.api.logic.order.OrderLogic;
import com.jiuyu.replay.api.task.OrderScheduledTasks;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.RedisOperationUtils;
import com.jiuyu.replay.order.bo.*;
import com.jiuyu.replay.order.repository.dao.OrderDao;
import com.jiuyu.replay.order.vo.CreateOrderVo;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderListVo;
import com.jiuyu.replay.order.vo.UserVersionOrderVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;


/**
 * 订单
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@RestController
@CrossOrigin
@RequestMapping("replay/order")
@Tag(name = "订单")
@AllArgsConstructor
public class OrderController {

    private final OrderLogic orderLogic;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderDao orderDao;
    private final OrderScheduledTasks orderScheduledTasks;

    /**
     * 订单列表
     * @param orderListBo 订单列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "订单列表")
    public R<PageUtils<OrderListVo>> list(@Parameter(description = "订单列表查询参数", required = true) @RequestBody OrderListBo orderListBo){
        return orderLogic.queryPage(orderListBo);
    }


    /**
     * 订单信息
     * @param id 订单id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "订单信息")
    public R<OrderInfoVo> info(@Parameter(description = "订单id", required = true) @RequestParam("id") java.lang.Long id){

        return orderLogic.info(id);
    }

    /**
     * 新增订单
     * @param orderBo 订单对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增订单")
    public R<String> save(@Parameter(description = "订单对象", required = true) @RequestBody OrderBo orderBo){

        return orderLogic.save(orderBo);
    }


    @GetMapping("/getOrderByUserId")
    @Operation(summary = "查询用户的在用订单")
    public R<List<OrderInfoVo>> getOrderByUserId(@Parameter(description = "用户id", required = true) Long userId){
        return orderLogic.getOrderByUserId(userId);
    }

    @GetMapping("/userVersionOrder")
    @Operation(summary = "获取用户当前的版本订单-多合一")
    public R<UserVersionOrderVo> userVersionOrder(@Parameter(description = "用户id") @RequestParam(required = false) Long userId){
        return orderLogic.userVersionOrder(userId);
    }

    /**
     * 修改订单
     * @param orderBo 订单对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改订单")
    public R<String> update(@Parameter(description = "订单对象", required = true) @RequestBody OrderBo orderBo){

        return orderLogic.update(orderBo);
    }

    /**
     * 删除订单
     * @param id 订单id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除订单")
    public R<String> delete(@Parameter(description = "订单id", required = true) @RequestParam("id") java.lang.Long id){

        return orderLogic.delete(id);
    }

//    @GetMapping("/deleteOrderCache")
//    @Operation(summary = "删除订单缓存")
//    public R<String> deleteOrderCache(){
//        Set<String> keys = RedisOperationUtils.scanKeys("replay:user-order:*");
//        Set<String> keys1 = RedisOperationUtils.scanKeys("replay:user-property*");
//        keys.addAll(keys1);
//        keys.forEach(key -> redisTemplate.delete(key));
//        orderDao.deleteMy1();
//        orderDao.deleteMy2();
//        orderDao.deleteMy3();
//        orderDao.deleteMy4();
//        return R.ok();
//    }

    @GetMapping("/newPcCreateOrder")
    @Operation(summary = "新下的订单（之前是没有的）")
    public R<String> newPcCreateOrder(Long userId){
        return orderLogic.newPcCreateInitOrder(userId);
    }

    @GetMapping("/updateFreeVersion")
    @Operation(summary = "从激活版升级到免费版")
    @UserLock
    public R<String> updateFreeVersion(Long userId){
        return orderLogic.updateFreeVersion(userId);
    }

    @GetMapping("/invitationCodeCreateOrder")
    @Operation(summary = "邀请码创建订单")
    public R<String> invitationCodeCreateOrder(Long userId, Long packageId, Long commodityPriceId){
        return orderLogic.invitationCodeCreateOrder(userId, packageId, commodityPriceId);
    }

    @PostMapping("/pcUpgradeOrder")
    @Operation(summary = "版本升级")
    public R<String> pcUpgradeOrder(@RequestBody CreateClientOrder createClientOrder){
        orderLogic.pcUpgradeOrder(createClientOrder);
        return R.ok("版本升级成功");
    }

    @PostMapping("/pcRenewalOrder")
    @Operation(summary = "版本续费")
    public R<String> pcRenewalOrder(@RequestBody CreateClientOrder createClientOrder){
        orderLogic.pcRenewalOrder(createClientOrder);
        return R.ok("版本续费成功");
    }

    @PostMapping("/pcIncrementsOrder")
    @Operation(summary = "购买增量包")
    public R<String> pcIncrementsOrder(@RequestBody CreateClientOrder createClientOrder){
        orderLogic.pcIncrementsOrder(createClientOrder);
        return R.ok("增量包购买成功");
    }

    @PostMapping("/createOrder")
    @Operation(summary = "创建订单")
    public R<String> createOrder(@RequestBody CreateOrderBo orderBo){
        return orderLogic.createOrder(orderBo);
    }

    @GetMapping("/orderStop")
    @Operation(summary = "取消订单")
    public R<String> orderStop(Long orderId){
        return orderLogic.orderStop(orderId);
    }

    @PostMapping("/orderEdit")
    @Operation(summary = "订单编辑-订单修改")
    public R<String> orderEdit(@RequestBody CreateClientOrder createClientOrder){
        return orderLogic.orderEdit(createClientOrder);
    }



    @PostMapping("/createOnlineOrder")
    @Operation(summary = "创建在线支付订单")
    @UserLock
    public R<CreateOrderVo> createClientOrder(@RequestBody OnlinePayOrderBo onlinePayOrderBo) {
        return orderLogic.createClientOrder(onlinePayOrderBo);
    }

    @GetMapping("/OrderScheduledTasks")
    @Operation(summary = "定时查询订单")
    public R<String> OrderScheduledTasks(){
        orderScheduledTasks.batchSaveAudioLog();
        return R.ok("完成");
    }

    @GetMapping("/currentUserStayOrder")
    @Operation(summary = "获取用户的待支付订单信息")
    public R<OrderInfoVo> currentUserStayOrder(@Parameter(description = "用户id")@RequestParam(required = false) Long userId){
        return orderLogic.currentUserStayOrder(userId);
    }

    @GetMapping("/closeOrder")
    @Operation(summary = "订单关闭")
    public R<String> closeOrder(@Parameter(description = "订单id") Long orderId) throws AlipayApiException {
        return orderLogic.closeOrder(orderId);
    }


}
