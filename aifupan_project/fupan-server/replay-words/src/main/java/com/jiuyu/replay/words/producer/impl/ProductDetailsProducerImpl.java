package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.ProductDetailsBo;
import com.jiuyu.replay.words.bo.ProductDetailsListBo;
import com.jiuyu.replay.words.entity.ProductDetailsEntity;
import com.jiuyu.replay.words.producer.ProductDetailsProducer;
import com.jiuyu.replay.words.repository.service.ProductDetailsService;
import com.jiuyu.replay.words.vo.ProductDetailsInfoVo;
import com.jiuyu.replay.words.vo.ProductDetailsListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;


/**
 * 商品
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-03-19
 */
@Service
public class ProductDetailsProducerImpl implements ProductDetailsProducer {

    @Resource
    private ProductDetailsService productDetailsService;


    @Override
    public PageUtils<ProductDetailsListVo> queryPage(ProductDetailsListBo productDetailsListBo) {
        LambdaQueryWrapper<ProductDetailsEntity> wrapper = new LambdaQueryWrapper<ProductDetailsEntity>()
                .like(ObjectUtil.isNotEmpty(productDetailsListBo.getKeyword()), ProductDetailsEntity::getTitle, productDetailsListBo.getKeyword())
                .eq(ObjectUtil.isNotEmpty(productDetailsListBo.getBatchNumber()), ProductDetailsEntity::getBatchNumber, productDetailsListBo.getBatchNumber())
                .eq(ObjectUtil.isNotEmpty(productDetailsListBo.getVideoId()), ProductDetailsEntity::getVideoId, productDetailsListBo.getVideoId())
                .eq(ObjectUtil.isNotEmpty(productDetailsListBo.getProductId()), ProductDetailsEntity::getProductId, productDetailsListBo.getProductId());

        IPage<ProductDetailsEntity> iPage = productDetailsService.page(new Query<ProductDetailsEntity>().getPage(productDetailsListBo.getPage(), productDetailsListBo.getLimit()), wrapper);

        PageUtils<ProductDetailsListVo> pageUtils = new PageUtils<>(productDetailsListBo.getPage(), productDetailsListBo.getLimit(), iPage);

        List<ProductDetailsEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            pageUtils.setList(BeanUtil.copyToList(records, ProductDetailsListVo.class));
        }

        return pageUtils;
    }

    @Override
    public ProductDetailsInfoVo info(Long id) {

        ProductDetailsEntity productDetailsEntity = productDetailsService.getById(id);
        if (productDetailsEntity != null) {
            ProductDetailsInfoVo productDetailsInfoVo = new ProductDetailsInfoVo();
            BeanUtils.copyProperties(productDetailsEntity, productDetailsInfoVo);
            return productDetailsInfoVo;
        }

        return null;
    }

    /**
     * 新增商品
     *
     * @param productDetailsBo 商品对象
     * @return
     */
    public ProductDetailsInfoVo save(ProductDetailsBo productDetailsBo) {

        ProductDetailsEntity productDetailsEntity = new ProductDetailsEntity();
        BeanUtils.copyProperties(productDetailsBo, productDetailsEntity);
        productDetailsEntity.setId(SnowflakeManager.nextValue());
        productDetailsEntity.setCreateDate(new Date());
        productDetailsEntity.setUpdateDate(new Date());

        productDetailsService.save(productDetailsEntity);

        ProductDetailsInfoVo productDetailsInfoVo = new ProductDetailsInfoVo();
        BeanUtils.copyProperties(productDetailsEntity, productDetailsInfoVo);

        return productDetailsInfoVo;
    }

    /**
     * 修改商品
     *
     * @param productDetailsBo 商品对象
     * @return
     */
    public void update(ProductDetailsBo productDetailsBo) {

        ProductDetailsEntity productDetailsEntity = new ProductDetailsEntity();
        BeanUtils.copyProperties(productDetailsBo, productDetailsEntity);
        productDetailsEntity.setUpdateDate(new Date());

        productDetailsService.updateById(productDetailsEntity);
    }

    /**
     * 删除商品
     *
     * @param id 商品id
     * @return
     */
    public void deleteById(Long id) {

        productDetailsService.removeById(id);
    }

    /**
     * 根据视频id查询商品列表
     *
     * @param videoId 视频id
     * @return
     */
    @Override
    public List<ProductDetailsEntity> listByVideoId(String videoId) {
        LambdaQueryWrapper<ProductDetailsEntity> wrapper = new LambdaQueryWrapper<ProductDetailsEntity>()
                .eq(ProductDetailsEntity::getVideoId, videoId);
        return productDetailsService.list(wrapper);
    }

    @Override
    public void saveBatch(List<ProductDetailsBo> productDetailsBo) {

        ProductDetailsBo bo = productDetailsBo.get(0);
        String videoId = bo.getVideoId();

        productDetailsService.remove(new LambdaQueryWrapper<ProductDetailsEntity>().eq(ProductDetailsEntity::getVideoId, videoId));
        Date now = new Date();

        List<ProductDetailsEntity> list = productDetailsBo.stream().map(item -> {
            ProductDetailsEntity productDetailsEntity = BeanUtil.copyProperties(item, ProductDetailsEntity.class);
            productDetailsEntity.setId(SnowflakeManager.nextValue());
            productDetailsEntity.setCreateDate(now);
            productDetailsEntity.setUpdateDate(now);
            return productDetailsEntity;
        }).toList();

        productDetailsService.saveBatch(list);
    }
}
