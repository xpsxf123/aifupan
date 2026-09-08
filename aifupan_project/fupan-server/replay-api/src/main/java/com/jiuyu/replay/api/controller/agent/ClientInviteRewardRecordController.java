package com.jiuyu.replay.api.controller.agent;

import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordBo;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordListBo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordListVo;
import com.jiuyu.replay.api.logic.agent.ClientInviteRewardRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 邀请奖励记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@RestController
@CrossOrigin
@RequestMapping("replay/clientinviterewardrecord")
@Tag(name = "邀请奖励记录")
public class ClientInviteRewardRecordController {

    @Resource
    private ClientInviteRewardRecordLogic clientInviteRewardRecordLogic;

    /**
     * 邀请奖励记录列表
     * @param clientInviteRewardRecordListBo 邀请奖励记录列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "邀请奖励记录列表")
    public R<PageUtils<ClientInviteRewardRecordListVo>> list(@Parameter(description = "邀请奖励记录列表查询参数", required = true) @RequestBody ClientInviteRewardRecordListBo clientInviteRewardRecordListBo){

        return clientInviteRewardRecordLogic.queryPage(clientInviteRewardRecordListBo);
    }


    /**
     * 邀请奖励记录信息
     * @param id 邀请奖励记录id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "邀请奖励记录信息")
    public R<ClientInviteRewardRecordInfoVo> info(@Parameter(description = "邀请奖励记录id", required = true) @RequestParam("id") Long id){

        return clientInviteRewardRecordLogic.info(id);
    }

    /**
     * 新增邀请奖励记录
     * @param clientInviteRewardRecordBo 邀请奖励记录对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增邀请奖励记录")
    public R<String> save(@Parameter(description = "邀请奖励记录对象", required = true) @RequestBody ClientInviteRewardRecordBo clientInviteRewardRecordBo){

        return clientInviteRewardRecordLogic.save(clientInviteRewardRecordBo);
    }

    /**
     * 修改邀请奖励记录
     * @param clientInviteRewardRecordBo 邀请奖励记录对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改邀请奖励记录")
    public R<String> update(@Parameter(description = "邀请奖励记录对象", required = true) @RequestBody ClientInviteRewardRecordBo clientInviteRewardRecordBo){

        return clientInviteRewardRecordLogic.update(clientInviteRewardRecordBo);
    }

    /**
     * 删除邀请奖励记录
     * @param id 邀请奖励记录id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除邀请奖励记录")
    public R<String> delete(@Parameter(description = "邀请奖励记录id", required = true) @RequestParam("id") Long id){

        return clientInviteRewardRecordLogic.delete(id);
    }

}
