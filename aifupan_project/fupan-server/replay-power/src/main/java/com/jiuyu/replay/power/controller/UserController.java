package com.jiuyu.replay.power.controller;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.SubUserListVo;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.bo.ClientPageList;
import com.jiuyu.replay.power.bo.DashboardListBo;
import com.jiuyu.replay.power.vo.DashboardListVo;
import com.jiuyu.replay.power.vo.DashboardStatisticsVo;
import com.jiuyu.replay.power.vo.PaidDashboardStatisticsVo;
import com.jiuyu.replay.power.vo.UserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("PowerUserController")
@RequestMapping("replay/power/user")
@Tag(name = "用户")
public class UserController {

    @Resource
    private UserBll userBll;

    /**
     * 客户端获取用户的子账号列表
     * @return
     */
    @GetMapping("/clientGetSubUserList")
    @Operation(summary = "客户端获取用户的子账号列表")
    public R<List<SubUserListVo>> clientGetSubUserList() {

        return this.userBll.clientGetSubUserList();
    }

    @GetMapping("/sendCustomerAcquisitionMsg")
    @Operation(summary = "发送获客短信")
    public R<String> sendCustomerAcquisitionMsg(Long userId) {
        this.userBll.sendCustomerAcquisitionMsg(userId);
        return R.ok();
    }

    @PostMapping("/clientUserPageList")
    @Operation(summary = "分页查询客户端用户列表")
    public R<PageUtils<UserVo>> clientUserPageList(ClientPageList page) {
        return R.ok(this.userBll.clientUserPageList(page));
    }

    @GetMapping("/dashboardStatistics")
    @Operation(summary = "看板数据统计")
    public R<DashboardStatisticsVo> dashboardStatistics(Integer trialOrder) {
        return R.ok(this.userBll.dashboardStatistics(trialOrder));
    }

    @GetMapping("/paidDashboardStatistics")
    @Operation(summary = "付费到期客户看板统计")
    public R<PaidDashboardStatisticsVo> paidDashboardStatistics() {
        return R.ok(this.userBll.paidDashboardStatistics());
    }

    @PostMapping("/pageDashboardList")
    @Operation(summary = "分页查询看板列表")
    public R<PageUtils<DashboardListVo>> pageDashboardList(@RequestBody DashboardListBo dashboardListBo) {
        return R.ok(this.userBll.pageDashboardList(dashboardListBo));
    }
}
