package com.jiuyu.replay.reward.contraller;

import com.jiuyu.replay.generic.bo.reward.ClientInviteRewardRecordListBo;
import com.jiuyu.replay.generic.bo.reward.UserRewardBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.reward.ClientInviteRewardRecordInfoVo;
import com.jiuyu.replay.generic.vo.reward.ClientUserRewardRecordVo;
import com.jiuyu.replay.generic.vo.reward.RewardSummaryVo;
import com.jiuyu.replay.reward.bll.ClientInviteRewardRecordBll;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 进度奖励记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 17:03:13
 */
@RestController
@RequestMapping("replay/reward/clientinviterewardrecord")
@Tag(name = "进度奖励记录")
public class ClientInviteRewardRecordController {

    @Resource
    private ClientInviteRewardRecordBll clientInviteRewardRecordBll;

    /**
     * 后台获取奖励列表
     * @param clientInviteRewardRecordListBo 查询参数
     * @return
     */
    @PostMapping("/listByBack")
    @Operation(summary = "后台获取奖励列表")
    public R<PageUtils<ClientInviteRewardRecordInfoVo>> listByBack(@Parameter(description = "后台奖励列表", required = true) @RequestBody ClientInviteRewardRecordListBo clientInviteRewardRecordListBo){
        return clientInviteRewardRecordBll.listByBack(clientInviteRewardRecordListBo);
    }

    /**
     * 客户端获取奖励汇总
     *
     * @return
     */
    @GetMapping("/clientGetRewardSummary")
    @Operation(summary = "客户端获取奖励汇总")
    public R<List<RewardSummaryVo>> clientGetRewardSummary() {
        return R.ok(clientInviteRewardRecordBll.clientGetRewardSummary());
    }

    /**
     * 客户端获取邀请奖励列表
     *
     * @return
     */
    @PostMapping("/clientGetUserRewardList")
    @Operation(summary = "客户端获取邀请奖励列表")
    public R<PageUtils<ClientUserRewardRecordVo>> clientGetUserRewardList(@RequestBody UserRewardBo userRewardBo) {
        return R.ok(clientInviteRewardRecordBll.clientGetUserRewardList(userRewardBo));
    }

}
