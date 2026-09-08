package com.jiuyu.replay.api.logic.order;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;


import com.jiuyu.replay.generic.vo.order.OrderExtendInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderExtendListVo;
import com.jiuyu.replay.order.bo.OrderExtendBo;
import com.jiuyu.replay.order.bo.OrderExtendListBo;


/**
 * 订单的扩展表
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-07-05 09:56:24
 */
public interface OrderExtendLogic {


    /**
     * 订单的扩展表列表
     * @param orderExtendListBo 订单的扩展表列表查询参数
     * @return
     */
    R<PageUtils<OrderExtendListVo>> queryPage(OrderExtendListBo orderExtendListBo);

    /**
    * 订单的扩展表信息
    * @param id 订单的扩展表id
    * @return
    */
    R<OrderExtendInfoVo> info(Long id);

    /**
     * 新增订单的扩展表
     * @param orderExtendBo 订单的扩展表对象
     * @return
     */
    R<String> save(OrderExtendBo orderExtendBo);

    /**
     * 修改订单的扩展表
     * @param orderExtendBo 订单的扩展表对象
     * @return
     */
    R<String> update(OrderExtendBo orderExtendBo);

    /**
     * 删除订单的扩展表
     * @param id 订单的扩展表id
     * @return
     */
    R<String> delete(Long id);


}

