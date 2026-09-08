package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.SyncContrastLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.ClientContrastListBo;
import com.jiuyu.replay.words.bo.SyncContrastBo;
import com.jiuyu.replay.words.bo.SyncContrastListBo;
import com.jiuyu.replay.words.vo.OnlineContrastAnalysisInfoVo;
import com.jiuyu.replay.words.vo.SyncContrastInfoVo;
import com.jiuyu.replay.words.vo.SyncContrastListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 客户端对比数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-26 14:51:59
 */
@RestController
@CrossOrigin
@RequestMapping("replay/synccontrast")
@Tag(name = "客户端对比数据")
public class SyncContrastController {

    @Resource
    private SyncContrastLogic syncContrastLogic;

    /**
     * 客户端获取对比列表
     * @param clientContrastListBo 查询参数
     * @return
     */
    @PostMapping("/clientContrastList")
    @Operation(summary = "客户端获取对比列表")
    public R<PageUtils<SyncContrastInfoVo>> clientContrastList(@RequestBody ClientContrastListBo clientContrastListBo) {

        return syncContrastLogic.clientContrastList(clientContrastListBo);
    }

    /**
     * 客户端删除对比记录
     * @param ids 对比uuid集合
     * @return
     */
    @PostMapping("/clientDeleteContrast")
    @Operation(summary = "客户端删除对比记录")
    public R<String> clientDeleteContrast(@Parameter(description = "对比uuid集合", required = true) @RequestBody List<String> ids) {

        return syncContrastLogic.clientDeleteContrast(ids);
    }

    /**
     * 客户端添加对比
     * @param syncContrastBo 对比数据
     * @return
     */
    @PostMapping("/clientAddContrast")
    @Operation(summary = "客户端添加对比")
    public R<String> clientAddContrast(@Parameter(description = "对比uuid集合", required = true) @RequestBody SyncContrastBo syncContrastBo) {

        return syncContrastLogic.clientAddContrast(syncContrastBo);
    }

    /**
     * 客户端添加云空间对比
     * @param syncContrastBo 对比数据
     * @return
     */
    @PostMapping("/clientAddCloudContrast")
    @Operation(summary = "客户端添加云空间对比")
    public R<String> clientAddCloudContrast(@Parameter(description = "对比uuid集合", required = true) @RequestBody SyncContrastBo syncContrastBo) {

        return syncContrastLogic.clientAddCloudContrast(syncContrastBo);
    }

    /**
     * 客户端删除云空间对比记录
     * @param contrastId 对比uuid
     * @return
     */
    @GetMapping("/clientDeleteCloudContrast")
    @Operation(summary = "客户端删除云空间对比记录")
    public R<String> clientDeleteCloudContrast(@Parameter(description = "对比uuid", required = true) @RequestParam String contrastId) {

        return syncContrastLogic.clientDeleteCloudContrast(contrastId);
    }

    /**
     * 客户端获取云空间对比列表
     * @param clientContrastListBo 查询参数
     * @return
     */
    @Operation(summary = "客户端获取云空间对比列表")
    @PostMapping("/clientListCloudContrast")
    public R<PageUtils<SyncContrastInfoVo>> clientListCloudContrast(@RequestBody ClientContrastListBo clientContrastListBo) {

        return syncContrastLogic.clientListCloudContrast(clientContrastListBo);
    }

    /**
     * 客户端获取对比记录信息
     * @param contrastId 对比uuid
     * @return
     */
    @GetMapping("/clientGetContrast")
    @Operation(summary = "客户端获取对比记录信息")
    public R<SyncContrastInfoVo> clientGetContrast(@Parameter(description = "对比uuid", required = true) @RequestParam String contrastId) {

        return syncContrastLogic.clientGetContrast(contrastId);
    }











    /**
     * 根据对比唯一标识获取对比数据信息
     * @param contrastId 对比唯一标识
     * @return
     */
    @GetMapping("/infoByContrastId")
    @Operation(summary = "根据对比唯一标识获取对比数据信息")
    public R<SyncContrastInfoVo> infoByContrastId(@Parameter(description = "对比唯一标识", required = true) @RequestParam("contrastId") String contrastId){

        return syncContrastLogic.infoByContrastId(contrastId);
    }

    /**
     * 客户端对比数据列表
     * @param syncContrastListBo 客户端对比数据列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "客户端对比数据列表")
    public R<PageUtils<SyncContrastListVo>> list(@Parameter(description = "客户端对比数据列表查询参数", required = true) @RequestBody SyncContrastListBo syncContrastListBo){

        return syncContrastLogic.queryPage(syncContrastListBo);
    }

    /**
     * 获取用户的对比数据列表
     * @return
     */
    @GetMapping("/listByToken")
    @Operation(summary = "获取用户的对比数据列表")
    public R<List<SyncContrastInfoVo>> listByToken(){

        return syncContrastLogic.listByToken();
    }


    /**
     * 客户端对比数据信息
     * @param id 客户端对比数据id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "客户端对比数据信息")
    public R<SyncContrastInfoVo> info(@Parameter(description = "客户端对比数据id", required = true) @RequestParam("id") Long id){

        return syncContrastLogic.info(id);
    }

    /**
     * 新增客户端对比数据
     * @param syncContrastBo 客户端对比数据对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增客户端对比数据")
    public R<String> save(@Parameter(description = "客户端对比数据对象", required = true) @RequestBody SyncContrastBo syncContrastBo){

        return syncContrastLogic.save(syncContrastBo);
    }

    /**
     * 修改客户端对比数据
     * @param syncContrastBo 客户端对比数据对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改客户端对比数据")
    public R<String> update(@Parameter(description = "客户端对比数据对象", required = true) @RequestBody SyncContrastBo syncContrastBo){

        return syncContrastLogic.update(syncContrastBo);
    }

    /**
     * 删除客户端对比数据
     * @param id 客户端对比数据id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除客户端对比数据")
    public R<String> delete(@Parameter(description = "客户端对比数据id", required = true) @RequestParam("id") Long id){

        return syncContrastLogic.delete(id);
    }

    /**
     * 查询所有对比分析记录且填充用户昵称、直播间昵称、行业
     * @param syncContrastListBo 查询参数
     * @return
     */
    @PostMapping("/listAllSyncContrast")
    @Operation(summary = "分页获取对比所有对比分析记录且填充用户昵称、直播间昵称、行业")
    public R<PageUtils<SyncContrastListVo>> listAllSyncContrast(@RequestBody SyncContrastListBo syncContrastListBo){
//        return syncContrastLogic.listAllSyncContrast(syncContrastListBo);
        return syncContrastLogic.listAllSyncContrastNew(syncContrastListBo);
    }

    /**
     * PC后端获取对比分析数据
     * @param contrastId 对比分析唯一标识
     * @return
     */
    @GetMapping("/getContrastAnalysisInfo")
    @Operation(summary = "查看视频或文件分析内容")
    public R<OnlineContrastAnalysisInfoVo> getContrastAnalysisInfo(@RequestParam(required = false) String contrastId) throws Exception {

        return syncContrastLogic.getContrastAnalysisInfo(contrastId);
    }
}
