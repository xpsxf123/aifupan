package com.jiuyu.replay.power.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.diff.Business;
import com.jiuyu.replay.common.utils.*;
import com.jiuyu.replay.generic.bo.power.statistics.SalesCardStatisticsDataBo;
import com.jiuyu.replay.generic.bo.power.statistics.SalesListStatisticsPageBo;
import com.jiuyu.replay.generic.bo.power.statistics.TeamCardStatisticsDataBo;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.common.OperationLogFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.statistics.*;
import com.jiuyu.replay.power.bo.*;
import com.jiuyu.replay.power.constant.PowerProperties;
import com.jiuyu.replay.power.entity.*;
import com.jiuyu.replay.power.producer.UserProducer;
import com.jiuyu.replay.power.repository.dao.RoleDao;
import com.jiuyu.replay.power.repository.dao.UserDao;
import com.jiuyu.replay.power.repository.dao.UserRoleDao;
import com.jiuyu.replay.power.repository.service.*;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.*;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UserProducerImpl implements UserProducer {

    @Resource
    private UserDao userDao;
    @Resource
    private UserService userService;
    @Resource
    private UserRoleDao userRoleDao;
    @Resource
    private RoleDao roleDao;
    @Resource
    private CompanyService companyService;
    @Resource
    private UserDetailsService userDetailsService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private PowerProperties powerProperties;
    @Resource
    private SalesService salesService;
    @Resource
    private UserRoleService userRoleService;
    @Resource
    private OperationLogFeign operationLogFeign;
    @Resource
    private UserTokenService userTokenService;



    @Override
    public UserVo getByUsernameOrPhone(String username, Integer userType) {

        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        wrapper.in(ObjectUtil.isNotEmpty(userType) && userType == 0 , "user_type", 0, 2);
        wrapper.eq(ObjectUtil.isNotEmpty(userType) && userType == 1 , "user_type", 1);

        wrapper.and(ObjectUtil.isNotEmpty(userType) && userType == 0, w -> w.eq("username", username).or().eq("phone", username))
                .and(ObjectUtil.isNotEmpty(userType) && userType == 1, w -> w.eq("phone", username));

        UserEntity userEntity = this.userDao.selectOne(wrapper);
        if (userEntity != null) {
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(userEntity, userVo);
            return userVo;
        }

        return null;
    }

    @Override
    public void updateUserIp(Long id, String ips) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(id);
        userEntity.setIps(ips);

        this.userDao.updateById(userEntity);
    }

    @Override
    public UserVo getById(Long id) {

        UserEntity userEntity = this.userDao.selectById(id);
        if (userEntity != null) {
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(userEntity, userVo);
            return userVo;
        }

        return null;
    }

    @Override
    public void updatePassword(Long id, String newPassword) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(id);
        userEntity.setPassword(MD5Utils.md5(newPassword));
        userEntity.setUpdateDate(new Date());

        this.userDao.updateById(userEntity);
    }

    @Override
    public PageUtils<UserListVo> queryPage(UserListBo userListBo) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .and(ObjectUtil.isNotEmpty(userListBo.getKeyword()), w ->
                        w.like(UserEntity::getUsername, userListBo.getKeyword())
                                .or()
                                .like(UserEntity::getNickName, userListBo.getKeyword())
                                .or()
                                .like(UserEntity::getPhone, userListBo.getKeyword())
                )
                .eq(ObjectUtil.isNotEmpty(userListBo.getId()), UserEntity::getId, userListBo.getId())
                .eq(ObjectUtil.isNotEmpty(userListBo.getPhone()), UserEntity::getPhone, userListBo.getPhone())
                .eq(ObjectUtil.isNotEmpty(userListBo.getUserType()), UserEntity::getUserType, userListBo.getUserType())
                .eq(ObjectUtil.isNotEmpty(userListBo.getParentId()), UserEntity::getParentId, userListBo.getParentId())
                .between(ObjectUtil.isNotEmpty(userListBo.getStartTime()) && ObjectUtil.isNotEmpty(userListBo.getEndTime()), UserEntity::getCreateDate, userListBo.getStartTime(), userListBo.getEndTime())
                .in(ObjectUtil.isNotEmpty(userListBo.getUserIds()), UserEntity::getId, userListBo.getUserIds())
                .gt(UserEntity::getId, 1);


        IPage<UserEntity> iPage = this.userService.page(new Query<UserEntity>().getPage(userListBo.getPage(), userListBo.getLimit()), wrapper);

        PageUtils<UserListVo> pageUtils = new PageUtils<>(userListBo.getPage(), userListBo.getLimit(), iPage);

        List<UserEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {

            // 查询用户列表全部的角色
            List<Long> userIds = records.stream().map(UserEntity::getId).toList();
            List<UserRoleEntity> userRoleEntities = this.userRoleDao.selectList(new QueryWrapper<UserRoleEntity>().in("user_id", userIds));
            Set<Long> roleIds = userRoleEntities.stream().map(UserRoleEntity::getRoleId).collect(Collectors.toSet());
            List<RoleEntity> roleEntities = this.roleDao.selectBatchIds(roleIds);

            List<UserListVo> vos = records.stream().map(item -> {
                UserListVo userListVo = new UserListVo();
                BeanUtils.copyProperties(item, userListVo);

                // 设置角色
                for (UserRoleEntity userRoleEntity : userRoleEntities) {
                    if (userRoleEntity.getUserId().equals(userListVo.getId())) {
                        userListVo.setRoleName("");
                        for (RoleEntity roleEntity : roleEntities) {
                            if (roleEntity.getId().equals(userRoleEntity.getRoleId())) {
                                userListVo.setRoleName(userListVo.getRoleName() + roleEntity.getName() + "、");
                                break;
                            }
                        }
                    }
                }
                if (userListVo.getRoleName() != null && userListVo.getRoleName().length() > 0) {
                    userListVo.setRoleName(userListVo.getRoleName().substring(0, userListVo.getRoleName().length() - 1));
                }
                return userListVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public void deleteById(Long id) {

        this.userDao.deleteById(id);
    }

    @Override
    public UserVo getByUsername(String username) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);

        UserEntity userEntity = this.userDao.selectOne(wrapper);
        if (userEntity != null) {
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(userEntity, userVo);
            return userVo;
        }

        return null;
    }

    @Override
    public UserVo getByPhone(String phone) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("phone", phone);
        wrapper.eq("is_deleted", 0);
        wrapper.last("limit 1");

        UserEntity userEntity = this.userDao.selectOne(wrapper);
        if (userEntity != null) {
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(userEntity, userVo);
            return userVo;
        }

        return null;
    }

    @Override
    public UserVo getByPhoneAndType(String phone, Integer userType) {
        UserEntity userEntity = this.userDao.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getPhone, phone)
                .eq(userType == UserEnums.userType.MANAGE_ADMIN_USER.getCode(), UserEntity::getUserType, UserEnums.userType.MANAGE_ADMIN_USER.getCode())
                .in(userType == UserEnums.userType.CLIENT_USER.getCode(), UserEntity::getUserType, UserEnums.userType.CLIENT_USER.getCode(), UserEnums.userType.CLIENT_CHILD_USER.getCode())
                .last("limit 1")
        );
        if (userEntity != null) {
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(userEntity, userVo);
            return userVo;
        }

        return null;
    }

    @Override
    public UserVo getByUsernameAndType(String username, Integer userType) {

        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        wrapper.eq(userType == 1 , "user_type", userType);
        wrapper.in(userType == 0 , "user_type", 0, 2);

        UserEntity userEntity = this.userDao.selectOne(wrapper);
        if (userEntity != null) {
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(userEntity, userVo);
            return userVo;
        }

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVo create(RegisterBo registerBo) {


        UserEntity userEntity = new UserEntity();
        userEntity.setId(SnowflakeManager.nextValue());

        userEntity.setUsername(ObjectUtil.isNotEmpty(registerBo.getUsername()) ? registerBo.getUsername() : userEntity.getId().toString());
        userEntity.setPassword(ObjectUtil.isNotEmpty(registerBo.getPassword()) ? MD5Utils.md5(registerBo.getPassword()) : MD5Utils.md5(createPassword()));
        userEntity.setPhone(registerBo.getPhone());
        String nickName = ObjectUtil.isNotEmpty(registerBo.getUsername()) ? registerBo.getUsername() : DesensitizedUtil.mobilePhone(userEntity.getPhone());
        userEntity.setNickName(ObjectUtil.isNotEmpty(registerBo.getNickName()) ? registerBo.getNickName() : nickName);
        if(!StringUtils.isEmpty(registerBo.getInviteUrlCode())) {
            userEntity.setInviteUrlCode(registerBo.getInviteUrlCode());
        }
        userEntity.setAdminUserType(registerBo.getAdminUserType());
        userEntity.setCreateDate(new Date());
        userEntity.setUpdateDate(new Date());
        // 保存用户信息
        this.userDao.insert(userEntity);

        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(userEntity, userVo);
        return userVo;

    }

    /**
     * 生成随机的密码
     * @return
     */
    private String createPassword() {
        String allChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();

        StringBuilder sb = new StringBuilder(10);

        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(allChars.length());
            sb.append(allChars.charAt(index));
        }

        return sb.toString();
    }

    @Override
    public UserVo save(UserAddBo userAddBo) {

        UserEntity userEntity = new UserEntity();
        userEntity.setId(SnowflakeManager.nextValue());
        userEntity.setUsername(userAddBo.getUsername());
        userEntity.setPassword(MD5Utils.md5(userAddBo.getPassword()));
        userEntity.setNickName(userAddBo.getNickName());
        userEntity.setPhone(userAddBo.getPhone());
        userEntity.setUserType(1);
        userEntity.setAdminUserType(userAddBo.getAdminUserType());
        userEntity.setCreateDate(new Date());
        userEntity.setUpdateDate(new Date());
        this.userDao.insert(userEntity);

        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(userEntity, userVo);
        return userVo;
    }

    @Override
    public boolean update(UserUpdateBo userUpdateBo) {
        UserEntity userEntity = new UserEntity();
        BeanUtils.copyProperties(userUpdateBo, userEntity);
        userEntity.setUpdateDate(new Date());
        return this.userService.updateById(userEntity);
    }

    @Override
    public List<UserListVo> listByIds(Collection<Long> userIds) {
        List<UserEntity> userEntities = this.userService.listByIds(userIds);
        if (userEntities != null && userEntities.size() > 0) {
            List<UserListVo> vos = userEntities.stream().map(item -> {
                UserListVo userListVo = new UserListVo();
                BeanUtils.copyProperties(item, userListVo);
                return userListVo;
            }).toList();

            return vos;
        }
        return null;
    }

    @Override
    public UserInfoVo infoById(Long id) {
        UserEntity userEntity = this.userDao.selectById(id);
        if (userEntity != null) {
            UserInfoVo userInfoVo = new UserInfoVo();
            BeanUtils.copyProperties(userEntity, userInfoVo);
            userInfoVo.setPassword("");
            userInfoVo.setIps("");
            if (ObjectUtil.isNotEmpty(userInfoVo.getPhone()) && userInfoVo.getPhone().length() >= 8) {
                userInfoVo.setPhone(userEntity.getPhone().substring(0, 3) + "****" + userEntity.getPhone().substring(7));
            }
            return userInfoVo;
        }
        return null;
    }

    /**
     * 根据用户name or nick 模糊查询
     *
     * @param userListBo
     * @return
     */
    @Override
    public List<Long> selectByNameOrNick(UserListBo userListBo) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(userListBo.getKeyword())) {
            wrapper.and(w -> {
                w.like("username", userListBo.getKeyword()).or().like("nick_name", userListBo.getKeyword()).or().like("phone", userListBo.getKeyword());
            });
        }
        if (userListBo.getPhone() != null) {
            wrapper.eq("phone", userListBo.getPhone());
        }
        // wrapper.ne("user_type",2);
        //过滤超过账号
        List<Long> longs = userRoleDao.selectUserIdByRoleId();
        if (longs != null && longs.size() > 0) {
            wrapper.notIn("id", longs);
        }

        List<UserEntity> list = userService.list(wrapper);
        if (list != null && list.size() > 0) {
            return list.stream().map(UserEntity::getId).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 根据id集合获取
     *
     * @param usrids
     * @return
     */
    @Override
    public R<PageUtils<UserListVo>> selectByIds(Integer limit, Integer pege, List<Long> usrids) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        wrapper.in("id", usrids);
        IPage<UserEntity> iPage = this.userService.page(new Query<UserEntity>().getPage(pege, limit), wrapper);

        PageUtils<UserListVo> pageUtils = new PageUtils<>(pege, limit, iPage);
        List<UserListVo> vos = BeanUtil.copyToList(iPage.getRecords(), UserListVo.class);
        pageUtils.setList(vos);

        return R.ok("", pageUtils);
    }

    /**
     * 用户更新
     *
     * @param userBo
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateUser(UserBo userBo) {
        UserEntity one = userService.getById(userBo.getUserId());
        //获取原关注字段 status（冻结状态）
        Integer status = one.getStatus();
        if (ObjectUtil.isNotEmpty(userBo.getStatus())) one.setStatus(userBo.getStatus());
        if (ObjectUtil.isNotEmpty(userBo.getUserType())) one.setUserType(userBo.getUserType());
        if (ObjectUtil.isNotEmpty(userBo.getParentId())) one.setParentId(userBo.getParentId());
        one.setUpdateDate(new Date());
        userService.updateById(one);
        //保存修改记录信息-目前只关注 字段 status（冻结状态）
        if (status!=null && userBo.getStatus()!=null && !ObjectUtil.equal(status,userBo.getStatus())){
            toSaveOperationLog(status,userBo.getStatus(),userBo.getUserId());
        }
        return R.ok();
    }

    /**
     * 保存操作日志
     * @param status
     * @param statusAfter
     * @param userId
     */
    private void toSaveOperationLog(Integer status, Integer statusAfter, Long userId) {
            Map<String, Object> beforeMap = Map.of("status", status);
            Map<String, Object> afterMap = Map.of("status", statusAfter);
            UserCacheVo localUser = GlobalObject.getLocalUser();
            if (localUser != null){
                operationLogFeign.saveOptLog(Business.USER_DETAILS.getDesc(), userId, userId
                        , beforeMap, afterMap, localUser.getId()
                        , localUser.getIp(), localUser.getNickName());
                return;
            }
            log.info("[用户详情]-无登录者-无需记录-status-userId:{}", userId);
    }

    /**
     * 根据条件查询用户信息
     *
     * @param userListBo
     * @return
     */
    @Override
    public R<PageUtils<UserListVo>> selectClientList(UserListBo userListBo) {
        // 设置默认的分页参数
        if (userListBo.getPage() == null) {
            userListBo.setPage(1);
        }
        if (userListBo.getLimit() == null) {
            userListBo.setLimit(10);
        }
        // 公司名称查询
        if (ObjectUtil.isNotEmpty(userListBo.getCompanyName())) {
            if (ObjectUtil.isEmpty(userListBo.getCompanyIds())) userListBo.setCompanyIds(new ArrayList<>());
            List<CompanyEntity> list = companyService.list(new LambdaQueryWrapper<CompanyEntity>()
                    .like(CompanyEntity::getName, userListBo.getCompanyName())
            );
            if (ObjectUtil.isNotEmpty(list)) {
                List<Long> companyIds = list.stream().map(CompanyEntity::getId).toList();
                userListBo.getCompanyIds().addAll(companyIds);
            } else {
                return R.ok(new PageUtils<>(new ArrayList<>(), 0, userListBo.getLimit(), userListBo.getPage()));
            }
        }
        // 行业查询-如果有公司ids也查询公司
        if (ObjectUtil.isNotEmpty(userListBo.getTradeId()) || ObjectUtil.isNotEmpty(userListBo.getCompanyIds()) || ObjectUtil.isNotEmpty(userListBo.getAgentId())) {
            List<UserDetailsEntity> list = userDetailsService.list(new LambdaQueryWrapper<UserDetailsEntity>()
                    .eq(ObjectUtil.isNotEmpty(userListBo.getTradeId()), UserDetailsEntity::getTradeId, userListBo.getTradeId())
                    .in(ObjectUtil.isNotEmpty(userListBo.getCompanyIds()), UserDetailsEntity::getCompanyId, userListBo.getCompanyIds())
                    .in(ObjectUtil.isNotEmpty(userListBo.getAgentId()), UserDetailsEntity::getAgentId, userListBo.getAgentId())
            );
            if (ObjectUtil.isNotEmpty(list)) {
                // 初始化 userIds 列表
                List<Long> currentUserIds = list.stream().map(UserDetailsEntity::getUserId).collect(Collectors.toList());
                if (userListBo.getUserIds() == null) {
                    userListBo.setUserIds(currentUserIds);
                } else {
                    List<Long> list1 = userListBo.getUserIds().stream().filter(currentUserIds::contains).toList();
                    if (ObjectUtil.isEmpty(list1)) {
                        return R.ok(new PageUtils<>(new ArrayList<>(), 0, userListBo.getLimit(), userListBo.getPage()));
                    }
                    userListBo.setUserIds(list1);
                }
            } else {
                return R.ok(new PageUtils<>(new ArrayList<>(), 0, userListBo.getLimit(), userListBo.getPage()));
            }
        }

        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>();
        //查询
        wrapper.and(ObjectUtil.isNotEmpty(userListBo.getKeyword()), w ->
                        w.like(UserEntity::getUsername, userListBo.getKeyword())
                                .or()
                                .like(UserEntity::getNickName, userListBo.getKeyword())
                                .or()
                                .like(UserEntity::getPhone, userListBo.getKeyword())
                )
                .eq(ObjectUtil.isNotEmpty(userListBo.getParentId()), UserEntity::getParentId, userListBo.getParentId())
                .eq(ObjectUtil.isNotEmpty(userListBo.getUserType()), UserEntity::getUserType, userListBo.getUserType())
                .eq(ObjectUtil.isNotEmpty(userListBo.getStatus()), UserEntity::getStatus, userListBo.getStatus())
                .like(ObjectUtil.isNotEmpty(userListBo.getUserName()), UserEntity::getUsername, userListBo.getUserName())
                .like(ObjectUtil.isNotEmpty(userListBo.getNickName()), UserEntity::getNickName, userListBo.getNickName())
                .like(ObjectUtil.isNotEmpty(userListBo.getPhone()), UserEntity::getPhone, userListBo.getPhone())
                .in(ObjectUtil.isNotEmpty(userListBo.getUserIds()), UserEntity::getId, userListBo.getUserIds())
                .in(ObjectUtil.isNotEmpty(userListBo.getInviteUrlCodes()), UserEntity::getInviteUrlCode, userListBo.getInviteUrlCodes())
                .ge(ObjectUtil.isNotEmpty(userListBo.getStartTime()), UserEntity::getCreateDate, userListBo.getStartTime())
                .le(ObjectUtil.isNotEmpty(userListBo.getEndTime()), UserEntity::getCreateDate, userListBo.getEndTime())
                .ne(ObjectUtil.isEmpty(userListBo.getUserType()), UserEntity::getUserType, 1)
                .gt(UserEntity::getId, 1)
        ;

        // 将所有数据查出以便进行特殊排序
        if (userListBo.getSpecialSorting() != null && userListBo.getSpecialSorting() == 1){
            userListBo.setLimit(-1);
        }

        IPage<UserEntity> iPage = this.userService.page(new Query<UserEntity>().getPage(userListBo.getPage(), userListBo.getLimit()), wrapper);
        PageUtils<UserListVo> pageUtils = new PageUtils<>(userListBo.getPage(), userListBo.getLimit(), iPage);

        // 手动设置分页参数
        if (userListBo.getSpecialSorting() != null && userListBo.getSpecialSorting() == 1){
            pageUtils.setTotalCount(iPage.getRecords().size());
            pageUtils.setTotalPage((iPage.getRecords().size() + 10 - 1) / 10);
            pageUtils.setCurrPage(userListBo.getPage());
            pageUtils.setPageSize(userListBo.getLimit());
        }

        List<UserEntity> records = iPage.getRecords();
        if (ObjectUtil.isNotEmpty(records)) {
            List<UserListVo> list = BeanUtil.copyToList(records, UserListVo.class);

            List<Long> userIds = records.stream().map(UserEntity::getId).toList();
            List<UserDetailsEntity> userDetailsEntities = userDetailsService.list(new LambdaQueryWrapper<UserDetailsEntity>().in(UserDetailsEntity::getUserId, userIds));
            List<SalesEntity> salesEntities = salesService.list();

            if (ObjectUtil.isNotEmpty(userDetailsEntities)) {
                List<UserDetailsInfoVo> userDetailsList = BeanUtil.copyToList(userDetailsEntities, UserDetailsInfoVo.class);
                // 查询用户详情信息
                List<Long> companyIds = userDetailsList.stream().map(UserDetailsVo::getCompanyId).toList();

                List<CompanyEntity> companyList = companyService.listByIds(companyIds);
                if (ObjectUtil.isNotEmpty(companyList)) {
                    List<CompanyVo> companyVos = BeanUtil.copyToList(companyList, CompanyVo.class);
                    DataUtils.setFieldObject(userDetailsList, "companyId", "company", companyVos, "id");
                }

                list.forEach(item -> {
                    UserDetailsInfoVo userDetails = userDetailsList.stream().filter(val -> val.getUserId().equals(item.getId()))
                            .findFirst().orElse(null);
                    if (userDetails != null) {
                        SalesEntity salesEntity = salesEntities.stream().filter(sal -> sal.getId().equals(userDetails.getSaleId())).findFirst().orElse(null);
                        item.setTradeId(userDetails.getTradeId());
                        item.setCompanyId(userDetails.getCompanyId());
                        item.setChannelId(userDetails.getChannelId());
                        item.setAgentSaleId(userDetails.getAgentSaleId());
                        if (userDetails.getAnchorType() == 1 && userDetails.getCompany() != null) {
                            item.setCompanyName(userDetails.getCompany().getName());
                            item.setLinkman(userDetails.getCompany().getLinkman());
                            item.setPhones(userDetails.getCompany().getPhones());
                        }
                        if (userDetails.getWxName() != null){
                            item.setWxName(userDetails.getWxName());
                        }
                        if (salesEntity != null){
                            item.setSalesName(salesEntity.getSalesName());
                        }
                    }
                });
            }
            pageUtils.setList(list);
        }

        return R.ok(pageUtils);
    }

    @Override
    public PageUtils<UserListVo> pageListNew(UserListBo userListBo) {
        PageUtils<UserListVo> result = new PageUtils<>(new ArrayList<>(), 0, userListBo.getLimit(), userListBo.getPage());
        // 公司名称查询
        if (ObjectUtil.isNotEmpty(userListBo.getCompanyName())) {
            if (ObjectUtil.isEmpty(userListBo.getCompanyIds())) {
                userListBo.setCompanyIds(new ArrayList<>());
            }
            List<Long> companyIds = companyService.list(new LambdaQueryWrapper<CompanyEntity>()
                    .like(CompanyEntity::getName, userListBo.getCompanyName())
                    .select(CompanyEntity::getId)
            ).stream().map(CompanyEntity::getId).toList();
            if (ObjectUtil.isNotEmpty(companyIds)) {
                userListBo.getCompanyIds().addAll(companyIds);
            } else {
                return result;
            }
        }

        Page<ServerUserListVo> page = userService.pageListNew(userListBo);

        if (ObjectUtil.isNotEmpty(page.getRecords())) {
            List<UserListVo> list = BeanUtil.copyToList(page.getRecords(), UserListVo.class);

            // 销售
            List<String> salesIds = list.stream().map(UserListVo::getSalesId).toList();
            Map<Long, SalesEntity> salesMap = new HashMap<>();
            if (ObjectUtil.isNotEmpty(salesIds)) {
                salesMap = salesService.lambdaQuery()
                        .in(SalesEntity::getId, salesIds)
                        .list()
                        .stream().collect(Collectors.toMap(SalesEntity::getId, Function.identity(), (a, b) -> a));
            }

            // 查询公司信息
            List<Long> companyIds = list.stream().map(UserListVo::getCompanyId).toList();
            Map<Long, CompanyEntity> companyMap = new HashMap<>();
            if (ObjectUtil.isNotEmpty(salesIds)) {
                companyMap = companyService.lambdaQuery()
                        .in(CompanyEntity::getId, companyIds)
                        .list()
                        .stream()
                        .collect(Collectors.toMap(CompanyEntity::getId, Function.identity(), (a, b) -> a));
            }

            // 赋值
            for (UserListVo item : list) {
                if (ObjectUtil.equals(item.getAnchorType(), 1)) {
                    CompanyEntity company = companyMap.get(item.getCompanyId());
                    if (ObjectUtil.isNotEmpty(company)) {
                        item.setCompanyName(company.getName());
                        item.setLinkman(company.getLinkman());
                        item.setPhones(company.getPhones());
                    }
                }

                if (ObjectUtil.isNotEmpty(item.getSalesId())) {
                    SalesEntity sales = salesMap.get(NumberUtil.parseLong(item.getSalesId(), 0L));
                    if (ObjectUtil.isNotEmpty(sales)) {
                        item.setSalesName(sales.getSalesName());
                    }
                }
                // 版本过期时间
                if (item.getExpirationDate() != null) {
                    item.setExpireTime(DateUtil.betweenDay(item.getExpirationDate(), new Date(), false));
                }

                // 最后一次分析时间
//                if (ObjectUtil.isNotEmpty(item.getLastAnalysis())) {
//                    item.setLongNotAnalysis(DateUtil.betweenDay(item.getLastAnalysis(), new Date(), false));
//                }
            }

            result.setList(list);
            result.setTotalCount((int) page.getTotal());
        }

        return result;
    }

    /**
     * 服务端批量删除用户
     *
     * @param ids
     * @return
     */
    @Override
    public R<String> deleteByIds(List<Long> ids) {
        userService.removeByIds(ids);
        return R.ok();
    }

    /**
     * 服务端修改用户账号类型
     *
     * @param userUpdateBo
     * @return
     */
    @Override
    public R<String> updateByUserId(UserUpdateBo userUpdateBo) {
        UserEntity entity = userService.getById(userUpdateBo.getId());
        entity.setUpdateDate(new Date());
        entity.setUserType(userUpdateBo.getUserType());
        entity.setUsername(userUpdateBo.getUsername());
        entity.setNickName(userUpdateBo.getNickName());
        entity.setPhone(userUpdateBo.getPhone());
        userService.updateById(entity);

        return R.ok();
    }

    @Override
    public List<UserVo> listByClientAll() {
        List<UserEntity> list = userService.list(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserType, 0)
        );
        return BeanUtil.copyToList(list, UserVo.class);
    }


    @Override
    public List<UserVo> listByParentId(Long subUserId) {
        List<UserEntity> list = userService.list(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getParentId, subUserId)
        );
        return BeanUtil.copyToList(list, UserVo.class);
    }

    @Override
    public List<UserInfoVo> listParentUser(List<Long> userIds) {
        List<UserEntity> list = userService.list(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserType, 0)
                .in(ObjectUtil.isNotEmpty(userIds) ,UserEntity::getId, userIds)
        );

        Map<Long, List<UserEntity>> childMap = userService.list(new QueryWrapper<UserEntity>()
                .lambda()
                .eq(UserEntity::getUserType, 2)
                .in(ObjectUtil.isNotEmpty(userIds) ,UserEntity::getParentId, userIds)
        ).stream().collect(Collectors.groupingBy(UserEntity::getParentId));

        return BeanUtil.copyToList(list, UserInfoVo.class)
                .stream()
                .peek(item -> {
                            item.setChildAccountCount(0);
                            item.setChildUserList(new ArrayList<>());
                            List<UserEntity> childList = childMap.get(item.getId());
                            if (ObjectUtil.isNotEmpty(childList)){
                                item.setChildAccountCount(childList.size());
                                item.setChildUserList(BeanUtil.copyToList(childList, UserVo.class));
                            }
                        }
                )
                .filter(item -> item.getChildAccountCount() > 0)
                .toList();
    }

    @Override
    public List<UserVo> listAllByParent() {
        List<UserEntity> parentCount = userService.list(new QueryWrapper<UserEntity>()
                .lambda()
                .eq(UserEntity::getUserType, 2)
                .gt(UserEntity::getParentId, 0)
        );
        return BeanUtil.copyToList(parentCount, UserVo.class);
    }


    @Override
    public PageUtils<UserListManageVo> listManage(UserListBo userListBo) {
        IPage<UserEntity> iPage = this.userService.listManage(userListBo);
        PageUtils<UserListManageVo> pageUtils = new PageUtils<>(userListBo.getPage(), userListBo.getLimit(), iPage);

        List<UserEntity> records = iPage.getRecords();
        if (ObjectUtil.isNotEmpty(records)) {
            List<UserListManageVo> list = BeanUtil.copyToList(records, UserListManageVo.class);

            List<Long> userIds = list.stream().map(UserVo::getId).toList();


            List<UserRoleEntity> userRoleEntities = userRoleDao.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                    .in(UserRoleEntity::getUserId, userIds)
            );
            if (ObjectUtil.isNotEmpty(userRoleEntities)){

                Map<Long, Integer> employeeStatusMap = userDetailsService.lambdaQuery()
                        .in(UserDetailsEntity::getUserId, userIds)
                        .select(UserDetailsEntity::getUserId, UserDetailsEntity::getEmployeeStatus)
                        .list()
                        .stream()
                        .collect(Collectors.toMap(UserDetailsEntity::getUserId, UserDetailsEntity::getEmployeeStatus, (existing, replacement) -> replacement));

                List<Long> roleIds = userRoleEntities.stream().map(UserRoleEntity::getRoleId).distinct().toList();
                List<RoleEntity> roleList = roleDao.selectBatchIds(roleIds);
                Map<Long, List<UserRoleEntity>> userRoleList = userRoleEntities.stream().collect(Collectors.groupingBy(UserRoleEntity::getUserId));

                list.forEach(item -> {

                    // 员工状态
                    item.setEmployeeStatus(employeeStatusMap.getOrDefault(item.getId(), 0));

                    // 角色列表
                    List<UserRoleEntity> roleTempList = userRoleList.get(item.getId());
                    if (ObjectUtil.isNotEmpty(roleTempList)){
                        List<RoleEntity> yesRoleList = roleList.stream()
                                .filter(val -> roleTempList.stream().anyMatch(val2 -> val2.getRoleId().equals(val.getId()))).toList();
                        item.setRoleList(BeanUtil.copyToList(yesRoleList, RoleVo.class));
                    }

                });
            }
            pageUtils.setList(list);
        }

        return pageUtils;
    }


    /**
     * 管理员用户列表（默认支持分页，传limit -1 为不分页）
     * 1、支持关键字搜索：用户名、昵称、手机号（统一关键词）
     * 2、支持用户名搜索
     * 3、支持手机号搜索
     * 4、支持用户昵称搜索
     * @param userListBo
     * @return
     */
    @Override
    public PageUtils<UserVo> listAdmin(UserListBo userListBo) {
        //构造器
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>();
        // 关键字搜索：用户名、昵称、手机号（统一关键词）
        wrapper.and(ObjectUtil.isNotEmpty(userListBo.getKeyword()), w ->
                w.like(UserEntity::getUsername, userListBo.getKeyword())
                        .or()
                        .like(UserEntity::getNickName, userListBo.getKeyword())
                        .or()
                        .like(UserEntity::getPhone, userListBo.getKeyword())
        )
                //用户类型 0：普通用户 1：后台管理员 2：子账号  已锁定后台管理员
                .eq(ObjectUtil.isNotEmpty(userListBo.getUserType()), UserEntity::getUserType, userListBo.getUserType())
                .like(StringUtil.isNotBlank(userListBo.getUserName()), UserEntity::getUsername, userListBo.getUserName())
                .like(StringUtil.isNotBlank(userListBo.getNickName()), UserEntity::getNickName, userListBo.getNickName())
                .like(StringUtil.isNotBlank(userListBo.getPhone()), UserEntity::getPhone, userListBo.getPhone());
        //进行分页查询
        IPage<UserEntity> iPage = this.userService.page(new Query<UserEntity>().getPage(userListBo.getPage(), userListBo.getLimit()), wrapper);
        //封装分页数据
        PageUtils<UserVo> pageUtils = new PageUtils<>(userListBo.getPage(), userListBo.getLimit(), iPage);
        //转换数据
        List<UserEntity> records = iPage.getRecords();
        if (ObjectUtil.isNotEmpty(records)) {
            List<UserVo> list = BeanUtil.copyToList(records, UserVo.class);
            pageUtils.setList(list);
        }
        return pageUtils;
    }

    /**
     * 重置随机密码
     *
     * @param id
     * @param psw
     * @return
     */
    @Override
    public boolean resetRandomPassword(Long id, String psw) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(id);
        userEntity.setPassword(MD5Utils.md5(psw));
        userEntity.setUpdateDate(new Date());
        return userService.updateById(userEntity);
    }

    @Override
    public List<Long> getTenantIdsByUserIds(List<Long> userIds) {
        if (ObjectUtil.isEmpty(userIds)) {
            return List.of();
        }
        return userService.lambdaQuery()
                .in(UserEntity::getId, userIds)
                .select(UserEntity::getActiveTenantId)
                .list()
                .stream()
                .map(UserEntity::getActiveTenantId)
                .toList();
    }


    /**
     * 获取用户手机号
     *
     * @param userIds 用户列表
     *
     * @return 手机号
     */
    @Override
    public Map<Long, String> getUserPhoneMap(Collection<Long> userIds) {
        if (EmptyUtil.isEmpty(userIds)) {
            return Map.of();
        }
       return userService.lambdaQuery().select(UserEntity::getId, UserEntity::getPhone)
                .in(UserEntity::getId, userIds)
                .list()
                .stream().filter(user -> EmptyUtil.isNotEmpty(user.getPhone()))
            .collect(Collectors.toMap(UserEntity::getId, UserEntity::getPhone));
    }

    /**
     * 获取用户昵称
     *
     * @param userIds 用户列表
     *
     * @return 昵称
     */
    @Override
    public Map<Long, String> getUserNameMap(Collection<Long> userIds) {
        if (EmptyUtil.isEmpty(userIds)) {
            return Map.of();
        }
        return userService.lambdaQuery().select(UserEntity::getId, UserEntity::getNickName, UserEntity::getUsername)
                .in(UserEntity::getId, userIds)
                .list()
                .stream()
            .collect(Collectors.toMap(UserEntity::getId, u -> EmptyUtil.isNotEmpty(u.getNickName()) ? u.getNickName() : u.getUsername()));
    }

    @Override
    public R<List<Long>> getUserChild(List<Long> userIds) {
        if (ObjectUtil.isEmpty(userIds)) {
            return R.error(StatusCode.MEDIA_TYPE_EX.getCode(), "参数不能为空");
        }
        List<UserEntity> parentId = userService.list(new QueryWrapper<UserEntity>().in("parent_id", userIds));
        return R.ok("",parentId.stream().map(UserEntity::getId).toList());
    }

    @Override
    public void updateActiveTenantId(Long userId, Long tenantId) {
        UserEntity userEntity = this.userService.getById(userId);
        userEntity.setActiveTenantId(tenantId);
        this.userService.updateById(userEntity);
    }

    @Override
    public void updateTenantIdRedisCacheById(Long userId, Long tenantId) {

        // 修改客户端登录用户缓存信息的租户id
        Map<Object, Object> entries = this.redisTemplate.opsForHash().entries(powerProperties.getUserLoginInfoRedisKey() + userId);
        if(entries.size() > 0) {
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                String token = (String) entry.getKey();
                UserCacheVo userCacheVo = (UserCacheVo) this.redisTemplate.opsForValue().get(powerProperties.getUserLoginTokenRedisKey() + token);
                if(userCacheVo != null) {
                    userCacheVo.setActiveTenantId(tenantId);
                    this.redisTemplate.opsForValue().set(powerProperties.getUserLoginTokenRedisKey() + token, userCacheVo, Duration.ofDays(30));
                }
            }
        }
    }

    /**
     * 查询所有用户ID
     * @return
     */
    @Override
    public List<Long> listUserId() {
        return userService.list(new LambdaQueryWrapper<UserEntity>().select(UserEntity::getId))
                .stream().map(UserEntity::getId).collect(Collectors.toList());
    }

    @Override
    public Long getUserIdByUserNickName(String userNickName) {
        UserEntity nickName = userService.getOne(new QueryWrapper<UserEntity>().eq("nick_name", userNickName));
        if (nickName != null) {
            return nickName.getId();
        }
        return null;
    }

    @Override
    public List<Long> listIdsByLikePhoneOrName(String keyword) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        wrapper.and(w -> {
            w.like("phone", keyword).or().like("nick_name", keyword);
        });

        List<UserEntity> userEntities = this.userService.list(wrapper);
        if(userEntities != null && userEntities.size() > 0) {
            List<Long> userIds = userEntities.stream().map(UserEntity::getId).toList();
            return userIds;
        }

        return null;
    }

    @Override
    public List<UserVo> listByRoleId(Long roleId) {

        List<UserRoleEntity> userRoleEntities = this.userRoleService.list(new QueryWrapper<UserRoleEntity>().eq("role_id", roleId));
        if(userRoleEntities != null && userRoleEntities.size() > 0) {
            List<Long> userIds = userRoleEntities.stream().map(UserRoleEntity::getUserId).collect(Collectors.toList());
            List<UserEntity> userEntities = this.userService.listByIds(userIds);
            if(userEntities != null && userEntities.size() > 0) {
                List<UserVo> userVoList = userEntities.stream().map(item -> {
                    UserVo userInfoVo = new UserVo();
                    BeanUtils.copyProperties(item, userInfoVo);
                    return userInfoVo;
                }).collect(Collectors.toList());

                return userVoList;
            }
        }

        return null;
    }

    @Override
    public List<UserInfoVo> listByChannelIds(List<Long> channelIds) {

        if(channelIds != null && channelIds.size() > 0) {
            List<UserDetailsEntity> userDetailsEntities = this.userDetailsService.list(new QueryWrapper<UserDetailsEntity>().in("channel_id", channelIds));
            if(userDetailsEntities != null && userDetailsEntities.size() > 0) {
                List<Long> userIds = userDetailsEntities.stream().map(UserDetailsEntity::getUserId).toList();
                List<UserEntity> userEntities = this.userService.listByIds(userIds);
                if(userEntities != null && userEntities.size() > 0) {
                    List<UserInfoVo> userInfoVos = userEntities.stream().map(item -> {
                        UserInfoVo userInfoVo = new UserInfoVo();
                        BeanUtils.copyProperties(item, userInfoVo);
                        return userInfoVo;
                    }).collect(Collectors.toList());

                    return userInfoVos;
                }
            }
        }

        return null;
    }

    @Override
    public UserVo getByPhoneTenant(String phone) {
        UserVo userVo = new UserVo();
        if (!phone.isBlank()) {
            List<UserEntity> list = userService.lambdaQuery()
                    .eq(UserEntity::getPhone, phone)
                    .ne(UserEntity::getUserType, 1)
                    .list();
            if (!list.isEmpty()) {
                UserEntity userEntity = list.get(0);
                BeanUtils.copyProperties(userEntity, userVo);
                return userVo;
            }
        }
        return null;
    }

    @Override
    public Long getUserIdNoPre(Long userId) {
        return toSel(userId);
    }

    private Long toSel(Long userId) {
        UserEntity user = userService.getById(userId);
        if (Objects.isNull(user)){
            throw new RRException("查无此人");
        }
        if (user.getParentId()==0){
            return user.getId();
        }else {
            return toGet(user.getParentId());
        }
    }

    @Override
    public long countByAdminUser(String phone, Long noUserId) {
        return userService.lambdaQuery()
                .eq(UserEntity::getPhone, phone)
                .eq(UserEntity::getUserType, UserEnums.userType.MANAGE_ADMIN_USER.getCode())
                .ne(ObjectUtil.isNotEmpty(noUserId), UserEntity::getId, noUserId)
                .count()
                ;
    }

    @Override
    public Integer countByActiveTenantId(Long tenantId) {
        if (ObjectUtil.isEmpty(tenantId)) {
            return 0;
        }
        Long count = userService.lambdaQuery()
                .eq(UserEntity::getActiveTenantId, tenantId)
                .eq(UserEntity::getIsDeleted, 0)
                .count();
        return count == null ? 0 : count.intValue();
    }


    @Override
    public List<UserEntity> updateStatusByParentId(Long parentId, Integer status) {
        List<UserEntity> list = userService.lambdaQuery()
                .eq(UserEntity::getParentId, parentId)
                .eq(UserEntity::getUserType, UserEnums.userType.MANAGE_ADMIN_USER.getCode())
                .eq(UserEntity::getAdminUserType, UserEnums.adminUserType.AGENT_SALE.getCode())
                .list();
        if (ObjectUtil.isEmpty(list)) {
            return List.of();
        }

        // 修改状态
        List<Long> userIds = list.stream().map(UserEntity::getId).toList();
        boolean update = userService.lambdaUpdate()
                .in(UserEntity::getId, userIds)
                .set(UserEntity::getStatus, status)
                .update();
        if (!update) {
            return List.of();
        }

        // 判断是否是冻结，是就获取token
        if (status == 0) {
            return List.of();
        }
        return list;
    }

    @Override
    public boolean updateStatusById(Long id, Integer status) {
        if (ObjectUtil.isEmpty(id) || ObjectUtil.isEmpty(status)) {
            return false;
        }
        return userService.lambdaUpdate()
                .eq(UserEntity::getId, id)
                .set(UserEntity::getStatus, status)
                .update();
    }

    @Override
    public List<UserTokenEntity> tokenListByUserIds(List<Long> userIds) {
        List<UserTokenEntity> userTokenList = userTokenService.lambdaQuery()
                .in(UserTokenEntity::getUserId, userIds)
                .ge(UserTokenEntity::getExpireTime, new Date())
                .select(UserTokenEntity::getUserId, UserTokenEntity::getToken)
                .list();

        if (ObjectUtil.isEmpty(userTokenList)) {
            return List.of();
        }

        return userTokenList;
    }

    private Long toGet(Long userId) {
        return toSel(userId);
    }

    @Override
    public List<UserVo> listUserRollupFieldAll() {
        List<UserEntity> list = userService.lambdaQuery()
                .select(UserEntity::getId, UserEntity::getActiveTenantId, UserEntity::getInviteUrlCode)
                .list();
        if (ObjectUtil.isEmpty(list)) {
            return List.of();
        }
        return BeanUtil.copyToList(list, UserVo.class);
    }

    @Override
    public PageUtils<UserVo> clientUserPageList(ClientPageList listBo) {
        if (ObjectUtil.equals(listBo.getUserType(), UserEnums.userType.MANAGE_ADMIN_USER.getCode())) {
            throw new BusinessException(StatusCode.OPERATION_EX.getCode(), "当前接口只能查询爱复盘的用户");
        }
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
                .like(ObjectUtil.isNotEmpty(listBo.getPhone()), UserEntity::getPhone, listBo.getPhone())
                .like(ObjectUtil.isNotEmpty(listBo.getNickName()), UserEntity::getNickName, listBo.getNickName())
                .eq(ObjectUtil.isNotEmpty(listBo.getUserType()), UserEntity::getUserType, listBo.getUserType())
                .ne(ObjectUtil.isEmpty(listBo.getUserType()), UserEntity::getUserType, UserEnums.userType.MANAGE_ADMIN_USER.getCode());

        IPage<UserEntity> iPage = userService.page(new Query<UserEntity>().getPageNoSort(listBo.getPage(), listBo.getLimit()), wrapper);

        PageUtils<UserVo> utils = new PageUtils<>(listBo.getPage(), listBo.getLimit(), iPage);
        if (ObjectUtil.isNotEmpty(iPage.getRecords())) {
            utils.setList(BeanUtil.copyToList(iPage.getRecords(), UserVo.class));
        }
        return utils;
    }

    @Override
    public List<UserDto> listByPhones(List<String> phoneList) {
        List<UserEntity> list = userService.lambdaQuery()
                .in(UserEntity::getPhone, phoneList)
                .ne(UserEntity::getUserType, 1)
                .list();
        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, UserDto.class);
        }
        return List.of();
    }

    @Override
    public Long getSubUserCountByUserId(Long userId) {
        Long count = userService.lambdaQuery()
                .eq(UserEntity::getParentId, userId)
                .eq(UserEntity::getUserType, UserEnums.userType.CLIENT_CHILD_USER.getCode())
                .count();
        return ObjectUtil.defaultIfNull(count, 0L);
    }

    @Override
    public void updateEmployeeStatus(Long id, Integer employeeStatus) {
        int status = 0;
        if (employeeStatus == 0) {
            status = 1;
        }
        userService.lambdaUpdate()
                .eq(UserEntity::getId, id)
                .set(UserEntity::getStatus, status)
                .update();
        userDetailsService.lambdaUpdate()
                .eq(UserDetailsEntity::getUserId, id)
                .set(UserDetailsEntity::getEmployeeStatus, employeeStatus)
                .update();
    }

    @Override
    public DashboardStatisticsVo dashboardStatistics(Integer trialOrder) {
        DashboardStatisticsVo res = new DashboardStatisticsVo();
        long totalCount = userService.lambdaQuery()
                .in(UserEntity::getUserType, UserEnums.userType.CLIENT_USER.getCode(), UserEnums.userType.CLIENT_CHILD_USER.getCode())
                .count();
        if (totalCount == 0) {
            return res;
        }

        res.setTotalUserCount((int) totalCount);


        DashboardStatisticsVo dashboardStatisticsVo = userService.prepareExpiredUserCount(trialOrder);
        res.setWithin3Days(dashboardStatisticsVo.getWithin3Days());
        res.setWithin3DaysRate(res.getWithin3Days() == 0 ? 0.0 : BigDecimal.valueOf(res.getWithin3Days()).divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP).doubleValue());

        res.setWithin7Days(dashboardStatisticsVo.getWithin7Days());
        res.setWithin7DaysRate(res.getWithin7Days() == 0 ? 0.0 : BigDecimal.valueOf(res.getWithin7Days()).divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP).doubleValue());

        DashboardStatisticsVo dashboardStatisticsVo1 = userService.alreadyExpiredUserCount(trialOrder);
        res.setExpired3Days(dashboardStatisticsVo1.getExpired3Days());
        res.setExpired7Days(dashboardStatisticsVo1.getExpired7Days());
        res.setOutside7Days(dashboardStatisticsVo1.getOutside7Days());
        return res;
    }

    @Override
    public PaidDashboardStatisticsVo paidDashboardStatistics() {
        PaidDashboardStatisticsVo res = new PaidDashboardStatisticsVo();
        long totalCount = userService.lambdaQuery()
                .in(UserEntity::getUserType, UserEnums.userType.CLIENT_USER.getCode(), UserEnums.userType.CLIENT_CHILD_USER.getCode())
                .count();
        if (totalCount == 0) {
            return res;
        }
        res.setTotalUserCount((int) totalCount);

        PaidDashboardStatisticsVo prepareVo = userService.prepareExpiredPaidUserCount();
        res.setWithin15Days(prepareVo.getWithin15Days());
        res.setWithin30Days(prepareVo.getWithin30Days());
        res.setWithin60Days(prepareVo.getWithin60Days());
        res.setWithin90Days(prepareVo.getWithin90Days());

        PaidDashboardStatisticsVo expiredVo = userService.alreadyExpiredPaidUserCount();
        res.setExpired7Days(expiredVo.getExpired7Days());
        res.setExpired15Days(expiredVo.getExpired15Days());
        res.setExpired30Days(expiredVo.getExpired30Days());
        res.setExpired60Days(expiredVo.getExpired60Days());
        return res;
    }

    @Override
    public PageUtils<DashboardListVo> pageDashboardList(DashboardListBo dashboardListBo) {
        Page<DashboardListVo> iPage = this.userService.pageDashboardList(dashboardListBo);

        PageUtils<DashboardListVo> pageUtils = new PageUtils<>(dashboardListBo.getPage(), dashboardListBo.getLimit(), iPage);

        List<DashboardListVo> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {

            // 查询用户列表全部的角色
            List<Long> salesIds = records.stream().map(DashboardListVo::getSalesId).distinct().toList();
            Map<Long, String> salesMap = salesService.lambdaQuery()
                    .in(SalesEntity::getId, salesIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(SalesEntity::getId, SalesEntity::getSalesName));

            for (DashboardListVo item : records) {
                if (ObjectUtil.isNotEmpty(item.getSalesId())) {
                    item.setSalesName(salesMap.getOrDefault(item.getSalesId(), null));
                }

                if (ObjectUtil.isNotEmpty(item.getLevel())) {
                    if (item.getLevel() <= 0 && item.getExpirationTwoDate() != null) {
                        item.setOrderStatus(1);
                        item.setExpirationDate(item.getExpirationTwoDate());
                    } else {
                        item.setOrderStatus(0);
                        item.setExpirationDate(item.getExpirationOneDate());
                    }
                    if (ObjectUtil.isNotEmpty(item.getExpirationDate())) {
                        if (item.getOrderStatus() == 0 && DateUtil.date().compareTo(item.getExpirationDate()) > 0) {
                            item.setExpireDays(0);
                        } else {
                            item.setExpireDays(Math.abs((int) DateUtil.between(item.getExpirationDate(), DateUtil.date(), DateUnit.HOUR)));
                        }
                    }
                }
            }
        }
        pageUtils.setList(records);
        return pageUtils;
    }

    @Override
    public SalesCardStatisticsDataVo statisticsRegister(SalesCardStatisticsDataBo salesCardStatisticsData) {
        return userDao.statisticsRegister(salesCardStatisticsData);
    }

    @Override
    public SalesCardStatisticsDataVo statisticsRegisterTeam(TeamCardStatisticsDataBo teamCardStatisticsDataBo) {
        return userDao.statisticsRegisterTeam(teamCardStatisticsDataBo);
    }

    @Override
    public SalesCardStatisticsDataVo statisticsTrial(SalesCardStatisticsDataBo salesCardStatisticsData) {
        return userDao.statisticsTrial(salesCardStatisticsData);
    }

    @Override
    public SalesCardStatisticsDataVo statisticsTrialTeam(TeamCardStatisticsDataBo teamCardStatisticsDataBo) {
        return userDao.statisticsTrialTeam(teamCardStatisticsDataBo);
    }

    @Override
    public SalesCardStatisticsDataVo statisticsRenewalExpires(SalesCardStatisticsDataBo salesCardStatisticsData) {
        return userDao.statisticsRenewalExpires(salesCardStatisticsData);
    }

    @Override
    public SalesCardStatisticsDataVo statisticsRenewalExpiresTeam(TeamCardStatisticsDataBo teamCardStatisticsDataBo) {
        return userDao.statisticsRenewalExpiresTeam(teamCardStatisticsDataBo);
    }

    @Override
    public SalesCardStatisticsDataVo statisticsDealCustomers(SalesCardStatisticsDataBo salesCardStatisticsData) {
        return userDao.statisticsDealCustomers(salesCardStatisticsData);
    }

    @Override
    public SalesCardStatisticsDataVo statisticsDealCustomersTeam(TeamCardStatisticsDataBo teamCardStatisticsDataBo) {
        return userDao.statisticsDealCustomersTeam(teamCardStatisticsDataBo);
    }

    @Override
    public List<UserAmbitionStatisticsVo> salesEchartsStatisticsData(SalesCardStatisticsDataBo salesCardStatisticsData) {
        return userDao.salesEchartsStatisticsData(salesCardStatisticsData);
    }

    @Override
    public PageUtils<SalesStatisticsList> salesTrialAboutTo3DayExpires(SalesListStatisticsPageBo bo) {
        Page<SalesStatisticsList> iPage = userDao.salesTrialAboutTo3DayExpires(new Page<>(bo.getPage(), bo.getLimit()), bo);
        PageUtils<SalesStatisticsList> pageUtils = new PageUtils<>(bo.getPage(), bo.getLimit(), iPage);
        pageUtils.setList(iPage.getRecords());
        return pageUtils;
    }

    @Override
    public PageUtils<SalesStatisticsList> salesAboutTo3Day(SalesListStatisticsPageBo bo) {
        Page<SalesStatisticsList> iPage = userDao.salesAboutTo3Day(new Page<>(bo.getPage(), bo.getLimit()), bo);
        PageUtils<SalesStatisticsList> pageUtils = new PageUtils<>(bo.getPage(), bo.getLimit(), iPage);
        pageUtils.setList(iPage.getRecords());
        return pageUtils;
    }

    @Override
    public PageUtils<SalesStatisticsList> salesTrialAboutTo15DayRenewal(SalesListStatisticsPageBo bo) {
        Page<SalesStatisticsList> iPage = userDao.salesTrialAboutTo15DayRenewal(new Page<>(bo.getPage(), bo.getLimit()), bo);
        PageUtils<SalesStatisticsList> pageUtils = new PageUtils<>(bo.getPage(), bo.getLimit(), iPage);
        pageUtils.setList(iPage.getRecords());
        return pageUtils;
    }

    @Override
    public List<TeamEchartsStatisticsVo> teamEchartsStatisticsData(TeamCardStatisticsDataBo teamCardStatisticsDataBo) {
        List<Map<String, Object>> teamEchartsStatisticsData = userDao.teamEchartsStatisticsData(teamCardStatisticsDataBo);
        if (teamEchartsStatisticsData == null || teamEchartsStatisticsData.isEmpty()) {
            return new ArrayList<>();
        }

        // 按 user_id 分组
        Map<String, List<Map<String, Object>>> listMap = teamEchartsStatisticsData.stream()
                .collect(Collectors.groupingBy(item -> String.valueOf(item.get("user_id"))));

        // 遍历分组后的数据
        ArrayList<TeamEchartsStatisticsVo> res = new ArrayList<>();
        listMap.forEach((key, values) -> {
            // 创建 TeamEchartsStatisticsVo 对象
            TeamEchartsStatisticsVo vo = new TeamEchartsStatisticsVo();
            // 设置 salesId -- 其实设置的是userId
            vo.setUserId(key == null || "null".equals(key) ? null : Long.valueOf(key));
            List<UserAmbitionStatisticsVo> dataList = new ArrayList<>();
            if (values != null && !values.isEmpty()) {
                dataList = values.stream()
                        .map(item -> {
                            UserAmbitionStatisticsVo data = new UserAmbitionStatisticsVo();
                            data.setUserAmbition(String.valueOf(item.get("user_ambition")));
                            data.setNum(Integer.valueOf(String.valueOf(item.get("num"))));
                            return data;
                        })
                        .toList();
            }
            vo.setDataList(dataList);
            res.add(vo);
        });
        return res;
    }

    @Override
    public Map<String, Integer> teamTrialAboutTo3DayExpiresStatistics(TeamCardStatisticsDataBo bo) {
        bo.setEndDate(DateUtil.formatDateTime(DateUtil.offsetDay(new Date(), 3)));
        List<Map<String, Object>> list = userDao.teamTrialAboutTo3DayExpiresStatistics(bo);

        if (list == null || list.isEmpty()) {
            return new HashMap<>();
        }
        return list.stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.get("user_ambition")),
                        item -> NumberUtil.parseInt(String.valueOf(item.get("user_count")), 0)));
    }

    @Override
    public PageUtils<SalesStatisticsList> teamTrialAboutTo3DayExpires(TeamCardStatisticsDataBo bo) {
        bo.setEndDate(DateUtil.formatDateTime(DateUtil.offsetDay(new Date(), 3)));
        Page<SalesStatisticsList> iPage = userDao.teamTrialAboutTo3DayExpires(new Page<>(bo.getPage(), bo.getLimit()), bo);
        PageUtils<SalesStatisticsList> pageUtils = new PageUtils<>(bo.getPage(), bo.getLimit(), iPage);
        pageUtils.setList(iPage.getRecords());
        return pageUtils;
    }

    @Override
    public List<EachSalesFollowStatisticsVo> teamSalesFollowList(TeamCardStatisticsDataBo bo) {
        // 1. 获取部门下的各个销售
        List<EachSalesFollowStatisticsVo> salesList = userDao.teamSalesFollowSalesList(bo);
        if (salesList == null || salesList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> salesIds = salesList.stream().map(EachSalesFollowStatisticsVo::getSalesId).toList();

        // 2. 获取各个销售对应的注册用户数、登录用户数、演示数
        List<EachSalesFollowStatisticsVo> registerStats = userDao.teamSalesFollowRegisterStats(salesIds, bo);
        Map<Long, EachSalesFollowStatisticsVo> registerMap = ObjectUtil.isEmpty(registerStats) ? new HashMap<>() :
                registerStats.stream().collect(Collectors.toMap(EachSalesFollowStatisticsVo::getSalesId, Function.identity(), (a, b) -> a));

        // 3. 获取各个销售对应的试用用户数、成交客户数、成交金额
        List<EachSalesFollowStatisticsVo> orderStats = userDao.teamSalesFollowOrderStats(salesIds, bo);
        Map<Long, EachSalesFollowStatisticsVo> orderMap = ObjectUtil.isEmpty(orderStats) ? new HashMap<>() :
                orderStats.stream().collect(Collectors.toMap(EachSalesFollowStatisticsVo::getSalesId, Function.identity(), (a, b) -> a));

        // 4. 组装结果，计算各个销售的试用率、登录率、演示率、成交率
        for (EachSalesFollowStatisticsVo vo : salesList) {
            // 填充注册/登录/演示数据
            EachSalesFollowStatisticsVo regData = registerMap.get(vo.getSalesId());
            if (regData != null) {
                vo.setRegisterUserNum(regData.getRegisterUserNum());
                vo.setLoginUserNum(regData.getLoginUserNum());
                vo.setDemoNum(regData.getDemoNum());
            }

            // 填充订单数据
            EachSalesFollowStatisticsVo ordData = orderMap.get(vo.getSalesId());
            if (ordData != null) {
                vo.setTrialUserNum(ordData.getTrialUserNum());
                vo.setTrialLoginUserNum(ordData.getTrialLoginUserNum());
                vo.setCustomerNum(ordData.getCustomerNum());
                vo.setCustomerAmount(ordData.getCustomerAmount() == 0 ? 0 : ordData.getCustomerAmount() / 100);
            }

            // 计算比率
            int registerNum = vo.getRegisterUserNum();
            int trialNum = vo.getTrialUserNum();
            vo.setTrialRate(calcRate(trialNum, registerNum));
            vo.setTrialLoginRate(calcRate(vo.getTrialLoginUserNum(), trialNum));
            vo.setDemoRate(calcRate(vo.getDemoNum(), registerNum));
            vo.setCustomerRate(calcRate(vo.getCustomerNum(), registerNum));
            vo.setUvValue(calcValue(vo.getCustomerAmount(), registerNum));
        }

        return salesList;
    }

    private String calcRate(int numerator, int denominator) {
        if (denominator == 0) return "0%";
        BigDecimal rate = new BigDecimal(numerator)
                .multiply(new BigDecimal(100))
                .divide(new BigDecimal(denominator), 2, RoundingMode.HALF_UP);
        return rate.stripTrailingZeros().toPlainString() + "%";
    }

    private String calcValue(int numerator, int denominator) {
        if (denominator == 0) return "0";
        BigDecimal value = new BigDecimal(numerator)
                .divide(new BigDecimal(denominator), 2, RoundingMode.HALF_UP);
        return value.stripTrailingZeros().toPlainString();
    }


    /**
     * 获取用户类型映射
     *
     * @param userIds 用户id
     *
     * @return {@link Map }<{@link Long }, {@link Integer }>
     */
    @Override
    public Map<Long, Integer> getUserTypeMap(List<Long> userIds) {
        if (EmptyUtil.isEmpty(userIds)) {
            return Map.of();
        }
        return userService.lambdaQuery()
            .select(UserEntity::getId, UserEntity::getUserType)
            .in(UserEntity::getId, userIds)
            .list().stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getUserType));
    }

    @Override
    public int getSubAccountNum(Long userId) {
        if (userId == null) return 0;
        return userService.lambdaQuery()
                .eq(UserEntity::getParentId, userId)
                .count()
                .intValue()
                ;
    }

    @Override
    public List<com.jiuyu.replay.power.vo.datahub.DataHubMemberVo> listDataHubMemberByPhones(List<String> phones) {
        if (EmptyUtil.isEmpty(phones)) {
            return List.of();
        }
        List<UserEntity> users = userService.lambdaQuery()
                .in(UserEntity::getPhone, phones)
                .ne(UserEntity::getUserType, 1)
                .eq(UserEntity::getIsDeleted, 0)
                .list();
        return buildDataHubMembers(users);
    }

    @Override
    public List<com.jiuyu.replay.power.vo.datahub.DataHubMemberVo> listDataHubMemberByIds(Collection<Long> userIds) {
        if (EmptyUtil.isEmpty(userIds)) {
            return List.of();
        }
        List<UserEntity> users = userService.lambdaQuery()
                .in(UserEntity::getId, userIds)
                .ne(UserEntity::getUserType, 1)
                .eq(UserEntity::getIsDeleted, 0)
                .list();
        return buildDataHubMembers(users);
    }

    @Override
    public List<com.jiuyu.replay.power.vo.datahub.DataHubMemberVo> listDataHubMemberByActiveTenantIds(Collection<Long> tenantIds) {
        if (EmptyUtil.isEmpty(tenantIds)) {
            return List.of();
        }
        // 成员名单需要覆盖已冻结/已删除成员，故不过滤 status 与 is_deleted
        List<UserEntity> users = userService.lambdaQuery()
                .in(UserEntity::getActiveTenantId, tenantIds)
                .ne(UserEntity::getUserType, 1)
                .list();
        return buildDataHubMembers(users);
    }

    /**
     * 组装 Data Hub 成员快照（批量补 tb_user_details.is_logged_in）
     *
     * @param users 用户实体列表
     *
     * @return 成员快照列表
     */
    private List<com.jiuyu.replay.power.vo.datahub.DataHubMemberVo> buildDataHubMembers(List<UserEntity> users) {
        if (EmptyUtil.isEmpty(users)) {
            return List.of();
        }
        List<Long> userIds = users.stream().map(UserEntity::getId).toList();
        Map<Long, Integer> loggedInMap = new java.util.HashMap<>();
        userDetailsService.lambdaQuery()
                .select(UserDetailsEntity::getUserId, UserDetailsEntity::getIsLoggedIn)
                .in(UserDetailsEntity::getUserId, userIds)
                .list()
                .forEach(d -> loggedInMap.put(d.getUserId(), d.getIsLoggedIn()));
        return users.stream().map(u -> {
            com.jiuyu.replay.power.vo.datahub.DataHubMemberVo vo = new com.jiuyu.replay.power.vo.datahub.DataHubMemberVo();
            vo.setId(u.getId());
            vo.setPhone(u.getPhone());
            vo.setNickName(u.getNickName());
            vo.setUserType(u.getUserType());
            vo.setParentId(u.getParentId());
            vo.setActiveTenantId(u.getActiveTenantId());
            vo.setStatus(u.getStatus());
            vo.setIsDeleted(u.getIsDeleted());
            vo.setIsLoggedIn(loggedInMap.get(u.getId()));
            vo.setCreateDate(u.getCreateDate());
            vo.setUpdateDate(u.getUpdateDate());
            return vo;
        }).toList();
    }
}
