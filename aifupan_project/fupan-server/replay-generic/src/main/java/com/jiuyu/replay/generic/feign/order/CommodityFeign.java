package com.jiuyu.replay.generic.feign.order;

import com.jiuyu.replay.generic.dto.order.CommodityDto;

import java.util.List;

/**
 * @author RayChou
 * @date 2025/5/30 9:41
 */
public interface CommodityFeign {

    /**
     * 获取所有商品集合
     *
     * @return
     */
    List<CommodityDto> listAll();
}
