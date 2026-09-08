package com.jiuyu.replay.order.rse.impl;

import com.jiuyu.replay.order.bo.CommodityBo;
import com.jiuyu.replay.order.repository.service.CommodityService;
import com.jiuyu.replay.order.rse.CommodityRse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author RayChou
 * @date 2025/5/30 9:47
 */
@Service
public class CommodityRseImpl implements CommodityRse {

    @Resource
    CommodityService commodityService;


    @Override
    public List<CommodityBo> listAll() {
        return commodityService.listAll();
    }
}
