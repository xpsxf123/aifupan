package com.jiuyu.replay.power.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.*;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.common.bll.FileBll;
import com.jiuyu.replay.common.bll.RocketMqBll;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.context.RequestContext;
import com.jiuyu.replay.common.enums.CacheFallbackDataTypeEnum;
import com.jiuyu.replay.common.event.UpdatePasswordEvent;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.properties.RocketMqProperties;
import com.jiuyu.replay.common.repository.service.CacheFallbackDataService;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.common.utils.MD5Utils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.constant.activity.InviteRewardRuleCodeEnum;
import com.jiuyu.replay.generic.dto.activity.UserInviteMqDto;
import com.jiuyu.replay.generic.dto.order.UserResourceConsumptionDto;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.dto.third.SmsResult;
import com.jiuyu.replay.generic.dto.words.UserAnchorCountDto;
import com.jiuyu.replay.generic.dto.words.UserNotesCountDto;
import com.jiuyu.replay.generic.dto.words.UserVideoCountDto;
import com.jiuyu.replay.generic.feign.agent.ChannelFeign;
import com.jiuyu.replay.generic.feign.order.PackageFeign;
import com.jiuyu.replay.generic.feign.order.UserPropertyDetailsFeign;
import com.jiuyu.replay.generic.feign.third.SmsServiceFeign;
import com.jiuyu.replay.generic.feign.words.AnchorUrlUserFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoFeign;
import com.jiuyu.replay.generic.feign.words.VideoTextNotesFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.RandomStringGenerator;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.PackageVo;
import com.jiuyu.replay.generic.vo.order.UserResourceConsumptionVo;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.power.SubUserListVo;
import com.jiuyu.replay.generic.vo.power.UpdateEmployeeStatusVo;
import com.jiuyu.replay.power.bo.*;
import com.jiuyu.replay.power.constant.Constant;
import com.jiuyu.replay.power.constant.PowerProperties;
import com.jiuyu.replay.power.entity.UserLoginInfoEntity;
import com.jiuyu.replay.power.enumeration.LoginSourceEnum;
import com.jiuyu.replay.power.producer.*;
import com.jiuyu.replay.power.entity.UserBusinessEntity;
import com.jiuyu.replay.power.repository.service.UserBusinessService;
import com.jiuyu.replay.power.rse.UserRse;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UserBll {

    @Resource
    private UserProducer userProducer;
    @Resource
    private UserDetailsProducer userDetailsProducer;
    @Resource
    private PowerProperties powerProperties;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RoleProducer roleProducer;
    @Resource
    private MenuProducer menuProducer;
    @Resource
    private FileBll fileBll;
    @Resource
    private UserLoginLogProducer userLoginLogProducer;
    @Resource
    private TenantProducer tenantProducer;
    @Resource
    private TenantUserProducer tenantUserProducer;
    @Resource
    private SalesProducer salesProducer;
    @Resource
    RocketMqBll rocketMqBll;
    @Resource
    RocketMqProperties rocketMqProperties;
    @Resource
    private UserRse userRse;
    @Resource
    private AnchorUrlUserFeign anchorUrlUserFeign;
    @Resource
    private AnchorVideoFeign anchorVideoFeign;
    @Resource
    private VideoTextNotesFeign videoTextNotesFeign;
    @Resource
    private UserPropertyDetailsFeign userPropertyDetailsFeign;
    @Resource
    ResilientRedisTemplate<String, Object> resilientRedisTemplate;
    @Resource
    CacheFallbackDataService cacheFallbackDataService;
    @Resource
    UserTokenProducer userTokenProducer;
    @Resource
    SystemKvProducer systemKvProducer;
    @Resource
    private SmsServiceFeign SmsServiceFeign;
    @Resource
    private ChannelFeign channelFeign;
    @Autowired
    private PackageFeign packageFeign;
    @Resource
    private UserBusinessService userBusinessService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 登录
     *
     * @param loginBo 数据
     * @param loginIp 登录ip
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<UserLoginVo> login(LoginBo loginBo, String loginIp) {
        if (Objects.isNull(loginBo.getUserType())) {
            loginBo.setUserType(0);
        }
        UserLoginVo userLoginVo = new UserLoginVo();
        String key = powerProperties.getPhoneCodeRedisKey() + loginBo.getPhone();
        if (StrUtil.isNotBlank(loginBo.getUsername()) && StrUtil.isNotBlank(loginBo.getPassword())) {
            // 账号/手机号 密码登录
            UserVo userVo = this.userProducer.getByUsernameOrPhone(loginBo.getUsername(), loginBo.getUserType());
            if (userVo == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户不存在");
            } else if (userVo.getStatus() == 1) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "账号已被冻结，请联系管理员");
            } else if (!userVo.getPassword().equals(MD5Utils.md5(loginBo.getPassword()))) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "密码错误");
            }
            BeanUtils.copyProperties(userVo, userLoginVo);

        } else if (StrUtil.isNotBlank(loginBo.getPhone()) && StrUtil.isNotBlank(loginBo.getCode())) {
            // 手机号 验证码登录
            UserVo userVo = this.userProducer.getByUsernameOrPhone(loginBo.getPhone(), loginBo.getUserType());
            if (userVo == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户不存在");
            } else if (userVo.getStatus() == 1) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "账号已被冻结，请联系管理员");
            }
            String code = !resilientRedisTemplate.isDegraded() ? (String) resilientRedisTemplate.getRedisTemplate().opsForValue().get(key) : (String) cacheFallbackDataService.getCacheValueByCache(key);
            if (StrUtil.isBlank(code) || !code.equals(loginBo.getCode())) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "验证码错误");
            }
            BeanUtils.copyProperties(userVo, userLoginVo);
        } else {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "账号或密码错误");
        }
        // 删除验证码
        if (!resilientRedisTemplate.isDegraded() && resilientRedisTemplate.getRedisTemplate().hasKey(key)) {
            resilientRedisTemplate.getRedisTemplate().delete(key);
        } else {
            cacheFallbackDataService.removeCacheFallbackDataByCacheKey(key);
        }
        // 判断是否允许登录
        boolean permission = checkUserLoginPermission(userLoginVo);
        if (!permission) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "账号已登录，请退出后30秒再试");
        }

        // TODO 临时过度方案，后续版本去掉，改成登录时选租户
        if (userLoginVo.getUserType() == 2) {
            // 当前为子账号，将租户id设置成主账号的租户
            TenantInfoVo tenantInfoVo = this.tenantProducer.infoByUserId(userLoginVo.getParentId());
            this.userProducer.updateActiveTenantId(userLoginVo.getId(), tenantInfoVo.getId());
            userLoginVo.setActiveTenantId(tenantInfoVo.getId());
        }

        // 填充剩余用户信息
        UserCacheVo userCacheVo = fillUserLoginVo(userLoginVo, loginIp, false);

        // 添加登录日志
        UserLoginLogBo bo = new UserLoginLogBo(userCacheVo, loginIp, "登录成功", 0, 0);
        userLoginLogProducer.save(bo);

        // 至此，登录成功
        // 修改用户详情[是否登录] -> 已登录
        if (userLoginVo.getUserType() != 1){
            boolean updated = this.userDetailsProducer.updateLoggedInStatus(userLoginVo.getId());
            if (!updated){
                log.error("修改用户详情[是否登录过] -> 已登录 修改失败userId:{},userType:{}",userLoginVo.getId(),userLoginVo.getUserType());
            }
        }

        return R.ok("登录成功", userLoginVo);

    }

    /**
     * 填充登录用户信息
     *
     * @param userLoginVo 用户信息
     * @param loginIp     ip
     * @param isOnline    是否是web端登录
     * @return
     */
    private UserCacheVo fillUserLoginVo(UserLoginVo userLoginVo, String loginIp, boolean isOnline) {

        // 保存登录ip到数据库
        if (StrUtil.isEmpty(userLoginVo.getIps())) {
            String ips = "_" + loginIp + "_";
            this.userProducer.updateUserIp(userLoginVo.getId(), ips);
        } else if (!userLoginVo.getIps().contains("_" + loginIp + "_")) {
            String ips = userLoginVo.getIps() + loginIp + "_";
            this.userProducer.updateUserIp(userLoginVo.getId(), ips);
        }

        // 设置token
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        userLoginVo.setToken(token);
        // 设置角色和菜单
        if (userLoginVo.getUserType() == 1) {
            userLoginVo.setRoleIdList(roleProducer.findIdsByUserId(userLoginVo.getId()));
            List<MenuVo> menuVos = menuProducer.listByUserId(userLoginVo.getId());
            userLoginVo.setMenuList(menuVos);
            userLoginVo.setMenuTreeList(menuProducer.packageMenuTree(menuVos, true));

            UserDetailsInfoVo userDetail = userDetailsProducer.getDetailByUserId(userLoginVo.getId());
            if (userDetail != null) {
                // agentId
                userLoginVo.setAgentId(userDetail.getAgentId());
            }
        }

        // 存到redis
        UserCacheVo userCacheVo = new UserCacheVo();
        BeanUtils.copyProperties(userLoginVo, userCacheVo);
        userCacheVo.setIp(loginIp);
        userCacheVo.setRequestTime(System.currentTimeMillis());
        userCacheVo.setOnlineStatus(1);

        // 存储token&登录信息
        String source = isOnline ? LoginSourceEnum.WEB.getLoginSourceValue() : (userCacheVo.getUserType() == 1 ? LoginSourceEnum.BACK.getLoginSourceValue() : LoginSourceEnum.CLIENT.getLoginSourceValue());
        userTokenProducer.saveUserLoginInfo(userCacheVo, token, loginIp, source, RequestContext.getFingerprint());

        userLoginVo.setPassword("");
        userLoginVo.setIps("");
        if (ObjectUtil.isNotEmpty(userLoginVo.getPhone()) && userLoginVo.getPhone().length() >= 8) {
            userLoginVo.setPhone(userLoginVo.getPhone().substring(0, 3) + "****" + userLoginVo.getPhone().substring(7));
        }
        // 查询头像
        FileShowVo fileShowVo = this.fileBll.getByResourceIdAndType(userLoginVo.getId(), 0);
        if (fileShowVo != null) {
            userLoginVo.setAvatar(fileShowVo.getUrl());
        }
        // 用户登录发送异步消息
        UserInviteMqDto userInviteMqDto = new UserInviteMqDto(InviteRewardRuleCodeEnum.DOWNLOAD.getCode(), userLoginVo.getId(), RequestContext.getFingerprint());
        rocketMqBll.syncSendNormalMessage(userLoginVo.getId(), rocketMqProperties.getTagUserInviteActivity(), IdUtil.simpleUUID(), JSON.toJSONString(userInviteMqDto));
        return userCacheVo;
    }

    /**
     * 修改密码
     *
     * @param updatePasswordBo 数据
     * @return
     */
    public R<String> updatePassword(UpdatePasswordBo updatePasswordBo) {
        if (!updatePasswordBo.getNewPassword().equals(updatePasswordBo.getCheckPassword())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "两次输入的密码不一致");
        }

        UserCacheVo userCacheVo = GlobalObject.getLocalUser();
        UserVo userVo = this.userProducer.getById(userCacheVo.getId());
        if (!userVo.getPassword().equals(MD5Utils.md5(updatePasswordBo.getPassword()))) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "原密码错误");
        }

        this.userProducer.updatePassword(userVo.getId(), updatePasswordBo.getNewPassword());
        UpdatePasswordEvent updatePasswordEvent = new UpdatePasswordEvent(userVo.getId(), userVo.getId(), updatePasswordBo.getNewPassword(), userVo.getActiveTenantId());
        eventPublisher.publishEvent(updatePasswordEvent);
        return R.ok("修改成功，请重新登录");
    }

    /**
     * 用户列表
     *
     * @param userListBo 列表查询参数
     * @return
     */
    public R<PageUtils<UserListVo>> queryPage(UserListBo userListBo) {
        return R.ok(this.userProducer.queryPage(userListBo));

    }

    /**
     * 重置密码
     *
     * @param id 用户id
     * @return
     */
    public R<String> resetPassword(Long id) {
        UserVo userVo = this.userProducer.getById(id);
        if (userVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户不存在");
        }
        UserCacheVo localUser = GlobalObject.getLocalUser();
        String newPassword = RandomStringGenerator.generateRandomString();
        this.userProducer.updatePassword(id, newPassword);
        UpdatePasswordEvent updatePasswordEvent = new UpdatePasswordEvent(localUser == null ? 0L : localUser.getId(), id, newPassword, userVo.getActiveTenantId());
        eventPublisher.publishEvent(updatePasswordEvent);
        return R.ok("重置密码成功", newPassword);

    }

    /**
     * 信息
     *
     * @param id           用户id
     * @param loadPassword
     *
     * @return
     */
    public R<UserInfoVo> info(Long id, boolean loadPassword) {

        UserVo userVo = this.userProducer.getById(id);

        if (userVo != null) {
            UserInfoVo userInfoVo = new UserInfoVo();
            BeanUtils.copyProperties(userVo, userInfoVo);
            if (!loadPassword) {
                userInfoVo.setPassword(null);
            }
            userInfoVo.setRoleIdList(roleProducer.findIdsByUserId(userInfoVo.getId()));

            return R.ok(userInfoVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户不存在");

    }

    /**
     * 根据id查询用户信息
     *
     * @param id
     * @return
     */
    public R<UserVo> getById(Long id) {
        return R.ok(this.userProducer.getById(id));
    }

    /**
     * 删除
     *
     * @param id 用户id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> delete(Long id) {
        // 删除角色关联
        this.roleProducer.deleteByUserId(id);
        // 删除用户
        this.userProducer.deleteById(id);
        // 删除redis
        userTokenProducer.removeUserTokenByUserId(id);
        return R.ok("删除成功");

    }

    /**
     * 注册检测
     *
     * @param registerBo
     * @return
     */
    public R<String> registerTheCheck(RegisterBo registerBo, boolean checkPhone) {
        String codeKey = powerProperties.getPhoneCodeRedisKey() + registerBo.getPhone();
        // 686868验证码直接过检测
        if (!"686868".equals(registerBo.getCode())) {
            String code = !resilientRedisTemplate.isDegraded() ? (String) resilientRedisTemplate.getRedisTemplate().opsForValue().get(codeKey) : (String) cacheFallbackDataService.getCacheValueByCache(codeKey);
            if (StrUtil.isBlank(code) || !code.equals(registerBo.getCode())) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "验证码错误");
            }
        }

        if (checkPhone) {
            UserVo byPhone = this.userProducer.getByPhoneAndType(registerBo.getPhone(), 0);
            if (byPhone != null) {
                return R.error(Constant.CodeMsgEnum.IS_REGISTER.getCode(), "手机号已存在，注册失败");
            }
        }

//        UserVo byUsername = this.userProducer.getByUsernameAndType(registerBo.getUsername(), 0);
//        if(byUsername != null) {
//            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户名已存在，注册失败");
//        }
        return R.ok();
    }

    /**
     * 注册检测
     * @param registerBo 参数
     */
    public R<String> registerTheCheck(RegisterBo registerBo) {

        return registerTheCheck(registerBo, true);
    }

    /**
     * 注册
     *
     * @param registerBo 数据
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<UserVo> register(RegisterBo registerBo) {

        // 保存用户信息
        UserVo userVo = this.userProducer.create(registerBo);
        // 保存用户详情信息
        UserDetailsBo userDetailsBo = new UserDetailsBo();
        userDetailsBo.setUserId(userVo.getId());
        if (registerBo.getSaleId() != null) {
            SalesInfoVo info = salesProducer.info(registerBo.getSaleId());
            if (info == null || ObjectUtil.equals(info.getIsChoose(), 0)) {
                log.warn("[爱复盘用户注册] 没有查询到销售或者对应的销售已关闭分配线索 salesId = {}", registerBo.getSaleId());
                registerBo.setSaleId(null);
            }
        }

        if (registerBo.getSaleId() != null) {
            userDetailsBo.setSaleId(registerBo.getSaleId());
        } else {
            log.info("[轮询销售] 没有销售id走兜底轮询销售逻辑");
            // 注册没有携带销售id，轮询获取一个
            Long saleId = this.salesProducer.getPollingSaleId();
            if (saleId != null) {
                registerBo.setSaleId(saleId);
                userDetailsBo.setSaleId(saleId);
            }
        }
//        else if(!StringUtils.isEmpty(registerBo.getInviteUrlCode()) && registerBo.getSaleId() == null) {
//            // 用户是通过邀请链接进来的，但是没有销售人员，为用户分配销售人员
//            Long saleId = this.salesProducer.getPollingSaleId();
//            if(saleId != null) {
//                userDetailsBo.setSaleId(saleId);
//            }
//        }
        if (registerBo.getAgentSaleId() != null) {
            userDetailsBo.setAgentSaleId(registerBo.getAgentSaleId());
        }
        if (registerBo.getChannelId() != null) {
            userDetailsBo.setChannelId(registerBo.getChannelId());
        }
        if (registerBo.getAgentId() != null) {
            userDetailsBo.setAgentId(registerBo.getAgentId());
        }
        // 保存用户详情信息
        this.userDetailsProducer.save(userDetailsBo);

        // 保存一个默认的角色
        this.roleProducer.createUserDefaultRole(userVo.getId());
        // 保存租户信息
        TenantBo tenantBo = new TenantBo();
        tenantBo.setTenantName(userVo.getPhone() + "的租户");
        tenantBo.setUserId(userVo.getId());
        TenantInfoVo tenantInfoVo = this.tenantProducer.save(tenantBo);
        // 绑定用户和租户的关系
        TenantUserBo tenantUserBo = new TenantUserBo();
        tenantUserBo.setUserId(userVo.getId());
        tenantUserBo.setTenantId(tenantInfoVo.getId());
        this.tenantUserProducer.save(tenantUserBo);

        // 设置激活的租户id
        this.userProducer.updateActiveTenantId(userVo.getId(), tenantInfoVo.getId());

        String codeKey = powerProperties.getPhoneCodeRedisKey() + registerBo.getPhone();
        if (!resilientRedisTemplate.isDegraded() && resilientRedisTemplate.getRedisTemplate().hasKey(codeKey)) {
            resilientRedisTemplate.getRedisTemplate().delete(codeKey);
        } else {
            cacheFallbackDataService.removeCacheFallbackDataByCacheKey(codeKey);
        }
        return R.ok("注册成功", userVo);

    }

    /**
     * 获取验证码
     *
     * @param phone 手机号
     * @return
     */
    public R<String> getPhoneCode(String phone) {
        // 构建6位数的验证码
        String code = RandomUtil.randomNumbers(6);
        log.info("--------手机号：{}，验证码：{}", phone, code);
        String key = powerProperties.getPhoneCodeRedisKey() + phone;
        if (!resilientRedisTemplate.isDegraded()) {
            // 存到redis
            resilientRedisTemplate.getRedisTemplate().opsForValue().set(key, code, Duration.ofMinutes(5));
        } else {
            // 降级下存入db中
            cacheFallbackDataService.saveCacheFallbackData(key, code, CacheFallbackDataTypeEnum.STRING, LocalDateTime.now().plusMinutes(5));
        }
        return R.ok("获取验证码成功", code.toString());

    }

    /**
     * 新增用户
     *
     * @param userAddBo 数据
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<UserVo> save(UserAddBo userAddBo) {

        UserVo byPhone = this.userProducer.getByPhoneAndType(userAddBo.getPhone(), 1);
        if (byPhone != null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "手机号已存在，添加失败");
        }

        UserVo byUsername = this.userProducer.getByUsernameAndType(userAddBo.getUsername(), 1);
        if (byUsername != null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "账号已存在，添加失败");
        }

        // 保存用户信息
        UserVo userVo = this.userProducer.save(userAddBo);
        // 保存用户的角色
        this.roleProducer.saveUserRoles(userVo.getId(), userAddBo.getRoleIdList());

        // 创建用户详情
        UserDetailsBo userDetailsBo = new UserDetailsBo();
        userDetailsBo.setUserId(userVo.getId());
        userDetailsProducer.save(userDetailsBo);

        return R.ok("添加成功", userVo);
    }

    /**
     * 修改用户
     *
     * @param userUpdateBo 数据
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> update(UserUpdateBo userUpdateBo) {

        UserVo oldUser = this.userProducer.getById(userUpdateBo.getId());
        if (!oldUser.getPhone().equals(userUpdateBo.getPhone())) {
            UserVo byPhone = this.userProducer.getByPhoneAndType(userUpdateBo.getPhone(), oldUser.getUserType());
            if (byPhone != null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "手机号已存在，修改失败");
            }
        }
        if (!oldUser.getUsername().equals(userUpdateBo.getUsername())) {
            UserVo byUsername = this.userProducer.getByUsernameAndType(userUpdateBo.getUsername(), oldUser.getUserType());
            if (byUsername != null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "账号已存在，修改失败");
            }
        }

        // 修改用户信息
        this.userProducer.update(userUpdateBo);
        // 修改用户的角色
        this.roleProducer.updateUserRoles(userUpdateBo.getId(), userUpdateBo.getRoleIdList());

        return R.ok("修改成功");
    }

    /**
     * 根据用户id集合获取用户列表
     *
     * @param userIds 用户id集合
     * @return
     */
    public R<List<UserListVo>> listByIds(Collection<Long> userIds) {

        return R.ok("获取成功", this.userProducer.listByIds(userIds));
    }

    /**
     * 客户端修改用户
     *
     * @param userUpdateClientBo 数据
     * @return
     */
    public R<UserInfoVo> updateByClient(UserUpdateClientBo userUpdateClientBo) {

        UserCacheVo userCacheVo = GlobalObject.getLocalUser();
        userUpdateClientBo.setId(userCacheVo.getId());

        if (StrUtil.isNotBlank(userUpdateClientBo.getPhone())) {
            userCacheVo.setPhone(userUpdateClientBo.getPhone());
            UserVo byPhone = this.userProducer.getByPhoneAndType(userUpdateClientBo.getPhone(), 0);
            if (byPhone != null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "手机号已存在，修改失败");
            }
        }
        if (!StringUtils.isEmpty(userUpdateClientBo.getAvatarImgId())) {
            // 更新头像
            this.fileBll.updateOne(userUpdateClientBo.getId(), 0, "用户头像", userUpdateClientBo.getAvatarImgId());
        }

        // 修改用户信息
        UserUpdateBo userUpdateBo = new UserUpdateBo();
        BeanUtils.copyProperties(userUpdateClientBo, userUpdateBo);
        this.userProducer.update(userUpdateBo);

        // 更新缓存
        if (StrUtil.isNotBlank(userUpdateClientBo.getNickName())) {
            userCacheVo.setNickName(userUpdateClientBo.getNickName());
        }
        // 更新缓存信息
        userTokenProducer.updateBaseUserInfo(userCacheVo);
        UserInfoVo userInfoVo = this.userProducer.infoById(userCacheVo.getId());

        // 查询头像
        FileShowVo fileShowVo = this.fileBll.getByResourceIdAndType(userInfoVo.getId(), 0);
        if (fileShowVo != null) {
            userInfoVo.setAvatar(fileShowVo.getUrl());
        }

        return R.ok(userInfoVo);
    }


    /**
     * 修改手机号
     *
     * @param userId    用户id
     * @param newMobile 新手机号
     * @param tenantId  租户id
     *
     * @return 结果
     */
    public R<Void> updateMobile(long userId, String newMobile, Long tenantId) {
        UserVo byPhone = this.userProducer.getByPhoneAndType(newMobile, UserEnums.userType.CLIENT_USER.getCode());
        if (byPhone != null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "手机号已存在，修改失败");
        }
        UserVo userVo = userProducer.getById(userId);
        if (userVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户信息不存在");
        }
        if (tenantId != null && !Objects.equals(tenantId, userVo.getActiveTenantId())) {
            return R.error(Constant.CodeMsgEnum.NO_POWER.getCode(), "手机号不属于当前租户，修改失败");
        }
        UserUpdateBo userUpdateBo = new UserUpdateBo();
        userUpdateBo.setId(userId);
        userUpdateBo.setPhone(newMobile);
        this.userProducer.update(userUpdateBo);

        userTokenProducer.updateUserMobileCache(userId, newMobile);
        return R.ok();

    }

    public R<UserInfoVo> infoByClient(Long id) {

        UserInfoVo userInfoVo = this.userProducer.infoById(id);

        if (userInfoVo == null) {

            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户信息不存在");
        }

        // 查询头像
        FileShowVo fileShowVo = this.fileBll.getByResourceIdAndType(userInfoVo.getId(), 0);
        if (fileShowVo != null) {
            userInfoVo.setAvatar(fileShowVo.getUrl());
        }

        return R.ok(userInfoVo);
    }

    /**
     * 校验验证码
     *
     * @param phone      手机号
     * @param code       验证码
     * @param userPhone  用户手机号
     * @param isNewPhone 是否是新手机号 0：否 1：是
     * @return
     */
    public R<String> checkPhoneCode(String phone, String code, String userPhone, Integer isNewPhone) {
        if (Objects.isNull(isNewPhone) || isNewPhone == 0) {
            if (!phone.equals(userPhone)) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "手机号错误");
            }
        }
        // 验证验证码
        String codeKey = powerProperties.getPhoneCodeRedisKey() + phone;
        String codeSms = !resilientRedisTemplate.isDegraded() ? (String) resilientRedisTemplate.getRedisTemplate().opsForValue().get(codeKey) : (String) cacheFallbackDataService.getCacheValueByCache(codeKey);
        if (StrUtil.isBlank(codeSms) || !codeSms.equals(code)) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "验证码错误");
        }

        // 删除验证码
        if (!resilientRedisTemplate.isDegraded() && resilientRedisTemplate.getRedisTemplate().hasKey(codeKey)) {
            resilientRedisTemplate.getRedisTemplate().delete(codeKey);
        } else {
            cacheFallbackDataService.removeCacheFallbackDataByCacheKey(codeKey);
        }
        return R.ok("验证成功");
    }

    /**
     * 登出
     *
     * @param token
     * @return
     */
    public R<String> logout(String token, String ip) {
        UserCacheVo userCacheVo = !resilientRedisTemplate.isDegraded() ? (UserCacheVo) resilientRedisTemplate.getRedisTemplate().opsForValue().get(powerProperties.getUserLoginTokenRedisKey() + token) : userTokenProducer.getUserByToken(token);
        log.info("退出登录，token={}, ip={}, user={}", token, ip, userCacheVo);
        if (userCacheVo != null) {
            // 添加登录日志
            UserLoginLogBo bo = new UserLoginLogBo(userCacheVo, ip, "退出成功", 3, 0);
            userLoginLogProducer.save(bo);
            log.info("退出登录，添加退出登录日志成功， token={}", token);
            // 删除缓存
            userTokenProducer.removeUserToken(token, userCacheVo.getId());
        }

        log.info("退出登录成功， token={}", token);
        return R.ok("退出成功");
    }

    /**
     * 根据用户namen or nick 比配查询
     *
     * @param userListBo
     * @return
     */
    public List<Long> selectByNameOrNick(UserListBo userListBo) {
        return userProducer.selectByNameOrNick(userListBo);
    }

    /**
     * 根据id集合获取
     *
     * @param usrids
     * @return
     */
    public R<PageUtils<UserListVo>> selectByIds(Integer limit, Integer pege, List<Long> usrids) {
        return userProducer.selectByIds(limit, pege, usrids);
    }

    /**
     * 服务端冻结与解冻和用户
     *
     * @param userBo
     * @return
     */
    public R<String> updateUser(UserBo userBo) {
        userProducer.updateUser(userBo);
        userTokenProducer.updateUserStatusByUserId(userBo.getUserId(), userBo.getStatus());
        return R.ok("操作成功");
    }

    public R<String> updateParentId(UserBo userBo) {
        UserBo bo = new UserBo();
        bo.setUserId(userBo.getUserId());
        bo.setParentId(userBo.getParentId());
        bo.setUserType(userBo.getUserType());
        userProducer.updateUser(bo);
        return R.ok("成功");
    }

    /**
     * 根据条件查询用户信息
     *
     * @param userListBo
     * @return
     */
    public R<PageUtils<UserListVo>> selectClientList(UserListBo userListBo) {
        return userProducer.selectClientList(userListBo);
    }

    /**
     * 根据用户id获取子账号
     *
     * @param userIds
     * @return
     */
    public R<List<Long>> getUserChild(List<Long> userIds) {
        return userProducer.getUserChild(userIds);
    }

    /**
     * 服务端批量删除用户
     *
     * @param ids
     * @return
     */
    public R<String> deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) RRException.create("用户不能为空");
        ids.forEach(this::delete);
        return R.ok("删除成功");
    }

    /**
     * 服务端修改用户账号类型
     *
     * @param userUpdateBo
     * @return
     */
    public R<String> updateByUserId(UserUpdateBo userUpdateBo) {
        return userProducer.updateByUserId(userUpdateBo);
    }

    /**
     * 根据token获取用户信息
     *
     * @return
     */
    public R<UserVo> getUserByToken(String token, String ip) {
        // 通过token拿用户id
        UserCacheVo userCacheVo = !resilientRedisTemplate.isDegraded() ? (UserCacheVo) resilientRedisTemplate.getRedisTemplate().opsForValue().get(powerProperties.getUserLoginTokenRedisKey() + token) : userTokenProducer.getUserByToken(token);
        if (userCacheVo != null) {
            boolean flag = false;
            if (!resilientRedisTemplate.isDegraded()) {
                UserLoginInfoCacheVo userLoginInfoCacheVo = (UserLoginInfoCacheVo) resilientRedisTemplate.getRedisTemplate().opsForHash().get(powerProperties.getUserLoginInfoRedisKey() + userCacheVo.getId(), token);
                if (Objects.nonNull(userLoginInfoCacheVo) && Objects.nonNull(userLoginInfoCacheVo.getLastRequestTime()) && Instant.now().toEpochMilli() - userLoginInfoCacheVo.getLastRequestTime() > 30000) {
                    flag = true;
                }
            } else {
                UserLoginInfoEntity userLoginInfoEntity = userTokenProducer.getUserLoginInfoByUserIdAndToken(userCacheVo.getId(), token);
                if (Objects.nonNull(userLoginInfoEntity) && Objects.nonNull(userLoginInfoEntity.getLastRequestTime()) && LocalDateTime.now().minusSeconds(30).isAfter(userLoginInfoEntity.getLastRequestTime())) {
                    flag = true;
                }
            }
            if (flag) {
                // 添加上线日志
                UserLoginLogBo bo = new UserLoginLogBo(userCacheVo, ip, "再次检测到心跳，自动改为上线状态", 2, 0);
                userLoginLogProducer.save(bo);
            }
            UserVo userInfoVo = new UserVo();
            BeanUtils.copyProperties(userCacheVo, userInfoVo);
            userInfoVo.setPassword("");
            userInfoVo.setIps("");
            return R.ok(userInfoVo);

        }

        return R.ok();
    }

    /**
     * 获取客户端所有用户信息
     *
     * @return
     */
    public R<List<UserVo>> listByClientAll() {
        return R.ok(userProducer.listByClientAll());
    }

    /**
     * 更新用户行业
     *
     * @param userId
     * @param tradeId
     * @return
     */
    public R<String> updateTrade(Long userId, Long tradeId) {
        UserDetailsBo bo = new UserDetailsBo();
        bo.setUserId(userId);
        bo.setTradeId(tradeId);
        userDetailsProducer.update(bo);
        return R.ok("保存成功");
    }

    /**
     * 查询是否有子账号，如果没有就为子账号注册
     *
     * @param aSubAccountBo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVo setUpASubAccount(bindingSubAccountBo aSubAccountBo) {
        if (ObjectUtil.isEmpty(aSubAccountBo.getSubPhone())) RRException.create("手机号不能为空");
        if (ObjectUtil.isEmpty(aSubAccountBo.getSubUserName())) RRException.create("用户名不能为空");

        // 判断是否已经注册-如果已经注册了，就直接返回
        UserVo byPhone = this.userProducer.getByPhoneAndType(aSubAccountBo.getSubPhone(), 0);
        if (byPhone != null) {
            if (!byPhone.getUsername().equals(aSubAccountBo.getSubUserName())) {
                RRException.create("该手机号已存在，对应的用户名不正确");
            } else {
                // 判断单前账号是否是父账号
                List<UserVo> list = userProducer.listByParentId(byPhone.getId());
                if (ObjectUtil.isNotEmpty(list)) RRException.create("当前账号存在子账号，不能设为子账号");
                return BeanUtil.copyProperties(byPhone, UserInfoVo.class);
            }
        }
        UserVo byUsername = this.userProducer.getByUsernameAndType(aSubAccountBo.getSubUserName(), 0);
        if (byUsername != null) {
            if (!byUsername.getPhone().equals(aSubAccountBo.getSubPhone())) {
                RRException.create("该登录账号已存在，对应的手机号不正确");
            } else {
                // 判断单前账号是否是父账号
                List<UserVo> list = userProducer.listByParentId(byUsername.getId());
                if (ObjectUtil.isNotEmpty(list)) RRException.create("当前账号存在子账号，不能设为子账号");
                return BeanUtil.copyProperties(byUsername, UserInfoVo.class);
            }
        }

        // 保存用户信息
        RegisterBo registerBo = new RegisterBo();
        registerBo.setUsername(aSubAccountBo.getSubUserName());
        registerBo.setPhone(aSubAccountBo.getSubPhone());
        registerBo.setPassword(powerProperties.getResetPassword());
        UserVo userVo = this.userProducer.create(registerBo);
        // 保存一个默认的角色
        this.roleProducer.createUserDefaultRole(userVo.getId());

        // 查主账号的租户
        Long parentUserId = aSubAccountBo.getCurrentUserId();
        TenantInfoVo parentTenant = this.tenantProducer.infoByUserId(parentUserId);
        if (parentTenant != null) {
            // 查子账号与主账号的租户关系是否存在
            TenantUserInfoVo tenantUserInfoVo = this.tenantUserProducer.infoByTenantIdAndUserId(parentTenant.getId(), userVo.getId());

            if (tenantUserInfoVo == null) {
                // 添加关联
                TenantUserBo tenantUserBo = new TenantUserBo();
                tenantUserBo.setUserId(userVo.getId());
                tenantUserBo.setTenantId(parentTenant.getId());
                this.tenantUserProducer.save(tenantUserBo);
            }
        }
        // 创建自己的租户
        this.tenantProducer.createMyTenant(userVo.getId(), userVo.getPhone());

        if (ObjectUtil.isNotEmpty(userVo)) {
            return BeanUtil.copyProperties(userVo, UserInfoVo.class);
        }
        return null;
    }

    /**
     * 查询是否有子账号，如果没有就为子账号注册
     *
     * @param aSubAccountBo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVo setUpASubAccount2(bindingSubAccountBo aSubAccountBo) {
        if (ObjectUtil.isEmpty(aSubAccountBo.getSubPhone())) RRException.create("手机号不能为空");

        // 判断是否已经注册-如果已经注册了，就直接返回
        UserVo byPhone = this.userProducer.getByPhoneAndType(aSubAccountBo.getSubPhone(), 0);
        if (byPhone != null) {
            // 判断单前账号是否是父账号
            List<UserVo> list = userProducer.listByParentId(byPhone.getId());
            if (ObjectUtil.isNotEmpty(list)) RRException.create("当前账号存在子账号，不能设为子账号");
            return BeanUtil.copyProperties(byPhone, UserInfoVo.class);
        }

        // 保存用户信息
        RegisterBo registerBo = new RegisterBo();
        registerBo.setUsername(null);
        registerBo.setPhone(aSubAccountBo.getSubPhone());
        registerBo.setPassword(powerProperties.getResetPassword());
        registerBo.setNickName(aSubAccountBo.getNickName());
        UserVo userVo = this.userProducer.create(registerBo);
        // 保存一个默认的角色
        this.roleProducer.createUserDefaultRole(userVo.getId());

        // 保存用户详情信息
        UserDetailsBo userDetailsBo = new UserDetailsBo();
        userDetailsBo.setUserId(userVo.getId());
        // 保存用户详情信息
        // 设置渠道
        SystemKvInfoVo subAccountChannel = systemKvProducer.getByKey("sub_account_channel_id");
        if (ObjectUtil.isNotEmpty(subAccountChannel)) {
            userDetailsBo.setChannelId(NumberUtil.parseLong(subAccountChannel.getKvValue(), null));
        }
        // 设置跟进销售
        UserDetailsInfoVo paUser = userDetailsProducer.getByUserId(aSubAccountBo.getCurrentUserId());
        if (ObjectUtil.isNotEmpty(paUser)) {
            userDetailsBo.setSaleId(paUser.getSaleId());
//            userDetailsBo.setAgentSaleId(paUser.getAgentSaleId());
        }

        this.userDetailsProducer.save(userDetailsBo);

        // 查主账号的租户
        Long parentUserId = aSubAccountBo.getCurrentUserId();
        TenantInfoVo parentTenant = this.tenantProducer.infoByUserId(parentUserId);
        if (parentTenant != null) {
            // 查子账号与主账号的租户关系是否存在
            TenantUserInfoVo tenantUserInfoVo = this.tenantUserProducer.infoByTenantIdAndUserId(parentTenant.getId(), userVo.getId());

            if (tenantUserInfoVo == null) {
                // 添加关联
                TenantUserBo tenantUserBo = new TenantUserBo();
                tenantUserBo.setUserId(userVo.getId());
                tenantUserBo.setTenantId(parentTenant.getId());
                this.tenantUserProducer.save(tenantUserBo);
            }
        }
        // 创建自己的租户
        this.tenantProducer.createMyTenant(userVo.getId(), userVo.getPhone());

        if (ObjectUtil.isNotEmpty(userVo)) {
            return BeanUtil.copyProperties(userVo, UserInfoVo.class);
        }
        return null;
    }

    /**
     * 在线登录
     *
     * @param loginBo 数据
     * @return
     */
    public R<UserLoginVo> loginOnline(LoginBo loginBo, String loginIp) {

        UserLoginVo userLoginVo = new UserLoginVo();
        String codeKey = powerProperties.getPhoneCodeRedisKey() + loginBo.getPhone();
        if (StrUtil.isNotBlank(loginBo.getUsername()) && StrUtil.isNotBlank(loginBo.getPassword())) {
            // 账号/手机号 密码登录
            UserVo userVo = this.userProducer.getByUsernameOrPhone(loginBo.getUsername(), 0);
            if (userVo == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户不存在");
            } else if (userVo.getStatus() == 1) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "账号已被冻结，请联系管理员");
            } else if (!userVo.getPassword().equals(MD5Utils.md5(loginBo.getPassword()))) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "密码错误");
            }
            BeanUtils.copyProperties(userVo, userLoginVo);

        } else if (StrUtil.isNotBlank(loginBo.getPhone()) && StrUtil.isNotBlank(loginBo.getCode())) {
            // 手机号 验证码登录
            UserVo userVo = this.userProducer.getByUsernameOrPhone(loginBo.getPhone(), 0);
            if (userVo == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户不存在");
            } else if (userVo.getStatus() == 1) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "账号已被冻结，请联系管理员");
            }
            String code = !resilientRedisTemplate.isDegraded() ? (String) resilientRedisTemplate.getRedisTemplate().opsForValue().get(codeKey) : (String) cacheFallbackDataService.getCacheValueByCache(codeKey);
            if (StrUtil.isBlank(code) || !code.equals(loginBo.getCode())) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "验证码错误");
            }
            BeanUtils.copyProperties(userVo, userLoginVo);
        } else {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "账号或密码错误");
        }
        // 删除验证码
        if (!resilientRedisTemplate.isDegraded() && resilientRedisTemplate.getRedisTemplate().hasKey(codeKey)) {
            resilientRedisTemplate.getRedisTemplate().delete(codeKey);
        } else {
            cacheFallbackDataService.removeCacheFallbackDataByCacheKey(codeKey);
        }
        // 删除7天未访问的token
        userTokenProducer.removeUserTokenLastRequestDateAfterDayByUserId(userLoginVo.getId(), 7);
        // 填充剩余用户信息
        fillUserLoginVo(userLoginVo, loginIp, true);

        return R.ok("登录成功", userLoginVo);
    }

    /**
     * 获取用户的父id
     *
     * @param userId
     * @return
     */
    public R<Long> getCacheUserParentId(Long userId) {
        UserVo byId = userProducer.getById(userId);
        if (ObjectUtil.isEmpty(byId)) {
            return R.ok(null, null);
        }
        Long parentId = byId.getParentId();
        return R.ok(parentId != null && parentId == 0 ? null : parentId);
    }

    /**
     * 获取所有父用户，里面存在子账号
     *
     * @return
     */
    public R<List<UserInfoVo>> listParentUser(List<Long> userIds) {
        return R.ok(userProducer.listParentUser(userIds));
    }

    /**
     * 获取所有子用户
     *
     * @return
     */
    public R<List<UserVo>> listAllByParent() {
        return R.ok(userProducer.listAllByParent());
    }

    /**
     * 管理员列表
     *
     * @param userListBo
     * @return
     */
    public R<PageUtils<UserListManageVo>> listManage(UserListBo userListBo) {
        return R.ok(userProducer.listManage(userListBo));
    }

    /**
     * 获取登录的临时凭证
     *
     * @param userId 用户id
     * @return
     */
    public R<String> getLoginTempToken(Long userId) {

        String token = UUID.randomUUID().toString().replaceAll("-", "");

        // 将凭证放到redis，15秒过期
        redisTemplate.opsForValue().set(powerProperties.getTempLoginToken() + token, userId, Duration.ofSeconds(15));

        return R.ok("获取成功", token);
    }

    /**
     * 通过临时凭证登录
     *
     * @param tempToken 临时凭证
     * @param loginIp   登录ip
     * @return
     */
    public R<UserLoginVo> loginByTempToken(String tempToken, String loginIp) {

        Object obj = redisTemplate.opsForValue().get(powerProperties.getTempLoginToken() + tempToken);
        if (obj != null) {
            redisTemplate.delete(powerProperties.getTempLoginToken() + tempToken);

            Long userId = (Long) obj;
            UserVo userVo = this.userProducer.getById(userId);
            if (userVo != null) {
                UserLoginVo userLoginVo = new UserLoginVo();
                BeanUtils.copyProperties(userVo, userLoginVo);
                // 填充剩余用户信息
                fillUserLoginVo(userLoginVo, loginIp, true);

                return R.ok("登录成功", userLoginVo);
            }

        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "凭证无效");
    }

    /**
     * 客户端修改密码
     *
     * @param updatePasswordBo 数据
     * @param userId           用户id
     * @return
     */
    public R<String> updatePasswordByClient(UpdatePasswordByClientBo updatePasswordBo, Long userId) {

        if (!updatePasswordBo.getNewPassword().equals(updatePasswordBo.getCheckPassword())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "两次输入的密码不一致");
        }

        UserVo userVo = this.userProducer.getById(userId);

        String codeKey = powerProperties.getPhoneCodeRedisKey() + userVo.getPhone();
        String code = !resilientRedisTemplate.isDegraded() ? (String) resilientRedisTemplate.getRedisTemplate().opsForValue().get(codeKey) : (String) cacheFallbackDataService.getCacheValueByCache(codeKey);
        if (StrUtil.isBlank(code) || !code.equals(updatePasswordBo.getCode())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "验证码错误");
        }

        this.userProducer.updatePassword(userVo.getId(), updatePasswordBo.getNewPassword());
        UpdatePasswordEvent updatePasswordEvent = new UpdatePasswordEvent(userId, userVo.getId(), updatePasswordBo.getNewPassword(), userVo.getActiveTenantId());
        eventPublisher.publishEvent(updatePasswordEvent);
        return R.ok("修改密码成功");

    }


    /**
     * 更新密码 从企业后台同步
     *
     * @param userId   用户ID
     * @param rawPassword 密码
     * @param tenantId 租户ID
     *
     * @return {@link R }<{@link Void }>
     */
    public R<Void> updatePasswordByGovernance(long userId, String rawPassword, long tenantId) {
        this.userProducer.updatePassword(userId, rawPassword);
        return R.ok("修改密码成功");
    }

    /**
     * 查询所有用户ID
     *
     * @return
     */
    public List<Long> listUserId() {
        return userProducer.listUserId();
    }

    public Long getUserIdByUserNickName(String userNickName) {
        return userProducer.getUserIdByUserNickName(userNickName);
    }

    /**
     * 根据手机号或昵称模糊查询获取用户id集合
     *
     * @param keyword 查询条件
     * @return
     */
    public R<List<Long>> listIdsByLikePhoneOrName(String keyword) {

        List<Long> userIds = this.userProducer.listIdsByLikePhoneOrName(keyword);

        return R.ok(userIds);
    }

    /**
     * 更新用户激活的租户id和修改缓存中的租户信息
     *
     * @param userId
     * @param tenantId
     * @return
     */
    public R<String> updateActiveTenantId(Long userId, Long tenantId) {
        this.userProducer.updateActiveTenantId(userId, tenantId);
        this.userProducer.updateTenantIdRedisCacheById(userId, tenantId);
        return R.ok();
    }

    /**
     * 根据角色id获取用户列表
     *
     * @param roleId 角色id
     * @return
     */
    public R<List<UserVo>> listByRoleId(Long roleId) {

        List<UserVo> userInfoVos = this.userProducer.listByRoleId(roleId);
        if (userInfoVos != null && userInfoVos.size() > 0) {
            for (UserVo userInfoVo : userInfoVos) {
                userInfoVo.setPassword("");
                userInfoVo.setIps("");
                if (ObjectUtil.isNotEmpty(userInfoVo.getPhone()) && userInfoVo.getPhone().length() >= 8) {
                    userInfoVo.setPhone(userInfoVo.getPhone().substring(0, 3) + "****" + userInfoVo.getPhone().substring(7));
                }
            }
            return R.ok(userInfoVos);
        }

        return R.error(Constant.CodeMsgEnum.NO_POWER.getCode(), "用户为空");
    }

    /**
     * 根据来源渠道id集合获取用户列表
     *
     * @param channelIds 来源渠道id集合
     * @return
     */
    public R<List<UserInfoVo>> listByChannelIds(List<Long> channelIds) {

        List<UserInfoVo> userInfoVos = this.userProducer.listByChannelIds(channelIds);

        return R.ok(userInfoVos);
    }

    public Long getUserIdNoPre(Long userId) {
        return userProducer.getUserIdNoPre(userId);
    }

    public R<UserVo> getByPhone(String phone) {
        return R.ok(this.userProducer.getByPhone(phone));
    }

    public List<UserDto> listByPhones(List<String> phones) {
        return this.userProducer.listByPhones(phones);
    }

    public R<UserVo> getByPhoneTenant(String phone) {
        return R.ok(this.userProducer.getByPhoneTenant(phone));
    }

    /**
     * 判断用户是否允许登录 （判断客户端只允许一台设备在线，如果客户端已经在线并且30s无心跳，则可以运行其它客户端登录并下线当前已经登录客户端返回true 反之则为false）
     *
     * @return 返回true 表示允许登录 false不允许登录
     * @author RayChou
     * @date 2023-06-20
     */
    private boolean checkUserLoginPermission(UserLoginVo userLoginVo) {
        // 只有普通用户和会员用户需要检查在线状态
        if (userLoginVo.getUserType() != 0 && userLoginVo.getUserType() != 2) {
            return true; // 直接返回null表示允许登录
        }
        // 判断是否处于降级状态
        String userLoginInfoKey = powerProperties.getUserLoginInfoRedisKey() + userLoginVo.getId();
        Map<Object, Object> userLoginInfoMap;
        if (!resilientRedisTemplate.isDegraded()) {
            userLoginInfoMap = resilientRedisTemplate.getRedisTemplate().opsForHash().entries(userLoginInfoKey);
        } else {
            // 如果用户登录信息不存在可以直接登录
            List<UserLoginInfoEntity> loginInfoEntityList = userTokenProducer.listUserLoginInfoByUserId(userLoginVo.getId());
            if (CollectionUtil.isEmpty(loginInfoEntityList)) {
                return true;
            }
            userLoginInfoMap = loginInfoEntityList.stream().collect(Collectors.toMap(UserLoginInfoEntity::getToken, item -> item, (t1, t2) -> t1));
        }
        // 如果没有登录信息，直接允许登录
        if (userLoginInfoMap.isEmpty()) {
            return true;
        }

        // 判断是否存在有效的客户端登录
        long currentTime = System.currentTimeMillis();
        boolean hasActiveClientLogin = false;

        // 存储需要删除的token
        List<String> tokensToDelete = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : userLoginInfoMap.entrySet()) {
            String loginToken = (String) entry.getKey();
            UserLoginInfoCacheVo loginInfo;
            if (!resilientRedisTemplate.isDegraded()) {
                loginInfo = (UserLoginInfoCacheVo) entry.getValue();
            } else {
                UserLoginInfoEntity entryValue = (UserLoginInfoEntity) entry.getValue();
                loginInfo = new UserLoginInfoCacheVo(entryValue.getSourceInfo(), entryValue.getLastRequestTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), entryValue.getFingerprint());
            }
            // 只检查客户端来源的登录
            if (!LoginSourceEnum.CLIENT.getLoginSourceValue().equals(loginInfo.getSource())) {
                continue;
            }
            // 判断是否在线（30秒内有活动）ro
            boolean isOnline = loginInfo.getLastRequestTime() != null &&
                    (currentTime - loginInfo.getLastRequestTime() <= 30000);
            if (isOnline) {
                hasActiveClientLogin = true;
            } else {
                // 已离线，收集需要删除的token
                tokensToDelete.add(loginToken);
            }
        }
        // 批量删除过期的登录信息
        userTokenProducer.batchRemoveTokenByUserId(userLoginVo.getId(), tokensToDelete);
        // 如果有活跃的客户端登录，不允许登录
        return !hasActiveClientLogin;
    }

    /**
     * 将Redis中的token信息全部加载到数据库
     *
     * @return 同步结果信息
     */
    public String loadRedisTokensToDatabase() {
        return userTokenProducer.loadRedisTokensToDatabase();
    }

    /**
     * 查询管理员列表
     *
     * @param userListBo
     * @return
     */
    public R<PageUtils<UserVo>> listAdmin(UserListBo userListBo) {
        return R.ok(this.userProducer.listAdmin(userListBo));
    }

    /**
     * 重置密码（随机六位数）
     *
     * @param id
     * @return
     */
    public R<String> resetRandomPassword(Long id) {
        UserVo userVo = this.userProducer.getById(id);
        if (userVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户不存在");
        }

        SecureRandom random = new SecureRandom();
        // 生成第一位数字 (1-9)
        int firstDigit = random.nextInt(9) + 1;
        // 后五位可以是 0-9
        StringBuilder password = new StringBuilder();
        password.append(firstDigit);

        for (int i = 0; i < 5; i++) {
            // 0-9
            password.append(random.nextInt(10));
        }

        boolean updated = this.userProducer.resetRandomPassword(id, password.toString());
        if (!updated) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "重置密码失败，请重试");
        }
        UserCacheVo localUser = GlobalObject.getLocalUser();
        UpdatePasswordEvent updatePasswordEvent = new UpdatePasswordEvent(localUser == null ? 0 : localUser.getId(), userVo.getId(), password.toString(), userVo.getActiveTenantId());
        eventPublisher.publishEvent(updatePasswordEvent);
        return R.ok(password.toString());
    }

    /**
     * 获取租户ids
     *
     * @param userIds 用户列表
     * @return 租户ids
     */
    public List<Long> getTenantIdsByUserIds(List<Long> userIds) {
        return userProducer.getTenantIdsByUserIds(userIds);
    }


    /**
     * 获取用户手机号
     *
     * @param userIds 用户列表
     * @return 手机号
     */
    public Map<Long, String> getUserPhoneMap(Collection<Long> userIds) {
        return userProducer.getUserPhoneMap(userIds);
    }

    /**
     * 客户端获取用户的子账号列表
     *
     * @return
     */
    public R<List<SubUserListVo>> clientGetSubUserList() {

        UserCacheVo user = GlobalObject.getLocalUser();

        List<SubUserListVo> subUserListVoList = new ArrayList<>();

        // 查询自己
        UserInfoVo userInfoVo = this.userProducer.infoById(user.getId());
        if (userInfoVo == null) {
            return R.error(StatusCode.OPERATION_EX.getCode(), "用户不存在");
        }
        subUserListVoList.add(BeanUtil.copyProperties(userInfoVo, SubUserListVo.class));

        // 查询子账号
        List<SubUserListVo> temp = this.userRse.getSubUserListByUserId(user.getId());
        if (ObjectUtil.isNotEmpty(temp)) {
            subUserListVoList.addAll(temp);
        }


        // 获取所有子用户的ID
        List<Long> subUserIds = subUserListVoList.stream().map(SubUserListVo::getId).collect(Collectors.toList());

        // 封装用户的主播数量
        packageUserAnchorNum(subUserListVoList, subUserIds, user.getActiveTenantId());

        // 封装用户的视频数量
        packageUserVideoNum(subUserListVoList, subUserIds, user.getActiveTenantId());

        // 封装用户的昨日小结数量
        packageUserNotesNum(subUserListVoList, subUserIds, user.getActiveTenantId());

        List<SubUserListVo> myList = subUserListVoList.stream().filter(item -> ObjectUtil.equals(item.getId(), user.getId())).toList();
        List<SubUserListVo> childList = subUserListVoList.stream().filter(item -> !ObjectUtil.equals(item.getId(), user.getId())).toList();

        if (ObjectUtil.isNotEmpty(myList)) {
            Long parentId = myList.stream().map(SubUserListVo::getParentId).findFirst().orElse(0L);
            // 封装用户的昨日资源消耗情况-自己的
            packageUserResourceConsumption(myList, myList.stream().map(SubUserListVo::getId).toList(), parentId);
            // 封装用户的本月资源消耗情况-自己的
            packageUserMonthlyResourceConsumption(myList, myList.stream().map(SubUserListVo::getId).toList(), parentId);
        }

        if (ObjectUtil.isNotEmpty(childList)) {
            // 封装用户的昨日资源消耗情况-子账号的
            packageUserResourceConsumption(childList, childList.stream().map(SubUserListVo::getId).toList(), user.getId());
            // 封装用户的本月资源消耗情况-子账号的
            packageUserMonthlyResourceConsumption(childList, childList.stream().map(SubUserListVo::getId).toList(), user.getId());
        }

        // 资源消耗默认为空列表
        for (SubUserListVo subUser : subUserListVoList) {
            if (subUser.getYesterdayResourceConsumption() == null) {
                subUser.setYesterdayResourceConsumption(new ArrayList<>());
            }
            if (subUser.getMonthlyResourceConsumption() == null) {
                subUser.setMonthlyResourceConsumption(new ArrayList<>());
            }
        }

        return R.ok(subUserListVoList);
    }

    /**
     * 封装用户的视频数量
     *
     * @param subUserListVoList 用户列表
     * @param subUserIds        用户id集合
     */
    private void packageUserVideoNum(List<SubUserListVo> subUserListVoList, List<Long> subUserIds, Long tenantId) {
        // 调用 Feign 接口获取每个子用户本月的录制视频数量
        try {
            R<List<UserVideoCountDto>> videoCountResult = anchorVideoFeign.getMonthlyVideoCountByUserIdsAndTenantId(subUserIds, tenantId);

            if (videoCountResult != null && videoCountResult.getData() != null) {
                List<UserVideoCountDto> videoCountList = videoCountResult.getData();
                // 转换为Map便于查找
                Map<Long, Integer> videoCountMap = videoCountList.stream()
                        .collect(Collectors.toMap(UserVideoCountDto::getUserId,
                                UserVideoCountDto::getVideoCount));
                // 设置每个子用户的视频数量
                for (SubUserListVo subUser : subUserListVoList) {
                    Integer videoCount = videoCountMap.get(subUser.getId());
                    subUser.setVideoCount(videoCount != null ? videoCount : 0);
                }
            }
        } catch (Exception e) {
            // 如果调用失败，视频数量默认为0
            for (SubUserListVo subUser : subUserListVoList) {
                subUser.setVideoCount(0);
            }
        }

        // 调用 Feign 接口获取每个子用户昨日的录制视频数量
        try {
            R<List<UserVideoCountDto>> yesterdayVideoCountResult = anchorVideoFeign.getYesterdayVideoCountByUserIdsAndTenantId(subUserIds, tenantId);

            if (yesterdayVideoCountResult != null && yesterdayVideoCountResult.getData() != null) {
                List<UserVideoCountDto> yesterdayVideoCountList = yesterdayVideoCountResult.getData();
                // 转换为Map便于查找
                Map<Long, Integer> yesterdayVideoCountMap = yesterdayVideoCountList.stream()
                        .collect(Collectors.toMap(UserVideoCountDto::getUserId,
                                UserVideoCountDto::getVideoCount));
                // 设置每个子用户的昨日视频数量
                for (SubUserListVo subUser : subUserListVoList) {
                    Integer yesterdayVideoCount = yesterdayVideoCountMap.get(subUser.getId());
                    subUser.setYesterdayVideoCount(yesterdayVideoCount != null ? yesterdayVideoCount : 0);
                }
            }
        } catch (Exception e) {
            // 如果调用失败，昨日视频数量默认为0
            for (SubUserListVo subUser : subUserListVoList) {
                subUser.setYesterdayVideoCount(0);
            }
        }
    }

    /**
     * 封装用户的主播数量
     *
     * @param subUserListVoList 用户列表
     * @param subUserIds        用户id集合
     */
    private void packageUserAnchorNum(List<SubUserListVo> subUserListVoList, List<Long> subUserIds, Long tenantId) {
        try {
            // 调用 Feign 接口获取每个子用户的主播数量
            R<List<UserAnchorCountDto>> anchorCountResult = anchorUrlUserFeign.getAnchorCountByUserIdsAndTenantId(subUserIds, tenantId);
            if (anchorCountResult != null && anchorCountResult.getData() != null) {
                List<UserAnchorCountDto> anchorCountList = anchorCountResult.getData();
                // 转换为Map便于查找
                Map<Long, Integer> anchorCountMap = anchorCountList.stream()
                        .collect(Collectors.toMap(UserAnchorCountDto::getUserId,
                                UserAnchorCountDto::getAnchorCount));
                // 设置每个子用户的主播数量
                for (SubUserListVo subUser : subUserListVoList) {
                    Integer anchorCount = anchorCountMap.get(subUser.getId());
                    subUser.setAnchorCount(anchorCount != null ? anchorCount : 0);
                }
            }
        } catch (Exception e) {
            // 如果调用失败，主播数量默认为0
            for (SubUserListVo subUser : subUserListVoList) {
                subUser.setAnchorCount(0);
            }
        }
    }

    /**
     * 封装用户的昨日小结数量
     *
     * @param subUserListVoList 用户列表
     * @param subUserIds        用户id集合
     */
    private void packageUserNotesNum(List<SubUserListVo> subUserListVoList, List<Long> subUserIds, Long tenantId) {
        try {
            // 调用 Feign 接口获取每个子用户昨日的小结数量
            R<List<UserNotesCountDto>> notesCountResult = videoTextNotesFeign.getYesterdayNotesCountByUserIds(subUserIds, tenantId);

            if (notesCountResult != null && notesCountResult.getData() != null) {
                List<UserNotesCountDto> notesCountList = notesCountResult.getData();
                // 转换为Map便于查找
                Map<Long, Integer> notesCountMap = notesCountList.stream()
                        .collect(Collectors.toMap(UserNotesCountDto::getUserId,
                                UserNotesCountDto::getNotesCount));
                // 设置每个子用户的昨日小结数量
                for (SubUserListVo subUser : subUserListVoList) {
                    Integer notesCount = notesCountMap.get(subUser.getId());
                    subUser.setYesterdayNotesCount(notesCount != null ? notesCount : 0);
                }
            }
        } catch (Exception e) {
            // 如果调用失败，昨日小结数量默认为0
            for (SubUserListVo subUser : subUserListVoList) {
                subUser.setYesterdayNotesCount(0);
            }
        }
    }

    /**
     * 封装用户的昨日资源消耗情况
     *
     * @param subUserListVoList 用户列表
     * @param subUserIds        用户id集合
     */
    private void packageUserResourceConsumption(List<SubUserListVo> subUserListVoList, List<Long> subUserIds, Long parentUserId) {
        try {
            // 调用 Feign 接口获取每个子用户昨日的资源消耗统计
            List<UserResourceConsumptionDto> resourceConsumptionList = userPropertyDetailsFeign.getYesterdayResourceConsumption(subUserIds, parentUserId);
            if (resourceConsumptionList != null && !resourceConsumptionList.isEmpty()) {
                // 按用户ID分组
                Map<Long, List<UserResourceConsumptionDto>> consumptionMap = resourceConsumptionList.stream()
                        .collect(Collectors.groupingBy(UserResourceConsumptionDto::getUserId));
                // 设置每个子用户的昨日资源消耗情况
                for (SubUserListVo subUser : subUserListVoList) {
                    List<UserResourceConsumptionDto> userConsumptions = consumptionMap.get(subUser.getId());
                    if (userConsumptions != null && !userConsumptions.isEmpty()) {
                        // 转换为VO
                        List<UserResourceConsumptionVo> consumptionVos = userConsumptions.stream()
                                .map(dto -> {
                                    UserResourceConsumptionVo vo = new UserResourceConsumptionVo();
                                    BeanUtil.copyProperties(dto, vo);
                                    return vo;
                                })
                                .collect(Collectors.toList());
                        subUser.setYesterdayResourceConsumption(consumptionVos);
                    } else {
                        subUser.setYesterdayResourceConsumption(new ArrayList<>());
                    }
                }
            }
        } catch (Exception ignored) {

        }
    }

    /**
     * 封装用户的本月资源消耗情况
     *
     * @param subUserListVoList 用户列表
     * @param subUserIds        用户id集合
     * @param parentUserId 父id
     */
    private void packageUserMonthlyResourceConsumption(List<SubUserListVo> subUserListVoList, List<Long> subUserIds, Long parentUserId) {
        try {
            // 调用 Feign 接口获取每个子用户本月的资源消耗统计
            List<UserResourceConsumptionDto> resourceConsumptionList = userPropertyDetailsFeign.getMonthlyResourceConsumption(subUserIds, parentUserId);
            if (resourceConsumptionList != null && !resourceConsumptionList.isEmpty()) {
                // 按用户ID分组
                Map<Long, List<UserResourceConsumptionDto>> consumptionMap = resourceConsumptionList.stream()
                        .collect(Collectors.groupingBy(UserResourceConsumptionDto::getUserId));
                // 设置每个子用户的本月资源消耗情况
                for (SubUserListVo subUser : subUserListVoList) {
                    List<UserResourceConsumptionDto> userConsumptions = consumptionMap.get(subUser.getId());
                    if (userConsumptions != null && !userConsumptions.isEmpty()) {
                        // 转换为VO
                        List<UserResourceConsumptionVo> consumptionVos = userConsumptions.stream()
                                .map(dto -> {
                                    UserResourceConsumptionVo vo = new UserResourceConsumptionVo();
                                    BeanUtil.copyProperties(dto, vo);
                                    return vo;
                                })
                                .collect(Collectors.toList());
                        subUser.setMonthlyResourceConsumption(consumptionVos);
                    } else {
                        subUser.setMonthlyResourceConsumption(new ArrayList<>());
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void sendCustomerAcquisitionMsg(Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            throw new BusinessException("用户id不能为空");
        }
        UserVo userDto = userProducer.getById(userId);
        if (ObjectUtil.isNull(userDto)) {
            throw new BusinessException("用户id错误，没有这个userId");
        }

        SalesInfoVo sales = salesProducer.getByUserId(userId);
        if (ObjectUtil.isNull(sales)) {
            throw new BusinessException("没有设置销售，不能发生短信");
        }

        if (ObjectUtil.isEmpty(sales.getSalesIntroductionUrl())) {
            throw new BusinessException("销售没有设置获客链接，请先设置后再发送");
        }

        // 发生短信
        SmsResult smsResult = sendCustomerAcquisitionMsg(userDto.getPhone(), sales.getSalesIntroductionUrl());
        if (!smsResult.isSuccess()) {
            throw new BusinessException(smsResult.getMessage());
        }
    }

    public SmsResult sendCustomerAcquisitionMsg(String phone, String url) {
        // TODO 上线前要修改参数，没有修改参数
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("code", url);
        return SmsServiceFeign.send("register_complete_first_send", phone, map, null);
    }

    /**
     * 条件分页查询用户列表
     *
     * @param userListBo
     * @return
     */
    public PageUtils<UserListVo> pageListNew(UserListBo userListBo) {

        return userProducer.pageListNew(userListBo);
    }

    public List<UserVo> listUserRollupFieldAll() {
        return userProducer.listUserRollupFieldAll();
    }

    /**
     * 客户端用户列表
     *
     * @param page 分页参数
     * @return 数据
     */
    public PageUtils<UserVo> clientUserPageList(ClientPageList page) {
        return userProducer.clientUserPageList(page);
    }

    /**
     * 修改员工状态
     * @param id id
     * @param employeeStatus 员工状态
     */
    public void updateEmployeeStatus(Long id, Integer employeeStatus) {
        userProducer.updateEmployeeStatus(id, employeeStatus);
    }

    /**
     * 获取修改员工状态的参数
     * @param userId 用户id
     * @return 参数
     */
    public UpdateEmployeeStatusVo getUpdateEmployeeParams(Long userId) {
        UpdateEmployeeStatusVo res = new UpdateEmployeeStatusVo();

        if (userId == null) {
            return res;
        }
        UserInfoVo user = userProducer.infoById(userId);
        UserDetailsInfoVo detail = userDetailsProducer.userDetailByUserId(userId);
        if (user == null || detail == null) {
            return res;
        }
        if (user.getAdminUserType() == UserEnums.adminUserType.AGENT.getCode()) {
            res.setAgentId(detail.getAgentId());
        } else if (user.getAdminUserType() == UserEnums.adminUserType.AGENT_SALE.getCode()) {
            res.setSalesId(detail.getSaleId());
        } else if (user.getUserType() == UserEnums.userType.MANAGE_ADMIN_USER.getCode() && ObjectUtil.isNotEmpty(detail.getSaleId())) {
            res.setSalesId(detail.getSaleId());
        }
        res.setUserId(userId);
        return res;
    }

    /**
     * 仪表盘统计
     *
     * @param trialOrder 试用订单
     * @return 数据
     */
    public DashboardStatisticsVo dashboardStatistics(Integer trialOrder) {
        return userProducer.dashboardStatistics(trialOrder);
    }

    public PaidDashboardStatisticsVo paidDashboardStatistics() {
        return userProducer.paidDashboardStatistics();
    }

    public PageUtils<DashboardListVo> pageDashboardList(DashboardListBo dashboardListBo) {
        PageUtils<DashboardListVo> res = userProducer.pageDashboardList(dashboardListBo);

        if (ObjectUtil.isNotEmpty(res.getList())) {
            List<Long> channelIds = res.getList().stream().map(DashboardListVo::getChannelId).distinct().toList();
            Map<Long, String> channelMap = channelFeign.getChannelParentNameByIds(channelIds);

            Map<Integer, String> packageMap = packageFeign.listSingleAll(1).stream().collect(Collectors.toMap(PackageVo::getLevel, PackageVo::getName));

            for (DashboardListVo item : res.getList()) {
                if (ObjectUtil.isNotEmpty(item.getChannelId())) {
                    item.setChannelName(channelMap.getOrDefault(item.getChannelId(), null));
                }

                if (ObjectUtil.equals(item.getOrderStatus(), 0)) {
                    item.setPackageName(packageMap.getOrDefault(item.getLevel(), null));
                } else if (ObjectUtil.equals(item.getOrderStatus(), 1)) {
                    item.setPackageName(packageMap.getOrDefault(item.getLevelTwo(), null));
                }

                // 手机号脱敏
                item.setPhone(CommonUtils.maskPhone(item.getPhone()));
            }
        }

        return res;
    }

    /**
     * 获取用户的子账号数量
     *
     * @param userId 用户id
     * @return 子账号数量
     */
    public int getSubAccountNum(Long userId) {
        return userProducer.getSubAccountNum(userId);
    }



    /**
     * 设置用户客户端版本
     *
     * @param userId 用户
     * @param level  用户版本等级
     */
    public void saveClientVersionByLevel(Long userId, Integer level) {
        // 从 KV 配置获取纯录制版对应的 level 值
        Integer pureRecordingLevel = systemKvProducer.getValueByKey("pure_recording_version_level", (Integer) null);
        if (pureRecordingLevel == null) {
            pureRecordingLevel = 0;
        }

        if (level == null || level == -1 || level == 0 || level.equals(pureRecordingLevel)) {
            return;
        }

        userRse.saveClientVersion(userId, OrderEnums.clientVersion.REPLAY.getCode());
    }


    public void saveClientVersion(Long userId, String clientVersion) {
        if (userId == null || clientVersion == null) {
            return;
        }
        UserBusinessEntity business = userBusinessService.getById(userId);
        if (business == null) {
            business = new UserBusinessEntity();
            business.setUserId(userId);
            business.setCreateDate(new Date());
        }
        business.setClientVersion(clientVersion);
        business.setUpdateDate(new Date());
        userBusinessService.saveOrUpdate(business);
    }

}
