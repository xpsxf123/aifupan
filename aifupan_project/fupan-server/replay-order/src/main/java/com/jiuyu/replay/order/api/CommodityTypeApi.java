package com.jiuyu.replay.order.api;

import com.jiuyu.replay.generic.feign.order.CommodityTypeFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.CommodityTypeInfoVo;
import com.jiuyu.replay.order.bll.CommodityTypeBll;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommodityTypeApi implements CommodityTypeFeign {

    @Resource
    private CommodityTypeBll commodityTypeBll;

    @Override
    public R<List<CommodityTypeInfoVo>> listAll() {

        return this.commodityTypeBll.listAll();

    }
}
