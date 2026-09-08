package com.jiuyu.replay.api.controller.agent;

import com.jiuyu.replay.agent.bo.ClientInviteProgressRewardBo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressRewardListBo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressRewardInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressRewardListVo;
import com.jiuyu.replay.api.logic.agent.ClientInviteProgressRewardLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 邀请进度奖励
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@RestController
@CrossOrigin
@RequestMapping("replay/clientinviteprogressreward")
@Tag(name = "邀请进度奖励")
public class ClientInviteProgressRewardController {

    @Resource
    private ClientInviteProgressRewardLogic clientInviteProgressRewardLogic;

    /**
     * 邀请进度奖励列表
     * @param clientInviteProgressRewardListBo 邀请进度奖励列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "邀请进度奖励列表")
    public R<PageUtils<ClientInviteProgressRewardListVo>> list(@Parameter(description = "邀请进度奖励列表查询参数", required = true) @RequestBody ClientInviteProgressRewardListBo clientInviteProgressRewardListBo){

        return clientInviteProgressRewardLogic.queryPage(clientInviteProgressRewardListBo);
    }


    /**
     * 邀请进度奖励信息
     * @param id 邀请进度奖励id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "邀请进度奖励信息")
    public R<ClientInviteProgressRewardInfoVo> info(@Parameter(description = "邀请进度奖励id", required = true) @RequestParam("id") Long id){

        return clientInviteProgressRewardLogic.info(id);
    }

    /**
     * 新增邀请进度奖励
     * @param clientInviteProgressRewardBo 邀请进度奖励对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增邀请进度奖励")
    public R<String> save(@Parameter(description = "邀请进度奖励对象", required = true) @RequestBody ClientInviteProgressRewardBo clientInviteProgressRewardBo){

        return clientInviteProgressRewardLogic.save(clientInviteProgressRewardBo);
    }

    /**
     * 修改邀请进度奖励
     * @param clientInviteProgressRewardBo 邀请进度奖励对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改邀请进度奖励")
    public R<String> update(@Parameter(description = "邀请进度奖励对象", required = true) @RequestBody ClientInviteProgressRewardBo clientInviteProgressRewardBo){

        return clientInviteProgressRewardLogic.update(clientInviteProgressRewardBo);
    }

    /**
     * 删除邀请进度奖励
     * @param id 邀请进度奖励id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除邀请进度奖励")
    public R<String> delete(@Parameter(description = "邀请进度奖励id", required = true) @RequestParam("id") Long id){

        return clientInviteProgressRewardLogic.delete(id);
    }

}
