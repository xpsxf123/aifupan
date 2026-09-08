package com.jiuyu.governance.business.performance.service;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.performance.pojo.request.*;
import com.jiuyu.governance.business.performance.pojo.response.*;

import java.util.List;

/**
 * 业绩汇总服务接口
 * <p>
 * 提供业绩数据的汇总统计功能，支持多维度查询：
 * - 分公司维度：统计分公司的所有业绩
 * - 部门维度：统计部门的所有业绩
 * - 小组维度：统计小组的所有业绩
 * - 直播间维度：统计直播间的所有业绩
 * </p>
 * <p>
 * 组织关系：
 * 1、租户》分公司》部门》小组》直播间
 * 2、租户》部门》小组》直播间
 * 3、租户》部门》直播间
 * 4、租户》直播间
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
public interface PerformanceSummaryService {

    /**
     * 各分公司业绩数据分页查询
     * <p>
     * 使用场景：业绩汇总-各分公司分页列表，集团业绩汇总罗盘-各公司
     * 主表为分公司，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request    查询请求
     * @param accessUser 访问用户
     * @return 分页数据，包含分公司名称、分公司ID、场观、销售额、退款、净销售额、投放、ROI
     */
    PageData<PerformanceSummaryResponse> pageQuerySubCompanyPerformance(SubCompanyPerformancePageRequest request, AccessUser accessUser);

    /**
     * 各分公司业绩数据列表查询
     * <p>
     * 使用场景：业绩汇总-各分公司列表，集团业绩汇总罗盘-各公司
     * 主表为分公司，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request    列表查询请求
     * @param accessUser 访问用户
     * @return 列表数据，包含分公司名称、分公司ID、场观、销售额、退款、净销售额、投放、ROI
     */
    List<PerformanceSummaryResponse> listQuerySubCompanyPerformance(SubCompanyPerformancePageRequest request, AccessUser accessUser);

    /**
     * 各部门业绩数据分页查询
     * <p>
     * 使用场景：业绩汇总-各部门分页列表，分公司业绩汇总详情-部门数据
     * 主表为部门，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request    查询请求
     * @param accessUser 访问用户
     * @return 分页数据，包含部门名称、部门ID、场观、销售额、退款、净销售额、投放、ROI
     */
    PageData<PerformanceSummaryResponse> pageQueryDeptPerformance(DeptPerformancePageRequest request, AccessUser accessUser);

    /**
     * 各部门业绩数据列表查询
     * <p>
     * 使用场景：业绩汇总-各部门列表，分公司业绩汇总详情-部门数据
     * 主表为部门，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request    列表查询请求
     * @param accessUser 访问用户
     * @return 列表数据，包含部门名称、部门ID、场观、销售额、退款、净销售额、投放、ROI
     */
    List<PerformanceSummaryResponse> listQueryDeptPerformance(DeptPerformancePageRequest request, AccessUser accessUser);

    /**
     * 各小组业绩数据分页查询
     * <p>
     * 使用场景：业绩汇总-各小组分页列表，分公司业绩汇总详情-小组数据，部门业绩汇总详情-小组数据
     * 主表为小组，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request    查询请求
     * @param accessUser 访问用户
     * @return 分页数据，包含小组名称、小组ID、场观、销售额、退款、净销售额、投放、ROI
     */
    PageData<PerformanceSummaryResponse> pageQueryTeamPerformance(TeamPerformancePageRequest request, AccessUser accessUser);

    /**
     * 各小组业绩数据列表查询
     * <p>
     * 使用场景：业绩汇总-各小组列表，分公司业绩汇总详情-小组数据，部门业绩汇总详情-小组数据
     * 主表为小组，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request    列表查询请求
     * @param accessUser 访问用户
     * @return 列表数据，包含小组名称、小组ID、场观、销售额、退款、净销售额、投放、ROI
     */
    List<PerformanceSummaryResponse> listQueryTeamPerformance(TeamPerformancePageRequest request, AccessUser accessUser);

    /**
     * 各直播间业绩数据分页查询
     * <p>
     * 使用场景：业绩汇总-各直播间分页列表，分公司业绩汇总详情-直播间数据，
     * 部门业绩汇总详情-直播间数据，小组业绩汇总详情-直播间数据
     * 主表为直播间，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request    查询请求
     * @param accessUser 访问用户
     * @return 分页数据，包含直播间名称、直播间ID、场观、销售额、退款、净销售额、投放、ROI
     */
    PageData<PerformanceSummaryResponse> pageQueryLiveRoomPerformance(LiveRoomPerformancePageRequest request, AccessUser accessUser);

    /**
     * 各直播间业绩数据列表查询
     * <p>
     * 使用场景：业绩汇总-各直播间列表，分公司业绩汇总详情-直播间数据，
     * 部门业绩汇总详情-直播间数据，小组业绩汇总详情-直播间数据
     * 主表为直播间，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request    列表查询请求
     * @param accessUser 访问用户
     * @return 列表数据，包含直播间名称、直播间ID、场观、销售额、退款、净销售额、投放、ROI
     */
    List<PerformanceSummaryResponse> listQueryLiveRoomPerformance(LiveRoomPerformancePageRequest request, AccessUser accessUser);

    /**
     * 获取组织数量统计
     * <p>
     * 使用场景：集团业绩汇总罗盘-详情名称和数量统计，分公司业绩汇总详情-详情名称和数量统计，
     * 部门业绩汇总详情-详情名称和数量统计，小组业绩汇总详情-详情名称和数量统计
     * </p>
     * <p>
     * 根据sourceType返回不同层级的组织数量：
     * - tenant：返回分公司、部门、小组、直播间数量
     * - subCompany：返回部门、小组、直播间数量
     * - dept：返回小组、直播间数量
     * - team：返回直播间数量
     * </p>
     *
     * @param request 查询请求
     * @return 组织数量统计
     */
    OrgCountResponse getOrgCount(OrgCountRequest request);

    /**
     * 获取业绩时段统计
     * <p>
     * 使用场景：业绩汇总详情页-业绩天、周、月的汇总统计
     * 统计的维度为：租户、分公司、部门、小组、直播间中的一个
     * </p>
     * <p>
     * 返回数据包含：直播场次（场次数量和直播时长）、场观、销售额、退款、净销售额、投放
     * 时段维度：今天、昨天、本周、上周、本月、上月
     * </p>
     *
     * @param request 查询请求
     * @return 时段统计数据
     */
    PerformancePeriodStatsResponse getPeriodStats(PerformancePeriodStatsRequest request);

    /**
     * 获取数据趋势（柱形图）
     * <p>
     * 使用场景：业绩汇总详情页-数据趋势柱形图
     * 返回柱形图数据，y轴是日期(yyyy-MM-dd)，x轴是对应的汇总数据
     * </p>
     *
     * @param request 查询请求
     * @return 每日业绩数据列表，按日期升序排列
     */
    List<DailyPerformanceResponse> getTrendData(PerformanceTrendRequest request);

    /**
     * 获取数据详情分页列表
     * <p>
     * 使用场景：业绩汇总详情页-数据详情分页列表
     * 以时间(yyyy-MM-dd)为维度的汇总数据
     * </p>
     *
     * @param request 查询请求
     * @return 分页数据，按日期降序排列
     */
    PageData<DailyPerformanceResponse> pageQueryDailyPerformance(PerformanceDailyPageRequest request);


    /**
     * 获取数据详情列表
     * <p>
     * 使用场景：业绩汇总详情页-数据详情列表
     * 以时间(yyyy-MM-dd)为维度的汇总数据
     * </p>
     *
     * @param request 查询请求
     * @return 列表数据，按日期降序排列
     */
    List<DailyPerformanceResponse> loadQueryDailyPerformance(PerformanceDailyPageRequest request);

    /**
     * 获取销售额汇总
     * <p>
     * 使用场景：分公司占比(前端自己计算占比)、top部门、top小组、top直播间
     * 以(分公司、部门、小组、直播间)为维度汇总销售额
     * </p>
     * <p>
     * 层级筛选规则：
     * - dimensionType=subCompany时，返回当前租户下所有分公司的销售额
     * - dimensionType=dept时，若传入companyId则仅统计该分公司下的部门
     * - dimensionType=team时，若传入deptId则仅统计该部门下的小组
     * - dimensionType=liveRoom时，若传入teamId则仅统计该小组下的直播间
     * </p>
     *
     * @param request 查询请求
     * @return 销售额汇总列表，按销售额降序排列
     */
    List<SalesRevenueSummaryResponse> getSalesRevenueSummary(SalesRevenueSummaryRequest request);
}
