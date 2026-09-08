package com.jiuyu.replay.order.rse;

import com.jiuyu.replay.order.bo.CommodityBo;

import java.util.List;

/**
 * @author RayChou
 * @date 2025/5/30 9:47
 */
public interface CommodityRse {

    /**
     * 获取所有有效的商品信息
     *
     * @return
     */
    List<CommodityBo> listAll();
}
