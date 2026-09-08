package com.jiuyu.replay.api.logic.order.impl;

import com.jiuyu.replay.api.logic.order.OrderExtendLogic;


import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.OrderExtendInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderExtendListVo;
import com.jiuyu.replay.order.bo.OrderExtendBo;
import com.jiuyu.replay.order.bo.OrderExtendListBo;

import com.jiuyu.replay.order.bll.OrderExtendBll;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;


/**
 * 订单的扩展表
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-07-05 09:56:24
 */
@Service
public class OrderExtendLogicImpl implements OrderExtendLogic {

    @Resource
    private OrderExtendBll orderExtendBll;


    @Override
    public R<PageUtils<OrderExtendListVo>> queryPage(OrderExtendListBo orderExtendListBo) {

        return orderExtendBll.queryPage(orderExtendListBo);
    }

    @Override
    public R<OrderExtendInfoVo> info(Long id) {

        return orderExtendBll.info(id);
    }

    @Override
    public R<String> save(OrderExtendBo orderExtendBo) {

        return orderExtendBll.save(orderExtendBo);
    }

    @Override
    public R<String> update(OrderExtendBo orderExtendBo) {

        return orderExtendBll.update(orderExtendBo);
    }

    @Override
    public R<String> delete(Long id) {

        return orderExtendBll.delete(id);
    }


}

