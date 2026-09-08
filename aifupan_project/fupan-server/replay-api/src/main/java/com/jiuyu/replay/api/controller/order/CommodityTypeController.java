package com.jiuyu.replay.api.controller.order;

import com.jiuyu.replay.api.logic.order.CommodityTypeLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bo.CommodityTypeBo;
import com.jiuyu.replay.order.bo.CommodityTypeListBo;
import com.jiuyu.replay.order.vo.CommodityTypeInfoVo;
import com.jiuyu.replay.order.vo.CommodityTypeListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 商品类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-19 20:52:26
 */
@RestController
@CrossOrigin
@RequestMapping("replay/commoditytype")
@Tag(name = "商品类型")
public class CommodityTypeController {

    @Resource
    private CommodityTypeLogic commodityTypeLogic;

    /**
     * 商品类型列表
     * @param commodityTypeListBo 商品类型列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "商品类型列表")
    public R<PageUtils<CommodityTypeListVo>> list(@Parameter(description = "商品类型列表查询参数", required = true) @RequestBody CommodityTypeListBo commodityTypeListBo){

        return commodityTypeLogic.queryPage(commodityTypeListBo);
    }


    /**
     * 商品类型信息
     * @param id 商品类型id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "商品类型信息")
    public R<CommodityTypeInfoVo> info(@Parameter(description = "商品类型id", required = true) @RequestParam("id") Long id){

        return commodityTypeLogic.info(id);
    }

    /**
     * 新增商品类型
     * @param commodityTypeBo 商品类型对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增商品类型")
    public R<String> save(@Parameter(description = "商品类型对象", required = true) @RequestBody CommodityTypeBo commodityTypeBo){

        return commodityTypeLogic.save(commodityTypeBo);
    }

    /**
     * 修改商品类型
     * @param commodityTypeBo 商品类型对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改商品类型")
    public R<String> update(@Parameter(description = "商品类型对象", required = true) @RequestBody CommodityTypeBo commodityTypeBo){

        return commodityTypeLogic.update(commodityTypeBo);
    }

    /**
     * 删除商品类型
     * @param id 商品类型id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除商品类型")
    public R<String> delete(@Parameter(description = "商品类型id", required = true) @RequestParam("id") Long id){

        return commodityTypeLogic.delete(id);
    }

    /**
     * 同步系统用户的资产类型
     * @param id 商品类型id
     * @return
     */
    @GetMapping("/synchronousUserAssets")
    @Operation(summary = "同步系统用户的资产类型")
    public R<String> synchronousUserAssets(Long id){
        return commodityTypeLogic.synchronousUserAssets(id);
    }

}
