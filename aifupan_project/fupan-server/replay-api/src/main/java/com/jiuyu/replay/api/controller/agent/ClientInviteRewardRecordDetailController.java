package com.jiuyu.replay.api.controller.agent;

import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordDetailBo;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordDetailListBo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordDetailInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordDetailListVo;
import com.jiuyu.replay.api.logic.agent.ClientInviteRewardRecordDetailLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 邀请奖励明细记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@RestController
@CrossOrigin
@RequestMapping("replay/clientinviterewardrecorddetail")
@Tag(name = "邀请奖励明细记录")
public class ClientInviteRewardRecordDetailController {

    @Resource
    private ClientInviteRewardRecordDetailLogic clientInviteRewardRecordDetailLogic;

    /**
     * 邀请奖励明细记录列表
     * @param clientInviteRewardRecordDetailListBo 邀请奖励明细记录列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "邀请奖励明细记录列表")
    public R<PageUtils<ClientInviteRewardRecordDetailListVo>> list(@Parameter(description = "邀请奖励明细记录列表查询参数", required = true) @RequestBody ClientInviteRewardRecordDetailListBo clientInviteRewardRecordDetailListBo){

        return clientInviteRewardRecordDetailLogic.queryPage(clientInviteRewardRecordDetailListBo);
    }


    /**
     * 邀请奖励明细记录信息
     * @param id 邀请奖励明细记录id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "邀请奖励明细记录信息")
    public R<ClientInviteRewardRecordDetailInfoVo> info(@Parameter(description = "邀请奖励明细记录id", required = true) @RequestParam("id") Long id){

        return clientInviteRewardRecordDetailLogic.info(id);
    }

    /**
     * 新增邀请奖励明细记录
     * @param clientInviteRewardRecordDetailBo 邀请奖励明细记录对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增邀请奖励明细记录")
    public R<String> save(@Parameter(description = "邀请奖励明细记录对象", required = true) @RequestBody ClientInviteRewardRecordDetailBo clientInviteRewardRecordDetailBo){

        return clientInviteRewardRecordDetailLogic.save(clientInviteRewardRecordDetailBo);
    }

    /**
     * 修改邀请奖励明细记录
     * @param clientInviteRewardRecordDetailBo 邀请奖励明细记录对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改邀请奖励明细记录")
    public R<String> update(@Parameter(description = "邀请奖励明细记录对象", required = true) @RequestBody ClientInviteRewardRecordDetailBo clientInviteRewardRecordDetailBo){

        return clientInviteRewardRecordDetailLogic.update(clientInviteRewardRecordDetailBo);
    }

    /**
     * 删除邀请奖励明细记录
     * @param id 邀请奖励明细记录id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除邀请奖励明细记录")
    public R<String> delete(@Parameter(description = "邀请奖励明细记录id", required = true) @RequestParam("id") Long id){

        return clientInviteRewardRecordDetailLogic.delete(id);
    }

}
