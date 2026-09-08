package com.jiuyu.replay.order.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.order.bo.*;
import com.jiuyu.replay.order.producer.CommodityPriceProducer;
import com.jiuyu.replay.order.producer.IncrementProducer;
import com.jiuyu.replay.order.producer.PackageProducer;
import com.jiuyu.replay.order.producer.TypeConsumptionProducer;
import com.jiuyu.replay.order.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户版本管理 - 新版本
 *
 * @author lujie
 * &#064;description：用户版本相关的控制器
 * @date ：2025/11/18 10:02
 */
@Slf4j
@RestController
@RequestMapping("replay/order/userVersion")
@Tag(name = "用户版本管理")
@AllArgsConstructor
public class UserVersionNewController {

    private final PackageProducer packageProducer;
    private final TypeConsumptionProducer typeConsumptionProducer;
    private final CommodityPriceProducer commodityPriceProducer;
    private final IncrementProducer incrementProducer;

    @GetMapping("/basicInfo/getPackageInfo")
    @Operation(summary = "版本基础信息 - 新增或修改")
    public R<PackageInfoVo> getPackageInfo(Long id) {
        if (id == null) {
            throw new BusinessException("id不能为空");
        }
        return R.ok("查询成功", packageProducer.infoDealsImg(id));
    }

    @PostMapping("/basicInfo/saveOrUpdate")
    @Operation(summary = "版本基础信息 - 新增或修改")
    public R<PackageInfoVo> saveOrUpdateBasicInfo(@Parameter(description = "版本基础信息", required = true) @Validated @RequestBody VersionBasicInfoBo versionBasicInfoBo) {
        // 转换为 PackageBo
        PackageBo packageBo = BeanUtil.copyProperties(versionBasicInfoBo, PackageBo.class);
        packageBo.setIsCompress(1);
        packageBo.setIsGive(1);
        PackageInfoVo res;
        packageProducer.checkMainLevel(packageBo);
        // 判断新增或修改
        if (ObjectUtil.isEmpty(versionBasicInfoBo.getId())) {
            // 新增
            packageProducer.save(packageBo);
        } else {
            // 修改
            packageProducer.update(packageBo);
        }
        res = packageProducer.infoDealsImg(packageBo.getId());

        return R.ok("操作成功", res);
    }

    @PostMapping("/commodity/saveOrUpdate")
    @Operation(summary = "版本商品信息 - 新增或修改一个商品")
    public R<TypeConsumptionInfoVo> saveOrUpdateCommodity(@Parameter(description = "版本商品信息", required = true) @Validated @RequestBody VersionCommodityBo versionCommodityBo) {
        // 转换为 TypeConsumptionBo
        TypeConsumptionBo typeConsumptionBo = BeanUtil.copyProperties(versionCommodityBo, TypeConsumptionBo.class);
        typeConsumptionBo.setType(1);

        // 调用 Producer 的方法，自动获取商品类型信息并保存或更新
        TypeConsumptionInfoVo res = typeConsumptionProducer.saveOrUpdateWithCommodityType(typeConsumptionBo);
        return R.ok("操作成功", res);
    }

    @PostMapping("/commodity/delete")
    @Operation(summary = "版本商品信息 - 删除商品")
    public R<String> deleteCommodity(@Parameter(description = "商品id列表", required = true) @RequestBody List<Long> ids) {
        // 批量删除商品类型用量关联表
        typeConsumptionProducer.deleteByIds(ids);

        return R.ok("删除成功");
    }

    @GetMapping("/commodity/list")
    @Operation(summary = "版本商品信息 - 分页查询商品列表")
    public R<PageUtils<TypeConsumptionListVo>> queryCommodityPage(@Parameter(description = "商品列表查询参数", required = true) TypeConsumptionListBo typeConsumptionListBo) {
        // 分页查询商品类型用量关联表列表
        PageUtils<TypeConsumptionListVo> pageUtils = typeConsumptionProducer.queryPage(typeConsumptionListBo);

        return R.ok("查询成功", pageUtils);
    }

    @PostMapping("/price/saveOrUpdate")
    @Operation(summary = "版本价格体系 - 新增或修改一个价格")
    public R<CommodityPriceInfoVo> saveOrUpdatePrice(@Parameter(description = "版本价格信息", required = true) @Validated @RequestBody VersionPriceBo versionPriceBo) {
        // 转换为 CommodityPriceBo
        CommodityPriceBo commodityPriceBo = new CommodityPriceBo();
        BeanUtils.copyProperties(versionPriceBo, commodityPriceBo);
        // 套餐类型
        commodityPriceBo.setType(1);
        CommodityPriceInfoVo res;
        // 判断新增或修改
        if (ObjectUtil.isEmpty(versionPriceBo.getId())) {
            // 新增
            res = commodityPriceProducer.save(commodityPriceBo);
        } else {
            // 修改
            commodityPriceBo.setId(versionPriceBo.getId());
            commodityPriceProducer.update(commodityPriceBo);
            res = commodityPriceProducer.info(versionPriceBo.getId());
        }

        return R.ok("操作成功", res);
    }

    @PostMapping("/price/delete")
    @Operation(summary = "版本价格体系 - 删除价格")
    public R<String> deletePrice(@Parameter(description = "价格id列表", required = true) @RequestBody List<Long> ids) {
        // 批量删除价格
        commodityPriceProducer.deleteByIds(ids);

        return R.ok("删除成功");
    }

    @GetMapping("/price/list")
    @Operation(summary = "版本价格体系 - 分页查询价格列表")
    public R<PageUtils<CommodityPriceListVo>> queryPricePage(@Parameter(description = "价格列表查询参数", required = true) CommodityPriceListBo commodityPriceListBo) {
        // 分页查询价格列表
        PageUtils<CommodityPriceListVo> pageUtils = commodityPriceProducer.queryPage(commodityPriceListBo);

        return R.ok("查询成功", pageUtils);
    }

    @PostMapping("/increment/saveOrUpdate")
    @Operation(summary = "版本增量包 - 新增或修改一个增量包")
    public R<IncrementInfoVo> saveOrUpdateIncrement(@Parameter(description = "版本增量包信息", required = true) @Validated @RequestBody VersionIncrementBo versionIncrementBo) {
        // 转换为 IncrementBo
        IncrementBo incrementBo = BeanUtil.copyProperties(versionIncrementBo, IncrementBo.class);
        IncrementInfoVo res;
        // 判断新增或修改
        if (ObjectUtil.isEmpty(versionIncrementBo.getId())) {
            if (incrementBo.getStatus() == null) {
                incrementBo.setStatus(1);
            }
            // 新增
            res = incrementProducer.save(incrementBo);
        } else {
            // 修改
            incrementBo.setId(versionIncrementBo.getId());
            incrementProducer.update(incrementBo);
            res = incrementProducer.info(incrementBo.getId());
        }

        return R.ok("操作成功", res);
    }

    @PostMapping("/increment/delete")
    @Operation(summary = "版本增量包 - 删除增量包")
    public R<String> deleteIncrement(@Parameter(description = "增量包id列表", required = true) @RequestBody List<Long> ids) {
        // 批量删除增量包
        incrementProducer.deleteByIds(ids);

        return R.ok("删除成功");
    }

    @GetMapping("/increment/list")
    @Operation(summary = "版本增量包 - 分页查询增量包列表")
    public R<PageUtils<IncrementListVo>> queryIncrementPage(@Parameter(description = "增量包列表查询参数", required = true) IncrementListBo incrementListBo) {
        // 分页查询增量包列表
        PageUtils<IncrementListVo> pageUtils = incrementProducer.queryPage(incrementListBo);

        return R.ok("查询成功", pageUtils);
    }
}
