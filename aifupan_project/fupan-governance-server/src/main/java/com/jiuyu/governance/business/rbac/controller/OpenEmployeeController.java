package com.jiuyu.governance.business.rbac.controller;

import com.jiuyu.framework.oauth.client.annotation.APIKey;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.pojo.entity.Employee;
import com.jiuyu.governance.business.rbac.pojo.request.BindSubAccountRequest;
import com.jiuyu.governance.business.rbac.pojo.request.EmployeeAddRequest;
import com.jiuyu.governance.business.rbac.pojo.request.ReviewUpdatePasswordRequest;
import com.jiuyu.governance.business.rbac.pojo.response.EmployeeUnbindResponse;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.business.rbac.service.impl.EmployeeOauthManage;
import com.jiuyu.governance.common.pojo.bo.IdRequest;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台端 - 人员开放接口
 *
 * @author HeHui
 * @date 2026-04-01 18:41
 */
@RestController
@RequestMapping("/api/governance/employee-open")
@RequiredArgsConstructor
@APIKey
public class OpenEmployeeController {

    private final EmployeeService employeeService;

    private final EmployeeOauthManage employeeOauthManage;



    /**
     * 解绑
     *
     * @param request 请求参数
     * @return {@link ApiResponse }<{@link Void }>
     */
    @PostMapping("/unbind")
    public ApiResponse<Void> unbind(@RequestBody @Validated IdRequest request) {
        ApiResponse<EmployeeUnbindResponse> apiResponse = employeeService.unbind(request.getId());
        if (apiResponse.ok() && EmptyUtil.isNotEmpty(apiResponse.getData())) {
            if (EmptyUtil.isNotEmpty(apiResponse.getData().getEmployeeIds())) {
                apiResponse.getData().getEmployeeIds().forEach(employeeOauthManage::forcedLogout);
            }
            if (EmptyUtil.isNotEmpty(apiResponse.getData().getTenantIds())) {
                employeeOauthManage.forcedForTenant(apiResponse.getData().getTenantIds());
            }
            return ApiResponse.success();
        }
        return ApiResponse.failed(apiResponse.getCode(), apiResponse.getMsg());
    }


    /**
     * 绑定
     *
     * @param request 绑定参数
     * @return {@link ApiResponse }<{@link Void }>
     */
    @PostMapping("/bind")
    public ApiResponse<Void> bind(@RequestBody @Validated BindSubAccountRequest request) {
       return ApiResponse.success();
        //return employeeService.bindCallback(request);
    }

    /**
     * 修改密码
     *
     * @param request 修改密码参数
     * @return {@link ApiResponse }<{@link Void }>
     */
    @PostMapping("/update-password")
    public ApiResponse<Void> updatePassword(@RequestBody @Validated ReviewUpdatePasswordRequest request) {
        employeeService.getClientEmployee(request.getUserId(), request.getTenantId(), List.of(Employee::getId))
            .ifPresent(employee -> employeeService.updatePassword(employee.getId(), request.getPassword(), request.getTenantId(), true));
        return ApiResponse.success();
    }
}
