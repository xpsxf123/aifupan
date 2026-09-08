package com.jiuyu.replay.api.controller.order;

import com.jiuyu.replay.api.logic.order.PackageLogic;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bo.PackageListBo;
import com.jiuyu.replay.order.bo.PackageSaveBo;
import com.jiuyu.replay.order.vo.PackageInfoVo;
import com.jiuyu.replay.order.vo.PackageListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 套餐表(用户版本)
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@RestController
@CrossOrigin
@RequestMapping("replay/package")
@Tag(name = "套餐表(用户版本)")
public class PackageController {

    @Resource
    private PackageLogic packageLogic;

    /**
     * 套餐表(用户版本)列表
     * @param packageListBo 套餐表(用户版本)列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "套餐表(用户版本)列表")
    public R<PageUtils<PackageListVo>> list(@Parameter(description = "套餐表(用户版本)列表查询参数", required = true) @RequestBody PackageListBo packageListBo){
        if (packageListBo != null && packageListBo.getCustomizeType() == null) {
            packageListBo.setCustomizeType(OrderEnums.customizeType.SYSTEM.getCode());
        }
        return packageLogic.queryPage(packageListBo);
    }

    @GetMapping("/trialVersionList")
    @Operation(summary = "试用版本列表")
    public R<List<PackageListVo>> trialVersionList(Long userId) {
        return packageLogic.trialVersionList(userId);
    }

    @PostMapping("/websiteList")
    @Operation(summary = "官网版本列表")
    public R<List<PackageListVo>> websiteList(@Parameter(description = "用户id") @RequestParam(required = false) Long userId){
        return packageLogic.websiteList(userId);
    }


    /**
     * 套餐表(用户版本)信息
     * @param id 套餐表(用户版本)id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "套餐表(用户版本)信息")
    public R<PackageInfoVo> info(@Parameter(description = "套餐表(用户版本)id", required = true) @RequestParam("id") Long id){

        return packageLogic.info(id);
    }

    /**
     * 新增套餐表(用户版本)
     * @param packageBo 套餐表(用户版本)对象
     * @return
     */
    @PostMapping("/saveOrUpdate")
    @Operation(summary = "新增套餐表(用户版本)")
    public R<String> saveOrUpdate(@Parameter(description = "套餐表(用户版本)对象", required = true) @RequestBody PackageSaveBo packageBo){

        return packageLogic.saveOrUpdate(packageBo);
    }

    /**
     * 删除套餐表(用户版本)
     * @param id 套餐表(用户版本)id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除套餐表(用户版本)")
    public R<String> delete(@Parameter(description = "套餐表(用户版本)id", required = true) @RequestParam("id") Long id){

        return packageLogic.delete(id);
    }

    @GetMapping("/canPurchasePackage")
    @Operation(summary = "可以购买的套餐列表")
    public R<List<PackageInfoVo>> canPurchasePackage(@Parameter(description = "用户id", required = true)Long userId){
        return packageLogic.canPurchasePackage(userId);
    }

    @GetMapping("/userRenewal")
    @Operation(summary = "用户续费套餐列表")
    public R<List<PackageInfoVo>> userRenewal(@Parameter(description = "用户id", required = true)Long userId){
        return packageLogic.userRenewal(userId);
    }

    /**
     * 用userid查询他可以购买的增量包
     * @param userId
     * @return
     */
    @GetMapping("/incrementByPackageId")
    @Operation(summary = "用userid查询他可以购买的增量包")
    public R<PackageInfoVo> incrementByPackageId(Long userId){
        return packageLogic.incrementByPackageId(userId);
    }

    @GetMapping("/synchronousPackage")
    @Operation(summary = "同步版本")
    public R<String> synchronousPackage(Long id){
        return packageLogic.synchronousPackage(id);
    }

}