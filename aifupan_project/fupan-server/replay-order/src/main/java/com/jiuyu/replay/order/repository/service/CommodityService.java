package com.jiuyu.replay.order.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.order.bo.CommodityBo;
import com.jiuyu.replay.order.entity.CommodityEntity;

import java.util.List;

/**
 * 商品
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
public interface CommodityService extends IService<CommodityEntity> {

    /**
     * 获取所有商品信息
     *
     * @return
     */
    List<CommodityBo> listAll();
}

