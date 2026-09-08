package com.jiuyu.replay.order.bll;

import cn.hutool.core.bean.BeanUtil;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bo.OrderBo;
import com.jiuyu.replay.order.bo.OrderPayBo;
import com.jiuyu.replay.order.bo.OrderPayListBo;
import com.jiuyu.replay.order.producer.OrderPayProducer;
import com.jiuyu.replay.order.producer.OrderProducer;
import com.jiuyu.replay.order.producer.TypeSurplusProducer;
import com.jiuyu.replay.order.producer.UserPropertyProducer;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderPayInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderPayListVo;
import jakarta.annotation.Resource;
import lombok.Synchronized;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


/**
 * 订单-支付信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Component
public class OrderPayBll {

    @Resource
    private OrderPayProducer orderPayProducer;

    @Resource
    private OrderProducer orderProducer;

    @Resource
    private UserPropertyProducer userPropertyProducer;

    @Resource
    private TypeSurplusProducer typeSurplusProducer;


    /**
     * 订单-支付信息列表
     *
     * @param orderPayListBo 订单-支付信息列表查询参数
     * @return
     */
    public R<PageUtils<OrderPayListVo>> queryPage(OrderPayListBo orderPayListBo) {

        return R.ok("获取成功", orderPayProducer.queryPage(orderPayListBo));
    }

    /**
     * 订单-支付信息信息
     *
     * @param id 订单-支付信息id
     * @return
     */
    public R<OrderPayInfoVo> info(java.lang.Long id) {

        OrderPayInfoVo orderPayInfoVo = orderPayProducer.info(id);
        return R.ok("获取成功", orderPayInfoVo);
    }

    /**
     * 新增订单-支付信息
     *
     * @param orderPayBo 订单-支付信息对象
     * @return
     */
    public R<String> save(OrderPayBo orderPayBo) {

        OrderPayInfoVo orderPayInfoVo = orderPayProducer.save(orderPayBo);
        return R.ok("添加成功");
    }

    /**
     * 修改订单-支付信息
     *
     * @param orderPayBo 订单-支付信息对象
     * @return
     */
    public R<String> update(OrderPayBo orderPayBo) {

        orderPayProducer.update(orderPayBo);
        return R.ok("修改成功");
    }

    /**
     * 删除订单-支付信息
     *
     * @param id 订单-支付信息id
     * @return
     */
    public R<String> delete(java.lang.Long id) {

        orderPayProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 获取有效订单列表,用于项目启动时把待支付的订单添加到redis中
     * @return
     */
    public R<List<OrderPayInfoVo>> getValidOrders(){
        return R.ok("", orderPayProducer.getValidOrders());
    }

    /**
     * 订单超时处理
     * @param orderPayId
     */
    @Transactional
    public void orderExpired(Long orderPayId) {
        R<OrderPayInfoVo> info = this.info(orderPayId);
        if (info.getCode() == 0 && info.getData() != null) {
            OrderPayInfoVo orderPayInfoVo = info.getData();
            if (orderPayInfoVo.getPayStatus() == 0) {
                // 修改支付单状态
                orderPayInfoVo.setPayStatus(2);
                this.update(BeanUtil.copyProperties(orderPayInfoVo, OrderPayBo.class));

                // 修改订单状态
                OrderInfoVo order = orderProducer.info(orderPayInfoVo.getOrderId());
                if (order.getStatus() == 0) {
                    order.setStatus(6);
                    orderProducer.update(BeanUtil.copyProperties(order, OrderBo.class));
                }
            }
        }
    }


    /**
     * 支付成功处理
     * @param orderPayId
     * @param transactionId
     * @param jsonString
     */
    @Transactional
    @Synchronized
    public R<OrderInfoVo> paySuccessHandle(Long orderPayId, String transactionId, String jsonString) {
        OrderPayInfoVo orderPayInfoVo = orderPayProducer.info(orderPayId);

        OrderInfoVo order = orderProducer.info(orderPayInfoVo.getOrderId());
        if (!(order.getStatus() == 0 || order.getStatus() == 6)){
            return R.ok();
        }

        // 修改支付单状态
        OrderPayBo orderPayBo = new OrderPayBo();
        orderPayBo.setId(orderPayInfoVo.getId());
        orderPayBo.setPayStatus(1);
        orderPayBo.setThirdOrderNum(transactionId);
        orderPayBo.setPayDate(new Date());
        orderPayBo.setThirdCallbackContent(jsonString);
        orderPayBo.setUpdateDate(new Date());
        orderPayProducer.update(orderPayBo);
        return R.ok(order);

    }
}