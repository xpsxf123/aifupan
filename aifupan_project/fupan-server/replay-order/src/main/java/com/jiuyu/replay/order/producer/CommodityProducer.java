package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.order.vo.CommodityListVo;
import com.jiuyu.replay.order.vo.CommodityInfoVo;
import com.jiuyu.replay.order.bo.CommodityBo;
import com.jiuyu.replay.order.bo.CommodityListBo;

import java.util.List;


/**
 * 商品
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
public interface CommodityProducer {


    /**
     * 商品列表
     * @param commodityListBo 商品列表查询参数
     * @return
     */
    PageUtils<CommodityListVo> queryPage(CommodityListBo commodityListBo);

    /**
    * 商品信息
    * @param id 商品id
    * @return
    */
    CommodityInfoVo info(Long id);

    /**
     * 新增商品
     * @param commodityBo 商品对象
     * @return
     */
     CommodityInfoVo save(CommodityBo commodityBo);

    /**
     * 修改商品
     * @param commodityBo 商品对象
     * @return
     */
    void update(CommodityBo commodityBo);

    /**
     * 批量修改商品
     * @param commodityBoList
     */
    void saveOrUpdateBatch(List<CommodityBo> commodityBoList);

    /**
     * 删除商品
     * @param id 商品id
     * @return
     */
    void deleteById(Long id);

    /**
     * 批量删除商品
     *
     * @param ids 商品id列表
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根据商品id集合查询商品
     * @param ids
     * @return
     */
    List<CommodityInfoVo> listByIds(List<Long> ids);

    /**
     * 根据商品id集合查询商品详情
     * @param ids
     * @return
     */
    List<CommodityInfoVo> listCommodityByIds(List<Long> ids);

    /**
     * 商品列表
     * @param commodityListBo
     * @return
     */
    List<CommodityInfoVo> list(CommodityListBo commodityListBo);

    /**
     * 判断价格id是否删除
     * @return
     */
    Boolean isDeletePriceId(Long priceId);

    /**
     * 查询所有商品
     *
     * @return 所有商品
     */
    List<CommodityInfoVo> listAll();
}

