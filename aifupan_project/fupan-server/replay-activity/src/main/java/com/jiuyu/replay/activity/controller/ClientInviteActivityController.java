package com.jiuyu.replay.activity.controller;

import com.jiuyu.replay.activity.bll.ClientInviteActivityBll;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.bo.activity.ClientInviteActivityBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteActivityListBo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteActivityInfoVo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteActivityListVo;
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
 * @date 2025-05-24 16:10:28
 */
@RestController
@RequestMapping("replay/activity/clientinviteactivity")
@Tag(name = "邀请活动")
public class ClientInviteActivityController {

    @Resource
    private ClientInviteActivityBll clientInviteActivityBll;

    /**
     * 邀请活动列表
     * @param clientInviteActivityListBo 邀请活动列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "邀请活动列表")
    public R<PageUtils<ClientInviteActivityListVo>> list(@Parameter(description = "邀请活动列表查询参数", required = true) @RequestBody ClientInviteActivityListBo clientInviteActivityListBo){

        return clientInviteActivityBll.queryPage(clientInviteActivityListBo);
    }



    /**
     * 邀请活动信息
     * @param id 邀请活动id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "邀请活动信息")
    public R<ClientInviteActivityInfoVo> info(@Parameter(description = "邀请活动id", required = true) @RequestParam("id") Long id){

        return clientInviteActivityBll.info(id, null);
    }

    /**
     * 新增邀请活动
     * @param clientInviteActivityBo 邀请活动对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增邀请活动")
    public R<String> save(@Parameter(description = "邀请活动对象", required = true) @RequestBody ClientInviteActivityBo clientInviteActivityBo){

        return clientInviteActivityBll.save(clientInviteActivityBo);
    }

    /**
     * 修改邀请活动
     * @param clientInviteActivityBo 邀请活动对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改邀请活动")
    public R<String> update(@Parameter(description = "邀请活动对象", required = true) @RequestBody ClientInviteActivityBo clientInviteActivityBo){

        return clientInviteActivityBll.update(clientInviteActivityBo);
    }

    /**
     * 删除邀请活动
     * @param id 邀请活动id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除邀请活动")
    public R<String> delete(@Parameter(description = "邀请活动id", required = true) @RequestParam("id") Long id){

        return clientInviteActivityBll.delete(id);
    }

    /**
     * 客户端获取邀请活动信息
     * @return
     */
    @GetMapping("/infoByClient")
    @Operation(summary = "客户端获取邀请活动信息")
    public R<ClientInviteActivityInfoVo> infoByClient(){

        return clientInviteActivityBll.infoByClient();
    }


}
