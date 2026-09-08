package com.jiuyu.replay.api.logic.order;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.order.vo.CommodityTypeListVo;
import com.jiuyu.replay.order.vo.CommodityTypeInfoVo;
import com.jiuyu.replay.order.bo.CommodityTypeBo;
import com.jiuyu.replay.order.bo.CommodityTypeListBo;


/**
 * 商品类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-19 20:52:26
 */
public interface CommodityTypeLogic {


    /**
     * 商品类型列表
     * @param commodityTypeListBo 商品类型列表查询参数
     * @return
     */
    R<PageUtils<CommodityTypeListVo>> queryPage(CommodityTypeListBo commodityTypeListBo);

    /**
    * 商品类型信息
    * @param id 商品类型id
    * @return
    */
    R<CommodityTypeInfoVo> info(Long id);

    /**
     * 新增商品类型
     * @param commodityTypeBo 商品类型对象
     * @return
     */
    R<String> save(CommodityTypeBo commodityTypeBo);

    /**
     * 修改商品类型
     * @param commodityTypeBo 商品类型对象
     * @return
     */
    R<String> update(CommodityTypeBo commodityTypeBo);

    /**
     * 删除商品类型
     * @param id 商品类型id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 同步用户资产
     * @param id
     * @return
     */
    R<String> synchronousUserAssets(Long id);
}

