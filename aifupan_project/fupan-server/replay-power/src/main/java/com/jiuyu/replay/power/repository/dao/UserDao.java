package com.jiuyu.replay.power.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.generic.bo.power.statistics.SalesCardStatisticsDataBo;
import com.jiuyu.replay.generic.bo.power.statistics.SalesListStatisticsPageBo;
import com.jiuyu.replay.generic.bo.power.statistics.TeamCardStatisticsDataBo;
import com.jiuyu.replay.generic.vo.power.statistics.EachSalesFollowStatisticsVo;
import com.jiuyu.replay.generic.vo.power.statistics.SalesCardStatisticsDataVo;
import com.jiuyu.replay.generic.vo.power.statistics.SalesStatisticsList;
import com.jiuyu.replay.generic.vo.power.statistics.UserAmbitionStatisticsVo;
import com.jiuyu.replay.power.bo.DashboardListBo;
import com.jiuyu.replay.power.bo.UserListBo;
import com.jiuyu.replay.power.entity.UserEntity;
import com.jiuyu.replay.power.vo.DashboardListVo;
import com.jiuyu.replay.power.vo.DashboardStatisticsVo;
import com.jiuyu.replay.power.vo.PaidDashboardStatisticsVo;
import com.jiuyu.replay.power.vo.ServerUserListVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-02-26 10:00:04
 */
@Mapper
public interface UserDao extends BaseMapper<UserEntity> {
    /**
     * 获取用户列表
     *
     * @param userListBo 获取参数
     * @return 用户列表
     */
    Page<ServerUserListVo> pageListNew(Page<ServerUserListVo> page, @Param("bo") UserListBo userListBo);

    /**
     * 统计准备过期用户的数量
     *
     * @param trialOrder 试用订单
     * @return 数量
     */
    DashboardStatisticsVo prepareExpiredUserCount(@Param("trialOrder") Integer trialOrder);

    /**
     * 统计已经过期用户的数量
     *
     * @param trialOrder 试用订单
     * @return 数量
     */
    DashboardStatisticsVo alreadyExpiredUserCount(@Param("trialOrder") Integer trialOrder);

    /**
     * 统计付费准备过期用户的数量（15/30/60/90天）
     */
    PaidDashboardStatisticsVo prepareExpiredPaidUserCount();

    /**
     * 统计付费已经过期用户的数量（7/15/30/60天）
     */
    PaidDashboardStatisticsVo alreadyExpiredPaidUserCount();

    /**
     * 获取数据看板的用户列表
     *
     * @param page            分页参数
     * @param dashboardListBo 参数
     * @return 用户列表
     */
    Page<DashboardListVo> pageDashboardList(Page<DashboardListVo> page, @Param("bo") DashboardListBo dashboardListBo);

    /**
     * 获取管理列表
     *
     * @param objectPage 分页参数
     * @param userListBo 参数
     * @return 用户列表
     */
    IPage<UserEntity> listManage(Page<Object> objectPage, @Param("bo") UserListBo userListBo);

    /**
     * 统计注册数
     *
     * @param salesCardStatisticsData 参数
     * @return 注册数
     */
    SalesCardStatisticsDataVo statisticsRegister(@Param("bo") SalesCardStatisticsDataBo salesCardStatisticsData);

    /**
     * 统计注册数-团队
     *
     * @param teamCardStatisticsDataBo 参数
     * @return 注册数
     */
    SalesCardStatisticsDataVo statisticsRegisterTeam(@Param("bo") TeamCardStatisticsDataBo teamCardStatisticsDataBo);

    /**
     * 统计试用数
     *
     * @param salesCardStatisticsData 参数
     * @return 试用数
     */
    SalesCardStatisticsDataVo statisticsTrial(@Param("bo") SalesCardStatisticsDataBo salesCardStatisticsData);

    /**
     * 统计试用数-团队
     *
     * @param teamCardStatisticsDataBo 参数
     * @return 试用数
     */
    SalesCardStatisticsDataVo statisticsTrialTeam(@Param("bo") TeamCardStatisticsDataBo teamCardStatisticsDataBo);


    /**
     * 统计续费到期数
     *
     * @param salesCardStatisticsData 参数
     * @return 续费到期数
     */
    SalesCardStatisticsDataVo statisticsRenewalExpires(@Param("bo") SalesCardStatisticsDataBo salesCardStatisticsData);

    /**
     * 统计续费到期数-团队
     *
     * @param teamCardStatisticsDataBo 参数
     * @return 续费到期数
     */
    SalesCardStatisticsDataVo statisticsRenewalExpiresTeam(@Param("bo") TeamCardStatisticsDataBo teamCardStatisticsDataBo);

    /**
     * 统计成交客户数
     *
     * @param salesCardStatisticsData 参数
     * @return 成交客户数
     */
    SalesCardStatisticsDataVo statisticsDealCustomers(@Param("bo") SalesCardStatisticsDataBo salesCardStatisticsData);

    /**
     * 统计成交客户数-团队
     *
     * @param teamCardStatisticsDataBo 参数
     * @return 成交客户数
     */
    SalesCardStatisticsDataVo statisticsDealCustomersTeam(@Param("bo") TeamCardStatisticsDataBo teamCardStatisticsDataBo);

    /**
     * 用户购买意愿度分布图
     *
     * @param salesCardStatisticsData 参数
     * @return 用户购买意愿度分布图
     */
    List<UserAmbitionStatisticsVo> salesEchartsStatisticsData(@Param("bo") SalesCardStatisticsDataBo salesCardStatisticsData);

    /**
     * 团队购买意愿度分布图
     *
     * @param teamCardStatisticsDataBo 参数
     * @return 团队购买意愿度分布图
     */
    List<Map<String, Object>> teamEchartsStatisticsData(@Param("bo") TeamCardStatisticsDataBo teamCardStatisticsDataBo);

    /**
     * 3日内试用即将到期客户
     *
     * @param page 分页参数
     * @param bo   参数
     * @return 用户列表
     */
    Page<SalesStatisticsList> salesTrialAboutTo3DayExpires(Page<DashboardListVo> page, @Param("bo") SalesListStatisticsPageBo bo);

    /**
     * 3日内待跟进客户
     *
     * @param objectPage 分页参数
     * @param bo         参数
     * @return 用户列表
     */
    Page<SalesStatisticsList> salesAboutTo3Day(Page<Object> objectPage, @Param("bo") SalesListStatisticsPageBo bo);

    /**
     * 15日内试用即将续费客户
     *
     * @param objectPage 分页参数
     * @param bo         参数
     * @return 用户列表
     */
    Page<SalesStatisticsList> salesTrialAboutTo15DayRenewal(Page<SalesListStatisticsPageBo> objectPage, @Param("bo") SalesListStatisticsPageBo bo);

    /**
     * 3日内团队试用即将到期客户统计
     *
     * @param bo 参数
     * @return 用户列表
     */
    List<Map<String, Object>> teamTrialAboutTo3DayExpiresStatistics(@Param("bo") TeamCardStatisticsDataBo bo);

    /**
     * 3日内团队试用即将到期客户
     *
     * @param objectPage 分页参数
     * @param bo         参数
     * @return 用户列表
     */
    Page<SalesStatisticsList> teamTrialAboutTo3DayExpires(Page<Object> objectPage, @Param("bo") TeamCardStatisticsDataBo bo);

    /**
     * 团队销售跟进统计-获取部门下的销售列表
     *
     * @param bo 参数
     * @return 销售列表
     */
    List<EachSalesFollowStatisticsVo> teamSalesFollowSalesList(@Param("bo") TeamCardStatisticsDataBo bo);

    /**
     * 团队销售跟进统计-获取各销售的注册/登录/演示数据
     *
     * @param salesIds 销售ID列表
     * @param bo       参数
     * @return 统计数据
     */
    List<EachSalesFollowStatisticsVo> teamSalesFollowRegisterStats(@Param("salesIds") List<Long> salesIds, @Param("bo") TeamCardStatisticsDataBo bo);

    /**
     * 团队销售跟进统计-获取各销售的试用/成交/成交金额数据
     *
     * @param salesIds 销售ID列表
     * @param bo       参数
     * @return 统计数据
     */
    List<EachSalesFollowStatisticsVo> teamSalesFollowOrderStats(@Param("salesIds") List<Long> salesIds, @Param("bo") TeamCardStatisticsDataBo bo);
}
