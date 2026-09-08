package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.order.bo.OrderDetailBo;
import com.jiuyu.replay.order.bo.OrderPayBo;
import com.jiuyu.replay.order.bo.OrderPayListBo;
import com.jiuyu.replay.generic.vo.order.OrderPayInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderPayListVo;

import java.util.List;


/**
 * 订单-支付信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
public interface OrderPayProducer {


    /**
     * 订单-支付信息列表
     * @param orderPayListBo 订单-支付信息列表查询参数
     * @return
     */
    PageUtils<OrderPayListVo> queryPage(OrderPayListBo orderPayListBo);

    /**
    * 订单-支付信息信息
    * @param id 订单-支付信息id
    * @return
    */
    OrderPayInfoVo info(Long id);

    /**
     * 新增订单-支付信息
     * @param orderPayBo 订单-支付信息对象
     * @return
     */
     OrderPayInfoVo save(OrderPayBo orderPayBo);

    /**
     * 修改订单-支付信息
     * @param orderPayBo 订单-支付信息对象
     * @return
     */
    void update(OrderPayBo orderPayBo);

    /**
     * 删除订单-支付信息
     * @param id 订单-支付信息id
     * @return
     */
    void deleteById(Long id);


    /**
     * 批量新增订单详情
     * @param orderDetailList
     */
    void saveBatch(List<OrderDetailBo> orderDetailList);

    /**
     * 获取有效订单
     *
     * @return
     */
    List<OrderPayInfoVo> getValidOrders();

}

