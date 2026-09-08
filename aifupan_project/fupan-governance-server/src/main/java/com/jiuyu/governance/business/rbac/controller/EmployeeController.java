package com.jiuyu.governance.business.rbac.controller;

import com.jiuyu.framework.lock.ResourceLock;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.Permissions;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.rbac.pojo.request.*;
import com.jiuyu.governance.business.rbac.pojo.response.EmployeeInfoResponse;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.business.rbac.service.impl.EmployeeOauthManage;
import com.jiuyu.governance.common.pojo.bo.IdRequest;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import com.jiuyu.governance.plugins.oauth.data.supports.DataPermissionsHandler;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 企业端-人员管理
 *
 * @author HeHui
 * @date 2026-03-18 17:23
 */
@RestController
@RequestMapping("/api/governance/employee")
@GovernanceUser
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    private final EmployeeOauthManage oauthManage;

    private final DataPermissionsHandler permissionsHandler;

    /**
     * 新增人员
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @ResourceLock(prefix = "governance:employee", key = "#request.mobile", message = "请勿频繁点击")
    @Permissions("sys:employee:manage:add")
    @PostMapping("/add")
    public ApiResponse<Void> addEmployee(@RequestBody @Validated EmployeeAddRequest request, AccessUser accessUser) {
        return employeeService.addEmployee(request, accessUser);
    }

    /**
     * 修改人员
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @PostMapping("/update")
    @ResourceLock(prefix = "governance:employee", key = "#request.id", message = "请勿频繁点击")
    @Permissions({"sys:employee:manage:update", "sys:employee:manage:add"})
    public ApiResponse<Void> updateEmployee(@RequestBody @Validated EmployeeUpdateRequest request, AccessUser accessUser) {
        return employeeService.updateEmployee(request, accessUser);
    }


    /**
     * 修改密码
     *
     * @param request 请求
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Permissions({"sys:employee:manage:update", "sys:employee:manage:add"})
    @PostMapping("/password")
    public ApiResponse<Void> updatePassword(@RequestBody @Validated EditEmployeePasswordRequest request, AccessUser accessUser) {
        Long employeeId = request.getEmployeeId();
        // 如果不是改直接密码
        if (!Objects.equals(employeeId, accessUser.userId()) && !OauthConstant.isTenantAdmin(accessUser)) {
            return permissionsHandler.run(accessUser, OauthConstant.EMPLOYEE, request.getEmployeeId(), () -> {
                return employeeService.updatePassword(employeeId, request.getNewPassword(), accessUser.currentTenantId(), true);
            });
        }
        return employeeService.updatePassword(employeeId, request.getNewPassword(), accessUser.currentTenantId(), true);
    }

    /**
     * 人员详情
     *
     * @param employId   员工ID
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link EmployeeInfoResponse }>
     */
    @GetMapping("/detail")
    public ApiResponse<EmployeeInfoResponse> detail(@RequestParam Long employId, AccessUser accessUser) {
        return ApiResponse.success(employeeService.detail(employId, accessUser.currentTenantId()));
    }

    /**
     * 分页查询人员
     *   排除主账户
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link PageData }<{@link EmployeeInfoResponse }>>
     */
    @PostMapping("/page")
    public ApiResponse<PageData<EmployeeInfoResponse>> pageQueryEmployee(@RequestBody @Validated EmployeeQueryRequest request, AccessUser accessUser) {
        return ApiResponse.success(employeeService.pageQueryEmployee(request, accessUser));
    }

    /**
     *   无功能权限的特殊分页
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link PageData }<{@link EmployeeInfoResponse }>>
     */
    @PostMapping("/special-page")
    public ApiResponse<PageData<EmployeeInfoResponse>> pageQuerySpecialEmployee(@RequestBody @Validated EmployeeQueryRequest request, AccessUser accessUser) {
        return ApiResponse.success(employeeService.pageQueryEmployee(request, accessUser));
    }

    /**
     * 开启录制权限
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Permissions({"sys:employee:manage:update", "sys:employee:manage:add"})
    @ResourceLock(prefix = "governance:employee", key = "#request.id", message = "请勿频繁点击")
    @PostMapping("/on-rec")
    public ApiResponse<Void> onRec(@RequestBody @Validated EmployOnRecRequest request, AccessUser accessUser) {
        return employeeService.onRec(request.getId(), accessUser);
    }

    /**
     * 关闭录制权限
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Permissions({"sys:employee:manage:update", "sys:employee:manage:add"})
    @ResourceLock(prefix = "governance:employee", key = "#request.id", message = "请勿频繁点击")
    @PostMapping("/off-rec")
    public ApiResponse<Void> offRec(@RequestBody @Validated EmployOnRecRequest request, AccessUser accessUser) {
        return employeeService.offRec(request.getId(), accessUser);
    }

    /**
     * 同步子账户
     *
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Permissions({"sys:employee:manage:update", "sys:employee:manage:add"})
    @PostMapping("/sync-sub-account")
    public ApiResponse<Void> syncSubAccount(AccessUser accessUser) {
        return employeeService.syncSubAccount(accessUser);
    }

    /**
     * 启用人员
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Permissions({"sys:employee:manage:update", "sys:employee:manage:add"})
    @ResourceLock(prefix = "governance:employee", key = "#request.id", message = "请勿频繁点击")
    @PostMapping("/enable")
    public ApiResponse<Void> enable(@RequestBody @Validated IdRequest request, AccessUser accessUser) {
        return employeeService.enable(request.getId(), accessUser);
    }

    /**
     * 禁用人员
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Permissions({"sys:employee:manage:update", "sys:employee:manage:add"})
    @ResourceLock(prefix = "governance:employee", key = "#request.id", message = "请勿频繁点击")
    @PostMapping("/disable")
    public ApiResponse<Void> disable(@RequestBody @Validated IdRequest request, AccessUser accessUser) {
        ApiResponse<Void> apiResponse = employeeService.disable(request.getId(), accessUser);
        if (apiResponse.ok()) {
            // 强制下线
            oauthManage.forcedLogout(request.getId());
        }
        return apiResponse;
    }

    /**
     * 搜索下拉
     *
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link List }<{@link LabelOption }>>
     */
    @GetMapping("/option")
    public ApiResponse<List<LabelOption>> search(EmployeeOptionSearchRequest request, AccessUser accessUser) {
        return ApiResponse.success(employeeService.search(request, accessUser));
    }

    /**
     * 删除人员
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Permissions("sys:employee:manage:delete")
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestBody @Validated IdRequest request, AccessUser accessUser) {
        return employeeService.delete(request.getId(), accessUser);
    }
}
