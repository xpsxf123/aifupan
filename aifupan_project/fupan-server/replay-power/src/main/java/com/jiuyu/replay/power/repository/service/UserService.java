package com.jiuyu.replay.power.repository.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.power.bo.DashboardListBo;
import com.jiuyu.replay.power.bo.UserListBo;
import com.jiuyu.replay.power.entity.UserEntity;
import com.jiuyu.replay.power.vo.DashboardListVo;
import com.jiuyu.replay.power.vo.DashboardStatisticsVo;
import com.jiuyu.replay.power.vo.PaidDashboardStatisticsVo;
import com.jiuyu.replay.power.vo.ServerUserListVo;

/**
 * 用户
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-02-26 10:00:04
 */
public interface UserService extends IService<UserEntity> {


    /**
     * 获取用户列表
     *
     * @param userListBo 查询参数
     * @return 用户列表
     */
    Page<ServerUserListVo> pageListNew(UserListBo userListBo);

    /**
     * 统计准备过期用户的数量
     *
     * @param trialOrder 试用订单
     * @return 数量
     */
    DashboardStatisticsVo prepareExpiredUserCount(Integer trialOrder);

    /**
     * 统计已经过期用户的数量
     *
     * @param trialOrder 试用订单
     * @return 数量
     */
    DashboardStatisticsVo alreadyExpiredUserCount(Integer trialOrder);

    PaidDashboardStatisticsVo prepareExpiredPaidUserCount();

    PaidDashboardStatisticsVo alreadyExpiredPaidUserCount();

    /**
     * 获取用户列表
     *
     * @param dashboardListBo 查询参数
     * @return 用户列表
     */
    Page<DashboardListVo> pageDashboardList(DashboardListBo dashboardListBo);

    /**
     * 获取用户列表
     *
     * @param userListBo 获取参数
     * @return 用户列表
     */
    IPage<UserEntity> listManage(UserListBo userListBo);
}

