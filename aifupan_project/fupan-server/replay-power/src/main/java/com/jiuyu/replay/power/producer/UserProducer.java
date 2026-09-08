package com.jiuyu.replay.power.producer;

import com.jiuyu.replay.generic.bo.power.statistics.SalesCardStatisticsDataBo;
import com.jiuyu.replay.generic.bo.power.statistics.SalesListStatisticsPageBo;
import com.jiuyu.replay.generic.bo.power.statistics.TeamCardStatisticsDataBo;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.statistics.*;
import com.jiuyu.replay.power.bo.*;
import com.jiuyu.replay.power.entity.UserEntity;
import com.jiuyu.replay.power.entity.UserTokenEntity;
import com.jiuyu.replay.power.vo.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserProducer {

    /**
     * 根据账号/手机号获取用户信息
     * @param username 账号/手机号
     * @param userType 用户类型
     * @return
     */
    UserVo getByUsernameOrPhone(String username, Integer userType);

    /**
     * 修改用户的登录ip
     * @param id 用户id
     * @param ips ip，多个用_隔开
     */
    void updateUserIp(Long id, String ips);

    /**
     * 根据id获取用户信息
     * @param id 用户id
     * @return
     */
    UserVo getById(Long id);

    /**
     * 修改密码
     * @param id 用户id
     * @param newPassword 新密码
     */
    void updatePassword(Long id, String newPassword);

    /**
     * 用户列表
     * @param userListBo 菜单列表查询参数
     * @return
     */
    PageUtils<UserListVo> queryPage(UserListBo userListBo);

    /**
     * 根据用户id删除用户
     * @param id 用户id
     */
    void deleteById(Long id);

    /**
     * 根据用户名查用户
     * @param username 用户名
     * @return
     */
    UserVo getByUsername(String username);
    /**
     * 根据手机号查用户
     * @param phone 手机号
     * @return
     */
    UserVo getByPhone(String phone);

    /**
     * 根据手机号和用户类型查找用户
     * @param phone 手机号
     * @param userType 用户类型
     * @return
     */
    UserVo getByPhoneAndType(String phone, Integer userType);

    /**
     * 根据用户名和用户类型查用户
     * @param username 用户名
     * @param userType 用户类型
     * @return
     */
    UserVo getByUsernameAndType(String username, Integer userType);

    /**
     * 保存用户信息
     * @param registerBo 注册信息
     * @return
     */
    UserVo create(RegisterBo registerBo);

    /**
     * 新增用户
     * @param userAddBo 数据
     * @return
     */
    UserVo save(UserAddBo userAddBo);

    /**
     * 修改用户信息
     * @param userUpdateBo
     * @return
     */
    boolean update(UserUpdateBo userUpdateBo);

    /**
     * 根据用户id集合获取用户列表
     * @param userIds 用户id集合
     * @return
     */
    List<UserListVo> listByIds(Collection<Long> userIds);

    /**
     * 根据id获取用户信息
     * @param id 用户id
     * @return
     */
    UserInfoVo infoById(Long id);

    /**
     * 根据用户namen or nick 比配查询
     * @param userListBo
     * @return
     */
    List<Long> selectByNameOrNick(UserListBo userListBo);

    /**
     * 根据id集合获取
     * @param usrids
     * @return
     */
    R<PageUtils<UserListVo>> selectByIds(Integer limit,Integer pege ,List<Long> usrids);

    /**
     * 服务端冻结与解冻和用户
     * @param userBo
     * @return
     */
    R<String> updateUser(UserBo userBo);

    /**
     * 根据条件查询用户信息
     * @param userListBo
     * @return
     */
    R<PageUtils<UserListVo>> selectClientList(UserListBo userListBo);

    /**
     * 服务端批量删除用户
     * @param ids
     * @return
     */
    R<String> deleteByIds(List<Long> ids);

    /**
     * 服务端修改用户账号类型
     * @param userUpdateBo
     * @return
     */
    R<String> updateByUserId(UserUpdateBo userUpdateBo);

    /**
     * 服务端获取所有用户信息
     *
     * @return
     */
    List<UserVo> listByClientAll();

    /**
     * 根据用户id获取所有子用户
     * @param subUserId
     * @return
     */
    List<UserVo> listByParentId(Long subUserId);

    /**
     * 获取所有父级用户
     *
     * @return
     */
    List<UserInfoVo> listParentUser(List<Long> userIds);

    /**
     * 获取所有父级用户
     * @return
     */
    List<UserVo> listAllByParent();

    /**
     * 管理员用户列表
     * @param userListBo
     * @return
     */
    PageUtils<UserListManageVo> listManage(UserListBo userListBo);


    R<List<Long>> getUserChild(List<Long> userIds);

    /**
     * 修改用户的激活租户id
     * @param userId 用户id
     * @param tenantId 租户id
     */
    void updateActiveTenantId(Long userId, Long tenantId);

    /**
     * 修改用户缓存中的租户id
     *
     * @param userId   用户id
     * @param tenantId 租户id
     */
    void updateTenantIdRedisCacheById(Long userId, Long tenantId);

    /**
     * 查询所有用户ID
     * @return
     */
    List<Long> listUserId();

    Long getUserIdByUserNickName(String userNickName);

    /**
     * 根据手机号或昵称模糊查询获取用户id集合
     * @param keyword 查询条件
     * @return
     */
    List<Long> listIdsByLikePhoneOrName(String keyword);

    /**
     * 根据角色id获取用户列表
     * @param roleId 角色id
     * @return
     */
    List<UserVo> listByRoleId(Long roleId);

    /**
     * 根据userid获取至父类parentId=0的userId
     * @param userId
     * @return
     */
    Long getUserIdNoPre(Long userId);

    /**
     * 根据来源渠道id集合获取用户列表
     * @param channelIds 来源渠道id集合
     * @return
     */
    List<UserInfoVo> listByChannelIds(List<Long> channelIds);

    UserVo getByPhoneTenant(String phone);

    /**
     * 管理员用户列表
     * @param userListBo
     * @return
     */
    PageUtils<UserVo> listAdmin(UserListBo userListBo);

    /**
     * 重置随机密码
     * @param id
     * @param psw
     * @return
     */
    boolean resetRandomPassword(Long id, String psw);

    /**
     * 获取租户ids
     *
     * @param userIds 用户列表
     * @return 租户ids
     */
    List<Long> getTenantIdsByUserIds(List<Long> userIds);



    /**
     * 获取用户手机号
     *
     * @param userIds 用户列表
     * @return 手机号
     */
    Map<Long, String> getUserPhoneMap(Collection<Long> userIds);

    /**
     * 获取用户昵称
     *
     * @param userIds 用户列表
     * @return 昵称
     */
    Map<Long, String> getUserNameMap(Collection<Long> userIds);

    /**
     * 根据手机号和用户类型统计管理员数量
     *
     * @param phone    手机号
     * @param noUserId 不查的用户id
     * @return 数量
     */
    long countByAdminUser(String phone, Long noUserId);

    /**
     * 统计 active_tenant_id 命中租户的用户数量（团队账号合计）
     *
     * @param tenantId 租户id
     *
     * @return 命中用户数，无记录返回 0
     */
    Integer countByActiveTenantId(Long tenantId);

    /**
     * 根据父级id修改用户详情表状态
     *
     * @param parentId 父级id
     * @param status   代理商状态 0：未启用 1：启用中
     */
    List<UserEntity> updateStatusByParentId(Long parentId, Integer status);

    /**
     * 根据用户id修改用户状态
     *
     * @param id     用户id
     * @param status 状态
     */
    boolean updateStatusById(Long id, Integer status);

    /**
     * 根据用户id集合查询用户token
     *
     * @param userIds 用户id集合
     * @return 用户token集合
     */
    List<UserTokenEntity> tokenListByUserIds(List<Long> userIds);

    /**
     * 服务器的用户列表接口
     *
     * @param userListBo 条件
     * @return 数据
     */
    PageUtils<UserListVo> pageListNew(UserListBo userListBo);

    /**
     * 查询所有用户，分析汇总需要的字段
     *
     * @return 用户列表
     */
    List<UserVo> listUserRollupFieldAll();

    /**
     * 客户端用户列表
     *
     * @param page 分页参数
     * @return 数据
     */
    PageUtils<UserVo> clientUserPageList(ClientPageList page);

    /**
     * 根据手机号查询用户
     *
     * @param phoneList 手机号集合
     * @return 用户列表
     */
    List<UserDto> listByPhones(List<String> phoneList);

    /**
     * 获取子账号数量
     *
     * @param userId 用户id
     * @return 子账号数量
     */
    Long getSubUserCountByUserId(Long userId);

    /**
     * 修改员工状态
     *
     * @param id             用户id
     * @param employeeStatus 员工状态
     */
    void updateEmployeeStatus(Long id, Integer employeeStatus);

    /**
     * 仪表盘统计
     *
     * @param trialOrder 试用订单
     * @return 数据
     */
    DashboardStatisticsVo dashboardStatistics(Integer trialOrder);

    PaidDashboardStatisticsVo paidDashboardStatistics();

    PageUtils<DashboardListVo> pageDashboardList(DashboardListBo dashboardListBo);

    /**
     * 统计注册数
     *
     * @param salesCardStatisticsData 参数
     * @return 注册数
     */
    SalesCardStatisticsDataVo statisticsRegister(SalesCardStatisticsDataBo salesCardStatisticsData);

    /**
     * 统计注册数-团队
     *
     * @param teamCardStatisticsDataBo 参数
     * @return 注册数
     */
    SalesCardStatisticsDataVo statisticsRegisterTeam(TeamCardStatisticsDataBo teamCardStatisticsDataBo);

    /**
     * 统计试用数
     *
     * @param salesCardStatisticsData 参数
     * @return 试用数
     */
    SalesCardStatisticsDataVo statisticsTrial(SalesCardStatisticsDataBo salesCardStatisticsData);

    /**
     * 统计试用数-团队
     *
     * @param teamCardStatisticsDataBo 参数
     * @return 试用数
     */
    SalesCardStatisticsDataVo statisticsTrialTeam(TeamCardStatisticsDataBo teamCardStatisticsDataBo);

    /**
     * 统计续费到期数
     *
     * @param salesCardStatisticsData 参数
     * @return 续费到期数
     */
    SalesCardStatisticsDataVo statisticsRenewalExpires(SalesCardStatisticsDataBo salesCardStatisticsData);

    /**
     * 统计续费到期数-团队
     *
     * @param teamCardStatisticsDataBo 参数
     * @return 续费到期数
     */
    SalesCardStatisticsDataVo statisticsRenewalExpiresTeam(TeamCardStatisticsDataBo teamCardStatisticsDataBo);

    /**
     * 统计成交客户数
     *
     * @param salesCardStatisticsData 参数
     * @return 成交客户数
     */
    SalesCardStatisticsDataVo statisticsDealCustomers(SalesCardStatisticsDataBo salesCardStatisticsData);

    /**
     * 统计成交客户数-团队
     *
     * @param salesCardStatisticsData 参数
     * @return 成交客户数
     */
    SalesCardStatisticsDataVo statisticsDealCustomersTeam(TeamCardStatisticsDataBo salesCardStatisticsData);

    /**
     * 用户购买意愿度分布图
     *
     * @param salesCardStatisticsData 参数
     * @return 用户购买意愿度分布图
     */
    List<UserAmbitionStatisticsVo> salesEchartsStatisticsData(SalesCardStatisticsDataBo salesCardStatisticsData);

    /**
     * 3日内试用即将到期客户
     *
     * @param bo 参数
     * @return 列表
     */
    PageUtils<SalesStatisticsList> salesTrialAboutTo3DayExpires(SalesListStatisticsPageBo bo);

    /**
     * 3日内待跟进客户
     *
     * @param bo 参数
     * @return 列表
     */
    PageUtils<SalesStatisticsList> salesAboutTo3Day(SalesListStatisticsPageBo bo);

    /**
     * 15日内试用即将续费客户
     *
     * @param bo 参数
     * @return 列表
     */
    PageUtils<SalesStatisticsList> salesTrialAboutTo15DayRenewal(SalesListStatisticsPageBo bo);

    /**
     * 团队购买意愿度分布图
     *
     * @param teamCardStatisticsDataBo 参数
     * @return 团队购买意愿度分布图
     */
    List<TeamEchartsStatisticsVo> teamEchartsStatisticsData(TeamCardStatisticsDataBo teamCardStatisticsDataBo);

    /**
     * 3日内试用即将到期团队统计
     *
     * @param bo 参数
     * @return 统计
     */
    Map<String, Integer> teamTrialAboutTo3DayExpiresStatistics(TeamCardStatisticsDataBo bo);

    /**
     * 3日内试用即将到期团队
     *
     * @param bo 参数
     * @return 列表
     */
    PageUtils<SalesStatisticsList> teamTrialAboutTo3DayExpires(TeamCardStatisticsDataBo bo);

    /**
     * 团队数据看板-销售跟进统计
     *
     * @param bo 参数
     * @return 各销售跟进统计列表
     */
    List<EachSalesFollowStatisticsVo> teamSalesFollowList(TeamCardStatisticsDataBo bo);


    /**
     * 获取用户类型映射
     *
     * @param userIds 用户id
     *
     * @return {@link Map }<{@link Long }, {@link Integer }>
     */
    Map<Long, Integer> getUserTypeMap(List<Long> userIds);

    /**
     * 获取用户的子账号数量
     *
     * @param userId 用户id
     * @return 子账号数量
     */
    int getSubAccountNum(Long userId);

    /**
     * Data Hub：按手机号批量查成员快照（含 is_logged_in，过滤后台管理员与已删除用户）
     *
     * @param phones 手机号列表
     *
     * @return {@link List }<{@link com.jiuyu.replay.power.vo.datahub.DataHubMemberVo }>
     */
    List<com.jiuyu.replay.power.vo.datahub.DataHubMemberVo> listDataHubMemberByPhones(List<String> phones);

    /**
     * Data Hub：按用户ID批量查成员快照（含 is_logged_in，过滤后台管理员与已删除用户）
     *
     * @param userIds 用户id列表
     *
     * @return {@link List }<{@link com.jiuyu.replay.power.vo.datahub.DataHubMemberVo }>
     */
    List<com.jiuyu.replay.power.vo.datahub.DataHubMemberVo> listDataHubMemberByIds(Collection<Long> userIds);

    /**
     * Data Hub：按激活租户ID批量查成员快照（含已删除、已冻结成员，含 is_logged_in，过滤后台管理员）
     *
     * @param tenantIds 租户id列表
     *
     * @return {@link List }<{@link com.jiuyu.replay.power.vo.datahub.DataHubMemberVo }>
     */
    List<com.jiuyu.replay.power.vo.datahub.DataHubMemberVo> listDataHubMemberByActiveTenantIds(Collection<Long> tenantIds);
}
