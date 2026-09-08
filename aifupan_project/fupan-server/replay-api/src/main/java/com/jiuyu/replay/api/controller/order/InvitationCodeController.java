package com.jiuyu.replay.api.controller.order;

import com.jiuyu.replay.api.logic.order.InvitationCodeLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bo.InvitationCodeBo;
import com.jiuyu.replay.order.bo.InvitationCodeListBo;
import com.jiuyu.replay.order.vo.InvitationCodeInfoVo;
import com.jiuyu.replay.order.vo.InvitationCodeListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 邀请码
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@RestController
@CrossOrigin
@RequestMapping("replay/invitationcode")
@Tag(name = "邀请码")
public class InvitationCodeController {

    @Resource
    private InvitationCodeLogic invitationCodeLogic;

    /**
     * 邀请码导出
     * @param invitationCodeBo
     * @return
     */
    @PostMapping("/exportInvitation")
    @Operation(summary = "修改已经导出的邀请码状态")
    public R<List<InvitationCodeListVo>> exportInvitation(@Parameter(description = "要被导出邀请码",required = true) @RequestBody List<InvitationCodeBo> invitationCodeBo){
        return invitationCodeLogic.exportInvitation(invitationCodeBo);
    }

    @PostMapping("/updateIsLssued")
    @Operation(summary = "修改已经导出的邀请码状态")
    public R<String> updateIsLssued(@Parameter(description = "邀请码id列表", required = true) @RequestBody List<Long> ids){
        return invitationCodeLogic.updateIsLssued(ids);
    }


    /**
     * 邀请码列表
     * @param invitationCodeListBo 邀请码列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "邀请码列表")
    public R<PageUtils<InvitationCodeListVo>> list(@Parameter(description = "邀请码列表查询参数", required = true) @RequestBody InvitationCodeListBo invitationCodeListBo){

        return invitationCodeLogic.queryPage(invitationCodeListBo);
    }


    /**
     * 邀请码信息
     * @param id 邀请码id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "邀请码信息")
    public R<InvitationCodeInfoVo> info(@Parameter(description = "邀请码id", required = true) @RequestParam("id") Long id){

        return invitationCodeLogic.info(id);
    }

    /**
     * 新增邀请码
     * @param invitationCodeBo 邀请码对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增邀请码")
    public R<String> save(@Parameter(description = "邀请码对象", required = true) @RequestBody InvitationCodeBo invitationCodeBo){

        return invitationCodeLogic.save(invitationCodeBo);
    }

    /**
     * 修改邀请码
     * @param invitationCodeBo 邀请码对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改邀请码")
    public R<String> update(@Parameter(description = "邀请码对象", required = true) @RequestBody InvitationCodeBo invitationCodeBo){

        return invitationCodeLogic.update(invitationCodeBo);
    }

    /**
     * 删除邀请码
     * @param id 邀请码id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除邀请码")
    public R<String> delete(@Parameter(description = "邀请码id", required = true) @RequestParam("id") Long id){

        return invitationCodeLogic.delete(id);
    }

}
