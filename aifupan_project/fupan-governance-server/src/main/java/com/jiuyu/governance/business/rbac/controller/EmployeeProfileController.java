package com.jiuyu.governance.business.rbac.controller;

import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.RandomUtil;
import com.jiuyu.framework.lock.ResourceLock;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.Permissions;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.business.rbac.pojo.request.EmployeeProfileUpdateRequest;
import com.jiuyu.governance.business.rbac.pojo.request.EmployeeSendBindMobileCodeRequest;
import com.jiuyu.governance.business.rbac.pojo.request.EmployeeUpdateMobileRequest;
import com.jiuyu.governance.business.rbac.pojo.request.EmployeeUpdatePasswordRequest;
import com.jiuyu.governance.business.rbac.pojo.response.EmployeeInfoResponse;
import com.jiuyu.governance.business.rbac.pojo.response.EmployeeProfileResponse;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.business.room.service.LiveRoomService;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.openfeign.replay.SmsService;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import com.jiuyu.governance.plugins.oauth.data.supports.DataPermissionsHandler;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 企业端 - 员工个人档案API
 *
 * @author HeHui
 * @date 2026-03-28 15:51
 */
@RestController
@RequestMapping("/api/governance/employee-profile")
@RequiredArgsConstructor
@GovernanceUser
public class EmployeeProfileController {

    private final EmployeeService employeeService;

    private final LiveRoomService liveRoomService;

    private final SmsService smsService;

    private final DataPermissionsHandler permissionsHandler;

    /**
     * 个人资料
     *
     * @param loadRoom   是否查询所属直播间信息
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link EmployeeProfileResponse }>
     */
    @Permissions("my:profile:page")
    @GetMapping("/info")
    public ApiResponse<EmployeeProfileResponse> info(@RequestParam(required = false, defaultValue = "false") Boolean loadRoom, AccessUser accessUser) {
        EmployeeInfoResponse detail = employeeService.detail(accessUser.userId(), accessUser.currentTenantId());
        if (detail == null) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "未查询到您的资料");
        }
        EmployeeProfileResponse profileResponse = new EmployeeProfileResponse(detail);
        if (Boolean.TRUE.equals(loadRoom) && profileResponse.getPositionId() != null && profileResponse.getPositionId() > 0) {
            profileResponse.setRoomInfos(liveRoomService.getEmployeeJoinRooms(accessUser.userId(), profileResponse.getPositionId(), LocalDate.now().minusMonths(1)));
        }
        return ApiResponse.success(profileResponse);
    }

    /**
     * 修改个人资料
     *
     * @param request    请求参数
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Permissions("my:profile:page")
    @PostMapping("/info")
    public ApiResponse<Void> updateProfile(@RequestBody @Validated EmployeeProfileUpdateRequest request, AccessUser accessUser) {
        return employeeService.updateProfile(request, accessUser.userId(), accessUser.currentTenantId());
    }


    /**
     * 发送密码重置验证码
     *
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @PostMapping("/send-password-code")
    public ApiResponse<Void> sendPasswordCode(AccessUser accessUser) {
        Optional<String> employeeInfo = employeeService.getEmployeeMobile(accessUser.userId(), accessUser.currentTenantId());
        if (employeeInfo.isEmpty()) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "未查询到您的资料");
        }
        return smsService.sendCode("reset-password", employeeInfo.get(), () -> {
            return RandomUtil.randomNumbers(6);
        });
    }

    /**
     * 修改密码
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @ResourceLock(prefix = "profile-update", key = "#accessUser.userId")
    @PostMapping("/update-password")
    public ApiResponse<Void> updatePassword(@RequestBody @Validated EmployeeUpdatePasswordRequest request, AccessUser accessUser) {
        Optional<String> employeeInfo = employeeService.getEmployeeMobile(accessUser.userId(), accessUser.currentTenantId());
        if (employeeInfo.isEmpty()) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "未查询到您的资料");
        }
        return smsService.checkCode("reset-password", employeeInfo.get(), request.getCode(), () -> {
            return employeeService.updatePassword(accessUser.userId(), request.getNewPassword(), accessUser.currentTenantId(), true);
        }, errorNum -> {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "验证码错误");
        });
    }


    /**
     * 发送绑定手机号码验证码
     *
     * @param codeRequest 请求参数
     * @param accessUser  访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @PostMapping("/send-bind-code")
    public ApiResponse<Void> sendBindCode(@RequestBody @Validated EmployeeSendBindMobileCodeRequest codeRequest, AccessUser accessUser) {
        if (!PhoneUtil.isMobile(codeRequest.getNewMobile())) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "手机号码格式错误");
        }
        return smsService.sendCode("bind-mobile", codeRequest.getNewMobile(), () -> {
            return RandomUtil.randomNumbers(6);
        });
    }

    /**
     * 更改手机号码
     *
     * @param request    请求参数
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @ResourceLock(prefix = "profile-update", key = "#accessUser.userId")
    @PostMapping("/update-mobile")
    public ApiResponse<Void> updateMobile(@RequestBody @Validated EmployeeUpdateMobileRequest request, AccessUser accessUser) {
        if (!PhoneUtil.isMobile(request.getNewMobile())) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "手机号码格式错误");
        }
        return smsService.checkCode("bind-mobile", request.getNewMobile(), request.getCode(), () -> {
            return employeeService.updateMobile(accessUser.userId(), request.getNewMobile(), accessUser.currentTenantId());
        }, errorNum -> {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "验证码错误");
        });
    }


    /**
     * 员工资料
     * @param employeeId 员工ID
     * @param loadRoom   是否查询所属直播间信息
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link EmployeeProfileResponse }>
     */
    @GetMapping("/base")
    public ApiResponse<EmployeeProfileResponse> baseInfo(@RequestParam Long employeeId, @RequestParam(required = false, defaultValue = "false") Boolean loadRoom, AccessUser accessUser) {
        if (accessUser.userId().equals(employeeId)) {
            return info(loadRoom, accessUser);
        }
        return permissionsHandler.run(accessUser, OauthConstant.EMPLOYEE, employeeId, () -> {
            EmployeeInfoResponse detail = employeeService.detail(employeeId, accessUser.currentTenantId());
            if (detail == null) {
                return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "未查询到您的资料");
            }
            EmployeeProfileResponse profileResponse = new EmployeeProfileResponse(detail);
            if (Boolean.TRUE.equals(loadRoom) && profileResponse.getPositionId() != null && profileResponse.getPositionId() > 0) {
                profileResponse.setRoomInfos(liveRoomService.getEmployeeJoinRooms(employeeId, profileResponse.getPositionId(), LocalDate.now().minusMonths(1)));
            }
            return ApiResponse.success(profileResponse);
        });
    }
}
