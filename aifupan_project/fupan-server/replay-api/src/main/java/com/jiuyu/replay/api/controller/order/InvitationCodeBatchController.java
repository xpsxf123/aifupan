package com.jiuyu.replay.api.controller.order;

import com.jiuyu.replay.api.logic.order.InvitationCodeBatchLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bo.InvitationCodeBatchBo;
import com.jiuyu.replay.order.bo.InvitationCodeBatchListBo;
import com.jiuyu.replay.order.vo.InvitationCodeBatchInfoVo;
import com.jiuyu.replay.order.vo.InvitationCodeBatchListVo;
import com.jiuyu.replay.order.vo.InvitationCodeListVo;
import com.jiuyu.replay.order.vo.TypeConsumptionInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 邀请码-批次
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@RestController
@CrossOrigin
@RequestMapping("replay/invitationcodebatch")
@Tag(name = "邀请码-批次")
public class InvitationCodeBatchController {

    @Resource
    private InvitationCodeBatchLogic invitationCodeBatchLogic;

    /**
     * 根据邀请码批次导出该批次的所有邀请码
     * @param batchId
     * @return
     */
    @GetMapping("/invitationByBatchId")
    @Operation(summary = "根据邀请码批次导出该批次的所有邀请码")
    public R<List<InvitationCodeListVo>> invitationByBatchId(@Parameter(description = "邀请码批次",required = true) @RequestParam("batchId") Long batchId){

        return invitationCodeBatchLogic.invitationByBatchId(batchId);
    }

    /**
     * 邀请码-批次列表
     * @param invitationCodeBatchListBo 邀请码-批次列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "邀请码-批次列表")
    public R<PageUtils<InvitationCodeBatchListVo>> list(@Parameter(description = "邀请码-批次列表查询参数", required = true) @RequestBody InvitationCodeBatchListBo invitationCodeBatchListBo){

        return invitationCodeBatchLogic.queryPage(invitationCodeBatchListBo);
    }


    /**
     * 邀请码-批次信息
     * @param id 邀请码-批次id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "邀请码-批次信息")
    public R<InvitationCodeBatchInfoVo> info(@Parameter(description = "邀请码-批次id", required = true) @RequestParam("id") Long id){

        return invitationCodeBatchLogic.info(id);
    }

    /**
     * 邀请码-批次信息
     * @return
     */
    @GetMapping("/checkOnlyActivationCode")
    @Operation(summary = "只查询激活码信息")
    public R<InvitationCodeBatchInfoVo> checkOnlyActivationCode(){
        return invitationCodeBatchLogic.checkOnlyActivationCode();
    }

    /**
     * 新增邀请码-批次
     * @param invitationCodeBatchBo 邀请码-批次对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增邀请码-批次")
    public R<String> save(@Parameter(description = "邀请码-批次对象", required = true) @RequestBody InvitationCodeBatchBo invitationCodeBatchBo){

        return invitationCodeBatchLogic.save(invitationCodeBatchBo);
    }

    /**
     * 修改邀请码-批次
     * @param invitationCodeBatchBo 邀请码-批次对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改邀请码-批次")
    public R<String> update(@Parameter(description = "邀请码-批次对象", required = true) @RequestBody InvitationCodeBatchBo invitationCodeBatchBo){

        return invitationCodeBatchLogic.update(invitationCodeBatchBo);
    }

    /**
     * 删除邀请码-批次
     * @param id 邀请码-批次id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除邀请码-批次")
    public R<String> delete(@Parameter(description = "邀请码-批次id", required = true) @RequestParam("id") Long id){

        return invitationCodeBatchLogic.delete(id);
    }

    /**
     * 获取邀请中的资源列表
     * @param id 邀请码-批次id
     * @return
     */
    @GetMapping("/getTypeConsumptionById")
    @Operation(summary = "获取邀请中的资源列表")
    public R<List<TypeConsumptionInfoVo>> getTypeConsumptionById(@RequestParam("id") Long id){
    	return invitationCodeBatchLogic.getTypeConsumptionById(id);
    }

}
