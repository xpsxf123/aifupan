package com.jiuyu.replay.api.logic.order.impl;

import com.jiuyu.replay.api.logic.order.OrderPayLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bll.OrderPayBll;
import com.jiuyu.replay.order.bo.OrderPayBo;
import com.jiuyu.replay.order.bo.OrderPayListBo;
import com.jiuyu.replay.generic.vo.order.OrderPayInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderPayListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 订单-支付信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Service
public class OrderPayLogicImpl implements OrderPayLogic {

    @Resource
    private OrderPayBll orderPayBll;


    @Override
    public R<PageUtils<OrderPayListVo>> queryPage(OrderPayListBo orderPayListBo) {

        return orderPayBll.queryPage(orderPayListBo);
    }

    @Override
    public R<OrderPayInfoVo> info(Long id) {

        return orderPayBll.info(id);
    }

    @Override
    public R<String> save(OrderPayBo orderPayBo) {

        return orderPayBll.save(orderPayBo);
    }

    @Override
    public R<String> update(OrderPayBo orderPayBo) {

        return orderPayBll.update(orderPayBo);
    }

    @Override
    public R<String> delete(Long id) {

        return orderPayBll.delete(id);
    }


}

