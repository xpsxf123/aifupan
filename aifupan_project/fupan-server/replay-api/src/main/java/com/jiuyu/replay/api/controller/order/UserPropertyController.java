package com.jiuyu.replay.api.controller.order;

import com.jiuyu.replay.api.logic.order.UserPropertyLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.MonitorPositionAuthVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;
import com.jiuyu.replay.order.bo.UserPropertyBo;
import com.jiuyu.replay.order.bo.UserPropertyDetailsListBo;
import com.jiuyu.replay.order.bo.UserPropertyListBo;
import com.jiuyu.replay.order.dto.UserPropertyTypeCacheDto;
import com.jiuyu.replay.order.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 用户资产
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@RestController
@CrossOrigin
@RequestMapping("replay/userproperty")
@Tag(name = "用户资产")
public class UserPropertyController {

    @Resource
    private UserPropertyLogic userPropertyLogic;

    /**
     * 用户资产列表
     * @param userPropertyListBo 用户资产列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "用户资产列表")
    public R<PageUtils<UserPropertyListVo>> list(@Parameter(description = "用户资产列表查询参数", required = true) @RequestBody UserPropertyListBo userPropertyListBo){

        return userPropertyLogic.queryPage(userPropertyListBo);
    }

    @PostMapping("/pagePropertyDetails")
    @Operation(summary = "用户资产使用详情列表")
    public R<PageUtils<UserPropertyDetailsListVo>> pagePropertyDetails(@RequestBody UserPropertyDetailsListBo bo){
        return userPropertyLogic.pagePropertyDetails(bo);
    }


    /**
     * 用户资产信息-缓存
     * @param userId 用户资产id
     * @return
     */
    @GetMapping("/getPropertyByUserId")
    @Operation(summary = "查询用户资产信息")
    public R<List<UserPropertyTypeInfoVo>> getPropertyByUserId(@Parameter(description = "用户id", required = true) @RequestParam("userId") Long userId){
        return userPropertyLogic.getPropertyByUserId(userId);
    }

    /**
     * 用户资产信息-数据库
     * @param propertyId 用户资产id
     * @return
     */
    @GetMapping("/getPropertyByPropertyId")
    @Operation(summary = "查询用户资产信息")
    public R<List<UserPropertyTypeInfoVo>> getPropertyByPropertyId(@Parameter(description = "用户资产id", required = true) @RequestParam("propertyId") Long propertyId){
        return userPropertyLogic.getPropertyByPropertyId(propertyId);
    }

    /**
     * 新增用户资产
     * @param userPropertyBo 用户资产对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增用户资产")
    public R<String> save(@Parameter(description = "用户资产对象", required = true) @RequestBody UserPropertyBo userPropertyBo){

        return userPropertyLogic.save(userPropertyBo);
    }

    /**
     * 修改用户资产
     * @param userPropertyBo 用户资产对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改用户资产")
    public R<String> update(@Parameter(description = "用户资产对象", required = true) @RequestBody UserPropertyBo userPropertyBo){

        return userPropertyLogic.update(userPropertyBo);
    }

    /**
     * 删除用户资产
     * @param id 用户资产id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除用户资产")
    public R<String> delete(@Parameter(description = "用户资产id", required = true) @RequestParam("id") Long id){

        return userPropertyLogic.delete(id);
    }


    @GetMapping("/checkUserPropertyAndCreate")
    @Operation(summary = "检查用户资产，没有就创建")
    public R<String> checkUserPropertyAndCreate(){
        userPropertyLogic.checkUserPropertyAndCreate();
        return R.ok("检查成功");
    }

    @GetMapping("/seleTokenInfo")
    @Operation(summary = "查询用户的资产")
    public R<UserPropertyTypeCacheDto> seleTokenInfo(Long userId){
        return userPropertyLogic.getUserProperty(userId);
    }

    @Operation(summary = "使用用户资产")
    @PostMapping("/saveUserPropertyDetails")
    public R<String> minusAssets(@RequestBody AssetsMinusOrPlusBo assets){
        if (assets.getNum() <= 0) return R.error(3001, "使用的量不能小于0");
        assets.setNum(-assets.getNum());
        return userPropertyLogic.assetsMinusOrPlus(assets);
    }

    @Operation(summary = "加回用户的资产")
    @PostMapping("/updateUserPropertyDetails")
    public R<String> plusAssets(@RequestBody AssetsMinusOrPlusBo assets){
        if (assets.getNum() <= 0) return R.error(3001, "使用的量不能小于0");
        assets.setNum(Math.abs(assets.getNum()));
        return userPropertyLogic.assetsMinusOrPlus(assets);
    }

    @Operation(summary = "重新统计用户的资产")
    @PostMapping("/statisticsUserProperty")
    public R<String> statisticsUserProperty(@RequestBody List<Long> userIds){
        return userPropertyLogic.statisticsUserProperty(userIds);
    }

    @Operation(summary = "刷新用户的资产缓存")
    @PostMapping("/refreshProperty")
    public R<String> refreshProperty(){
        return userPropertyLogic.refreshProperty();
    }

    @Operation(summary = "查询aiToken记录列表")
    @GetMapping("/aiTokenUseRecordByDetailId")
    public R<List<AiTokenUseRecordInfoVo>> aiTokenUseRecordByDetailId(Long propertyDetailsId){
        return userPropertyLogic.aiTokenUseRecordByDetailId(propertyDetailsId);
    }

    @Operation(summary = "客户端获取用户资产详情")
    @GetMapping("/clintGetData")
    public R<ClintPackageAssetsVo> clintGetData(){
        return userPropertyLogic.clintGetDataFormat(userPropertyLogic.clintGetData());
    }

    /**
     * 三类 AI 监控能力监控位资产统计
     *
     * <p>按固定顺序（话术质检 → 话术还原度 → 互动巡检）返回当前用户的监控位授权量、使用量及剩余量。</p>
     *
     * @return 三类监控位授权量信息列表
     */
    @GetMapping("/monitorPositionStatistics")
    @Operation(summary = "三类监控位资产统计")
    public R<List<MonitorPositionAuthVo>> monitorPositionStatistics() {
        return userPropertyLogic.monitorPositionStatistics();
    }

}
