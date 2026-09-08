package com.jiuyu.replay.api.controller.agent;

import com.jiuyu.replay.agent.bo.ClientInviteActivityBo;
import com.jiuyu.replay.agent.bo.ClientInviteActivityListBo;
import com.jiuyu.replay.agent.vo.ClientInviteActivityInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteActivityListVo;
import com.jiuyu.replay.api.logic.agent.ClientInviteActivityLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 邀请活动
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-23 16:45:49
 */
//@RestController
@CrossOrigin
@RequestMapping("replay/clientinviteactivity")
@Tag(name = "邀请活动")
public class ClientInviteActivityController {

    @Resource
    private ClientInviteActivityLogic clientInviteActivityLogic;

    /**
     * 邀请活动列表
     * @param clientInviteActivityListBo 邀请活动列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "邀请活动列表")
    public R<PageUtils<ClientInviteActivityListVo>> list(@Parameter(description = "邀请活动列表查询参数", required = true) @RequestBody ClientInviteActivityListBo clientInviteActivityListBo){

        return clientInviteActivityLogic.queryPage(clientInviteActivityListBo);
    }



    /**
     * 邀请活动信息
     * @param id 邀请活动id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "邀请活动信息")
    public R<ClientInviteActivityInfoVo> info(@Parameter(description = "邀请活动id", required = true) @RequestParam("id") Long id){

        return clientInviteActivityLogic.info(id);
    }

    /**
     * 新增邀请活动
     * @param clientInviteActivityBo 邀请活动对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增邀请活动")
    public R<String> save(@Parameter(description = "邀请活动对象", required = true) @RequestBody ClientInviteActivityBo clientInviteActivityBo){

        return clientInviteActivityLogic.save(clientInviteActivityBo);
    }

    /**
     * 修改邀请活动
     * @param clientInviteActivityBo 邀请活动对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改邀请活动")
    public R<String> update(@Parameter(description = "邀请活动对象", required = true) @RequestBody ClientInviteActivityBo clientInviteActivityBo){

        return clientInviteActivityLogic.update(clientInviteActivityBo);
    }

    /**
     * 删除邀请活动
     * @param id 邀请活动id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除邀请活动")
    public R<String> delete(@Parameter(description = "邀请活动id", required = true) @RequestParam("id") Long id){

        return clientInviteActivityLogic.delete(id);
    }

}
