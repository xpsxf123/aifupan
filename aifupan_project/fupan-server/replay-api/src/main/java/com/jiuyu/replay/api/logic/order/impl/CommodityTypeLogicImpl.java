package com.jiuyu.replay.api.logic.order.impl;

import com.jiuyu.replay.api.logic.order.CommodityTypeLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bll.CommodityTypeBll;
import com.jiuyu.replay.order.bo.CommodityTypeBo;
import com.jiuyu.replay.order.bo.CommodityTypeListBo;
import com.jiuyu.replay.order.vo.CommodityTypeInfoVo;
import com.jiuyu.replay.order.vo.CommodityTypeListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 商品类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-19 20:52:26
 */
@Service
public class CommodityTypeLogicImpl implements CommodityTypeLogic {

    @Resource
    private CommodityTypeBll commodityTypeBll;


    @Override
    public R<PageUtils<CommodityTypeListVo>> queryPage(CommodityTypeListBo commodityTypeListBo) {

        return commodityTypeBll.queryPage(commodityTypeListBo);
    }

    @Override
    public R<CommodityTypeInfoVo> info(Long id) {

        return commodityTypeBll.info(id);
    }

    @Override
    public R<String> save(CommodityTypeBo commodityTypeBo) {

        return commodityTypeBll.save(commodityTypeBo);
    }

    @Override
    public R<String> update(CommodityTypeBo commodityTypeBo) {

        return commodityTypeBll.update(commodityTypeBo);
    }

    @Override
    public R<String> delete(Long id) {

        return commodityTypeBll.delete(id);
    }

    @Override
    public R<String> synchronousUserAssets(Long id) {
        return commodityTypeBll.synchronousUserAssets(id);
    }
}

