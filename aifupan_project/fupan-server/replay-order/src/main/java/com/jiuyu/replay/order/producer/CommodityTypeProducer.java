package com.jiuyu.replay.order.producer;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.order.bo.CommodityTypeBo;
import com.jiuyu.replay.order.bo.CommodityTypeListBo;
import com.jiuyu.replay.order.entity.CommodityTypeEntity;
import com.jiuyu.replay.order.vo.CommodityTypeInfoVo;
import com.jiuyu.replay.order.vo.CommodityTypeListVo;

import java.util.List;


/**
 * 商品类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-19 20:52:26
 */
public interface CommodityTypeProducer {


    /**
     * 商品类型列表
     * @param commodityTypeListBo 商品类型列表查询参数
     * @return
     */
    PageUtils<CommodityTypeListVo> queryPage(CommodityTypeListBo commodityTypeListBo);

    /**
     * 商品类型列表
     * @param queryWrapper
     * @return
     */
    List<CommodityTypeListVo> list(QueryWrapper<CommodityTypeEntity> queryWrapper);

    /**
    * 商品类型信息
    * @param id 商品类型id
    * @return
    */
    CommodityTypeInfoVo info(Long id);

    /**
     * 新增商品类型
     * @param commodityTypeBo 商品类型对象
     * @return
     */
     CommodityTypeInfoVo save(CommodityTypeBo commodityTypeBo);

    /**
     * 修改商品类型
     * @param commodityTypeBo 商品类型对象
     * @return
     */
    void update(CommodityTypeBo commodityTypeBo);

    /**
     * 删除商品类型
     * @param id 商品类型id
     * @return
     */
    void deleteById(Long id);

    /**
     * 商品类型列表
     * @param ids
     * @return
     */
    List<CommodityTypeInfoVo> listByIds(List<Long> ids);

    /**
     * 同步用户资产
     * @param id
     */
    void synchronousUserAssets(Long id);

    /**
     * 根据code获取
     *
     * @param code
     * @return
     */
    CommodityTypeInfoVo getByCode(String code);

    /**
     * 获取所有
     *
     * @return 全部的商品类型
     */
    List<CommodityTypeEntity> listAll();
}

