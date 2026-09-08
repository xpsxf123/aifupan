package com.jiuyu.governance.business.rbac.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.json.JsonTemplate;
import com.jiuyu.framework.lock.ResourceLock;
import com.jiuyu.framework.mybatispuls.util.CustomPage;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.util.FunctionUtil;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.org.service.DeptService;
import com.jiuyu.governance.business.org.service.PositionService;
import com.jiuyu.governance.business.org.service.SubCompanyService;
import com.jiuyu.governance.business.org.service.TeamService;
import com.jiuyu.governance.business.org.service.impl.ManagerConnectorProcessor;
import com.jiuyu.governance.business.rbac.mapper.EmployeeMapper;
import com.jiuyu.governance.business.rbac.mapper.UserRoleMapper;
import com.jiuyu.governance.business.rbac.pojo.bo.MobileLoginEmployee;
import com.jiuyu.governance.business.rbac.pojo.constants.AccountStatus;
import com.jiuyu.governance.business.rbac.pojo.constants.DefaultPosition;
import com.jiuyu.governance.business.rbac.pojo.constants.JobType;
import com.jiuyu.governance.business.rbac.pojo.entity.Employee;
import com.jiuyu.governance.business.rbac.pojo.entity.UserRole;
import com.jiuyu.governance.business.rbac.pojo.request.*;
import com.jiuyu.governance.business.rbac.pojo.response.EmployeeInfoResponse;
import com.jiuyu.governance.business.rbac.pojo.response.EmployeeUnbindResponse;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.business.rbac.service.RoleService;
import com.jiuyu.governance.business.rbac.service.TenantPrivilegeService;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.init.InitializationTenant;
import com.jiuyu.governance.common.init.TenantInitContext;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.bo.CountData;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.common.utils.PasswordHandler;
import com.jiuyu.governance.openfeign.replay.PropertyService;
import com.jiuyu.governance.openfeign.replay.UserAccountService;
import com.jiuyu.governance.openfeign.replay.consts.PropertyType;
import com.jiuyu.governance.openfeign.replay.response.ReplayAccountDetailResponse;
import com.jiuyu.governance.openfeign.replay.response.ReplayUserDetailsInfo;
import com.jiuyu.governance.plugins.oauth.data.BeforePermission;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 员工服务实现类
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService, InitializationTenant {

    private final SubCompanyService subCompanyService;

    private final DeptService deptService;

    private final RoleService roleService;

    private final UserRoleMapper roleMapper;

    private final PositionService positionService;

    private final TeamService teamService;

    private final UserAccountService userAccountService;

    private final PropertyService propertyService;

    private final ManagerConnectorProcessor connectorProcessor;

    private final StringRedisTemplate redisTemplate;

    private final String employeeRoleCachePrefix = "oauth:employee-role:";

    private final TenantPrivilegeService tenantPrivilegeService;

    /**
     * 获取员工角色缓存key
     *
     * @param employeeId 员工ID
     * @return 缓存key
     */
    private String getEmployeeRoleKey(long employeeId) {
        return employeeRoleCachePrefix + employeeId;
    }

    /**
     * 默认顺序
     *
     * @return 顺序
     */
    @Override
    public int getOrder() {
        return 10;
    }

    /**
     * 初始化
     *
     * @param tenantContext 租户初始化上下文
     * @param accessUser    访问用户
     */
    @Override
    public void init(TenantInitContext tenantContext, AccessUser accessUser) {
        ApiResponse<ReplayAccountDetailResponse> apiResponse = userAccountService.getMainAccountDetail(tenantContext.getMainAccountId());
        if (apiResponse.failed()) {
            throw new BusinessException(BizErrorCode.GENERAL_FAILED, "获取主账户信息失败: " + apiResponse.getMsg());
        }
        ReplayAccountDetailResponse replayAccount = apiResponse.getData();
        // 判断是否主账户
        if (!Objects.equals(replayAccount.getUserType(), 0)) {
            throw new BusinessException(SystemErrorCode.UNPROCESSABLE_ENTITY, "当前账户[" + replayAccount.getNickName() + "]不是客户端主账户");
        }
        // 判断套餐
        if (replayAccount.getPackageLevel() == null) {
            throw new BusinessException(BizErrorCode.UNAUTHORIZED, "当前账户[" + replayAccount.getNickName() + "]未开通套餐");
        }
        if (replayAccount.getPackageLevel() < InitializationTenant.MIN_GOVERNANCE_LEVEL) {
            throw new BusinessException(BizErrorCode.PACKAGE_INVALID, "当前账户[" + replayAccount.getNickName() + "]套餐版本低于企业版");
        }
        Optional<Long> optional = this.getTenantMainAccountId(replayAccount.getTenantId());
        if (optional.isPresent()) {
            throw new BusinessException(BizErrorCode.DATA_DUPLICATED, "当前租户已存在主账户");
        }
        tenantContext.setTenantId(replayAccount.getTenantId());
        if (this.clientUserExist(tenantContext.getMainAccountId(), null)) {
            throw new BusinessException(BizErrorCode.DATA_DUPLICATED, "账户已存在,请查看企业管理数据");
        }
//        if (this.mobileOccupy(replayAccount.getPhone(), null)) {
//            throw new BusinessException(BizErrorCode.DATA_DUPLICATED, "手机号码已存在,请查看企业管理数据");
//        }
        Employee employee = new Employee();
        employee.setCreateDate(LocalDateTime.now());
        employee.setUpdateDate(employee.getCreateDate());
        employee.setCreateBy(0L);
        employee.setUpdateBy(0L);
        employee.setIsDeleted(false);
        employee.setTenantId(tenantContext.getTenantId());
        employee.setHoldTenant(true);
        employee.setName(replayAccount.getNickName());
        employee.setUserAvatar("");
        employee.setStaffNumber("1");
        employee.setMobile(replayAccount.getPhone());
        employee.setEmail("");
        employee.setCompanyId(tenantContext.getCompanyId() == null ? 0L : tenantContext.getCompanyId());
        employee.setDeptId(0L);
        employee.setTeamId(0L);
        employee.setPositionId(0L);
        employee.setOnRec(true);
        employee.setConnectorClientUserId(tenantContext.getMainAccountId());
        employee.setJobType(JobType.FULL_TIME);
        employee.setAccountStatus(AccountStatus.NORMAL);
        // 默认密码为手机号码
        employee.setPassword(PasswordHandler.encode(replayAccount.getPhone()));

        // 如果当前用户为租户主账户
        if (Objects.equals(accessUser.userType(), OauthConstant.CLIENT_USER) && Objects.equals(accessUser.userId(), replayAccount.getUserId())) {
            // 同时元数据中携带了密码
            if (EmptyUtil.isNotEmpty(accessUser.metadata()) && EmptyUtil.isNotEmpty(accessUser.metadata().get("password"))) {
                employee.setPassword(accessUser.metadata().get("password"));
            }
        }

        super.save(employee);
        log.info("[租户初始化] init tenant, new main account, id: {}, {} tenant[{}]", employee.getId(), employee.getName(), employee.getTenantId());
        tenantContext.put("mainAccountId", replayAccount.getUserId());
        tenantContext.put("adminId", employee.getId());
    }


    /**
     * 客户用户是否被占用
     *
     * @param clientUserId 客户用户ID
     * @param excludeId    排除ID
     *
     * @return {@link Boolean}
     */
    @Override
    public boolean clientUserExist(long clientUserId, Long excludeId) {
        return super.lambdaQuery().eq(Employee::getConnectorClientUserId, clientUserId)
            .eq(Employee::getIsDeleted, false)
            .eq(excludeId != null, Employee::getId, excludeId)
            .exists();
    }

    /**
     * 手机号码是否被占用(可用租户下)
     *
     * @param mobile    手机号码
     * @param excludeId 排除ID
     *
     * @return {@link Boolean}
     */
    @Override
    public boolean mobileOccupy(String mobile, Long excludeId) {
        List<Long> tenantIds = super.lambdaQuery().eq(Employee::getMobile, mobile)
            .eq(Employee::getIsDeleted, false)
            .eq(Employee::getAccountStatus, AccountStatus.NORMAL.getValue())
            .ne(excludeId != null, Employee::getId, excludeId)
            .select(Employee::getTenantId)
            .list().stream().map(Employee::getTenantId).toList();
        if (EmptyUtil.isEmpty(tenantIds)) {
            return false;
        }
        List<Long> availableTenantIds = tenantPrivilegeService.availableTenantIds(tenantIds, true);
        return EmptyUtil.isNotEmpty(availableTenantIds);
    }

    /**
     * 租户下手机号码是否被占用
     *
     * @param tenantId  租户ID
     * @param mobile    手机号码
     * @param excludeId 排除ID
     *
     * @return {@link Boolean}
     */
    @Override
    public boolean tenantMobileOccupy(long tenantId, String mobile, Long excludeId) {
        return super.lambdaQuery().eq(Employee::getMobile, mobile)
            .eq(Employee::getIsDeleted, false)
            .eq(Employee::getTenantId, tenantId)
            .ne(excludeId != null, Employee::getId, excludeId).exists();
    }

    /**
     * 创建录制账号
     *
     * @param employee 员工信息
     */
    private void createReplayAccount(Employee employee) {
        Optional<Long> optional = this.getTenantMainAccountId(employee.getTenantId());
        if (optional.isEmpty()) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "当前租户找不到主账户ID");
        }
        long mainAccountId = optional.get();
        ApiResponse<Long> apiResponse;
        if (employee.getConnectorClientUserId() == null || employee.getConnectorClientUserId() <= 0) {
            apiResponse = userAccountService.bind(mainAccountId, employee.getMobile(), employee.getName());
        } else {
            apiResponse = userAccountService.bind(mainAccountId, employee.getConnectorClientUserId());
        }
        if (apiResponse.failed()) {
            throw new BusinessException(BizErrorCode.GENERAL_FAILED, "创建录制账号失败: " + apiResponse.getMsg());
        }
        super.lambdaUpdate()
            .eq(Employee::getId, employee.getId())
            .set(Employee::getConnectorClientUserId, apiResponse.getData())
            .update();
        log.info("[员工] employee new connector client account, employeeId: {}, clientAccountId: {}", employee.getId(), apiResponse.getData());
    }


    /**
     * 解绑录制账号
     */
    private void unbindReplayAccount(Employee employee) {
        if (EmptyUtil.isEmpty(employee.getConnectorClientUserId()) || employee.getConnectorClientUserId() == 0) {
            return;
        }
        Optional<Long> optional = this.getTenantMainAccountId(employee.getTenantId());
        if (optional.isEmpty()) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "当前租户找不到主账户ID");
        }
        Long mainAccountId = optional.get();
        ApiResponse<Void> apiResponse = userAccountService.unbind(mainAccountId, employee.getConnectorClientUserId());
        if (apiResponse.failed() && !Objects.equals(apiResponse.getCode(), BizErrorCode.ALLOW_SKIP.getCode())) {
            throw new BusinessException(BizErrorCode.GENERAL_FAILED, "解绑录制账号失败: " + apiResponse.getMsg());
        }
        super.lambdaUpdate()
            .eq(Employee::getId, employee.getId())
            .set(Employee::getOnRec, false)
            .set(Employee::getUpdateDate, LocalDateTime.now())
            .set(Employee::getUpdateBy, 0L)
            .set(Employee::getConnectorClientUserId, 0L)
            .update();
        log.info("[员工] employee unbind connector client account, employeeId: {}", employee.getId());
    }

    /**
     * 批量加入角色
     *
     * @param employeeId 员工ID
     * @param roleIds    角色ID列表
     */
    private void joinRoles(long employeeId, List<Long> roleIds) {
        List<UserRole> userRoles = roleIds.stream().map(roleId -> {
            UserRole userRole = new UserRole();
            userRole.setId(IdUtil.getSnowflakeNextId());
            userRole.setUserId(employeeId);
            userRole.setRoleId(roleId);
            userRole.setCreateDate(LocalDateTime.now());
            userRole.setUpdateDate(userRole.getCreateDate());
            userRole.setIsDeleted(false);
            return userRole;
        }).toList();
        roleMapper.insertBatch(userRoles);
        redisTemplate.opsForValue().set(this.getEmployeeRoleKey(employeeId), JsonTemplate.toJson(roleIds), Duration.ofHours(1));
    }


    public void batchJoinRole(List<Long> employeeIds, long roleId) {
        if (roleId < 1) {
            return;
        }
        List<UserRole> userRoles = employeeIds.stream().map(employeeId -> {
            UserRole userRole = new UserRole();
            userRole.setId(IdUtil.getSnowflakeNextId());
            userRole.setUserId(employeeId);
            userRole.setRoleId(roleId);
            userRole.setCreateDate(LocalDateTime.now());
            userRole.setUpdateDate(userRole.getCreateDate());
            userRole.setIsDeleted(false);
            return userRole;
        }).toList();
        String cacheBody = JsonTemplate.toJson(List.of(roleId));
        roleMapper.insertBatch(userRoles);
        employeeIds.forEach(employeeId -> redisTemplate.opsForValue().set(this.getEmployeeRoleKey(employeeId), cacheBody, Duration.ofHours(1)));
    }

    /**
     * 获取人员角色ID列表
     *
     * @param employeeId 人员ID
     *
     * @return 角色ID列表
     */
    private List<Long> getRoleIds(long employeeId) {
        String cacheKey = this.getEmployeeRoleKey(employeeId);
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);
        if (cacheValue != null) {
            if (!JsonTemplate.isJsonArray(cacheValue)) {
                return List.of();
            }
            return JsonTemplate.toList(cacheValue, Long.class);
        }
        List<Long> roleIds = ChainWrappers.lambdaQueryChain(roleMapper)
            .eq(UserRole::getUserId, employeeId)
            .eq(UserRole::getIsDeleted, false)
            .select(UserRole::getRoleId)
            .list()
            .stream()
            .map(UserRole::getRoleId)
            .collect(Collectors.toList());
        redisTemplate.opsForValue().set(cacheKey, JsonTemplate.toJson(roleIds), Duration.ofHours(1));
        return roleIds;
    }

    /**
     * 批量删除角色
     *
     * @param employeeId 员工ID
     * @param roleIds    角色ID列表 如果不传表示删除所有
     */
    private void deleteRoles(long employeeId, List<Long> roleIds) {
        ChainWrappers.lambdaUpdateChain(roleMapper)
            .eq(UserRole::getUserId, employeeId)
            .eq(UserRole::getIsDeleted, false)
            .in(EmptyUtil.isNotEmpty(roleIds), UserRole::getRoleId, roleIds)
            .set(UserRole::getIsDeleted, true)
            .set(UserRole::getUpdateDate, LocalDateTime.now())
            .update();
        redisTemplate.delete(this.getEmployeeRoleKey(employeeId));
    }


    /**
     * 获取人员角色ID列表
     *
     * @param employeeIds 人员ID列表
     *
     * @return 人员角色ID列表
     */
    private Map<Long, Long> getEmployeeRoleMap(List<Long> employeeIds) {
        if (EmptyUtil.isEmpty(employeeIds)) {
            return Map.of();
        }
        return ChainWrappers.lambdaQueryChain(roleMapper)
            .eq(UserRole::getIsDeleted, false)
            .in(UserRole::getUserId, employeeIds)
            .select(UserRole::getUserId, UserRole::getRoleId)
            .list()
            .stream()
            .collect(Collectors.toMap(UserRole::getUserId, UserRole::getRoleId, FunctionUtil::mergeFirst));
    }

    /**
     * 新增人员
     *
     * @param request    人员新增请求
     * @param accessUser 访问用户
     */
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission.Multiple(enableLevel = true,
        value = {
            @BeforePermission(type = OauthConstant.COMPANY, dataId = "#request.companyId"),
            @BeforePermission(type = OauthConstant.DEPT, dataId = "#request.deptId", ignoreEmpty = true),
            @BeforePermission(type = OauthConstant.TEAM, dataId = "#request.teamId", ignoreEmpty = true),
        })
    @Override
    public ApiResponse<Void> addEmployee(EmployeeAddRequest request, AccessUser accessUser) {
        if (!PhoneUtil.isMobile(request.getMobile())) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "手机号码格式错误");
        }
        // 1. 校验手机号/工号是否已存在
        boolean exists = super.lambdaQuery()
            .eq(Employee::getIsDeleted, false)
            .eq(Employee::getTenantId, accessUser.currentTenantId())
            .eq(Employee::getStaffNumber, request.getStaffNumber())
            .exists();
        if (exists) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "手机号或工号已存在");
        }
        // 判断手机号码是否已在其他租户存在
        if (this.mobileOccupy(request.getMobile(), null)) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "手机号已存在,请联系员工先解绑账户");
        }
        boolean tenantMobile = this.tenantMobileOccupy(accessUser.currentTenantId(), request.getMobile(), null);
        if (tenantMobile) {
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "该账户已在您的人员账户中");
        }
        // 2. 转换实体
        Employee employee = BeanUtil.copyProperties(request, Employee.class);
        // 3. 设置默认密码与状态
        String password = RandomUtil.randomString(6);
        employee.setPassword(PasswordHandler.encode(password));
        employee.setCreateDate(LocalDateTime.now());
        employee.setUpdateDate(employee.getCreateDate());
        employee.setTenantId(accessUser.currentTenantId());
        employee.setHoldTenant(false);
        employee.setJobType(request.getJobType());
        employee.setCreateBy(accessUser.userId());
        employee.setUpdateBy(accessUser.userId());
        employee.setIsDeleted(false);
        employee.setAccountStatus(AccountStatus.NORMAL);
        if (employee.getOnRec() == null) {
            employee.setOnRec(false);
        }

        long countEmployee = this.countEmployee(accessUser.currentTenantId());
        // 扣除额度
        return propertyService.editTenantPropertyQuota(accessUser.currentTenantId(), countEmployee + 1, PropertyType.ENTERPRISE_PERSON_NUM, result -> {
            if (!result) {
                return ApiResponse.failed(BizErrorCode.QUOTA_EXCEEDED.getCode(), "人员数量超出限制");
            }
            // 4. 保存
            super.save(employee);

            // 创建子账户
            if (employee.getOnRec()) {
                this.createReplayAccount(employee);
            }

            if (request.getRoleId() != null) {
                this.joinRoles(employee.getId(), Collections.singletonList(request.getRoleId()));
            }

            log.info("[员工] employee new mobile: {}, tenantId: {}", request.getMobile(), accessUser.currentTenantId());

            connectorProcessor.clearTenantCache(employee.getTenantId(), List.of(ManagerType.EMPLOYEE));
            connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, List.of(employee.getCompanyId()), ManagerType.DEPT, List.of(employee.getDeptId()), ManagerType.TEAM, List.of(employee.getTeamId())));
            return ApiResponse.success();
        });
    }


    /**
     * 基于客户端账户创建人员
     *
     * @param account 客户端账户
     *
     * @return {@link ApiResponse<Void>}
     */
    @ResourceLock(prefix = "governance:employee:add-for-replay-account", key = "#account.userId", message = "正在添加人员,请勿频繁点击")
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ApiResponse<Void> addForReplayAccount(ReplayUserDetailsInfo account) {
        // 检查该手机号是否已存在员工记录
        Optional<Employee> employeeOptional = super.lambdaQuery().eq(Employee::getTenantId, account.getTenantId())
            .eq(Employee::getMobile, account.getPhone())
            .eq(Employee::getIsDeleted, false)
            .last("limit 1")
            .oneOpt();

        // 如果已存在且已绑定其他账户，则拒绝操作
        if (employeeOptional.isPresent() && employeeOptional.get().getConnectorClientUserId() > 0) {
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "该账户已绑定其他账户,请联系主账户先解除其他人员绑定");
        }

        // 如果已存在但未绑定账户，则更新绑定关系
        if (employeeOptional.isPresent()) {
            Long employeeId = employeeOptional.get().getId();
            super.lambdaUpdate()
                .eq(Employee::getId, employeeId)
                .set(Employee::getConnectorClientUserId, account.getUserId())
                .update();
            log.info("[员工] employee new bind mobile: {}, tenantId: {}", account.getPhone(), account.getTenantId());
            return ApiResponse.success();
        }

        // 获取租户下的第一个公司ID
        Optional<Long> tenantFirstCompany = subCompanyService.getTenantFirst(account.getTenantId());
        if (tenantFirstCompany.isEmpty()) {
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "请先添加公司");
        }
        long companyId = tenantFirstCompany.get();
        long defaultRoleId = roleService.getDefaultRoleId(account.getTenantId());
        // 创建新员工记录并设置基本信息
        Employee employee = new Employee();
        employee.setCreateDate(LocalDateTime.now());
        employee.setUpdateDate(LocalDateTime.now());
        employee.setCreateBy(0L);
        employee.setUpdateBy(0L);
        employee.setIsDeleted(false);
        employee.setTenantId(account.getTenantId());
        employee.setHoldTenant(false);
        employee.setName(EmptyUtil.isEmpty(account.getRealName()) ? account.getNickName() : account.getRealName());
        employee.setUserAvatar("");
        employee.setStaffNumber(RandomUtil.randomNumbers(8));
        employee.setMobile(account.getPhone());
        employee.setEmail(account.getEmail() == null ? "" : account.getEmail());
        employee.setCompanyId(companyId);
        employee.setDeptId(0L);
        employee.setTeamId(0L);
        employee.setPositionId(null);
        employee.setOnRec(true);
        employee.setConnectorClientUserId(account.getUserId());
        employee.setJobType(JobType.FULL_TIME);
        employee.setAccountStatus(AccountStatus.NORMAL);
        employee.setPassword(EmptyUtil.isEmpty(account.getPassword()) ? PasswordHandler.encode(account.getPhone()) : account.getPassword());

        super.save(employee);
        this.joinRoles(employee.getId(), Collections.singletonList(defaultRoleId));
        log.info("[员工] employee new mobile: {}, tenantId: {}", account.getPhone(), account.getTenantId());
        connectorProcessor.clearTenantCache(employee.getTenantId(), List.of(ManagerType.EMPLOYEE));
        connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, List.of(employee.getCompanyId())));
        return ApiResponse.success();
    }

    /**
     * 同步子账户
     *
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse<Void>}
     */
    @Transactional(rollbackFor = Throwable.class)
    @ResourceLock(prefix = "governance:employee:sync-sub-account", key = "#accessUser.currentTenantId()", message = "正在同步,请勿频繁点击")
    @Override
    public ApiResponse<Void> syncSubAccount(AccessUser accessUser) {
        long tenantId = accessUser.currentTenantId();
        ApiResponse<List<ReplayAccountDetailResponse>> apiResponse = userAccountService.getTenantAllUserList(tenantId);
        if (apiResponse.failed()) {
            return ApiResponse.failed(apiResponse.getCode(), apiResponse.getMsg());
        }
        List<ReplayAccountDetailResponse> dataList = apiResponse.getData();
        if (dataList == null) {
            dataList = Collections.emptyList();
        }
        List<ReplayAccountDetailResponse> remoteSubAccountList = dataList.stream()
            .filter(this::isSubAccount)
            .filter(item -> item.getUserId() != null && item.getUserId() > 0)
            .toList();
        Set<Long> remoteSubAccountIds = remoteSubAccountList.stream()
            .map(ReplayAccountDetailResponse::getUserId)
            .collect(Collectors.toSet());

        List<Employee> boundEmployeeList = super.lambdaQuery()
            .eq(Employee::getTenantId, tenantId)
            .eq(Employee::getIsDeleted, false)
            .eq(Employee::getHoldTenant, false)
            .gt(Employee::getConnectorClientUserId, 0L)
            .list();
//        Map<Long, Employee> boundEmployeeMap = boundEmployeeList.stream()
//            .filter(e -> e.getConnectorClientUserId() != null && e.getConnectorClientUserId() > 0)
//            .collect(Collectors.toMap(Employee::getConnectorClientUserId, Function.identity(), (l, r) -> l));

        Optional<Long> tenantFirstCompany = subCompanyService.getTenantFirst(tenantId);
        if (tenantFirstCompany.isEmpty()) {
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "请先添加公司");
        }
        long firstCompanyId = tenantFirstCompany.get();
        Set<Long> changeCompanyIds = new HashSet<>();
        changeCompanyIds.add(firstCompanyId);

        List<String> remoteMobileList = remoteSubAccountList.stream()
            .map(ReplayAccountDetailResponse::getPhone)
            .filter(EmptyUtil::isNotEmpty)
            .distinct()
            .toList();
        Map<String, Employee> employeeMobileMap = super.lambdaQuery()
            .eq(Employee::getTenantId, tenantId)
            .eq(Employee::getIsDeleted, false)
            .eq(Employee::getHoldTenant, false)
            .in(EmptyUtil.isNotEmpty(remoteMobileList), Employee::getMobile, remoteMobileList)
            .list()
            .stream()
            .collect(Collectors.toMap(Employee::getMobile, Function.identity(), (l, r) -> l));

        LocalDateTime now = LocalDateTime.now();
        List<Employee> needUpdateList = new ArrayList<>();
        List<Employee> needSaveList = new ArrayList<>();
        for (ReplayAccountDetailResponse remoteAccount : remoteSubAccountList) {
            Long subAccountId = remoteAccount.getUserId();
            if (subAccountId == null || subAccountId <= 0) {
                continue;
            }

            if (EmptyUtil.isEmpty(remoteAccount.getPhone())) {
                log.warn("[员工] sync sub account skip: mobile empty, tenantId: {}, subAccountId: {}", tenantId, subAccountId);
                continue;
            }


            Employee mobileEmployee = employeeMobileMap.get(remoteAccount.getPhone());
            if (mobileEmployee != null) {
                Long existsClientUserId = mobileEmployee.getConnectorClientUserId();
                if (existsClientUserId != null && existsClientUserId > 0 && !Objects.equals(existsClientUserId, subAccountId)) {
                    log.warn("[员工] sync sub account skip: employee already bind other account, employeeId: {}, tenantId: {}, existsClientUserId: {}, subAccountId: {}",
                        mobileEmployee.getId(), tenantId, existsClientUserId, subAccountId);
                    continue;
                }
                Employee updateEmployee = new Employee();
                updateEmployee.setId(mobileEmployee.getId());
                updateEmployee.setOnRec(true);
                updateEmployee.setUpdateDate(now);
                updateEmployee.setUpdateBy(accessUser.userId());
                updateEmployee.setConnectorClientUserId(subAccountId);
                if (EmptyUtil.isNotEmpty(remoteAccount.getPassword())) {
                    updateEmployee.setPassword(remoteAccount.getPassword());
                }
                if (mobileEmployee.getAccountStatus() != null && mobileEmployee.getAccountStatus().equals(AccountStatus.FREEZE)) {
                    updateEmployee.setAccountStatus(AccountStatus.NORMAL);
                }
                needUpdateList.add(updateEmployee);

                changeCompanyIds.add(mobileEmployee.getCompanyId());
                continue;
            }

            Employee employee = new Employee();
            employee.setCreateDate(now);
            employee.setUpdateDate(now);
            employee.setCreateBy(accessUser.userId());
            employee.setUpdateBy(accessUser.userId());
            employee.setIsDeleted(false);
            employee.setTenantId(tenantId);
            employee.setHoldTenant(false);
            employee.setName(this.getSyncEmployeeName(remoteAccount));
            employee.setUserAvatar("");
            employee.setStaffNumber(RandomUtil.randomNumbers(8));
            employee.setMobile(remoteAccount.getPhone());
            employee.setEmail(remoteAccount.getEmail() == null ? "" : remoteAccount.getEmail());
            employee.setCompanyId(firstCompanyId);
            employee.setDeptId(0L);
            employee.setTeamId(0L);
            employee.setPositionId(null);
            employee.setOnRec(true);
            employee.setConnectorClientUserId(subAccountId);
            employee.setJobType(JobType.FULL_TIME);
            employee.setAccountStatus(AccountStatus.NORMAL);
            if (EmptyUtil.isNotEmpty(remoteAccount.getPassword())) {
                employee.setPassword(remoteAccount.getPassword());
            } else {
                employee.setPassword(PasswordHandler.encode(remoteAccount.getPhone()));
            }
            needSaveList.add(employee);
        }
        if (EmptyUtil.isNotEmpty(needSaveList)) {
            super.saveBatch(needSaveList);
            long defaultRoleId = roleService.getDefaultRoleId(tenantId);
            batchJoinRole(needSaveList.stream().map(Employee::getId).toList(), defaultRoleId);
        }
        if (EmptyUtil.isNotEmpty(needUpdateList)) {
            super.updateBatchById(needUpdateList);
        }

        for (Employee employee : boundEmployeeList) {
            Long subAccountId = employee.getConnectorClientUserId();
            if (subAccountId == null || subAccountId <= 0) {
                continue;
            }
            if (remoteSubAccountIds.contains(subAccountId)) {
                continue;
            }
            this.unbindReplayAccount(employee);
            changeCompanyIds.add(employee.getCompanyId());
        }

        connectorProcessor.clearTenantCache(tenantId, List.of(ManagerType.EMPLOYEE));
        if (EmptyUtil.isNotEmpty(changeCompanyIds)) {
            connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, new ArrayList<>(changeCompanyIds)));
        }
        if (EmptyUtil.isNotEmpty(remoteMobileList)) {
            super.lambdaUpdate()
                .in(Employee::getMobile, remoteMobileList)
                .ne(Employee::getTenantId, tenantId)
                .eq(Employee::getIsDeleted, false)
                .eq(Employee::getHoldTenant, false)
                .eq(Employee::getAccountStatus, AccountStatus.NORMAL.getValue())
                .set(Employee::getOnRec, false)
                .set(Employee::getAccountStatus, AccountStatus.FREEZE.getValue())
                .set(Employee::getUpdateDate, LocalDateTime.now())
                .update();
        }
        return ApiResponse.success();
    }

    /**
     * 是否子账户
     *
     * @param remoteAccount 客户端账户
     * @return boolean
     */
    private boolean isSubAccount(ReplayAccountDetailResponse remoteAccount) {
        if (remoteAccount == null) {
            return false;
        }
        return Objects.equals(remoteAccount.getUserType(), 2);
    }

    /**
     * 获取同步员工名称
     *
     * @param remoteAccount 账户信息
     *
     * @return 员工名称
     */
    private String getSyncEmployeeName(ReplayAccountDetailResponse remoteAccount) {
        if (remoteAccount == null) {
            return "子账户" + RandomUtil.randomNumbers(6);
        }
        if (EmptyUtil.isNotEmpty(remoteAccount.getRealName())) {
            return remoteAccount.getRealName();
        }
        if (EmptyUtil.isNotEmpty(remoteAccount.getNickName())) {
            return remoteAccount.getNickName();
        }
        if (EmptyUtil.isNotEmpty(remoteAccount.getUsername())) {
            return remoteAccount.getUsername();
        }
        if (EmptyUtil.isNotEmpty(remoteAccount.getPhone())) {
            return remoteAccount.getPhone();
        }
        return "子账户" + RandomUtil.randomNumbers(6);
    }

    /**
     * 统计租户员工数量(排除管理员)
     *
     * @param tenantId 租户ID
     *
     * @return 员工数量
     */
    private long countEmployee(long tenantId) {
        return super.lambdaQuery()
            .eq(Employee::getTenantId, tenantId)
            .eq(Employee::getIsDeleted, false)
            .eq(Employee::getHoldTenant, false)
            .count();
    }

    /**
     * 修改人员信息
     *
     * @param request    人员修改请求
     * @param accessUser 访问用户
     */
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission.Multiple(enableLevel = true,
        value = {
            @BeforePermission(type = OauthConstant.COMPANY, dataId = "#request.companyId"),
            @BeforePermission(type = OauthConstant.DEPT, dataId = "#request.deptId", ignoreEmpty = true),
            @BeforePermission(type = OauthConstant.TEAM, dataId = "#request.teamId", ignoreEmpty = true),
        })
    @Override
    public ApiResponse<Void> updateEmployee(EmployeeUpdateRequest request, AccessUser accessUser) {
        if (!PhoneUtil.isMobile(request.getMobile())) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "手机号码格式错误");
        }
        Employee employee = super.lambdaQuery().eq(Employee::getId, request.getId()).eq(Employee::getTenantId, accessUser.currentTenantId()).eq(Employee::getIsDeleted, false)
            .one();
        if (employee == null) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "人员不存在");
        }
        if (employee.getHoldTenant() && !Objects.equals(employee.getMobile(), request.getMobile())) {
            log.warn("[员工] employee update mobile: {}, tenantId: {} is main account not edit.", request.getMobile(), accessUser.currentTenantId());
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "主账户不允许编辑");
        }
        // 1. 工号是否冲突
        boolean exists = super.lambdaQuery()
            .eq(Employee::getTenantId, accessUser.currentTenantId())
            .eq(Employee::getIsDeleted, false)
            .ne(Employee::getId, request.getId())
            .eq(Employee::getStaffNumber, request.getStaffNumber())
            .exists();
        if (exists) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "修改后的手机号或工号与他人冲突");
        }
        // 验证手机号 在其他可用租户是否存在
        if (this.mobileOccupy(request.getMobile(), employee.getId())) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "手机号已存在,请联系员工先解绑账户");
        }
        boolean tenantMobile = this.tenantMobileOccupy(accessUser.currentTenantId(), request.getMobile(), employee.getId());
        if (tenantMobile) {
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "该账户已在您的人员账户中");
        }
        Long oldCompanyId = employee.getCompanyId();
        Long oldDeptId = employee.getDeptId();
        Long oldTeamId = employee.getTeamId();
        boolean chanceMobile = !Objects.equals(employee.getMobile(), request.getMobile());
        // 2. 更新属性
        BeanUtil.copyProperties(request, employee);
        employee.setUpdateDate(LocalDateTime.now());
        employee.setUpdateBy(accessUser.userId());
        super.updateById(employee);
        List<Long> roleIds = this.getRoleIds(employee.getId());
        if (EmptyUtil.isEmpty(roleIds)) {
            this.joinRoles(employee.getId(), Collections.singletonList(request.getRoleId()));
        } else {
            List<Long> deleteRoleIds = roleIds.stream().filter(roleId -> !roleId.equals(request.getRoleId())).toList();
            if (EmptyUtil.isNotEmpty(deleteRoleIds)) {
                this.deleteRoles(employee.getId(), deleteRoleIds);
            }
            if (!roleIds.contains(request.getRoleId())) {
                this.joinRoles(employee.getId(), Collections.singletonList(request.getRoleId()));
            }
        }
        // 如果修改了手机号码 且绑定了子账户
        if (chanceMobile && employee.getOnRec() && employee.getConnectorClientUserId() > 0 && employee.getAccountStatus().equals(AccountStatus.NORMAL)) {
            ApiResponse<Void> apiResponse = userAccountService.updateMobile(employee.getConnectorClientUserId(), request.getMobile(), employee.getTenantId());
            if (apiResponse.failed()) {
                throw new BusinessException(BizErrorCode.DATA_DUPLICATED, apiResponse.getMsg());
            }
        }
        connectorProcessor.clearTenantCache(employee.getTenantId(), List.of(ManagerType.EMPLOYEE));
        Map<ManagerType, List<Long>> clearMap = new EnumMap<>(ManagerType.class);
        clearMap.put(ManagerType.COMPANY, Stream.of(oldCompanyId, employee.getCompanyId()).filter(Objects::nonNull).distinct().toList());
        clearMap.put(ManagerType.DEPT, Stream.of(oldDeptId, employee.getDeptId()).filter(Objects::nonNull).distinct().toList());
        clearMap.put(ManagerType.TEAM, Stream.of(oldTeamId, employee.getTeamId()).filter(Objects::nonNull).distinct().toList());
        connectorProcessor.clearOrgCache(clearMap);
        return ApiResponse.success();
    }

    /**
     * 人员详情
     *
     * @param id       人员ID
     * @param tenantId 租户ID
     *
     * @return {@link EmployeeInfoResponse}
     */
    @Override
    public EmployeeInfoResponse detail(long id, long tenantId) {
        Optional<Employee> employeeOptional = super.lambdaQuery()
            .eq(Employee::getId, id)
            .eq(Employee::getTenantId, tenantId)
            .eq(Employee::getIsDeleted, false)
            .oneOpt();
        if (employeeOptional.isEmpty()) {
            log.warn("[员工] employee detail not found: {} - tenant: {}", id, tenantId);
            return null;
        }
        Employee employee = employeeOptional.get();
        EmployeeInfoResponse response = BeanUtil.copyProperties(employee, EmployeeInfoResponse.class);
        Complete.start(Collections.singletonList(response))
            .build(EmployeeInfoResponse::getCompanyId, EmployeeInfoResponse::setCompanyName, subCompanyService::getNameMap)
            .then()
            .build(EmployeeInfoResponse::getDeptId, EmployeeInfoResponse::setDeptName, deptService::getDeptNameMap)
            .then()
            .build(EmployeeInfoResponse::getPositionId, EmployeeInfoResponse::setPositionName, positionService::getPositionNameMap)
            .then()
            .build(EmployeeInfoResponse::getTeamId, EmployeeInfoResponse::setTeamName, teamService::getTeamNameMap)
            .then()
            .build(EmployeeInfoResponse::getId, EmployeeInfoResponse::setRoleId, this::getEmployeeRoleMap)
            .doThen()
            .build(EmployeeInfoResponse::getRoleId, EmployeeInfoResponse::setRoleName, roleService::getRoleNameMap)
            .then()
            .over();
        return response;
    }

    /**
     * 分页查询人员
     * 排除主账户
     *
     * @param request    人员查询请求
     * @param accessUser 访问用户
     *
     * @return {@link PageData<EmployeeInfoResponse>}
     */
    @Override
    public PageData<EmployeeInfoResponse> pageQueryEmployee(EmployeeQueryRequest request, AccessUser accessUser) {
        boolean isMain = EmptyUtil.isNotEmpty(accessUser.metadata()) && Objects.equals(accessUser.metadata().get(OauthConstant.TENANT_ADMIN), "true");
        if (request.empty()) {
            return PageData.empty();
        }

        if (isMain) {
            if (EmptyUtil.isNotEmpty(request.getDataCompanyIds())) {
                request.setDataCompanyIds(Stream.concat(request.getDataCompanyIds().stream(), Stream.of(0L)).toList());
            } else {
                request.setDataCompanyIds(Collections.singletonList(0L));
            }
        }

        PageData<Employee> pageData = CustomPage.execute(request, (page, req) -> {
            return super.getBaseMapper().pageQueryEmployee(page, req, accessUser.currentTenantId(), isMain);
        });

        if (EmptyUtil.isEmpty(pageData.getList())) {
            return PageData.empty();
        }

        // 2. 转换为 Response 并使用 Complete 工具类组装字典数据
        PageData<EmployeeInfoResponse> responsePageData = pageData.conversion(emp -> BeanUtil.copyProperties(emp, EmployeeInfoResponse.class));

        Complete.start(responsePageData.getList())
            .build(EmployeeInfoResponse::getCompanyId, EmployeeInfoResponse::setCompanyName, subCompanyService::getNameMap)
            .then()
            .build(EmployeeInfoResponse::getDeptId, EmployeeInfoResponse::setDeptName, deptService::getDeptNameMap)
            .then()
            .build(EmployeeInfoResponse::getPositionId, EmployeeInfoResponse::setPositionName, positionService::getPositionNameMap)
            .then()
            .build(EmployeeInfoResponse::getTeamId, EmployeeInfoResponse::setTeamName, teamService::getTeamNameMap)
            .then()
            .build(EmployeeInfoResponse::getId, EmployeeInfoResponse::setRoleId, this::getEmployeeRoleMap)
            .doThen()
            .build(EmployeeInfoResponse::getRoleId, EmployeeInfoResponse::setRoleName, roleService::getRoleNameMap)
            .then()
            .over();

        return responsePageData;
    }

    /**
     * 开启录制权限
     *
     * @param id         人员ID
     * @param accessUser 访问用户
     */
    @Override
    public ApiResponse<Void> onRec(Long id, AccessUser accessUser) {
        Optional<Employee> employeeOptional = super.lambdaQuery().eq(Employee::getId, id).eq(Employee::getTenantId, accessUser.currentTenantId())
            .eq(Employee::getIsDeleted, false).oneOpt();
        if (employeeOptional.isEmpty()) {
            log.warn("[员工] employee onRec not found: {} - tenant: {}", id, accessUser.currentTenantId());
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "人员不存在");
        }
        Employee employee = employeeOptional.get();
        if (employee.getOnRec()) {
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "人员已开启录制权限");
        }
        if (employee.getHoldTenant()) {
            log.warn("[员工] employee onRec: {} - tenant: {} is main account not on", id, accessUser.currentTenantId());
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "主账户不能关闭录制权限");
        }
        // 判断之前是否已创建过子账户
        if (employee.getConnectorClientUserId() == null || employee.getConnectorClientUserId() <= 0) {
            this.createReplayAccount(employee);
        } else {
            this.unbindReplayAccount(employee);
            log.info("[员工] employee onRec: {} - tenant: {}", id, accessUser.currentTenantId());
        }
        return ApiResponse.success();
    }

    /**
     * 关闭录制权限
     *
     * @param id         人员ID
     * @param accessUser 访问用户
     */
    @Override
    public ApiResponse<Void> offRec(Long id, AccessUser accessUser) {
        Optional<Employee> employeeOptional = super.lambdaQuery().eq(Employee::getId, id).eq(Employee::getTenantId, accessUser.currentTenantId())
            .eq(Employee::getIsDeleted, false).oneOpt();
        if (employeeOptional.isEmpty()) {
            log.warn("[员工] employee offRec not found: {} - tenant: {}", id, accessUser.currentTenantId());
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "人员不存在");
        }
        Employee employee = employeeOptional.get();
        if (!employee.getOnRec()) {
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "人员已关闭录制权限");
        }
        if (employee.getHoldTenant()) {
            log.warn("[员工] employee offRec: {} - tenant: {} is main account not off", id, accessUser.currentTenantId());
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "主账户不能关闭录制权限");
        }
        this.unbindReplayAccount(employee);
        log.info("[员工] employee offRec: {} - tenant: {}", id, accessUser.currentTenantId());
        return ApiResponse.success();
    }

    /**
     * 启用人员
     *
     * @param id         人员ID
     * @param accessUser 访问用户
     */
    @Override
    public ApiResponse<Void> enable(long id, AccessUser accessUser) {
        Optional<Employee> employeeOptional = super.lambdaQuery()
            .select(Employee::getId, Employee::getTenantId, Employee::getAccountStatus, Employee::getMobile)
            .eq(Employee::getId, id)
            .eq(Employee::getTenantId, accessUser.currentTenantId())
            .eq(Employee::getIsDeleted, false)
            .oneOpt();
        if (employeeOptional.isEmpty()) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "人员不存在");
        }
        Employee employee = employeeOptional.get();
        if (employee.getAccountStatus() == AccountStatus.NORMAL) {
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "人员已启用");
        }
        if (employee.getAccountStatus() == AccountStatus.FREEZE) {
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "人员已冻结,不可启用。如需恢复可通过同步子账户恢复");
        }
        if (this.mobileOccupy(employee.getMobile(), id)) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "手机号已占用,请联系员工处理");
        }
        boolean update = super.lambdaUpdate()
            .eq(Employee::getId, id)
            .eq(Employee::getTenantId, accessUser.currentTenantId())
            .eq(Employee::getIsDeleted, false)
            .eq(Employee::getHoldTenant, false)
            .eq(Employee::getAccountStatus, AccountStatus.DISABLED.getValue())
            .set(Employee::getAccountStatus, AccountStatus.NORMAL.getValue())
            .set(Employee::getUpdateDate, LocalDateTime.now())
            .set(Employee::getUpdateBy, accessUser.userId())
            .update();
        if (!update) {
            log.warn("[员工] employee enable not found: {} - tenant: {}", id, accessUser.currentTenantId());
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "人员不存在或状态已启用");
        }
        return ApiResponse.success();
    }

    /**
     * 禁用人员
     *
     * @param id         人员ID
     * @param accessUser 访问用户
     */
    @Override
    public ApiResponse<Void> disable(long id, AccessUser accessUser) {
        boolean update = this.lambdaUpdate()
            .eq(Employee::getId, id)
            .eq(Employee::getTenantId, accessUser.currentTenantId())
            .eq(Employee::getIsDeleted, false)
            .eq(Employee::getHoldTenant, false)
            .eq(Employee::getAccountStatus, AccountStatus.NORMAL.getValue())
            .set(Employee::getAccountStatus, AccountStatus.DISABLED.getValue())
            .set(Employee::getUpdateDate, LocalDateTime.now())
            .set(Employee::getUpdateBy, accessUser.userId())
            .update();
        if (!update) {
            log.warn("[员工] employee disable not found: {} - tenant: {}", id, accessUser.currentTenantId());
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "人员不存在或状态已停用");
        }
        return ApiResponse.success();
    }

    /**
     * 搜索员工下拉选项
     * <p>
     * 支持根据职位编码、关键词、手机号码、公司、部门、团队等条件进行筛选
     * 当提供职位编码列表时，会验证编码的有效性并转换为对应的职位 ID
     *
     * @param searchRequest 搜索请求参数，包含：
     *                      - keyword: 姓名或手机号码（纯数字时搜索手机号码）
     *                      - limit: 返回条数，默认 10 条
     *                      - mobile: 手机号码（忽略字段）
     *                      - positionIds: 所属岗位 ID 列表
     *                      - positionCodeList: 职位编码列表
     *                      - onRec: 开启录制权限
     *                      - jobType: 就职类型（1 全职，2 兼职）
     *                      - accountStatus: 账户状态（0 停用，1 正常）
     *                      - tenantId: 租户 id（忽略字段）
     *                      - currentUserId: 当前用户 id（忽略字段）
     *                      - companyIds: 所属公司 ID 列表
     *                      - deptIds: 所属部门 ID 列表
     *                      - teamIds: 所属小组 ID 列表
     * @param accessUser    访问用户
     *
     * @return 员工下拉选项列表，每个选项包含 key（员工 ID）和 label（员工姓名）
     *
     * @throws BusinessException 当职位编码无效时抛出业务异常
     */
    @Override
    public List<LabelOption> search(EmployeeOptionSearchRequest searchRequest, AccessUser accessUser) {
        // 处理职位编码列表：验证有效性并转换为职位 ID
        if (EmptyUtil.isNotEmpty(searchRequest.getPositionCodeList())) {
            // 构建职位编码与 DefaultPosition 的映射关系
            Map<String, DefaultPosition> positionMap = Arrays.stream(DefaultPosition.values()).collect(Collectors.toMap(DefaultPosition::getValue, Function.identity()));
            // 筛选出无效的职位编码
            String illegalCode = searchRequest.getPositionCodeList().stream().filter(code -> !positionMap.containsKey(code)).collect(Collectors.joining(","));
            if (EmptyUtil.isNotEmpty(illegalCode)) {
                throw new BusinessException(BizErrorCode.PARAM_INVALID, "无效的职位编码：" + illegalCode);
            }
            // 将职位编码转换为 DefaultPosition 对象
            List<DefaultPosition> list = searchRequest.getPositionCodeList().stream().map(positionMap::get).toList();
            // 根据 DefaultPosition 获取实际的职位 ID 映射
            Collection<Long> positionIds = positionService.getDefaultPositionMap(list, accessUser.currentTenantId()).values();
            // 如果未找到对应的职位 ID，返回空列表
            if (EmptyUtil.isEmpty(positionIds)) {
                log.warn("[员工] employee search DefaultPosition not found: {} - tenant: {}", searchRequest.getPositionCodeList(), searchRequest.getTenantId());
                return Collections.emptyList();
            }
            // 如果已存在职位 ID 列表，则过滤保留有效的职位 ID
            if (EmptyUtil.isNotEmpty(searchRequest.getPositionIds())) {
                List<Long> ids = searchRequest.getPositionIds().stream().filter(positionIds::contains).toList();
                if (EmptyUtil.isEmpty(ids)) {
                    log.warn("[员工] employee search Position code and id conflict: {} - tenant: {}", searchRequest.getPositionIds(), searchRequest.getTenantId());
                    return Collections.emptyList();
                }
                searchRequest.setPositionIds(ids);
            } else {
                // 否则使用转换后的职位 ID 列表
                searchRequest.setPositionIds(new ArrayList<>(positionIds));
            }
        }
        if (searchRequest.getLimit() == null || searchRequest.getLimit() <= 0) {
            searchRequest.setLimit(10);
        } else if (searchRequest.getLimit() > 100) {
            searchRequest.setLimit(100);
        }
        boolean isMain = EmptyUtil.isNotEmpty(accessUser.metadata()) && Objects.equals(accessUser.metadata().get(OauthConstant.TENANT_ADMIN), "true");
        if (isMain && EmptyUtil.isNotEmpty(searchRequest.getDataCompanyIds())) {
            searchRequest.setDataCompanyIds(Stream.concat(searchRequest.getDataCompanyIds().stream(), Stream.of(0L)).distinct().toList());
        }
        return super.lambdaQuery()
            .select(Employee::getId, Employee::getName)
            .like(EmptyUtil.isNotEmpty(searchRequest.getKeyword()) && EmptyUtil.isEmpty(searchRequest.getMobile()), Employee::getName, searchRequest.getKeyword())
            .eq(Employee::getTenantId, accessUser.currentTenantId())
            .eq(Employee::getIsDeleted, false)
            .like(EmptyUtil.isNotEmpty(searchRequest.getMobile()), Employee::getMobile, searchRequest.getMobile())
            .and(!Boolean.TRUE.equals(searchRequest.getSystemUser())
                && (EmptyUtil.isNotEmpty(searchRequest.getDataCompanyIds())
                || EmptyUtil.isNotEmpty(searchRequest.getDataDeptIds())
                || EmptyUtil.isNotEmpty(searchRequest.getDataTeamIds())), c -> {
                c.in(EmptyUtil.isNotEmpty(searchRequest.getDataCompanyIds()), Employee::getCompanyId, searchRequest.getDataCompanyIds())
                    .or().in(EmptyUtil.isNotEmpty(searchRequest.getDataDeptIds()), Employee::getDeptId, searchRequest.getDataDeptIds())
                    .or().in(EmptyUtil.isNotEmpty(searchRequest.getDataTeamIds()), Employee::getTeamId, searchRequest.getDataTeamIds());
            })
            .in(EmptyUtil.isNotEmpty(searchRequest.getCompanyIds()), Employee::getCompanyId, searchRequest.getCompanyIds())
            .in(EmptyUtil.isNotEmpty(searchRequest.getDeptIds()), Employee::getDeptId, searchRequest.getDeptIds())
            .in(EmptyUtil.isNotEmpty(searchRequest.getTeamIds()), Employee::getTeamId, searchRequest.getTeamIds())
            .in(EmptyUtil.isNotEmpty(searchRequest.getPositionIds()), Employee::getPositionId, searchRequest.getPositionIds())
            .eq(searchRequest.getAccountStatus() != null, Employee::getAccountStatus, searchRequest.getAccountStatus())
            .eq(searchRequest.getOnRec() != null, Employee::getOnRec, searchRequest.getOnRec())
            .eq(searchRequest.getJobType() != null, Employee::getJobType, searchRequest.getJobType())
            .last("limit " + searchRequest.getLimit())
            .list()
            .stream()
            .map(e -> new LabelOption(e.getId(), e.getName()))
            .collect(Collectors.toList());
    }


    /**
     * 获取租户的主账户ID
     *
     * @param tenantId 租户ID
     *
     * @return {@link Optional<Long>}
     */
    @Override
    public Optional<Long> getTenantMainAccountId(long tenantId) {
        return super.lambdaQuery()
            .select(Employee::getConnectorClientUserId)
            .eq(Employee::getTenantId, tenantId)
            .eq(Employee::getIsDeleted, false)
            .eq(Employee::getHoldTenant, true)
            .oneOpt()
            .map(Employee::getConnectorClientUserId);
    }


    /**
     * 主账户是否存在
     *
     * @param mainAccountIds 主账户ID列表
     *
     * @return {@link List<Long>}
     */
    @Override
    public List<Long> mainAccountExist(List<Long> mainAccountIds) {
        if (EmptyUtil.isEmpty(mainAccountIds)) {
            return List.of();
        }
        return super.lambdaQuery()
            .select(Employee::getConnectorClientUserId)
            .eq(Employee::getIsDeleted, false)
            .eq(Employee::getHoldTenant, true)
            .in(Employee::getConnectorClientUserId, mainAccountIds)
            .list()
            .stream()
            .map(Employee::getConnectorClientUserId)
            .collect(Collectors.toList());
    }


    /**
     * 获取岗位员工数量
     *
     * @param positionIds 岗位ID列表
     *
     * @return {@link Map<Long, Long>}
     */
    @Override
    public Map<Long, Long> countPositionEmployee(List<Long> positionIds) {
        if (EmptyUtil.isEmpty(positionIds)) {
            return Map.of();
        }
        List<CountData> countDataList = super.getBaseMapper().countPositionEmployee(positionIds);
        if (EmptyUtil.isEmpty(countDataList)) {
            return Map.of();
        }
        return countDataList.stream().collect(Collectors.toMap(CountData::getId, CountData::getCount));
    }


    /**
     * 登录场景下特殊根据手机号码查询逻辑
     * <p>
     * 查询策略：
     * 1. 优先查询状态正常且未删除的账号列表
     * 2. 如果没有正常状态的账号，则查询任意状态的未删除账号
     * 3. 对于开启录制权限的账号，需要匹配客户端当前租户ID
     * 4. 过滤出有可用租户权限的账号
     * 5. 如果找不到合适的账号，返回第一个账号并标记为冻结状态
     *
     * @param mobile 手机号码
     * @return {@link Optional}<{@link MobileLoginEmployee}> 匹配的员工账号，如果手机号为空或不存在则返回empty；
     *         如果所有租户都不可用，返回第一个员工并设置状态为冻结
     */
    @Override
    public Optional<MobileLoginEmployee> getMobileLoginAccount(String mobile) {
        if (EmptyUtil.isEmpty(mobile)) {
            return Optional.empty();
        }
        // 优先查询状态正常且未删除的员工列表
        List<Employee> employeeList = super.lambdaQuery()
            .eq(Employee::getMobile, mobile)
            .eq(Employee::getAccountStatus, AccountStatus.NORMAL.getValue())
            .eq(Employee::getIsDeleted, false).list();
        // 如果没有正常状态的账号，降级查询任意状态的未删除账号
        if (EmptyUtil.isEmpty(employeeList)) {
            return super.lambdaQuery()
                .eq(Employee::getMobile, mobile)
                .eq(Employee::getIsDeleted, false)
                .last("limit 1")
                .oneOpt().map(e -> MobileLoginEmployee.of(e, null));
        }

        // 获取客户端账户当前锚定的租户ID（针对开启录制权限的账号）
        List<Employee> ifupanAccountList = employeeList.stream().filter(Employee::getOnRec).toList();
        Map<Long, Long> employeeTenantMap = new HashMap<>();
        if (EmptyUtil.isNotEmpty(ifupanAccountList)) {
            ifupanAccountList.forEach(e -> employeeTenantMap.put(e.getId(), 0L));
            ApiResponse<ReplayUserDetailsInfo> apiResponse = userAccountService.getUserInfo(mobile);
            if (apiResponse.ok() && EmptyUtil.isNotEmpty(apiResponse.getData())) {
                ifupanAccountList.forEach(e -> employeeTenantMap.put(e.getId(), Objects.equals(apiResponse.getData().getTenantId(), e.getTenantId()) ? apiResponse.getData().getTenantId() : 0L));
            }
        }

        // 筛选出有可用租户权限的账号
        List<Long> tenantIds = employeeList.stream().map(Employee::getTenantId).toList();
        List<Long> availableTenantIds = tenantPrivilegeService.availableTenantIds(tenantIds, true);
        // 如果所有租户都不可用，返回第一个员工并标记为冻结状态
        if (EmptyUtil.isEmpty(availableTenantIds)) {
            Employee employee = employeeList.get(0);
            return Optional.of(MobileLoginEmployee.of(employee, AccountStatus.DISABLED));
        }
        // 优先返回同时满足以下条件的账号：
        // 1. 租户ID在可用权限列表中
        // 2. 如果有锚定租户ID，则必须匹配该租户
        // 如果找不到符合条件的账号，返回第一个员工并标记为冻结状态
        return employeeList.stream()
            .filter(e -> availableTenantIds.contains(e.getTenantId()))
            .filter(e -> !employeeTenantMap.containsKey(e.getId()) || Objects.equals(employeeTenantMap.get(e.getId()), e.getTenantId()))
            .max(Comparator.comparing(Employee::getOnRec)).map(e -> MobileLoginEmployee.of(e, AccountStatus.NORMAL)).or(() -> {
                Employee employee = employeeList.get(0);
                return Optional.of(MobileLoginEmployee.of(employee, employee.getAccountStatus()));
            });

    }


    /**
     * 获取员工手机号码
     *
     * @param mobile   手机号码
     * @param tenantId 租户ID
     *
     * @return {@link Optional<Employee>}
     */
    @Override
    public Optional<Employee> getMobileEmployee(String mobile, long tenantId) {
        return super.lambdaQuery()
            .eq(Employee::getMobile, mobile)
            .eq(Employee::getTenantId, tenantId)
            .eq(Employee::getIsDeleted, false)
            .last("limit 1")
            .oneOpt();
    }

    /**
     * 获取员工角色ID列表
     *
     * @param employeeId 员工ID
     * @param tenantId   租户ID
     *
     * @return {@link List<Long>}
     */
    @Override
    public List<Long> getEmployeeRoleIds(long employeeId, long tenantId) {
        return this.getRoleIds(employeeId);
    }

    /**
     * 获取员工名称映射
     *
     * @param ids 员工ID列表
     *
     * @return {@link Map<Long, String>}
     */
    @Override
    public Map<Long, String> getNameMap(List<Long> ids) {
        if (EmptyUtil.isEmpty(ids)) {
            return Map.of();
        }
        return super.lambdaQuery()
            .select(Employee::getId, Employee::getName)
            .in(Employee::getId, ids)
            .list()
            .stream()
            .collect(Collectors.toMap(Employee::getId, Employee::getName));
    }


    /**
     * 删除员工
     *
     * @param employeeId 员工ID
     * @param accessUser 访问用户
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ApiResponse<Void> delete(long employeeId, AccessUser accessUser) {
        Optional<Employee> employeeOptional = super.lambdaQuery().eq(Employee::getId, employeeId)
            .eq(Employee::getTenantId, accessUser.currentTenantId())
            .eq(Employee::getIsDeleted, false)
            .oneOpt();
        if (employeeOptional.isEmpty()) {
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "人员不存在或状态已停用");
        }
        Employee employee = employeeOptional.get();
        if (employee.getHoldTenant()) {
            log.warn("员工[{}]是租户[{}]的主账户，不能删除", employeeId, employee.getTenantId());
            return ApiResponse.failed(BizErrorCode.HAS_DEPENDENCY.getCode(), "不能删除主账户");
        }
        long countEmployee = this.countEmployee(accessUser.currentTenantId());
        if (countEmployee < 1) {
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "您至少需要有一个员工");
        }
        // 还回额度时就不用管够不够了，因为可能总额度调整了，当额度够扣除失败时是直接抛异常
        return propertyService.editTenantPropertyQuota(accessUser.currentTenantId(), countEmployee - 1, PropertyType.ENTERPRISE_PERSON_NUM, result -> {
            super.lambdaUpdate()
                .eq(Employee::getId, employeeId)
                .set(Employee::getIsDeleted, true)
                .update();
            if (employee.getOnRec()) {
                userAccountService.unbind(employee.getConnectorClientUserId(), employeeId);
            }
            connectorProcessor.clearTenantCache(employee.getTenantId(), List.of(ManagerType.EMPLOYEE));
            connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, List.of(employee.getCompanyId()), ManagerType.DEPT, List.of(employee.getDeptId()), ManagerType.TEAM, List.of(employee.getTeamId())));
            return ApiResponse.success();
        });
    }




    /**
     * 修改密码
     *
     * @param employeeId 员工ID
     * @param password   密码
     * @param tenantId   租户ID
     * @param sync       是否同步
     */
    @ResourceLock(prefix = "oauth:employee-update-password", key = "#employeeId", message = "修改密码中，请稍后...")
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ApiResponse<Void> updatePassword(long employeeId, String password, long tenantId, boolean sync) {
        if (EmptyUtil.isEmpty(password)) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "密码不能为空");
        }
        Optional<Employee> employeeOptional = super.lambdaQuery().eq(Employee::getId, employeeId)
            .eq(Employee::getTenantId, tenantId)
            .eq(Employee::getIsDeleted, false)
            .select(Employee::getId, Employee::getPassword, Employee::getOnRec, Employee::getConnectorClientUserId)
            .oneOpt();
        if (employeeOptional.isEmpty()) {
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "人员不存在或状态已停用");
        }
        Employee employee = employeeOptional.get();
        boolean update = super.lambdaUpdate()
            .eq(Employee::getId, employeeId)
            .eq(Employee::getTenantId, tenantId)
            .eq(Employee::getIsDeleted, false)
            .set(Employee::getPassword, PasswordHandler.encode(password))
            .update();
        if (update) {
            log.info("[员工] update employee password success. employeeId:{}", employeeId);
            if (sync && employee.getOnRec() && employee.getConnectorClientUserId() > 0) {
                ApiResponse<Void> apiResponse = userAccountService.updatePassword(employee.getConnectorClientUserId(), password, tenantId);
                if (apiResponse.failed()) {
                    log.error("[员工] update employee sync password failed. employeeId:{}, message:{}", employeeId, apiResponse.getMsg());
                    throw new BusinessException(BizErrorCode.DATA_ACCESS_FAILED, apiResponse.getMsg());
                }
                log.info("[员工] update employee sync password success. employeeId:{}", employeeId);
            }
            return ApiResponse.success();
        }
        return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "修改失败");
    }

    /**
     * 获取员工信息
     *
     * @param employeeId 员工ID
     * @param tenantId   租户ID
     *
     * @return {@link Optional<Employee>}
     */
    @Override
    public Optional<Employee> getEmployeeInfo(long employeeId, long tenantId) {
        return super.lambdaQuery()
            .eq(Employee::getId, employeeId)
            .eq(Employee::getTenantId, tenantId)
            .eq(Employee::getIsDeleted, false)
            .oneOpt();
    }

    /**
     * 获取员工手机号码
     *
     * @param employeeId 员工ID
     * @param tenantId   租户ID
     *
     * @return {@link Optional<String>}
     */
    @Override
    public Optional<String> getEmployeeMobile(long employeeId, long tenantId) {
        return super.lambdaQuery()
            .select(Employee::getMobile)
            .eq(Employee::getId, employeeId)
            .eq(Employee::getTenantId, tenantId)
            .eq(Employee::getIsDeleted, false)
            .oneOpt()
            .map(Employee::getMobile);
    }


    /**
     * 修改手机号码
     *
     * @param employeeId 员工ID
     * @param newMobile  新手机号码
     * @param tenantId   租户ID
     *
     * @return {@link ApiResponse<Void>}
     */
    @Override
    public ApiResponse<Void> updateMobile(long employeeId, String newMobile, long tenantId) {
        boolean mobileExists = this.mobileOccupy(newMobile, employeeId);
        if (mobileExists) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "手机号已存在, 请先解绑账户");
        }
        if (this.tenantMobileOccupy(tenantId, newMobile, employeeId)) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "您当前公司已存在该账户");
        }
        Optional<Employee> employeeOptional = this.getEmployeeInfo(employeeId, tenantId);
        if (employeeOptional.isEmpty()) {
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "人员不存在或状态已停用");
        }
        Employee employee = employeeOptional.get();
        if (employee.getOnRec() && employee.getConnectorClientUserId() != null && employee.getConnectorClientUserId() > 0) {
            ApiResponse<Void> apiResponse = userAccountService.updateMobile(employee.getConnectorClientUserId(), newMobile, tenantId);
            if (apiResponse.failed()) {
                log.warn("[员工] update employee mobile failed. employeeId:{}, newMobile:{}, errorCode:{}, errorMsg: {}", employeeId, newMobile, apiResponse.getCode(), apiResponse.getMsg());
                // 如果更改失败，且不是没有权限就直接返回
                if (!apiResponse.getCode().equals(BizErrorCode.NO_POWER.getCode())) {
                    return apiResponse;
                }
            }
        }
        super.lambdaUpdate()
            .eq(Employee::getId, employeeId)
            .eq(Employee::getTenantId, tenantId)
            .eq(Employee::getIsDeleted, false)
            .set(Employee::getMobile, newMobile)
            .update();
        log.info("[员工] update employee mobile success. employeeId:{}, newMobile:{}", employeeId, newMobile);
        return ApiResponse.success();
    }


    /**
     * 获取租户的管理员用户
     *
     * @param idx       索引
     * @param limit     限制
     * @param tenantIds 租户ID列表
     *
     * @return {@link List<Employee>}
     */
    @Override
    public List<Employee> loadTenantHoldEmployee(Long idx, int limit, Collection<Long> tenantIds) {
        return super.lambdaQuery()
            .gt(Employee::getId, idx)
            .in(EmptyUtil.isNotEmpty(tenantIds), Employee::getTenantId, tenantIds)
            .eq(Employee::getHoldTenant, true)
            .eq(Employee::getIsDeleted, false)
            .last("limit " + limit)
            .list();
    }

    /**
     * 获取租户的所有员工
     *
     * @param idx       索引
     * @param limit     限制
     * @param tenantIds 租户ID列表
     *
     * @return {@link List<Employee>}
     */
    @Override
    public List<Employee> loadTenantAllEmployee(Long idx, int limit, List<Long> tenantIds) {
        if (EmptyUtil.isEmpty(tenantIds)) {
            return List.of();
        }
        return super.lambdaQuery()
            .gt(Employee::getId, idx)
            .eq(Employee::getIsDeleted, false)
            .in(Employee::getTenantId, tenantIds)
            .last("limit " + limit)
            .list();
    }




    /**
     * 查询客户端用户对应的员工
     *
     * @param clientUserId 账号ID
     * @param tenantId     租户ID
     * @param select       查询字段
     * @return {@link Optional<Employee>}
     */
    @Override
    public Optional<Employee> getClientEmployee(long clientUserId, long tenantId, List<SFunction<Employee, ?>> select) {
        return super.lambdaQuery()
            .select(EmptyUtil.isNotEmpty(select), select)
            .eq(Employee::getConnectorClientUserId, clientUserId)
            .eq(Employee::getIsDeleted, false)
            .eq(Employee::getTenantId, tenantId)
            .last("limit 1")
            .oneOpt();
    }

    /**
     * 修改个人资料
     *
     * @param request    员工信息
     * @param employeeId 员工ID
     * @param tenantId   租户ID
     *
     * @return {@link ApiResponse<Void>}
     */
    @Override
    public ApiResponse<Void> updateProfile(EmployeeProfileUpdateRequest request, long employeeId, long tenantId) {
        boolean update = super.lambdaUpdate()
            .eq(Employee::getId, employeeId)
            .eq(Employee::getTenantId, tenantId)
            .eq(Employee::getIsDeleted, false)
            .set(Employee::getUpdateDate, LocalDateTime.now())
            .set(Employee::getUpdateBy, employeeId)
            .set(request.getName() != null, Employee::getName, request.getName())
            .set(request.getUserAvatar() != null, Employee::getUserAvatar, request.getUserAvatar())
            .set(request.getEmail() != null, Employee::getEmail, request.getEmail())
            .update();
        if (update) {
            log.info("[员工] update employee profile success. employeeId:{}, request:{}", employeeId, request);
            return ApiResponse.success();
        }
        return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "修改失败");
    }


    /**
     * 解绑通知
     *
     * @param clientAccountId 客户账号ID
     *
     * @return {@link ApiResponse<EmployeeUnbindResponse>}
     */
    @Override
    public ApiResponse<EmployeeUnbindResponse> unbind(long clientAccountId) {
        log.info("[员工] unbind employee. clientAccountId:{}", clientAccountId);
        List<Employee> employeeList = super.lambdaQuery()
            .eq(Employee::getConnectorClientUserId, clientAccountId)
            .eq(Employee::getIsDeleted, false)
            .eq(Employee::getAccountStatus, AccountStatus.NORMAL.getValue())
            .list();
        if (EmptyUtil.isEmpty(employeeList)) {
            return ApiResponse.success(EmployeeUnbindResponse.of(List.of(), null));
        }
        // 如果是主账户被禁用
        List<Long> tenantIds = employeeList.stream().filter(Employee::getHoldTenant).map(Employee::getTenantId).toList();
        if (EmptyUtil.isNotEmpty(tenantIds)) {
            tenantPrivilegeService.expired(tenantIds);
            super.lambdaUpdate()
                .in(Employee::getTenantId, tenantIds)
                .eq(Employee::getIsDeleted, false)
                .eq(Employee::getHoldTenant, false)
                .eq(Employee::getAccountStatus, AccountStatus.NORMAL.getValue())
                .set(Employee::getAccountStatus, AccountStatus.DISABLED.getValue())
                .update();
        }
        List<Long> ids = employeeList.stream().map(Employee::getId).collect(Collectors.toList());
        super.lambdaUpdate()
            .in(Employee::getId, ids)
            .eq(Employee::getHoldTenant, false)
            .set(Employee::getConnectorClientUserId, 0)
            .set(Employee::getOnRec, false)
            .set(Employee::getAccountStatus, AccountStatus.DISABLED.getValue())
            .set(Employee::getUpdateDate, LocalDateTime.now())
            .update();
        log.info("[员工] unbind success. employeeIds:{}", ids);
        return ApiResponse.success(EmployeeUnbindResponse.of(ids, tenantIds));
    }


    /**
     * 绑定回调添加人员
     *
     * @param request 绑定请求
     *
     * @return {@link ApiResponse<Void>}
     */
    @ResourceLock(prefix = "governance:employee:add-for-replay-account", key = "#account.userId", message = "正在添加人员,请勿频繁点击")
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ApiResponse<Void> bindCallback(BindSubAccountRequest request) {
        return this.addForReplayAccount(BeanUtil.copyProperties(request, ReplayUserDetailsInfo.class));
    }
}
