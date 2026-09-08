package com.jiuyu.replay.generic.feign.order;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.CommodityTypeInfoVo;

import java.util.List;

public interface CommodityTypeFeign {

    /**
     * 获取全部商品类型信息列表
     * @return
     */
    R<List<CommodityTypeInfoVo>> listAll();
}
