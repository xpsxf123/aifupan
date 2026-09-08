package com.jiuyu.replay.api.controller.order;


import com.jiuyu.replay.api.logic.order.CommodityLogic;
import com.jiuyu.replay.order.vo.CommodityTypeDataVo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

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
@RestController
@CrossOrigin
@RequestMapping("replay/commodity")
@Tag(name = "商品")
public class CommodityController {

    @Resource
    private CommodityLogic commodityLogic;

    /**
     * 商品列表
     * @param commodityListBo 商品列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "商品列表")
    public R<PageUtils<CommodityListVo>> list(@Parameter(description = "商品列表查询参数", required = true) @RequestBody CommodityListBo commodityListBo){

        return commodityLogic.queryPage(commodityListBo);
    }

    /**
     * 商品列表
     * @param commodityListBo 商品列表查询参数
     * @return
     */
    @PostMapping("/listAll")
    @Operation(summary = "商品列表")
    public R<List<CommodityInfoVo>> listAll(@Parameter(description = "商品列表查询参数", required = true) @RequestBody CommodityListBo commodityListBo){

        return commodityLogic.list(commodityListBo);
    }


    /**
     * 商品列表（含类型）
     * @param commodityListBo 商品列表查询参数
     * @return
     */
    @PostMapping("/listTypeAll")
    @Operation(summary = "商品列表（含类型）")
    public R<List<CommodityTypeDataVo>> listTypeAll(@Parameter(description = "商品列表查询参数", required = true) @RequestBody CommodityListBo commodityListBo){

        return commodityLogic.listTypeAll(commodityListBo);
    }


    /**
     * 商品信息
     * @param id 商品id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "商品信息")
    public R<CommodityInfoVo> info(@Parameter(description = "商品id", required = true) @RequestParam("id") Long id){

        return commodityLogic.info(id);
    }

    /**
     * 新增商品
     * @param commodityBo 商品对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增商品")
    public R<String> save(@Parameter(description = "商品对象", required = true) @RequestBody CommodityBo commodityBo){

        return commodityLogic.save(commodityBo);
    }

    /**
     * 修改商品
     * @param commodityBo 商品对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改商品")
    public R<String> update(@Parameter(description = "商品对象", required = true) @RequestBody CommodityBo commodityBo){

        return commodityLogic.update(commodityBo);
    }

    /**
     * 修改商品
     * @param commodityBo 商品对象
     * @return
     */
    @PostMapping("/saveOrUpdate")
    @Operation(summary = "修改或保存商品")
    public R<String> saveOrUpdate(@Parameter(description = "商品对象", required = true) @RequestBody CommodityBo commodityBo){

        return commodityLogic.saveOrUpdate(commodityBo);
    }

    /**
     * 删除商品
     * @param id 商品id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除商品")
    public R<String> delete(@Parameter(description = "商品id", required = true) @RequestParam("id") Long id){

        return commodityLogic.delete(id);
    }

    @GetMapping("/isDeletePriceId")
    @Operation(summary = "是否能删除增量包的价格")
    public R<Boolean> isDeletePriceId(@Parameter(description = "价格id", required = true) Long priceId){
        return commodityLogic.isDeletePriceId(priceId);
    }

}
