package com.jiuyu.replay.power.repository.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.power.bo.DashboardListBo;
import com.jiuyu.replay.power.bo.UserListBo;
import com.jiuyu.replay.power.entity.UserEntity;
import com.jiuyu.replay.power.repository.dao.UserDao;
import com.jiuyu.replay.power.repository.service.UserService;
import com.jiuyu.replay.power.vo.DashboardListVo;
import com.jiuyu.replay.power.vo.DashboardStatisticsVo;
import com.jiuyu.replay.power.vo.PaidDashboardStatisticsVo;
import com.jiuyu.replay.power.vo.ServerUserListVo;
import org.springframework.stereotype.Service;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserDao, UserEntity> implements UserService {

    @Override
    public Page<ServerUserListVo> pageListNew(UserListBo userListBo) {
        return baseMapper.pageListNew(new Page<>(userListBo.getPage(), userListBo.getLimit()), userListBo);
    }

    @Override
    public DashboardStatisticsVo prepareExpiredUserCount(Integer trialOrder) {
        return baseMapper.prepareExpiredUserCount(trialOrder);
    }

    @Override
    public DashboardStatisticsVo alreadyExpiredUserCount(Integer trialOrder) {
        return baseMapper.alreadyExpiredUserCount(trialOrder);
    }

    @Override
    public PaidDashboardStatisticsVo prepareExpiredPaidUserCount() {
        return baseMapper.prepareExpiredPaidUserCount();
    }

    @Override
    public PaidDashboardStatisticsVo alreadyExpiredPaidUserCount() {
        return baseMapper.alreadyExpiredPaidUserCount();
    }

    @Override
    public Page<DashboardListVo> pageDashboardList(DashboardListBo dashboardListBo) {
        return baseMapper.pageDashboardList(new Page<>(dashboardListBo.getPage(), dashboardListBo.getLimit()), dashboardListBo);
    }

    @Override
    public IPage<UserEntity> listManage(UserListBo userListBo) {
        return baseMapper.listManage(new Page<>(userListBo.getPage(), userListBo.getLimit()), userListBo);
    }
}