package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.ProductDetailsBo;
import com.jiuyu.replay.words.bo.ProductDetailsListBo;
import com.jiuyu.replay.words.entity.ProductDetailsEntity;
import com.jiuyu.replay.words.vo.ProductDetailsInfoVo;
import com.jiuyu.replay.words.vo.ProductDetailsListVo;

import java.util.List;

/**
 * 商品
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-03-19
 */
public interface ProductDetailsProducer {

    /**
     * 商品列表
     *
     * @param productDetailsListBo 商品列表查询参数
     * @return
     */
    PageUtils<ProductDetailsListVo> queryPage(ProductDetailsListBo productDetailsListBo);

    /**
     * 商品信息
     *
     * @param id 商品id
     * @return
     */
    ProductDetailsInfoVo info(Long id);

    /**
     * 新增商品
     *
     * @param productDetailsBo 商品对象
     * @return
     */
    ProductDetailsInfoVo save(ProductDetailsBo productDetailsBo);

    /**
     * 修改商品
     *
     * @param productDetailsBo 商品对象
     * @return
     */
    void update(ProductDetailsBo productDetailsBo);

    /**
     * 删除商品
     *
     * @param id 商品id
     * @return
     */
    void deleteById(Long id);

    /**
     * 根据视频id查询商品列表
     *
     * @param videoId 视频id
     * @return
     */
    List<ProductDetailsEntity> listByVideoId(String videoId);

    /**
     * 批量新增商品
     *
     * @param productDetailsBo 商品列表
     */
    void saveBatch(List<ProductDetailsBo> productDetailsBo);
}
