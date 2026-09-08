package com.jiuyu.governance.business.rbac.service.impl;

import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.RandomUtil;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.lock.LockTemplate;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.pojo.bo.MobileLoginEmployee;
import com.jiuyu.governance.business.rbac.pojo.constants.AccountStatus;
import com.jiuyu.governance.business.rbac.pojo.entity.Employee;
import com.jiuyu.governance.business.rbac.pojo.request.EmployeeOauthPasswordLoginRequest;
import com.jiuyu.governance.business.rbac.pojo.response.EmployeeOauthResponse;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.business.rbac.service.RoleService;
import com.jiuyu.governance.business.rbac.service.TenantPrivilegeService;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.init.InitializationTenant;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.utils.PasswordHandler;
import com.jiuyu.governance.openfeign.replay.SmsService;
import com.jiuyu.governance.openfeign.replay.UserAccountService;
import com.jiuyu.governance.openfeign.replay.response.ReplayAccountResponse;
import com.jiuyu.governance.openfeign.replay.response.ReplayUserDetailsInfo;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import com.jiuyu.governance.plugins.oauth.server.AuthenticationTokenStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 员工登录授权管理
 *
 * @author HeHui
 * @date 2026-03-27 15:53
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmployeeOauthManage {

    private final EmployeeService employeeService;

    private final AuthenticationTokenStorage authenticationTokenStorage;

    private final TenantPrivilegeService tenantPrivilegeService;

    private final RoleService roleService;

    private final SmsService smsService;

    private final UserAccountService userAccountService;

    private final TenantInitializationManage tenantInitializationManage;

    private final LockTemplate lockTemplate;


    /**
     * 手机号码 + 密码登录
     *
     * @param request   登录请求
     * @param ip        客户端IP
     * @param userAgent 浏览器UA
     *
     * @return 登录响应
     */
    public ApiResponse<EmployeeOauthResponse> passwordLogin(EmployeeOauthPasswordLoginRequest request, String ip, String userAgent) {
        Optional<MobileLoginEmployee> optional = employeeService.getMobileLoginAccount(request.getMobile());
        if (optional.isEmpty()) {
            return this.doInitLogin(request.getMobile(), account -> {
                return PasswordHandler.matches(request.getPassword(), account.getPassword());
            }, ip, userAgent);
        }
        Employee employee = optional.get().getEmployee();
        if (!PasswordHandler.matches(request.getPassword(), employee.getPassword())) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "账户或密码错误");
        }
        if (employee.getAccountStatus() != AccountStatus.NORMAL) {
            return ApiResponse.failed(SystemErrorCode.FORBIDDEN.getCode(), "您的账户已被停用,请联系公司管理员处理");
        }
        if (optional.get().getTenantStatus() == null) {
            if (!tenantPrivilegeService.available(employee.getTenantId())) {
                return ApiResponse.failed(BizErrorCode.PACKAGE_INVALID.getCode(), "您的套餐版本已失效,请联系您的专属销售处理");
            }
        } else if (optional.get().getTenantStatus() != AccountStatus.NORMAL) {
            return ApiResponse.failed(SystemErrorCode.FORBIDDEN.getCode(), "您的套餐版本已失效,请联系您的专属销售处理");
        }

        return ApiResponse.success(this.doLogin(employee, ip, userAgent));
    }


    /**
     * 主账户第一次登录
     *
     * @param mobile 手机号码
     *
     * @return 登录响应
     */
    private ApiResponse<EmployeeOauthResponse> doInitLogin(String mobile, Predicate<ReplayUserDetailsInfo> predicate, String ip, String userAgent) {
        log.info("[手机登录] unmatched account, try replay main account. get main account detail mobile: {}", mobile);
        ApiResponse<ReplayUserDetailsInfo> replayAccountResponse = userAccountService.getUserInfo(mobile);
        if (replayAccountResponse == null || replayAccountResponse.getData() == null) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "账户或密码错误");
        }
        // 只允许客户端账户第一次登录 初始化
        ReplayUserDetailsInfo account = replayAccountResponse.getData();
        if (!predicate.test(account)) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "账户或密码错误");
        }
        if (!Objects.equals(account.getUserType(), 0) && !Objects.equals(account.getUserType(), 2)) {
            log.warn("[手机登录] unmatched account, try replay main account. get main account detail mobile: {}, is not main account", mobile);
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "请先联系主账户登录");
        }
        if (account.getPackageLevel() == null) {
            log.warn("[手机登录] unmatched account, try replay main account. get main account detail mobile: {}, packageLevel is null", mobile);
            return ApiResponse.failed(BizErrorCode.UNAUTHORIZED.getCode(), "当前账户[" + account.getNickName() + "]未开通套餐");
        }
        if (account.getPackageLevel() < InitializationTenant.MIN_GOVERNANCE_LEVEL) {
            log.warn("[手机登录] unmatched account, try replay main account. get main account detail mobile: {}, packageLevel {} < {}, tenant: {}, accountId: {}, {} ", account.getPackageName(), account.getPackageLevel(), InitializationTenant.MIN_GOVERNANCE_LEVEL, account.getTenantId(), account.getUserId(), account.getNickName());
            return ApiResponse.failed(BizErrorCode.PACKAGE_INVALID.getCode(), "当前账户[" + account.getNickName() + "]套餐版本低于企业版,请联系您的专属销售处理");
        }
        this.doLoginInitMainAccount(account);
        Optional<Employee> optional = employeeService.getMobileEmployee(mobile, account.getTenantId());
        if (optional.isEmpty()) {
            log.warn("[手机登录] unmatched account, try replay main account. get main account detail mobile: {}, create new account after query empty", mobile);
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "账户错误");
        }
        return ApiResponse.success(this.doLogin(optional.get(), ip, userAgent));
    }


    /**
     * 账户第一次登录初始化
     *
     * @param account 主账户信息
     */
    private void doLoginInitMainAccount(ReplayUserDetailsInfo account) {
        // 判断是否初始化了租户
        if (!tenantPrivilegeService.hasTenant(account.getTenantId())) {
            AccessUser accessUser = null;
            // 当前是不是主账户
            if (account.getUserType() == 0) {
                Map<String, String> metadata = Map.of("password", account.getPassword() == null ? "" : account.getPassword());
                accessUser = new AccessUser("", account.getUserId(), account.getUsername(), account.getTenantId(), OauthConstant.CLIENT_USER, "", metadata);
            } else {
                // 子账户没有父账户？？
                if (account.getParentId() == null) {
                    log.warn("[手机登录] unmatched account, try replay main account. get main account detail mobile: {}, parentId is null", account.getPhone());
                    throw new BusinessException(BizErrorCode.FORMAT_ERROR, "请先联系主账户登录");
                }
                ApiResponse<ReplayAccountResponse> parentAccountResult = userAccountService.getAccount(account.getParentId());
                if (parentAccountResult.failed()) {
                    log.warn("[手机登录] unmatched account, try replay main account. get main account detail mobile: {}, parentId: {}, get parent account detail failed", account.getPhone(), account.getParentId());
                    throw new BusinessException(BizErrorCode.FORMAT_ERROR, "请先联系主账户登录");
                }
                ReplayAccountResponse parentAccount = parentAccountResult.getData();
                Map<String, String> metadata = Map.of("password", parentAccount.getPassword() == null ? "" : parentAccount.getPassword());
                accessUser = new AccessUser("", parentAccount.getId(), parentAccount.getUsername(), parentAccount.getActiveTenantId(), OauthConstant.CLIENT_USER, "", metadata);
            }
            tenantInitializationManage.runInit(accessUser, accessUser.userId());
        }

        // 已经是主账户 逻辑结束
        if (account.getUserType() == 0) {
            return;
        }
        employeeService.addForReplayAccount(account);

    }


    /**
     * 发送手机验证码
     *
     * @param mobile 手机号码
     *
     * @return 发送结果
     */
    public ApiResponse<Void> sendCode(String mobile) {
        if (EmptyUtil.isEmpty(mobile)) {
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "请输入手机号码");
        }
        if (!PhoneUtil.isMobile(mobile)) {
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "请输入正确的手机号码");
        }
        ApiResponse<Employee> checkResult = smsLoginCheck(mobile);
        if (checkResult.failed()) {
            if (Objects.equals(checkResult.getCode(), BizErrorCode.UNAUTHORIZED.getCode())) {
                // 降级
                ApiResponse<ReplayUserDetailsInfo> apiResponse = userAccountService.getUserInfo(mobile);
                if (apiResponse.getData() == null) {
                    return ApiResponse.failed(checkResult.getCode(), checkResult.getMsg());
                }
            } else {
                return ApiResponse.failed(checkResult.getCode(), checkResult.getMsg());
            }
        }
        // 发送短信验证码
        return smsService.sendCode("login", mobile, () -> {
            return RandomUtil.randomNumbers(6);
        });
    }

    /**
     * 手机号码登录检查
     *
     * @param mobile 手机号码
     *
     * @return 检查结果
     */
    private ApiResponse<Employee> smsLoginCheck(String mobile) {
        Optional<MobileLoginEmployee> optional = employeeService.getMobileLoginAccount(mobile);
        if (optional.isEmpty()) {
            return ApiResponse.failed(BizErrorCode.UNAUTHORIZED.getCode(), "您还未开通账户,请联系销售帮你开通");
        }
        Employee employee = optional.get().getEmployee();
        if (employee.getAccountStatus() != AccountStatus.NORMAL) {
            return ApiResponse.failed(SystemErrorCode.FORBIDDEN.getCode(), "您的账户已被停用,请联系公司管理员处理");
        }
        if (optional.get().getTenantStatus() == null) {
            if (!tenantPrivilegeService.available(employee.getTenantId())) {
                return ApiResponse.failed(BizErrorCode.PACKAGE_INVALID.getCode(), "您的套餐版本已失效,请联系您的专属销售处理");
            }
        } else if (optional.get().getTenantStatus() != AccountStatus.NORMAL) {
            return ApiResponse.failed(SystemErrorCode.FORBIDDEN.getCode(), "您的套餐版本已失效,请联系您的专属销售处理");
        }
        return ApiResponse.success(employee);
    }


    /**
     * 手机号码 + 验证码登录
     *
     * @param mobile    手机号码
     * @param code      验证码
     * @param ip        客户端IP
     * @param userAgent 浏览器UA
     *
     * @return 登录响应
     */
    public ApiResponse<EmployeeOauthResponse> codeLogin(String mobile, String code, String ip, String userAgent) {
        if (EmptyUtil.isEmpty(mobile)) {
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "请输入手机号码");
        }
        return smsService.checkCode("login", mobile, code, () -> {
            ApiResponse<Employee> checkResult = this.smsLoginCheck(mobile);
            if (checkResult.failed()) {
                if (Objects.equals(checkResult.getCode(), BizErrorCode.UNAUTHORIZED.getCode())) {
                    return this.doInitLogin(mobile, account -> true, ip, userAgent);
                }
                return ApiResponse.failed(checkResult.getCode(), checkResult.getMsg());
            }
            return ApiResponse.success(this.doLogin(checkResult.getData(), ip, userAgent));
        }, errorNum -> {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "验证码错误");
        });
    }


    /**
     * 客户端授权
     *
     * @param clientUserId 客户端用户ID
     * @param tenantId     租户ID
     * @param ip          客户端IP
     * @param userAgent   浏览器UA
     * @return 授权结果
     */
    public ApiResponse<EmployeeOauthResponse> clientAuth(long clientUserId, long tenantId, String ip, String userAgent) {
        Optional<Employee> clientEmployee = employeeService.getClientEmployee(clientUserId, tenantId, null);
        if (clientEmployee.isEmpty()) {
            ApiResponse<ReplayAccountResponse> apiResponse = userAccountService.getAccount(clientUserId);
            if (apiResponse.failed()) {
                return ApiResponse.failed(BizErrorCode.UNAUTHORIZED.getCode(), "授权失败,请稍后再试.");
            }
            if (apiResponse.getData() == null) {
                return ApiResponse.failed(BizErrorCode.UNAUTHORIZED.getCode(), "您还未开通账户,请联系销售帮你开通");
            }
            return this.doInitLogin(apiResponse.getData().getPhone(), account -> true, ip, userAgent);
        }
        return ApiResponse.success(this.doLogin(clientEmployee.get(), ip, userAgent));
    }


    /**
     * 登出
     *
     * @param accessUser 访问用户
     *
     * @return 登出结果
     */
    public ApiResponse<Void> logout(AccessUser accessUser) {
        authenticationTokenStorage.logout(accessUser.accessToken());
        return ApiResponse.success();
    }


    /**
     * 强制登出
     *
     * @param employeeId 员工ID
     */
    public void forcedLogout(long employeeId) {
        authenticationTokenStorage.logout(OauthConstant.GOVERNANCE_USER, employeeId);
    }


    /**
     * 强制登出 - 租户版
     *
     * @param tenantIds 租户ID
     */
    public void forcedForTenant(List<Long> tenantIds) {
        if (EmptyUtil.isEmpty(tenantIds)) {
            return;
        }
        log.info("[授权] forced tenant all account, tenant: {}", tenantIds);
        BatchQuery<Long, Employee> batchQuery = new BatchQuery<>((limit, idx) -> {
            log.info("[授权] load tenant all employee... idx: {}, limit: {}", idx, limit);
            return employeeService.loadTenantAllEmployee(idx, limit, tenantIds);
        }, Employee::getId);

        batchQuery.consumer(employees -> {
            log.info("[授权] forced tenant all account... employee size: {}", employees.size());
            List<Long> employeeIds = employees.stream().map(Employee::getId).toList();
            authenticationTokenStorage.logout(OauthConstant.GOVERNANCE_USER, employeeIds);
            log.info("[授权] forced tenant all account... employeeIds: {}", employeeIds);
        });
        batchQuery.run();
        log.info("[授权] forced tenant all account... done");
    }


    /**
     * 登录
     *
     * @param employee  员工
     * @param ip        客户端IP
     * @param userAgent 浏览器UA
     *
     * @return 登录响应
     */
    private EmployeeOauthResponse doLogin(Employee employee, String ip, String userAgent) {
        boolean isTenantAdmin = Objects.equals(employee.getHoldTenant(), true);
        // 如果 holdTenant 不是 true，检查员工是否拥有默认管理员角色
        if (!isTenantAdmin) {
            List<Long> roleIds = employeeService.getEmployeeRoleIds(employee.getId(), employee.getTenantId());
            if (EmptyUtil.isNotEmpty(roleIds)) {
                isTenantAdmin = roleService.hasDefaultRole(roleIds, employee.getTenantId());
            }
        }
        String accessToken = authenticationTokenStorage.login(OauthConstant.GOVERNANCE_USER, employee.getId(), employee.getName(), ip, employee.getTenantId(), userAgent, Map.of(OauthConstant.TENANT_ADMIN, String.valueOf(isTenantAdmin)));
        EmployeeOauthResponse response = new EmployeeOauthResponse();
        response.setId(employee.getId());
        response.setName(employee.getName());
        response.setUserAvatar(employee.getUserAvatar());
        response.setAccessToken(accessToken);
        response.setTenantId(employee.getTenantId());
        return response;
    }


}
