package com.jiuyu.replay.order.rse;


import com.jiuyu.replay.generic.vo.order.CommodityTypeInfoVo;

import java.util.List;

public interface CommodityTypeRse {

    /**
     * 获取全部商品类型列表
     * @return
     */
    List<CommodityTypeInfoVo> listAll();
}
