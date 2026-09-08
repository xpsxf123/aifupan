package com.jiuyu.replay.api.controller.agent;

import com.jiuyu.replay.api.logic.agent.InviteUrlCodeLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeBo;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeListBo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeListVo;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 邀请链接的code
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@RestController
@CrossOrigin
@RequestMapping("replay/inviteurlcode")
@Tag(name = "邀请链接的code")
public class InviteUrlCodeController {

    @Resource
    private InviteUrlCodeLogic inviteUrlCodeLogic;

    /**
     * 邀请链接的code列表
     * @param inviteUrlCodeListBo 邀请链接的code列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "邀请链接的code列表")
    public R<PageUtils<InviteUrlCodeListVo>> list(@Parameter(description = "邀请链接的code列表查询参数", required = true) @RequestBody InviteUrlCodeListBo inviteUrlCodeListBo){

        return inviteUrlCodeLogic.queryPage(inviteUrlCodeListBo);
    }

    /**
     * 根据邀请链接的code获取信息
     * @param code 邀请链接的code
     * @return
     */
    @GetMapping("/infoByCode")
    @Operation(summary = "根据邀请链接的code获取信息")
    public R<InviteUrlCodeInfoVo> infoByCode(@Parameter(description = "邀请链接的code", required = true) @RequestParam("code") String code){

        return inviteUrlCodeLogic.infoByCode(code);
    }

    /**
     * 邀请链接的code信息
     * @param id 邀请链接的codeid
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "邀请链接的code信息")
    public R<InviteUrlCodeInfoVo> info(@Parameter(description = "邀请链接的codeid", required = true) @RequestParam("id") Long id){

        return inviteUrlCodeLogic.info(id);
    }

    /**
     * 新增邀请链接的code
     * @param inviteUrlCodeBo 邀请链接的code对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增邀请链接的code")
    public R<String> save(@Parameter(description = "邀请链接的code对象", required = true) @RequestBody InviteUrlCodeBo inviteUrlCodeBo){

        return inviteUrlCodeLogic.save(inviteUrlCodeBo);
    }

    /**
     * 修改邀请链接的code
     * @param inviteUrlCodeBo 邀请链接的code对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改邀请链接的code")
    public R<String> update(@Parameter(description = "邀请链接的code对象", required = true) @RequestBody InviteUrlCodeBo inviteUrlCodeBo){

        return inviteUrlCodeLogic.update(inviteUrlCodeBo);
    }

    /**
     * 删除邀请链接的code
     * @param id 邀请链接的codeid
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除邀请链接的code")
    public R<String> delete(@Parameter(description = "邀请链接的codeid", required = true) @RequestParam("id") Long id){

        return inviteUrlCodeLogic.delete(id);
    }

}
