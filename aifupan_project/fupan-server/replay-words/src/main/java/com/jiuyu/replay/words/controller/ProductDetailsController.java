package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.ProductDetailsBll;
import com.jiuyu.replay.words.bo.ProductDetailsBo;
import com.jiuyu.replay.words.bo.ProductDetailsListBo;
import com.jiuyu.replay.words.vo.ProductDetailsInfoVo;
import com.jiuyu.replay.words.vo.ProductDetailsListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品Controller
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-03-19
 */
@RestController
@RequestMapping("replay/words/productDetails")
@Tag(name = "商品管理")
public class ProductDetailsController {

    @Resource
    private ProductDetailsBll productDetailsBll;

    /**
     * 商品列表
     *
     * @param productDetailsListBo 商品列表查询参数
     * @return 商品列表
     */
    @GetMapping("/list")
    @Operation(summary = "商品列表")
    public R<PageUtils<ProductDetailsListVo>> list(ProductDetailsListBo productDetailsListBo) {

        return productDetailsBll.queryPage(productDetailsListBo);
    }

    /**
     * 商品信息
     *
     * @param id 商品id
     * @return 商品信息
     */
    @GetMapping("/info")
    @Operation(summary = "商品信息")
    public R<ProductDetailsInfoVo> info(@Parameter(description = "商品ID", required = true) @RequestParam Long id) {

        return productDetailsBll.info(id);
    }

    /**
     * 新增商品
     *
     * @param productDetailsBo 商品对象
     * @return 操作结果
     */
    @PostMapping("/save")
    @Operation(summary = "新增商品")
    public R<String> save(@Validated(ProductDetailsBo.add.class) @RequestBody ProductDetailsBo productDetailsBo) {
        productDetailsBll.save(productDetailsBo);
        return R.ok();
    }

    /**
     * 新增商品
     *
     * @param productDetailsBo 商品对象
     * @return 操作结果
     */
    @PostMapping("/saveBatch")
    @Operation(summary = "添加商品列表")
    public R<String> saveBatch(@Validated(ProductDetailsBo.add.class) @RequestBody List<ProductDetailsBo> productDetailsBo) {
        productDetailsBll.saveBatch(productDetailsBo);
        return R.ok();
    }


    /**
     * 修改商品
     *
     * @param productDetailsBo 商品对象
     * @return 操作结果
     */
    @PostMapping("/update")
    @Operation(summary = "修改商品")
    public R<String> update(@Validated(ProductDetailsBo.update.class) @RequestBody ProductDetailsBo productDetailsBo) {

        return productDetailsBll.update(productDetailsBo);
    }

    /**
     * 删除商品
     *
     * @param id 商品id
     * @return 操作结果
     */
    @PostMapping("/delete")
    @Operation(summary = "删除商品")
    public R<String> delete(@Parameter(description = "商品ID", required = true) @RequestParam Long id) {

        return productDetailsBll.delete(id);
    }
}
