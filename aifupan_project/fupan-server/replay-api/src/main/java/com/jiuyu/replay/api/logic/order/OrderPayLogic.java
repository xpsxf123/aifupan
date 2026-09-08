package com.jiuyu.replay.api.logic.order;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.order.OrderPayListVo;
import com.jiuyu.replay.generic.vo.order.OrderPayInfoVo;
import com.jiuyu.replay.order.bo.OrderPayBo;
import com.jiuyu.replay.order.bo.OrderPayListBo;


/**
 * 订单-支付信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
public interface OrderPayLogic {


    /**
     * 订单-支付信息列表
     * @param orderPayListBo 订单-支付信息列表查询参数
     * @return
     */
    R<PageUtils<OrderPayListVo>> queryPage(OrderPayListBo orderPayListBo);

    /**
    * 订单-支付信息信息
    * @param id 订单-支付信息id
    * @return
    */
    R<OrderPayInfoVo> info(Long id);

    /**
     * 新增订单-支付信息
     * @param orderPayBo 订单-支付信息对象
     * @return
     */
    R<String> save(OrderPayBo orderPayBo);

    /**
     * 修改订单-支付信息
     * @param orderPayBo 订单-支付信息对象
     * @return
     */
    R<String> update(OrderPayBo orderPayBo);

    /**
     * 删除订单-支付信息
     * @param id 订单-支付信息id
     * @return
     */
    R<String> delete(Long id);


}

