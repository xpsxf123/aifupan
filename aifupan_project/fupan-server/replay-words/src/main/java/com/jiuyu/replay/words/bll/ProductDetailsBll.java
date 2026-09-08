package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.ProductDetailsBo;
import com.jiuyu.replay.words.bo.ProductDetailsListBo;
import com.jiuyu.replay.words.producer.ProductDetailsProducer;
import com.jiuyu.replay.words.vo.ProductDetailsInfoVo;
import com.jiuyu.replay.words.vo.ProductDetailsListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 商品
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-03-19
 */
@Component
public class ProductDetailsBll {

    @Resource
    private ProductDetailsProducer productDetailsProducer;


    /**
     * 商品列表
     *
     * @param productDetailsListBo 商品列表查询参数
     * @return
     */
    public R<PageUtils<ProductDetailsListVo>> queryPage(ProductDetailsListBo productDetailsListBo) {

        return R.ok("获取成功", productDetailsProducer.queryPage(productDetailsListBo));
    }

    /**
     * 商品信息
     *
     * @param id 商品id
     * @return
     */
    public R<ProductDetailsInfoVo> info(Long id) {

        ProductDetailsInfoVo productDetailsInfoVo = productDetailsProducer.info(id);
        return R.ok("获取成功", productDetailsInfoVo);
    }

    /**
     * 新增商品
     *
     * @param productDetailsBo 商品对象
     * @return
     */
    public void save(ProductDetailsBo productDetailsBo) {

        ProductDetailsInfoVo productDetailsInfoVo = productDetailsProducer.save(productDetailsBo);
    }

    /**
     * 批量新增商品
     *
     * @param productDetailsBo 商品列表
     */
    public void saveBatch(List<ProductDetailsBo> productDetailsBo) {
        productDetailsProducer.saveBatch(productDetailsBo);
    }

    /**
     * 修改商品
     *
     * @param productDetailsBo 商品对象
     * @return
     */
    public R<String> update(ProductDetailsBo productDetailsBo) {

        productDetailsProducer.update(productDetailsBo);
        return R.ok("修改成功");
    }

    /**
     * 删除商品
     *
     * @param id 商品id
     * @return
     */
    public R<String> delete(Long id) {

        productDetailsProducer.deleteById(id);
        return R.ok("删除成功");
    }
}
