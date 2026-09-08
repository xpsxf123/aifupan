package com.jiuyu.replay.api.logic.order;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.order.vo.CommodityListVo;
import com.jiuyu.replay.order.vo.CommodityInfoVo;
import com.jiuyu.replay.order.bo.CommodityBo;
import com.jiuyu.replay.order.bo.CommodityListBo;
import com.jiuyu.replay.order.vo.CommodityTypeDataVo;

import java.util.List;


/**
 * 商品
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
public interface CommodityLogic {


    /**
     * 商品列表
     * @param commodityListBo 商品列表查询参数
     * @return
     */
    R<PageUtils<CommodityListVo>> queryPage(CommodityListBo commodityListBo);

    /**
     * 商品列表
     * @param commodityListBo
     * @return
     */
    R<List<CommodityInfoVo>> list(CommodityListBo commodityListBo);

    /**
    * 商品信息
    * @param id 商品id
    * @return
    */
    R<CommodityInfoVo> info(Long id);

    /**
     * 新增商品
     * @param commodityBo 商品对象
     * @return
     */
    R<String> save(CommodityBo commodityBo);

    /**
     * 修改商品
     * @param commodityBo 商品对象
     * @return
     */
    R<String> update(CommodityBo commodityBo);

    /**
     * 删除商品
     * @param id 商品id
     * @return
     */
    R<String> delete(Long id);

    /**
     * 新增商品或修改商品
     * @param commodityBo
     * @return
     */
    R<String> saveOrUpdate(CommodityBo commodityBo);

    /**
     * 判断价格id是否删除
     * @param priceId
     * @return
     */
    R<Boolean> isDeletePriceId(Long priceId);

    /**
     * 商品列表（含类型）
     * @param commodityListBo
     * @return
     */
    R<List<CommodityTypeDataVo>> listTypeAll(CommodityListBo commodityListBo);
}

