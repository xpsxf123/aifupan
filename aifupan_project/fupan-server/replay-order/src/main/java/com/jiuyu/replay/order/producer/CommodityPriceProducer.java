package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.order.vo.CommodityPriceListVo;
import com.jiuyu.replay.order.vo.CommodityPriceInfoVo;
import com.jiuyu.replay.order.bo.CommodityPriceBo;
import com.jiuyu.replay.order.bo.CommodityPriceListBo;
import com.jiuyu.replay.order.vo.CommodityPriceVo;

import java.util.List;


/**
 * 商品价格
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
public interface CommodityPriceProducer {


    /**
     * 商品价格列表
     * @param commodityPriceListBo 商品价格列表查询参数
     * @return
     */
    PageUtils<CommodityPriceListVo> queryPage(CommodityPriceListBo commodityPriceListBo);

    /**
    * 商品价格信息
    * @param id 商品价格id
    * @return
    */
    CommodityPriceInfoVo info(Long id);

    /**
     * 新增商品价格
     * @param commodityPriceBo 商品价格对象
     * @return
     */
     CommodityPriceInfoVo save(CommodityPriceBo commodityPriceBo);

    /**
     * 批量新增商品价格
     * @param commodityPriceBoList
     * @return
     */
     List<CommodityPriceInfoVo> saveOrUpdateBatch(List<CommodityPriceBo> commodityPriceBoList);

    /**
     * 修改商品价格
     * @param commodityPriceBo 商品价格对象
     * @return
     */
    void update(CommodityPriceBo commodityPriceBo);

    /**
     * 删除商品价格
     * @param id 商品价格id
     * @return
     */
    void deleteById(Long id);

    /**
     * 批量删除商品价格
     *
     * @param ids 商品价格id列表
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根据商品id删除商品价格
     * @param commodityId
     * @param type
     */
    void deleteByCommodityId(Long commodityId, Integer type);

    /**
     * 根据商品id批量删除商品价格
     * @param commodityId
     * @param type
     */
    void deleteByCommodityId(List<Long> commodityId, Integer type);

    /**
     * 根据商品id获取商品价格列表
     * @param commodityId
     * @param type
     * @return
     */
    List<CommodityPriceVo> listByCommodityId(Long commodityId, Integer type);

    /**
     * 根据商品id批量获取商品价格列表
     * @param commodityId
     * @param type
     * @return
     */
    List<CommodityPriceVo> listByCommodityId(List<Long> commodityId, Integer type);


    /**
     * 根据商品id批量获取试用商品价格列表
     *
     * @param commodityId 商品id
     * @param type        类型
     * @return 列表
     */
    List<CommodityPriceVo> listByCommodityIdAndTrial(List<Long> commodityId, Integer type);

    /**
     * 根据商品价格id批量获取商品价格列表
     * @param ids
     * @return
     */
    List<CommodityPriceVo> listByIds(List<Long> ids);

    /**
     * 获取所有商品价格列表
     *
     * @return 列表
     */
    List<CommodityPriceVo> listAll();
}

