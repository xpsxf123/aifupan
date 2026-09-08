package com.jiuyu.replay.order.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.order.entity.CommodityTypeEntity;

import java.util.List;

/**
 * 商品类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-19 20:52:26
 */
public interface CommodityTypeService extends IService<CommodityTypeEntity> {

    List<CommodityTypeEntity> listAllByCache();
}

