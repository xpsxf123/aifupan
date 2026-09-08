package com.jiuyu.replay.power.controller;

import com.jiuyu.replay.generic.bo.power.statistics.SalesCardStatisticsDataBo;
import com.jiuyu.replay.generic.bo.power.statistics.SalesListStatisticsPageBo;
import com.jiuyu.replay.generic.bo.power.statistics.TeamCardStatisticsDataBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.SalesListVo;
import com.jiuyu.replay.generic.vo.power.statistics.*;
import com.jiuyu.replay.power.bll.SalesStatisticsBll;
import com.jiuyu.replay.power.producer.DeptProducer;
import com.jiuyu.replay.power.vo.DeptListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/29 17:16
 */
@RestController
@RequestMapping("replay/power/salesStatistics")
@AllArgsConstructor
@Tag(name = "销售统计相关的接口")
public class SalesStatisticsController {

    private final SalesStatisticsBll salesStatisticsBll;
    private final DeptProducer deptProducer;


    @GetMapping("/salesSelect")
    @Operation(summary = "客户数据看板-销售列表")
    public R<List<SalesListVo>> salesSelect() {
        return R.ok(salesStatisticsBll.selectSales());
    }

    @PostMapping("/salesCardStatisticsData")
    @Operation(summary = "客户数据看板-数据概览")
    public R<SalesCardStatisticsDataVo> salesCardStatisticsData(@RequestBody @Validated SalesCardStatisticsDataBo salesCardStatisticsData) {
        return R.ok(salesStatisticsBll.salesCardStatisticsData(salesCardStatisticsData));
    }

    @PostMapping("/salesEchartsStatisticsData")
    @Operation(summary = "客户数据看板-用户购买意愿度分布图")
    public R<List<UserAmbitionStatisticsVo>> salesEchartsStatisticsData(@RequestBody @Validated SalesCardStatisticsDataBo salesCardStatisticsData) {
        return R.ok(salesStatisticsBll.salesEchartsStatisticsData(salesCardStatisticsData));
    }

    @PostMapping("/salesTrialAboutTo3DayExpires")
    @Operation(summary = "客户数据看板-3日内试用即将到期客户")
    public R<PageUtils<SalesStatisticsList>> salesTrialAboutTo3DayExpires(@RequestBody @Validated SalesListStatisticsPageBo bo) {
        return R.ok(salesStatisticsBll.salesTrialAboutTo3DayExpires(bo));
    }

    @PostMapping("/salesAboutTo3Day")
    @Operation(summary = "客户数据看板-3日内待跟进客户")
    public R<PageUtils<SalesStatisticsList>> salesAboutTo3Day(@RequestBody @Validated SalesListStatisticsPageBo bo) {
        return R.ok(salesStatisticsBll.salesAboutTo3Day(bo));
    }

    @PostMapping("/salesTrialAboutTo15DayRenewal")
    @Operation(summary = "客户数据看板-15日内试用即将续费客户")
    public R<PageUtils<SalesStatisticsList>> salesTrialAboutTo15DayRenewal(@RequestBody @Validated SalesListStatisticsPageBo bo) {
        return R.ok(salesStatisticsBll.salesTrialAboutTo15DayRenewal(bo));
    }


    @GetMapping("/deptList")
    @Operation(summary = "团队数据看板-查询全部部门")
    public R<List<DeptListVo>> deptList() {
        return R.ok(deptProducer.treeList());
    }

    @PostMapping("/teamCardStatisticsData")
    @Operation(summary = "团队数据看板-数据概览")
    public R<SalesCardStatisticsDataVo> teamCardStatisticsData(@RequestBody @Validated(TeamCardStatisticsDataBo.DateChange.class) TeamCardStatisticsDataBo teamCardStatisticsDataBo) {
        return R.ok(salesStatisticsBll.teamCardStatisticsData(teamCardStatisticsDataBo));
    }

    @PostMapping("/teamEchartsStatisticsData")
    @Operation(summary = "团队数据看板-用户购买意愿度分布图")
    public R<List<TeamEchartsStatisticsVo>> teamEchartsStatisticsData(@RequestBody @Validated(TeamCardStatisticsDataBo.DateChange.class) TeamCardStatisticsDataBo teamCardStatisticsDataBo) {
        return R.ok(salesStatisticsBll.teamEchartsStatisticsData(teamCardStatisticsDataBo));
    }

    @PostMapping("/teamTrialAboutTo3DayExpiresStatistics")
    @Operation(summary = "团队数据看板-3日内试用即将到期客户统计")
    public R<Map<String, Integer>> teamTrialAboutTo3DayExpiresStatistics(@RequestBody @Validated TeamCardStatisticsDataBo bo) {
        return R.ok(salesStatisticsBll.teamTrialAboutTo3DayExpiresStatistics(bo));
    }

    @PostMapping("/teamTrialAboutTo3DayExpires")
    @Operation(summary = "团队数据看板-3日内试用即将到期客户")
    public R<PageUtils<SalesStatisticsList>> teamTrialAboutTo3DayExpires(@RequestBody @Validated TeamCardStatisticsDataBo bo) {
        return R.ok(salesStatisticsBll.teamTrialAboutTo3DayExpires(bo));
    }

    @PostMapping("/teamSalesFollowList")
    @Operation(summary = "团队数据看板-各个销售人员跟进数据列表")
    public R<List<EachSalesFollowStatisticsVo>> teamSalesFollowList(@RequestBody @Validated(TeamCardStatisticsDataBo.DateChange.class) TeamCardStatisticsDataBo bo) {
        return R.ok(salesStatisticsBll.teamSalesFollowList(bo));
    }
}
