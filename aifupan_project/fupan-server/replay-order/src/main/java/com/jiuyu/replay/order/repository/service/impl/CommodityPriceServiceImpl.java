package com.jiuyu.replay.order.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.order.repository.dao.CommodityPriceDao;
import com.jiuyu.replay.order.entity.CommodityPriceEntity;
import com.jiuyu.replay.order.repository.service.CommodityPriceService;


@Service("commodityPriceService")
public class CommodityPriceServiceImpl extends ServiceImpl<CommodityPriceDao, CommodityPriceEntity> implements CommodityPriceService {



}